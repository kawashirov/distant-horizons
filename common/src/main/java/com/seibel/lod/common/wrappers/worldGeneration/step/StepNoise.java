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
#if MC_VERSION_1_18_2 || MC_VERSION_1_18_1
import net.minecraft.world.level.levelgen.blending.Blender;
#endif

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
			#if MC_VERSION_1_18_2 || MC_VERSION_1_18_1
			chunk = environment.joinSync(environment.params.generator.fillFromNoise(Runnable::run, Blender.of(worldGenRegion),
						tParams.structFeat.forWorldGenRegion(worldGenRegion), chunk));
			#elif MC_VERSION_1_17_1
			chunk = environment.joinSync(environment.params.generator.fillFromNoise(Runnable::run,
					tParams.structFeat.forWorldGenRegion(worldGenRegion), chunk));
			#endif
		}
	}
}