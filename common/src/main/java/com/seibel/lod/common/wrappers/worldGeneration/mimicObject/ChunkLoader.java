
package com.seibel.lod.common.wrappers.worldGeneration.mimicObject;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.seibel.lod.core.api.ClientApi;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelChunkTicks;
import org.apache.logging.log4j.Logger;

public class ChunkLoader
{
	private static final Codec<PalettedContainer<BlockState>> BLOCK_STATE_CODEC = PalettedContainer.codec(Block.BLOCK_STATE_REGISTRY, BlockState.CODEC, PalettedContainer.Strategy.SECTION_STATES, Blocks.AIR.defaultBlockState());
	private static final Logger LOGGER = ClientApi.LOGGER;
	private static final String TAG_UPGRADE_DATA = "UpgradeData";
	private static final String BLOCK_TICKS_TAG = "block_ticks";
	private static final String FLUID_TICKS_TAG = "fluid_ticks";
	
	private static BlendingData readBlendingData(CompoundTag chunkData)
	{
		BlendingData blendingData = null;
		if (chunkData.contains("blending_data", 10))
		{
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Dynamic<CompoundTag> blendingDataTag = new Dynamic(NbtOps.INSTANCE, chunkData.getCompound("blending_data"));
			blendingData = BlendingData.CODEC.parse(blendingDataTag).resultOrPartial(LOGGER::error).orElse(null);
		}
		return blendingData;
	}
	
