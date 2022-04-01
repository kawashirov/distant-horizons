package com.seibel.lod.common.wrappers.worldGeneration.mimicObject;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
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
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
#if MC_VERSION_1_18_2 || MC_VERSION_1_18_1
import net.minecraft.world.level.levelgen.structure.StructureCheck;
#endif
import net.minecraft.world.level.levelgen.structure.StructureStart;

public class WorldGenStructFeatManager extends StructureFeatureManager {
	final WorldGenLevel genLevel;
	WorldGenSettings worldGenSettings;
	#if MC_VERSION_1_18_2 || MC_VERSION_1_18_1
	StructureCheck structureCheck;
	public WorldGenStructFeatManager(WorldGenSettings worldGenSettings,
			WorldGenLevel genLevel, StructureCheck structureCheck) {

		super(genLevel, worldGenSettings, structureCheck);
		this.genLevel = genLevel;
		this.worldGenSettings = worldGenSettings;
	}
	#elif MC_VERSION_1_17_1
	public WorldGenStructFeatManager(WorldGenSettings worldGenSettings,
			WorldGenLevel genLevel) {

		super(genLevel, worldGenSettings);
		this.genLevel = genLevel;
		this.worldGenSettings = worldGenSettings;
	}
	#endif

	@Override
	public WorldGenStructFeatManager forWorldGenRegion(WorldGenRegion worldGenRegion) {
		if (worldGenRegion == genLevel)
			return this;
		#if MC_VERSION_1_18_2 || MC_VERSION_1_18_1
		return new WorldGenStructFeatManager(worldGenSettings, worldGenRegion, structureCheck);
		#elif MC_VERSION_1_17_1
		return new WorldGenStructFeatManager(worldGenSettings, worldGenRegion);
		#endif
	}

	private ChunkAccess _getChunk(int x, int z, ChunkStatus status) {
		if (genLevel == null) return null;
		return genLevel.getChunk(x, z, status, false);
	}

	#if MC_VERSION_1_18_2 || MC_VERSION_1_18_1
	@Override
    public boolean hasAnyStructureAt(BlockPos blockPos) {
        SectionPos sectionPos = SectionPos.of(blockPos);
		ChunkAccess chunk = _getChunk(sectionPos.x(), sectionPos.z(), ChunkStatus.STRUCTURE_REFERENCES);
		if (chunk == null) return false;
        return chunk.hasAnyStructureReferences();
    }
	#endif


	#if MC_VERSION_1_17_1
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
	#elif MC_VERSION_1_18_1
	@Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public List<? extends StructureStart<?>> startsForFeature(SectionPos sectionPos,
															  StructureFeature<?> structureFeature) {

		ChunkAccess chunk = _getChunk(sectionPos.x(), sectionPos.z(), ChunkStatus.STRUCTURE_REFERENCES);
		if (chunk == null) return List.of();

		// Copied from StructureFeatureManager::startsForFeature(...) with slight tweaks
		LongSet longSet = chunk.getReferencesForFeature(structureFeature);
			ImmutableList.Builder builder = ImmutableList.builder();
	        LongIterator longIterator = longSet.iterator();
	        while (longIterator.hasNext()) {
	            long l = (Long)longIterator.next();
	            SectionPos sectPos = SectionPos.of(new ChunkPos(l), genLevel.getMinSection());
	            ChunkAccess startChunk = _getChunk(sectPos.x(), sectPos.z(), ChunkStatus.STRUCTURE_STARTS);
	            if (startChunk == null) continue;
	            StructureStart<?> structureStart = this.getStartForFeature(sectPos, structureFeature, startChunk);
	            if (structureStart == null || !structureStart.isValid()) continue;
	            builder.add(structureStart);
	        }
	        return builder.build();
	}

	#elif MC_VERSION_1_18_2
	@Override
	public List<StructureStart> startsForFeature(SectionPos sectionPos, Predicate<ConfiguredStructureFeature<?, ?>> predicate) {
		ChunkAccess chunk = _getChunk(sectionPos.x(), sectionPos.z(), ChunkStatus.STRUCTURE_REFERENCES);
		if (chunk == null) return List.of();

		// Copied from StructureFeatureManager::startsForFeature(...)
		Map<ConfiguredStructureFeature<?, ?>, LongSet> map = chunk.getAllReferences();

		ImmutableList.Builder<StructureStart> builder = ImmutableList.builder();
		Iterator<Map.Entry<ConfiguredStructureFeature<?, ?>, LongSet>> var5 = map.entrySet().iterator();

		while(var5.hasNext()) {
			Map.Entry<ConfiguredStructureFeature<?, ?>, LongSet> entry = var5.next();
			ConfiguredStructureFeature<?, ?> configuredStructureFeature = entry.getKey();
			if (predicate.test(configuredStructureFeature)) {
				LongSet var10002 = (LongSet)entry.getValue();
				Objects.requireNonNull(builder);
				this.fillStartsForFeature(configuredStructureFeature, var10002, builder::add);
			}
		}

		return builder.build();
	}

	@Override
	public List<StructureStart> startsForFeature(SectionPos sectionPos, ConfiguredStructureFeature<?, ?> configuredStructureFeature) {
		ChunkAccess chunk = _getChunk(sectionPos.x(), sectionPos.z(), ChunkStatus.STRUCTURE_REFERENCES);
		if (chunk == null) return List.of();

		// Copied from StructureFeatureManager::startsForFeature(...)
		LongSet longSet = chunk.getReferencesForFeature(configuredStructureFeature);
		ImmutableList.Builder<StructureStart> builder = ImmutableList.builder();
		Objects.requireNonNull(builder);
		this.fillStartsForFeature(configuredStructureFeature, longSet, builder::add);
		return builder.build();
	}

	@Override
	public Map<ConfiguredStructureFeature<?, ?>, LongSet> getAllStructuresAt(BlockPos blockPos) {
		SectionPos sectionPos = SectionPos.of(blockPos);
		ChunkAccess chunk = _getChunk(sectionPos.x(), sectionPos.z(), ChunkStatus.STRUCTURE_REFERENCES);
		if (chunk == null) return Map.of();
		return chunk.getAllReferences();
	}
	#endif

}