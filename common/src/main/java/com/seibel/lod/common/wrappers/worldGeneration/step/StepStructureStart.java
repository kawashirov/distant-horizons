package com.seibel.lod.common.wrappers.worldGeneration.step;

import java.util.ArrayList;
import java.util.List;

import com.seibel.lod.common.wrappers.worldGeneration.ThreadedParameters;
import com.seibel.lod.common.wrappers.worldGeneration.BatchGenerationEnvironment;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;

public final class StepStructureStart {
	/**
	 * 
	 */
	private final BatchGenerationEnvironment envionment;

	/**
	 * @param worldGenerationEnvironment
	 */
	public StepStructureStart(BatchGenerationEnvironment worldGenerationEnvironment) {
		envionment = worldGenerationEnvironment;
	}

	public final ChunkStatus STATUS = ChunkStatus.STRUCTURE_STARTS;

	public void generateGroup(ThreadedParameters tParams, WorldGenRegion worldGenRegion, List<ChunkAccess> chunks) {

		ArrayList<ChunkAccess> chunksToDo = new ArrayList<ChunkAccess>();

		for (ChunkAccess chunk : chunks) {
			if (chunk.getStatus().isOrAfter(STATUS))
				continue;
			((ProtoChunk) chunk).setStatus(STATUS);
			chunksToDo.add(chunk);
		}

		if (envionment.params.worldGenSettings.generateFeatures()) {
			for (ChunkAccess chunk : chunksToDo) {
				// System.out.println("StepStructureStart: "+chunk.getPos());
				envionment.params.generator.createStructures(envionment.params.registry, tParams.structFeat, chunk,
						envionment.params.structures, envionment.params.worldSeed);
				// try {
				envionment.params.generator.createStructures(envionment.params.registry, tParams.structFeat, chunk,
						envionment.params.structures, envionment.params.worldSeed);
				// tParams.structCheck.onStructureLoad(chunk.getPos(), chunk.getAllStarts());
				/*
				 * } catch (ArrayIndexOutOfBoundsException e) { // There's a rare issue with
				 * StructStart where it throws ArrayIndexOutOfBounds // This means the
				 * structFeat is corrupted (For some reason) and I need to reset it. // TODO:
				 * Figure out in the future why this happens even though I am using new
				 * structFeat throw new StructStartCorruptedException(e); }
				 */
			}
		}
	}
}