package com.seibel.lod.common.wrappers.worldGeneration.step;

import java.util.ArrayList;
import java.util.List;

import com.seibel.lod.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.lod.common.wrappers.worldGeneration.ThreadedParameters;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public final class StepStructureReference {
	/**
	 * 
	 */
	private final BatchGenerationEnvironment environment;

	/**
	 * @param batchGenerationEnvironment
	 */
	public StepStructureReference(BatchGenerationEnvironment batchGenerationEnvironment)
	{
		environment = batchGenerationEnvironment;
	}

	public final ChunkStatus STATUS = ChunkStatus.STRUCTURE_REFERENCES;

	private void createReferences(WorldGenRegion worldGenLevel, StructureFeatureManager structureFeatureManager,
			ChunkAccess chunkAccess) {
		ChunkPos chunkPos = chunkAccess.getPos();
		int j = chunkPos.x;
		int k = chunkPos.z;
		int l = chunkPos.getMinBlockX();
		int m = chunkPos.getMinBlockZ();

		SectionPos sectionPos = SectionPos.bottomOf(chunkAccess);

		for (int n = j - 8; n <= j + 8; n++) {
			for (int o = k - 8; o <= k + 8; o++) {
				if (!worldGenLevel.hasChunk(n, o))
					continue;
				long p = ChunkPos.asLong(n, o);
				for (StructureStart<?> structureStart : worldGenLevel.getChunk(n, o).getAllStarts().values()) {
					try {
						if (structureStart.isValid()
								&& structureStart.getBoundingBox().intersects(l, m, l + 15, m + 15)) {
							structureFeatureManager.addReferenceForFeature(sectionPos, structureStart.getFeature(),
									p, chunkAccess);
						}
					} catch (Exception exception) {
						CrashReport crashReport = CrashReport.forThrowable(exception,
								"Generating structure reference");
						CrashReportCategory crashReportCategory = crashReport.addCategory("Structure");
						crashReportCategory.setDetail("Id",
								() -> Registry.STRUCTURE_FEATURE.getKey(structureStart.getFeature()).toString());
						crashReportCategory.setDetail("Name", () -> structureStart.getFeature().getFeatureName());
						crashReportCategory.setDetail("Class",
								() -> structureStart.getFeature().getClass().getCanonicalName());
						throw new ReportedException(crashReport);
					}
				}
			}
		}
	}

	public void generateGroup(ThreadedParameters tParams, WorldGenRegion worldGenRegion,
			List<ChunkAccess> chunks) {

		ArrayList<ChunkAccess> chunksToDo = new ArrayList<ChunkAccess>();
		
		for (ChunkAccess chunk : chunks) {
			if (chunk.getStatus().isOrAfter(STATUS)) continue;
			((ProtoChunk) chunk).setStatus(STATUS);
			chunksToDo.add(chunk);
		}
		
		for (ChunkAccess chunk : chunksToDo) {
			// System.out.println("StepStructureReference: "+chunk.getPos());
			createReferences(worldGenRegion, tParams.structFeat.forWorldGenRegion(worldGenRegion), chunk);
		}
	}
}