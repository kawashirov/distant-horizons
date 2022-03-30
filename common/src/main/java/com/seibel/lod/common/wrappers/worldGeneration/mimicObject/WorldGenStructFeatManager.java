package com.seibel.lod.common.wrappers.worldGeneration.mimicObject;

import java.util.stream.Stream;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public class WorldGenStructFeatManager extends StructureFeatureManager {
	final WorldGenLevel genLevel;
	WorldGenSettings worldGenSettings;
	public WorldGenStructFeatManager(WorldGenSettings worldGenSettings,
			WorldGenLevel genLevel) {
		super(genLevel, worldGenSettings);
		this.genLevel = genLevel;
		this.worldGenSettings = worldGenSettings;
	}

	@Override
	public WorldGenStructFeatManager forWorldGenRegion(WorldGenRegion worldGenRegion) {
		if (worldGenRegion == genLevel)
			return this;
		return new WorldGenStructFeatManager(worldGenSettings, worldGenRegion);
	}

	private ChunkAccess _getChunk(int x, int z, ChunkStatus status) {
		if (genLevel == null) return null;
		return genLevel.getChunk(x, z, status, false);
	}

	@Override
	public Stream<? extends StructureStart<?>> startsForFeature(SectionPos sectionPos2,
			StructureFeature<?> structureFeature) {
		if (genLevel == null)
			return Stream.empty();
		ChunkAccess chunk = genLevel.getChunk(sectionPos2.x(), sectionPos2.z(), ChunkStatus.STRUCTURE_REFERENCES,
				false);
		if (chunk == null)
			return Stream.empty();
		return chunk.getReferencesForFeature(structureFeature).stream().map(pos -> {
			SectionPos sectPos = SectionPos.of(ChunkPos.getX(pos), 0, ChunkPos.getZ(pos));
			ChunkAccess startChunk = genLevel.getChunk(sectPos.x(), sectPos.z(), ChunkStatus.STRUCTURE_STARTS, false);
			if (startChunk == null)
				return null;
			return this.getStartForFeature(sectPos, structureFeature, startChunk);
		}).filter(structureStart -> structureStart != null && structureStart.isValid());
	}
}