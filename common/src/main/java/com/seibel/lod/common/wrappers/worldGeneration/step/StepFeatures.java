package com.seibel.lod.common.wrappers.worldGeneration.step;

import java.util.ArrayList;

import com.seibel.lod.common.wrappers.worldGeneration.ThreadedParameters;
import com.seibel.lod.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.lod.core.util.GridList;

import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.WorldgenRandom;

public final class StepFeatures {
	/**
	 * 
	 */
	private final BatchGenerationEnvironment envionment;

	/**
	 * @param worldGenerationEnvironment
	 */
	public StepFeatures(BatchGenerationEnvironment worldGenerationEnvironment) {
		envionment = worldGenerationEnvironment;
	}

	public final ChunkStatus STATUS = ChunkStatus.FEATURES;

	public void applyBiomeDecoration(ChunkGenerator generator, WorldGenRegion worldGenRegion,
			StructureFeatureManager structureFeatureManager, ChunkAccess chunk) {
		int i = chunk.getPos().x;
		int j = chunk.getPos().z;
		int k = i * 16;
		int l = j * 16;
		BlockPos blockPos = new BlockPos(k, 0, l);
		Biome biome = generator.biomeSource.getNoiseBiome((i << 2) + 2, 2, (j << 2) + 2);
		WorldgenRandom worldgenRandom = new WorldgenRandom();
		long m = worldgenRandom.setDecorationSeed(worldGenRegion.getSeed(), k, l);
		try {
			synchronized (generator) {
				biome.generate(structureFeatureManager, generator, worldGenRegion, m, worldgenRandom, blockPos);
			}
		} catch (Exception exception) {
			CrashReport crashReport = CrashReport.forThrowable(exception, "Biome decoration");
			crashReport.addCategory("Generation")

					.setDetail("CenterX", Integer.valueOf(i)).setDetail("CenterZ", Integer.valueOf(j))
					.setDetail("Seed", Long.valueOf(m)).setDetail("Biome", biome);
			throw new ReportedException(crashReport);
		}
	}

	public void generateGroup(ThreadedParameters tParams, WorldGenRegion worldGenRegion, GridList<ChunkAccess> chunks) {
		ArrayList<ChunkAccess> chunksToDo = new ArrayList<ChunkAccess>();

		for (ChunkAccess chunk : chunks) {
			if (chunk.getStatus().isOrAfter(STATUS))
				continue;
			((ProtoChunk) chunk).setStatus(STATUS);
			chunksToDo.add(chunk);
		}

		for (ChunkAccess chunk : chunksToDo) {
			try {
				applyBiomeDecoration(envionment.params.generator, worldGenRegion, tParams.structFeat, chunk);
			} catch (ReportedException e) {
				e.printStackTrace();
			}
		}
	}
}