
package com.seibel.lod.common.wrappers.worldGeneration;

import com.seibel.lod.core.api.ClientApi;

import java.util.Objects;

import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ChunkTickList;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.TickList;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoTickList;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import org.apache.logging.log4j.Logger;

public class ChunkLoader {

	private static final Logger LOGGER = ClientApi.LOGGER;

	private static LevelChunkSection[] readSections(WorldGenLevel level, LevelLightEngine lightEngine,
			ChunkPos chunkPos, CompoundTag tagLevel) {
		boolean isLightOn = tagLevel.getBoolean("isLightOn");
		ListTag listTag = tagLevel.getList("Sections", 10);
		LevelChunkSection[] levelChunkSections = new LevelChunkSection[16];
		boolean bl2 = level.getLevel().dimensionType().hasSkyLight();
		if (isLightOn)
			lightEngine.retainData(chunkPos, true);
		for (int j = 0; j < listTag.size(); j++) {
			CompoundTag compoundTag3 = listTag.getCompound(j);
			int k = compoundTag3.getByte("Y");
			if (compoundTag3.contains("Palette", 9) && compoundTag3.contains("BlockStates", 12)) {
				LevelChunkSection levelChunkSection = new LevelChunkSection(k << 4);
				levelChunkSection.getStates().read(compoundTag3.getList("Palette", 10),
						compoundTag3.getLongArray("BlockStates"));
				levelChunkSection.recalcBlockCounts();
				if (!levelChunkSection.isEmpty())
					levelChunkSections[k] = levelChunkSection;
			}
			if (isLightOn) {
				if (compoundTag3.contains("BlockLight", 7))
					lightEngine.queueSectionData(LightLayer.BLOCK, SectionPos.of(chunkPos, k),
							new DataLayer(compoundTag3.getByteArray("BlockLight")), true);
				if (bl2 && compoundTag3.contains("SkyLight", 7))
					lightEngine.queueSectionData(LightLayer.SKY, SectionPos.of(chunkPos, k),
							new DataLayer(compoundTag3.getByteArray("SkyLight")), true);
			}
		}
		return levelChunkSections;
	}

	private static void readHeightmaps(LevelChunk chunk, CompoundTag chunkData) {
		CompoundTag tagHeightmaps = chunkData.getCompound("Heightmaps");
		for (Heightmap.Types type : ChunkStatus.FULL.heightmapsAfter()) {
			String heightmap = type.getSerializationKey();
			if (tagHeightmaps.contains(heightmap, 12))
				chunk.setHeightmap(type, tagHeightmaps.getLongArray(heightmap));
		}
		Heightmap.primeHeightmaps(chunk, ChunkStatus.FULL.heightmapsAfter());
	}
	
	private static void readPostPocessings(LevelChunk chunk, CompoundTag chunkData) {
		ListTag tagPostProcessings = chunkData.getList("PostProcessing", 9);
		for (int n = 0; n < tagPostProcessings.size(); ++n) {
			ListTag listTag3 = tagPostProcessings.getList(n);
			for (int o = 0; o < listTag3.size(); ++o) {
				chunk.addPackedPostProcess(listTag3.getShort(o), n);
			}
		}
	}

	public static ChunkStatus.ChunkType readChunkType(CompoundTag compoundTag) {
		return ChunkStatus.byName(compoundTag.getString("Status")).getChunkType();
	}

	public static LevelChunk read(WorldGenLevel level, LevelLightEngine lightEngine,
			ChunkPos chunkPos, CompoundTag chunkData) {
		ChunkStatus.ChunkType chunkType = readChunkType(chunkData);
		if (chunkType == ChunkStatus.ChunkType.PROTOCHUNK)
			return null;
		
		ChunkGenerator chunkGenerator = level.getLevel().getChunkSource().getGenerator();
		CompoundTag tagLevel = chunkData.getCompound("Level");

		ChunkPos actualPos = new ChunkPos(tagLevel.getInt("xPos"), tagLevel.getInt("zPos"));
		if (!Objects.equals(chunkPos, actualPos)) {
			LOGGER.error("Distant Horizons: Chunk file at {} is in the wrong location; Ignoring. (Expected {}, got {})",
					(Object) chunkPos, (Object) chunkPos, (Object) actualPos);
			return null;
		}

		ChunkBiomeContainer chunkBiomeContainer = new ChunkBiomeContainer(
				level.getLevel().registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), chunkPos,
				chunkGenerator.getBiomeSource(),
				tagLevel.contains("Biomes", 11) ? tagLevel.getIntArray("Biomes") : null);


		// Read params for making the LevelChunk

		UpgradeData upgradeData = tagLevel.contains("UpgradeData", 10)
				? new UpgradeData(tagLevel.getCompound("UpgradeData"))
				: UpgradeData.EMPTY;
		TickList<Block> blockTicks = tagLevel.contains("TileTicks", 9)
				? ChunkTickList.create(tagLevel.getList("TileTicks", 10), Registry.BLOCK::getKey, Registry.BLOCK::get)
				: new ProtoTickList<Block>(block -> (block == null || block.defaultBlockState().isAir()), chunkPos,
						tagLevel.getList("ToBeTicked", 9));
		TickList<Fluid> liquidTicks = tagLevel.contains("LiquidTicks", 9)
				? ChunkTickList.create(tagLevel.getList("LiquidTicks", 10), Registry.FLUID::getKey, Registry.FLUID::get)
				: new ProtoTickList<Fluid>(fluid -> (fluid == null || fluid == Fluids.EMPTY), chunkPos,
						tagLevel.getList("LiquidsToBeTicked", 9));
		long inhabitedTime = tagLevel.getLong("InhabitedTime");
		LevelChunkSection[] levelChunkSections = readSections(level, lightEngine, chunkPos, tagLevel);

		LevelChunk chunk = new LevelChunk(level.getLevel(), chunkPos, chunkBiomeContainer, upgradeData, blockTicks,
				liquidTicks, inhabitedTime, levelChunkSections, null);

		chunk.setLightCorrect(tagLevel.getBoolean("isLightOn"));
		readHeightmaps(chunk, tagLevel);
		readPostPocessings(chunk, tagLevel);
		return chunk;
	}
}
