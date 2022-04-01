
package com.seibel.lod.common.wrappers.worldGeneration.mimicObject;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.seibel.lod.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.lod.core.api.ApiShared;

import com.seibel.lod.core.logging.ConfigBasedLogger;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

#if MC_VERSION_1_18_2
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
#endif

import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.Heightmap;
#if MC_VERSION_1_18_2 || MC_VERSION_1_18_1
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.ticks.LevelChunkTicks;
#endif
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.apache.logging.log4j.Logger;

public class ChunkLoader
{
	#if MC_VERSION_1_18_2 || MC_VERSION_1_18_1
	private static final Codec<PalettedContainer<BlockState>> BLOCK_STATE_CODEC = PalettedContainer.codec(Block.BLOCK_STATE_REGISTRY, BlockState.CODEC, PalettedContainer.Strategy.SECTION_STATES, Blocks.AIR.defaultBlockState());
	private static final String TAG_UPGRADE_DATA = "UpgradeData";
	private static final String BLOCK_TICKS_TAG = "block_ticks";
	private static final String FLUID_TICKS_TAG = "fluid_ticks";
	#endif
	private static final ConfigBasedLogger LOGGER = BatchGenerationEnvironment.LOAD_LOGGER;

	#if MC_VERSION_1_18_2 || MC_VERSION_1_18_1
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
	#endif
	
