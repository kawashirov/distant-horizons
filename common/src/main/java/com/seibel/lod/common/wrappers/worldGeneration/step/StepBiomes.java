package com.seibel.lod.common.wrappers.worldGeneration.step;

import java.util.ArrayList;
import java.util.List;

import com.seibel.lod.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.lod.common.wrappers.worldGeneration.ThreadedParameters;

import net.minecraft.core.Registry;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
#if MC_VERSION_1_18_2 || MC_VERSION_1_18_1
import net.minecraft.world.level.levelgen.blending.Blender;
#endif

public final class StepBiomes {
	/**
	 * 
	 */
	private final BatchGenerationEnvironment environment;

	/**
	 * @param batchGenerationEnvironment
	 */
	public StepBiomes(BatchGenerationEnvironment batchGenerationEnvironment)
	{
		environment = batchGenerationEnvironment;
	}

	public final ChunkStatus STATUS = ChunkStatus.BIOMES;
	
	public void generateGroup(ThreadedParameters tParams, WorldGenRegion worldGenRegion,
			List<ChunkAccess> chunks) {

		ArrayList<ChunkAccess> chunksToDo = new ArrayList<ChunkAccess>();
		
		for (ChunkAccess chunk : chunks) {
			if (chunk.getStatus().isOrAfter(STATUS)) continue;
			((ProtoChunk) chunk).setStatus(STATUS);
			chunksToDo.add(chunk);
		}
		
		for (ChunkAccess chunk : chunksToDo) {
			// System.out.println("StepBiomes: "+chunk.getPos());
			#if MC_VERSION_1_18_2 || MC_VERSION_1_18_1
			chunk = environment.joinSync(environment.params.generator.createBiomes(environment.params.biomes, Runnable::run, Blender.of(worldGenRegion),
					tParams.structFeat.forWorldGenRegion(worldGenRegion), chunk));
			#elif MC_VERSION_1_17_1
			environment.params.generator.createBiomes(environment.params.biomes, chunk);
			#endif
		}
	}
}