	private static LevelChunkSection[] readSections(LevelAccessor level, LevelLightEngine lightEngine, ChunkPos chunkPos, CompoundTag chunkData)
	{
		Registry<Biome> biomes = level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
		Codec<PalettedContainer<Biome>> biomeCodec = PalettedContainer.codec(
				biomes, biomes.byNameCodec(), PalettedContainer.Strategy.SECTION_BIOMES, biomes.getOrThrow(Biomes.PLAINS));
		
		int i = level.getSectionsCount();
		LevelChunkSection[] chunkSections = new LevelChunkSection[i];
		
		boolean isLightOn = chunkData.getBoolean("isLightOn");
		boolean hasSkyLight = level.dimensionType().hasSkyLight();
		ListTag tagSections = chunkData.getList("sections", 10);
		
		for (int j = 0; j < tagSections.size(); ++j)
		{
			CompoundTag tagSection = tagSections.getCompound(j);
			byte sectionYPos = tagSection.getByte("Y");
			int sectionId = level.getSectionIndexFromSectionY(sectionYPos);
			if (sectionId >= 0 && sectionId < chunkSections.length)
			{
				PalettedContainer<BlockState> blockStateContainer;
				PalettedContainer<Biome> biomeContainer;
				
				blockStateContainer = tagSection.contains("block_states", 10)
						? BLOCK_STATE_CODEC.parse(NbtOps.INSTANCE, tagSection.getCompound("block_states")).promotePartial(string -> logErrors(chunkPos, sectionYPos, string)).getOrThrow(false, LOGGER::error)
						: new PalettedContainer<BlockState>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES);
				
				biomeContainer = tagSection.contains("biomes", 10)
						? biomeCodec.parse(NbtOps.INSTANCE, tagSection.getCompound("biomes")).promotePartial(string -> logErrors(chunkPos, sectionYPos, string)).getOrThrow(false, LOGGER::error)
						: new PalettedContainer<Biome>(biomes, biomes.getOrThrow(Biomes.PLAINS), PalettedContainer.Strategy.SECTION_BIOMES);
				
				chunkSections[sectionId] = new LevelChunkSection(sectionYPos, blockStateContainer, biomeContainer);
			}
			
			if (!isLightOn)
				continue;
			
			if (tagSection.contains("BlockLight", 7))
				lightEngine.queueSectionData(LightLayer.BLOCK, SectionPos.of(chunkPos, sectionYPos), new DataLayer(tagSection.getByteArray("BlockLight")), true);
			if (hasSkyLight && tagSection.contains("SkyLight", 7))
				lightEngine.queueSectionData(LightLayer.SKY, SectionPos.of(chunkPos, sectionYPos), new DataLayer(tagSection.getByteArray("SkyLight")), true);
		}
		return chunkSections;
	}
	
	private static void readHeightmaps(LevelChunk chunk, CompoundTag chunkData)
	{
		CompoundTag tagHeightmaps = chunkData.getCompound("Heightmaps");
		for (Heightmap.Types type : ChunkStatus.FULL.heightmapsAfter())
		{
			String heightmap = type.getSerializationKey();
			if (tagHeightmaps.contains(heightmap, 12))
				chunk.setHeightmap(type, tagHeightmaps.getLongArray(heightmap));
		}
		Heightmap.primeHeightmaps(chunk, ChunkStatus.FULL.heightmapsAfter());
	}
	
	private static Map<StructureFeature<?>, StructureStart<?>> unpackStructureStart(StructurePieceSerializationContext structurePieceSerializationContext, CompoundTag compoundTag, long l)
	{
		HashMap<StructureFeature<?>, StructureStart<?>> map = Maps.newHashMap();
		CompoundTag compoundTag2 = compoundTag.getCompound("starts");
		for (String string : compoundTag2.getAllKeys())
		{
			String string2 = string.toLowerCase(Locale.ROOT);
			StructureFeature<?> structureFeature = StructureFeature.STRUCTURES_REGISTRY.get(string2);
			if (structureFeature == null)
			{
				LOGGER.error("Unknown structure start: {}", (Object) string2);
				continue;
			}
			StructureStart<?> structureStart = StructureFeature.loadStaticStart(structurePieceSerializationContext, compoundTag2.getCompound(string), l);
			if (structureStart == null)
				continue;
			map.put(structureFeature, structureStart);
		}
		return map;
	}
	
	private static Map<StructureFeature<?>, LongSet> unpackStructureReferences(ChunkPos chunkPos, CompoundTag compoundTag)
	{
		HashMap<StructureFeature<?>, LongSet> map = Maps.newHashMap();
		CompoundTag compoundTag2 = compoundTag.getCompound("References");
		for (String string : compoundTag2.getAllKeys())
		{
			String string2 = string.toLowerCase(Locale.ROOT);
			StructureFeature<?> structureFeature = StructureFeature.STRUCTURES_REGISTRY.get(string2);
			if (structureFeature == null)
			{
				LOGGER.warn("Found reference to unknown structure '{}' in chunk {}, discarding", (Object) string2, (Object) chunkPos);
				continue;
			}
			map.put(structureFeature, new LongOpenHashSet(Arrays.stream(compoundTag2.getLongArray(string)).filter(l ->
			{
				ChunkPos chunkPos2 = new ChunkPos(l);
				if (chunkPos2.getChessboardDistance(chunkPos) > 8)
				{
					LOGGER.warn("Found invalid structure reference [ {} @ {} ] for chunk {}.", (Object) string2, (Object) chunkPos2, (Object) chunkPos);
					return false;
				}
				return true;
			}).toArray()));
		}
		return map;
	}
	
	private static void readStructures(WorldGenLevel level, LevelChunk chunk, CompoundTag chunkData)
	{
		CompoundTag tagStructures = chunkData.getCompound("structures");
		chunk.setAllStarts(
				unpackStructureStart(StructurePieceSerializationContext.fromLevel(level.getLevel()), tagStructures, level.getSeed()));
		chunk.setAllReferences(unpackStructureReferences(chunk.getPos(), tagStructures));
	}
	
	private static void readPostPocessings(LevelChunk chunk, CompoundTag chunkData)
	{
		ListTag tagPostProcessings = chunkData.getList("PostProcessing", 9);
		for (int n = 0; n < tagPostProcessings.size(); ++n)
		{
			ListTag listTag3 = tagPostProcessings.getList(n);
			for (int o = 0; o < listTag3.size(); ++o)
			{
				chunk.addPackedPostProcess(listTag3.getShort(o), n);
			}
		}
	}
	
	public static ChunkStatus.ChunkType readChunkType(CompoundTag compoundTag)
	{
		return ChunkStatus.byName(compoundTag.getString("Status")).getChunkType();
	}
	
	public static LevelChunk read(WorldGenLevel level, LevelLightEngine lightEngine, ChunkPos chunkPos, CompoundTag chunkData)
	{
		
		ChunkPos actualPos = new ChunkPos(chunkData.getInt("xPos"), chunkData.getInt("zPos"));
		if (!Objects.equals(chunkPos, actualPos))
		{
			LOGGER.error("Distant Horizons: Chunk file at {} is in the wrong location; Ignoring. (Expected {}, got {})", (Object) chunkPos, (Object) chunkPos, (Object) actualPos);
			return null;
		}
		
		ChunkStatus.ChunkType chunkType = readChunkType(chunkData);
		BlendingData blendingData = readBlendingData(chunkData);
		if (chunkType == ChunkStatus.ChunkType.PROTOCHUNK && (blendingData == null || !blendingData.oldNoise()))
			return null;
		
		// Prepare the light engine
		boolean isLightOn = chunkData.getBoolean("isLightOn");
		if (isLightOn)
			level.getLightEngine().retainData(chunkPos, true);
		
		// Read params for making the LevelChunk
		UpgradeData upgradeData = chunkData.contains(TAG_UPGRADE_DATA, 10)
				? new UpgradeData(chunkData.getCompound(TAG_UPGRADE_DATA), level)
				: UpgradeData.EMPTY;
		LevelChunkTicks<Block> blockTicks = LevelChunkTicks.load(chunkData.getList(BLOCK_TICKS_TAG, 10),
				string -> Registry.BLOCK.getOptional(ResourceLocation.tryParse(string)), chunkPos);
		LevelChunkTicks<Fluid> fluidTicks = LevelChunkTicks.load(chunkData.getList(FLUID_TICKS_TAG, 10),
				string -> Registry.FLUID.getOptional(ResourceLocation.tryParse(string)), chunkPos);
		long inhabitedTime = chunkData.getLong("InhabitedTime");
		LevelChunkSection[] chunkSections = readSections(level, lightEngine, actualPos, chunkData);
		
		// Make chunk
		LevelChunk chunk = new LevelChunk((Level) level, chunkPos, upgradeData, blockTicks, fluidTicks, inhabitedTime, chunkSections, null, blendingData);
		
		// Set some states after object creation
		chunk.setLightCorrect(isLightOn);
		readHeightmaps(chunk, chunkData);
		readStructures(level, chunk, chunkData);
		readPostPocessings(chunk, chunkData);
		return chunk;
	}
	
	private static void logErrors(ChunkPos chunkPos, int i, String string)
	{
		LOGGER.error("Distant Horizons: Recoverable errors when loading section [" + chunkPos.x + ", " + i + ", " + chunkPos.z + "]: " + string);
	}
}

