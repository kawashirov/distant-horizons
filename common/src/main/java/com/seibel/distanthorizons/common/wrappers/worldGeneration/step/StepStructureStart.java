/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.distanthorizons.common.wrappers.worldGeneration.step;

import java.util.ArrayList;
import java.util.List;

import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.ThreadedParameters;

import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.apache.logging.log4j.Logger;

public final class StepStructureStart
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	private final BatchGenerationEnvironment environment;
	
	/**
	 * @param batchGenerationEnvironment
	 */
	public StepStructureStart(BatchGenerationEnvironment batchGenerationEnvironment)
	{
		environment = batchGenerationEnvironment;
	}
	
	public final ChunkStatus STATUS = ChunkStatus.STRUCTURE_STARTS;
	
	public static class StructStartCorruptedException extends RuntimeException
	{
		private static final long serialVersionUID = -8987434342051563358L;
		
		public StructStartCorruptedException(ArrayIndexOutOfBoundsException e)
		{
			super("StructStartCorruptedException");
			super.initCause(e);
			fillInStackTrace();
		}
		
	}
	
	public void generateGroup(
			ThreadedParameters tParams, WorldGenRegion worldGenRegion,
			List<ChunkWrapper> chunkWrappers) throws InterruptedException
	{
		ArrayList<ChunkAccess> chunksToDo = new ArrayList<>();
		
		for (ChunkWrapper chunkWrapper : chunkWrappers)
		{
			ChunkAccess chunk = chunkWrapper.getChunk();
			if (!chunk.getStatus().isOrAfter(STATUS))
			{
				((ProtoChunk) chunk).setStatus(STATUS);
				chunksToDo.add(chunk);
			}
		}
		
		#if PRE_MC_1_19_2
		if (environment.params.worldGenSettings.generateFeatures())
		{
		#elif PRE_MC_1_19_4
		if (environment.params.worldGenSettings.generateStructures()) {
		#else
		if (environment.params.worldOptions.generateStructures())
		{
		#endif
			for (ChunkAccess chunk : chunksToDo)
			{
				// System.out.println("StepStructureStart: "+chunk.getPos());
				
				// there are a few cases where the structure generator call may lock up (either due to teleporting or leaving the world).
				// hopefully allowing interrupts here will prevent that from happening.
				BatchGenerationEnvironment.throwIfThreadInterrupted();
				
				#if PRE_MC_1_19_2
				environment.params.generator.createStructures(environment.params.registry, tParams.structFeat, chunk, environment.params.structures,
						environment.params.worldSeed);
				#elif PRE_MC_1_19_4
				environment.params.generator.createStructures(environment.params.registry, environment.params.randomState, tParams.structFeat, chunk, environment.params.structures,
						environment.params.worldSeed);
				#else
				environment.params.generator.createStructures(environment.params.registry,
						environment.params.level.getChunkSource().getGeneratorState(),
						tParams.structFeat, chunk, environment.params.structures);
				#endif
				#if POST_MC_1_18_2
				try
				{
					tParams.structCheck.onStructureLoad(chunk.getPos(), chunk.getAllStarts());
				}
				catch (ArrayIndexOutOfBoundsException firstEx)
				{
					// There's a rare issue with StructStart where it throws ArrayIndexOutOfBounds
					// This means the structFeat is corrupted (For some reason) and I need to reset it.
					// TODO: Figure out in the future why this happens even though I am using new structFeat - OLD
					
					// reset the structureStart
					tParams.recreateStructureCheck();
					
					try
					{
						// try running the structure logic again
						tParams.structCheck.onStructureLoad(chunk.getPos(), chunk.getAllStarts());
					}
					catch (ArrayIndexOutOfBoundsException secondEx)
					{
						// the structure logic failed again, log it and move on
						LOGGER.error("Unable to create structure starts for " + chunk.getPos() + ". This is an error with MC's world generation. Ignoring and continuing generation. Error: " + secondEx.getMessage()); // don't log the full stack trace since it is long and will generally end up in MC's code
						
						//throw new StepStructureStart.StructStartCorruptedException(secondEx);
					}
				}
				#endif
			}
		}
	}
	
}