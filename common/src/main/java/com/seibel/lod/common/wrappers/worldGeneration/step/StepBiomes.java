package com.seibel.lod.common.wrappers.worldGeneration.step;

import java.util.ArrayList;
import java.util.List;

import com.seibel.lod.common.wrappers.worldGeneration.ThreadedParameters;
import com.seibel.lod.common.wrappers.worldGeneration.BatchGenerationEnvironment;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;

public final class StepBiomes {
	/**
	 * 
	 */
	private final BatchGenerationEnvironment envionment;

	/**
	 * @param worldGenerationEnvironment
	 */
	public StepBiomes(BatchGenerationEnvironment worldGenerationEnvironment) {
		envionment = worldGenerationEnvironment;
	}

	public final ChunkStatus STATUS = ChunkStatus.BIOMES;

	public void generateGroup(ThreadedParameters tParams, WorldGenRegion worldGenRegion, List<ChunkAccess> chunks) {

		ArrayList<ChunkAccess> chunksToDo = new ArrayList<ChunkAccess>();

		for (ChunkAccess chunk : chunks) {
			if (chunk.getStatus().isOrAfter(STATUS))
				continue;
			((ProtoChunk) chunk).setStatus(STATUS);
			chunksToDo.add(chunk);
		}

		for (ChunkAccess chunk : chunksToDo) {
			// System.out.println("StepBiomes: "+chunk.getPos());
			envionment.params.generator.createBiomes(envionment.params.biomes, chunk);
		}
	}
}