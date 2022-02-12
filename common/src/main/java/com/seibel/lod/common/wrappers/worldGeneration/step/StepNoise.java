package com.seibel.lod.common.wrappers.worldGeneration.step;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.Sets;
import com.seibel.lod.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.lod.common.wrappers.worldGeneration.ThreadedParameters;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.blending.Blender;

public final class StepNoise {
	/**
	 * 
	 */
	private final BatchGenerationEnvironment environment;

	/**
	 * @param batchGenerationEnvironment
	 */
	public StepNoise(BatchGenerationEnvironment batchGenerationEnvironment)
	{
		environment = batchGenerationEnvironment;
	}
	
	public final ChunkStatus STATUS = ChunkStatus.NOISE;

    private ChunkAccess NoiseBased$fillFromNoise(NoiseBasedChunkGenerator generator, Blender blender, StructureFeatureManager structureFeatureManager, ChunkAccess chunkAccess) {
        NoiseSettings noiseSettings = generator.settings.get().noiseSettings();
        LevelHeightAccessor levelHeightAccessor = chunkAccess.getHeightAccessorForGeneration();
        int i = Math.max(noiseSettings.minY(), levelHeightAccessor.getMinBuildHeight());
        int j = Math.min(noiseSettings.minY() + noiseSettings.height(), levelHeightAccessor.getMaxBuildHeight());
        int k = Mth.intFloorDiv(i, noiseSettings.getCellHeight());
        int l = Mth.intFloorDiv(j - i, noiseSettings.getCellHeight());
        if (l <= 0) {
            return chunkAccess;
        }
        int m = chunkAccess.getSectionIndex(l * noiseSettings.getCellHeight() - 1 + i);
        int n = chunkAccess.getSectionIndex(i);
        HashSet<LevelChunkSection> set = Sets.newHashSet();
        for (int o = m; o >= n; --o) {
            LevelChunkSection levelChunkSection = chunkAccess.getSection(o);
            levelChunkSection.acquire();
            set.add(levelChunkSection);
        }
        chunkAccess = generator.doFill(blender, structureFeatureManager, chunkAccess, k, l);
        for (LevelChunkSection levelChunkSection : set) {
            levelChunkSection.release();
        };
        return chunkAccess;
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
			// System.out.println("StepNoise: "+chunk.getPos());
			if (environment.params.generator instanceof NoiseBasedChunkGenerator) {
				chunk = NoiseBased$fillFromNoise((NoiseBasedChunkGenerator)environment.params.generator,Blender.of(worldGenRegion),
						tParams.structFeat.forWorldGenRegion(worldGenRegion), chunk);
			} else {
				chunk = environment.joinSync(environment.params.generator.fillFromNoise(Runnable::run, Blender.of(worldGenRegion),
						tParams.structFeat.forWorldGenRegion(worldGenRegion), chunk));
			}
		}
	}
}