	private static LevelChunkSection[] readSections(LevelAccessor level, LevelLightEngine lightEngine, ChunkPos chunkPos, CompoundTag chunkData)
	{
		#if MC_VERSION_1_18_2 || MC_VERSION_1_18_1
		Registry<Biome> biomes = level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
		#endif
		#if MC_VERSION_1_18_1
		Codec<PalettedContainer<Biome>> biomeCodec = PalettedContainer.codec(
				biomes, biomes.byNameCodec(), PalettedContainer.Strategy.SECTION_BIOMES, biomes.getOrThrow(Biomes.PLAINS));
		#elif MC_VERSION_1_18_2
		Codec<PalettedContainer<Holder<Biome>>> biomeCodec = PalettedContainer.codec(
				biomes.asHolderIdMap(), biomes.holderByNameCodec(), PalettedContainer.Strategy.SECTION_BIOMES, biomes.getHolderOrThrow(Biomes.PLAINS));
		#endif

		int i = level.getSectionsCount();
		LevelChunkSection[] chunkSections = new LevelChunkSection[i];

		boolean isLightOn = chunkData.getBoolean("isLightOn");
		boolean hasSkyLight = level.dimensionType().hasSkyLight();
		ListTag tagSections = chunkData.getList("sections", 10);

		#if MC_VERSION_1_17_1
		boolean bl2 = level.dimensionType().hasSkyLight();
		if (isLightOn)
			lightEngine.retainData(chunkPos, true);
		#endif
		for (int j = 0; j < tagSections.size(); ++j)
		{
			CompoundTag tagSection = tagSections.getCompound(j);
			int sectionYPos = tagSection.getByte("Y");

			#if MC_VERSION_1_18_2 || MC_VERSION_1_18_1
			int sectionId = level.getSectionIndexFromSectionY(sectionYPos);
			if (sectionId >= 0 && sectionId < chunkSections.length)
			{
				PalettedContainer<BlockState> blockStateContainer;
				#if MC_VERSION_1_18_1
				PalettedContainer<Biome> biomeContainer;
				#elif MC_VERSION_1_18_2
				PalettedContainer<Holder<Biome>> biomeContainer;
				#endif

				blockStateContainer = tagSection.contains("block_states", 10)
						? BLOCK_STATE_CODEC.parse(NbtOps.INSTANCE, tagSection.getCompound("block_states")).promotePartial(string -> logErrors(chunkPos, sectionYPos, string)).getOrThrow(false, LOGGER::error)
						: new PalettedContainer<BlockState>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES);

				#if MC_VERSION_1_18_1
				biomeContainer = tagSection.contains("biomes", 10)
						? biomeCodec.parse(NbtOps.INSTANCE, tagSection.getCompound("biomes")).promotePartial(string -> logErrors(chunkPos, sectionYPos, string)).getOrThrow(false, LOGGER::error)
						: new PalettedContainer<Biome>(biomes, biomes.getOrThrow(Biomes.PLAINS), PalettedContainer.Strategy.SECTION_BIOMES);
				#elif MC_VERSION_1_18_2
				biomeContainer = tagSection.contains("biomes", 10)
						? biomeCodec.parse(NbtOps.INSTANCE, tagSection.getCompound("biomes")).promotePartial(string -> logErrors(chunkPos, i, (String) string)).getOrThrow(false, LOGGER::error)
						: new PalettedContainer<Holder<Biome>>(biomes.asHolderIdMap(), biomes.getHolderOrThrow(Biomes.PLAINS), PalettedContainer.Strategy.SECTION_BIOMES);
				#endif


				chunkSections[sectionId] = new LevelChunkSection(sectionYPos, blockStateContainer, biomeContainer);
			}

			if (!isLightOn)
				continue;

			if (tagSection.contains("BlockLight", 7))
				lightEngine.queueSectionData(LightLayer.BLOCK, SectionPos.of(chunkPos, sectionYPos), new DataLayer(tagSection.getByteArray("BlockLight")), true);
			if (hasSkyLight && tagSection.contains("SkyLight", 7))
				lightEngine.queueSectionData(LightLayer.SKY, SectionPos.of(chunkPos, sectionYPos), new DataLayer(tagSection.getByteArray("SkyLight")), true);
			#elif MC_VERSION_1_17_1
			if (tagSection.contains("Palette", 9) && tagSection.contains("BlockStates", 12)) {
				LevelChunkSection levelChunkSection = new LevelChunkSection(sectionYPos << 4);
				levelChunkSection.getStates().read(tagSection.getList("Palette", 10),
						tagSection.getLongArray("BlockStates"));
				levelChunkSection.recalcBlockCounts();
				if (!levelChunkSection.isEmpty())
					chunkSections[level.getSectionIndexFromSectionY(sectionYPos)] = levelChunkSection;
			}
			if (isLightOn) {
				if (tagSection.contains("BlockLight", 7))
					lightEngine.queueSectionData(LightLayer.BLOCK, SectionPos.of(chunkPos, sectionYPos),
							new DataLayer(tagSection.getByteArray("BlockLight")), true);
				if (bl2 && tagSection.contains("SkyLight", 7))
					lightEngine.queueSectionData(LightLayer.SKY, SectionPos.of(chunkPos, sectionYPos),
							new DataLayer(tagSection.getByteArray("SkyLight")), true);
			}
			#endif
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

	#if MC_VERSION_1_18_1
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
	#elif MC_VERSION_1_18_2
	private static Map<ConfiguredStructureFeature<?, ?>, StructureStart> unpackStructureStart(StructurePieceSerializationContext structurePieceSerializationContext, CompoundTag compoundTag, long l) {
		Map<ConfiguredStructureFeature<?, ?>, StructureStart> map = Maps.newHashMap();
		Registry<ConfiguredStructureFeature<?, ?>> structStartRegistry = structurePieceSerializationContext.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
		CompoundTag compoundTag2 = compoundTag.getCompound("starts");
		for (String string : compoundTag2.getAllKeys()) {
			ResourceLocation resourceLocation = ResourceLocation.tryParse(string);
			ConfiguredStructureFeature<?, ?> structureFeature = structStartRegistry.get(resourceLocation);
//			String string2 = string.toLowerCase(Locale.ROOT);
//			ConfiguredStructureFeature<?, ?> structureFeature = StructureFeature.STRUCTURES_REGISTRY.get(string2);
			if (structureFeature == null) {
				LOGGER.error("Unknown structure start: {}", resourceLocation);
				continue;
			}
			StructureStart structureStart = StructureFeature.loadStaticStart(structurePieceSerializationContext, compoundTag2.getCompound(string), l);
			if (structureStart == null)
				continue;
			map.put(structureFeature, structureStart);
		}
		return map;
	}

	private static Map<ConfiguredStructureFeature<?, ?>, LongSet> unpackStructureReferences(RegistryAccess registryAccess, ChunkPos chunkPos, CompoundTag compoundTag)
	{
		Map<ConfiguredStructureFeature<?, ?>, LongSet> map = Maps.newHashMap();
		Registry<ConfiguredStructureFeature<?, ?>> structRegistry = registryAccess.registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
		CompoundTag compoundTag2 = compoundTag.getCompound("References");
		for (String string : compoundTag2.getAllKeys())
		{
		    ResourceLocation resourceLocation = ResourceLocation.tryParse(string);
			ConfiguredStructureFeature<?, ?> structureFeature = structRegistry.get(resourceLocation);
//			String string2 = string.toLowerCase(Locale.ROOT);
//			ConfiguredStructureFeature<?, ?> structureFeature = StructureFeature.STRUCTURES_REGISTRY.get(string2);
			if (structureFeature == null)
			{
				LOGGER.warn("Found reference to unknown structure '{}' in chunk {}, discarding", resourceLocation, chunkPos);
				continue;
			}
			map.put(structureFeature, new LongOpenHashSet(Arrays.stream(compoundTag2.getLongArray(string)).filter(l ->
			{
				ChunkPos chunkPos2 = new ChunkPos(l);
				if (chunkPos2.getChessboardDistance(chunkPos) > 8)
				{
					LOGGER.warn("Found invalid structure reference [ {} @ {} ] for chunk {}.", resourceLocation, chunkPos2, chunkPos);
					return false;
				}
				return true;
			}).toArray()));
		}
		return map;
	}
	#endif

	#if MC_VERSION_1_18_2 || MC_VERSION_1_18_1
	private static void readStructures(WorldGenLevel level, LevelChunk chunk, CompoundTag chunkData)
	{
		CompoundTag tagStructures = chunkData.getCompound("structures");
		chunk.setAllStarts(
				unpackStructureStart(StructurePieceSerializationContext.fromLevel(level.getLevel()), tagStructures, level.getSeed()));
		chunk.setAllReferences(unpackStructureReferences(#if MC_VERSION_1_18_2 level.registryAccess() ,#endif chunk.getPos(), tagStructures));
	}
	#endif

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

		// TODO Check if we should use the lines underneath to return or use the thing above
//		ChunkStatus chunkStatus = ChunkStatus.byName(tagLevel.getString("Status"));
//		if (chunkStatus != null) {
//			return chunkStatus.getChunkType();
//		}
//		return ChunkStatus.ChunkType.PROTOCHUNK;
	}
	
	public static LevelChunk read(WorldGenLevel level, LevelLightEngine lightEngine, ChunkPos chunkPos, CompoundTag chunkData)
	{
		#if MC_VERSION_1_17_1
		CompoundTag tagLevel = chunkData.getCompound("Level");

		ChunkStatus.ChunkType chunkType = readChunkType(tagLevel);
		if (chunkType != ChunkStatus.ChunkType.LEVELCHUNK)
			return null;
		#endif

		ChunkPos actualPos = new ChunkPos(chunkData.getInt("xPos"), chunkData.getInt("zPos"));
		if (!Objects.equals(chunkPos, actualPos))
		{
			LOGGER.error("Chunk file at {} is in the wrong location; Ignoring. (Expected {}, got {})", (Object) chunkPos, (Object) chunkPos, (Object) actualPos);
			return null;
		}

		#if MC_VERSION_1_18_1 || MC_VERSION_1_18_2
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
		#elif MC_VERSION_1_17_1
		// ====================== Read params for making the LevelChunk
		// ============================
		ChunkBiomeContainer chunkBiomeContainer = new ChunkBiomeContainer(
				level.getLevel().registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), level, chunkPos,
				level.getLevel().getChunkSource().getGenerator().getBiomeSource(),
				tagLevel.contains("Biomes", 11) ? tagLevel.getIntArray("Biomes") : null);

		UpgradeData upgradeData = tagLevel.contains("UpgradeData", 10)
				? new UpgradeData(tagLevel.getCompound("UpgradeData"), level)
				: UpgradeData.EMPTY;

		TickList<Block> blockTicks = tagLevel.contains("TileTicks", 9)
				? ChunkTickList.create(tagLevel.getList("TileTicks", 10), Registry.BLOCK::getKey, Registry.BLOCK::get)
				: new ProtoTickList<Block>(block -> (block == null || block.defaultBlockState().isAir()), chunkPos,
				tagLevel.getList("ToBeTicked", 9), level);

		TickList<Fluid> liquidTicks = tagLevel.contains("LiquidTicks", 9)
				? ChunkTickList.create(tagLevel.getList("LiquidTicks", 10), Registry.FLUID::getKey, Registry.FLUID::get)
				: new ProtoTickList<Fluid>(fluid -> (fluid == null || fluid == Fluids.EMPTY), chunkPos,
				tagLevel.getList("LiquidsToBeTicked", 9), level);

		long inhabitedTime = tagLevel.getLong("InhabitedTime");

		LevelChunkSection[] levelChunkSections = readSections(level, lightEngine, chunkPos, tagLevel);

		// ======================== Make the chunk
		// ===========================================
		LevelChunk chunk = new LevelChunk(level.getLevel(), chunkPos, chunkBiomeContainer, upgradeData, blockTicks,
				liquidTicks, inhabitedTime, levelChunkSections, null);
		#endif

		// Set some states after object creation
		#if MC_VERSION_1_18_1 || MC_VERSION_1_18_2
		readStructures(level, chunk, chunkData);
		chunk.setLightCorrect(isLightOn);
		#elif MC_VERSION_1_17_1
		chunk.setLightCorrect(tagLevel.getBoolean("isLightOn"));
		#endif
		readHeightmaps(chunk, chunkData);
		readPostPocessings(chunk, chunkData);
		return chunk;
	}
	
	private static void logErrors(ChunkPos chunkPos, int i, String string)
	{
		LOGGER.error("Distant Horizons: Recoverable errors when loading section [" + chunkPos.x + ", " + i + ", " + chunkPos.z + "]: " + string);
	}
}

