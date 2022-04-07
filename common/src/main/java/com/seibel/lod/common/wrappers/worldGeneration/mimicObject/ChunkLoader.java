/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
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
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
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
	#endif
	private static final String TAG_UPGRADE_DATA = "UpgradeData";
	private static final String BLOCK_TICKS_TAG_18 = "block_ticks";
	private static final String FLUID_TICKS_TAG_18 = "fluid_ticks";
	private static final String BLOCK_TICKS_TAG_PRE18 = "TileTicks";
	private static final String FLUID_TICKS_TAG_PRE18 = "LiquidTicks";
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
			#if MC_VERSION_1_18_1
			Codec<PalettedContainer<Biome>> biomeCodec = PalettedContainer.codec(
					biomes, biomes.byNameCodec(), PalettedContainer.Strategy.SECTION_BIOMES, biomes.getOrThrow(Biomes.PLAINS));
			#elif MC_VERSION_1_18_2
			Codec<PalettedContainer<Holder<Biome>>> biomeCodec = PalettedContainer.codec(
					biomes.asHolderIdMap(), biomes.holderByNameCodec(), PalettedContainer.Strategy.SECTION_BIOMES, biomes.getHolderOrThrow(Biomes.PLAINS));
			#endif
		#endif
		int i = #if MC_VERSION_1_16_5 16; #else level.getSectionsCount(); #endif
		LevelChunkSection[] chunkSections = new LevelChunkSection[i];

		boolean isLightOn = chunkData.getBoolean("isLightOn");
		boolean hasSkyLight = level.dimensionType().hasSkyLight();
		ListTag tagSections = chunkData.getList("Sections", 10);
		if (tagSections.isEmpty()) tagSections = chunkData.getList("sections", 10);

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
			#elif MC_VERSION_1_16_5 || MC_VERSION_1_17_1
			if (tagSection.contains("Palette", 9) && tagSection.contains("BlockStates", 12)) {
				LevelChunkSection levelChunkSection = new LevelChunkSection(sectionYPos << 4);
				levelChunkSection.getStates().read(tagSection.getList("Palette", 10),
						tagSection.getLongArray("BlockStates"));
				levelChunkSection.recalcBlockCounts();
				if (!levelChunkSection.isEmpty())
					chunkSections[#if MC_VERSION_1_16_5 sectionYPos #else level.getSectionIndexFromSectionY(sectionYPos) #endif]
							 = levelChunkSection;
			}
			#endif
			if (!isLightOn) continue;
			if (tagSection.contains("BlockLight", 7))
				lightEngine.queueSectionData(LightLayer.BLOCK, SectionPos.of(chunkPos, sectionYPos),
						new DataLayer(tagSection.getByteArray("BlockLight")), true);
			if (hasSkyLight && tagSection.contains("SkyLight", 7))
				lightEngine.queueSectionData(LightLayer.SKY, SectionPos.of(chunkPos, sectionYPos),
						new DataLayer(tagSection.getByteArray("SkyLight")), true);
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
	
	public static ChunkStatus.ChunkType readChunkType(CompoundTag tagLevel)
	{
		ChunkStatus chunkStatus = ChunkStatus.byName(tagLevel.getString("Status"));
		if (chunkStatus != null) {
			return chunkStatus.getChunkType();
		}
		return ChunkStatus.ChunkType.PROTOCHUNK;
	}
	
	public static LevelChunk read(WorldGenLevel level, LevelLightEngine lightEngine, ChunkPos chunkPos, CompoundTag chunkData)
	{
		#if MC_VERSION_1_16_5 || MC_VERSION_1_17_1
		CompoundTag tagLevel = chunkData.getCompound("Level");
		#else
		CompoundTag tagLevel = chunkData;
		#endif

		ChunkPos actualPos = new ChunkPos(tagLevel.getInt("xPos"), tagLevel.getInt("zPos"));
		if (!Objects.equals(chunkPos, actualPos)) {
			LOGGER.error("Chunk file at {} is in the wrong location; Ignoring. (Expected {}, got {})", chunkPos, chunkPos, actualPos);
			return null;
		}

		ChunkStatus.ChunkType chunkType = readChunkType(tagLevel);
		#if MC_VERSION_1_18_1 || MC_VERSION_1_18_2
		BlendingData blendingData = readBlendingData(tagLevel);
		if (chunkType == ChunkStatus.ChunkType.PROTOCHUNK && (blendingData == null || !blendingData.oldNoise()))
			return null;
		#else
		if (chunkType != ChunkStatus.ChunkType.LEVELCHUNK)
			return null;
		#endif

		long inhabitedTime = tagLevel.getLong("InhabitedTime");

		//================== Read params for making the LevelChunk ==================
		UpgradeData upgradeData = tagLevel.contains(TAG_UPGRADE_DATA, 10)
				? new UpgradeData(tagLevel.getCompound(TAG_UPGRADE_DATA)#if !MC_VERSION_1_16_5, level #endif)
				: UpgradeData.EMPTY;

		boolean isLightOn = tagLevel.getBoolean("isLightOn");
		if (isLightOn) lightEngine.retainData(chunkPos, true);
		#if MC_VERSION_1_18_1 || MC_VERSION_1_18_2

		LevelChunkTicks<Block> blockTicks = LevelChunkTicks.load(tagLevel.getList(BLOCK_TICKS_TAG_18, 10),
				string -> Registry.BLOCK.getOptional(ResourceLocation.tryParse(string)), chunkPos);
		LevelChunkTicks<Fluid> fluidTicks = LevelChunkTicks.load(tagLevel.getList(FLUID_TICKS_TAG_18, 10),
				string -> Registry.FLUID.getOptional(ResourceLocation.tryParse(string)), chunkPos);

		#elif MC_VERSION_1_16_5 || MC_VERSION_1_17_1
		ChunkBiomeContainer chunkBiomeContainer = new ChunkBiomeContainer(
				level.getLevel().registryAccess().registryOrThrow(Registry.BIOME_REGISTRY)#if !MC_VERSION_1_16_5, level #endif,
				chunkPos, level.getLevel().getChunkSource().getGenerator().getBiomeSource(),
				tagLevel.contains("Biomes", 11) ? tagLevel.getIntArray("Biomes") : null);

		TickList<Block> blockTicks = tagLevel.contains(BLOCK_TICKS_TAG_PRE18, 9)
				? ChunkTickList.create(tagLevel.getList(BLOCK_TICKS_TAG_PRE18, 10), Registry.BLOCK::getKey, Registry.BLOCK::get)
				: new ProtoTickList<Block>(block -> (block == null || block.defaultBlockState().isAir()), chunkPos,
				tagLevel.getList("ToBeTicked", 9)#if !MC_VERSION_1_16_5, level #endif);

		TickList<Fluid> fluidTicks = tagLevel.contains(FLUID_TICKS_TAG_PRE18, 9)
				? ChunkTickList.create(tagLevel.getList(FLUID_TICKS_TAG_PRE18, 10), Registry.FLUID::getKey, Registry.FLUID::get)
				: new ProtoTickList<Fluid>(fluid -> (fluid == null || fluid == Fluids.EMPTY), chunkPos,
				tagLevel.getList("LiquidsToBeTicked", 9)#if !MC_VERSION_1_16_5, level #endif);
		#endif

		LevelChunkSection[] levelChunkSections = readSections(level, lightEngine, chunkPos, tagLevel);

		// ======================== Make the chunk
		// ===========================================
		#if MC_VERSION_1_18_1 || MC_VERSION_1_18_2
		LevelChunk chunk = new LevelChunk((Level) level, chunkPos, upgradeData, blockTicks,
		 		fluidTicks, inhabitedTime, levelChunkSections, null, blendingData);
		#else
		LevelChunk chunk = new LevelChunk((Level) level.getLevel(), chunkPos, chunkBiomeContainer, upgradeData, blockTicks,
				fluidTicks, inhabitedTime, levelChunkSections, null);
		#endif

		// Set some states after object creation
		chunk.setLightCorrect(isLightOn);
		readHeightmaps(chunk, chunkData);
		readPostPocessings(chunk, chunkData);
		return chunk;
	}
	
	private static void logErrors(ChunkPos chunkPos, int i, String string)
	{
		LOGGER.error("Distant Horizons: Recoverable errors when loading section [" + chunkPos.x + ", " + i + ", " + chunkPos.z + "]: " + string);
	}
}

