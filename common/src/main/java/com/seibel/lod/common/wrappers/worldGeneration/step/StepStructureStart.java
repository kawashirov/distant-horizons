/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package com.seibel.lod.common.wrappers.worldGeneration.step;

import java.util.ArrayList;
import java.util.List;

import com.seibel.lod.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.lod.common.wrappers.worldGeneration.ThreadedParameters;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;

public final class StepStructureStart {
	/**
	 * 
	 */
	private final BatchGenerationEnvironment environment;

	/**
	 * @param batchGenerationEnvironment
	 */
	public StepStructureStart(BatchGenerationEnvironment batchGenerationEnvironment)
	{
		environment = batchGenerationEnvironment;
	}

	public final ChunkStatus STATUS = ChunkStatus.STRUCTURE_STARTS;
	
	public static class StructStartCorruptedException extends RuntimeException {
		private static final long serialVersionUID = -8987434342051563358L;

		public StructStartCorruptedException(ArrayIndexOutOfBoundsException e) {
			super("StructStartCorruptedException");
			super.initCause(e);
			fillInStackTrace();
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
		
		if (environment.params.worldGenSettings.generateFeatures()) {
			for (ChunkAccess chunk : chunksToDo) {
				// System.out.println("StepStructureStart: "+chunk.getPos());
				environment.params.generator.createStructures(environment.params.registry, tParams.structFeat, chunk, environment.params.structures,
						environment.params.worldSeed);

				#if MC_VERSION_1_18_1 || MC_VERSION_1_18_2
				try {
					tParams.structCheck.onStructureLoad(chunk.getPos(), chunk.getAllStarts());
				} catch (ArrayIndexOutOfBoundsException e) {
					// There's a rare issue with StructStart where it throws ArrayIndexOutOfBounds
					// This means the structFeat is corrupted (For some reason) and I need to reset it.
					// TODO: Figure out in the future why this happens even though I am using new structFeat
					throw new StepStructureStart.StructStartCorruptedException(e);
				}
				#endif
			}
		}
	}
}