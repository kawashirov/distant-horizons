package com.seibel.lod.common.wrappers.worldGeneration.mimicObject;

import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public class WorldGenStructFeatManager extends StructureFeatureManager {
	WorldGenLevel genLevel;
	WorldGenSettings worldGenSettings;
	StructureCheck structureCheck;
	public WorldGenStructFeatManager(LevelAccessor levelAccessor, WorldGenSettings worldGenSettings,
			WorldGenLevel genLevel, StructureCheck structureCheck) {
		super(levelAccessor, worldGenSettings, structureCheck);
		this.genLevel = genLevel;
		this.worldGenSettings = worldGenSettings;
	}
	
	public void setGenLevel(WorldGenLevel genLevel) {
		this.genLevel = genLevel;
	}

	@Override
	public WorldGenStructFeatManager forWorldGenRegion(WorldGenRegion worldGenRegion) {
		if (worldGenRegion == genLevel)
			return this;
		return new WorldGenStructFeatManager(worldGenRegion, worldGenSettings, worldGenRegion, structureCheck);
	}

	@Override
    public boolean hasAnyStructureAt(BlockPos blockPos) {
        SectionPos sectionPos = SectionPos.of(blockPos);
		ChunkAccess chunk = genLevel.getChunk(sectionPos.x(), sectionPos.z(), ChunkStatus.STRUCTURE_REFERENCES,
				false);
		if (chunk == null) return false;
        return chunk.hasAnyStructureReferences();
    }

	// TODO Check this
	/*
	@Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public List<? extends StructureStart<?>> startsForFeature(SectionPos sectionPos,
			StructureFeature<?> structureFeature) {
		if (genLevel == null)
			return List.of();
		ChunkAccess chunk = genLevel.getChunk(sectionPos.x(), sectionPos.z(), ChunkStatus.STRUCTURE_REFERENCES,
				false);
		if (chunk == null)
			return List.of();
		LongSet longSet = chunk.getReferencesForFeature(structureFeature);
			ImmutableList.Builder builder = ImmutableList.builder();
	        LongIterator longIterator = longSet.iterator();
	        while (longIterator.hasNext()) {
	            long l = (Long)longIterator.next();
	            SectionPos sectPos = SectionPos.of(new ChunkPos(l), genLevel.getMinSection());
	            ChunkAccess startChunk = genLevel.getChunk(sectPos.x(), sectPos.z(), ChunkStatus.STRUCTURE_STARTS, false);
	            if (startChunk == null) continue;
	            StructureStart<?> structureStart = this.getStartForFeature(sectPos, structureFeature, startChunk);
	            if (structureStart == null || !structureStart.isValid()) continue;
	            builder.add(structureStart);
	        }
	        return builder.build();
	}
	 */
}