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

import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.mimicObject.WorldGenLevelLightEngine;
import com.seibel.distanthorizons.core.util.gridList.ArrayGridList;

import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.lighting.LightEventListener;

public final class StepLight {
	/**
	 * 
	 */
	private final BatchGenerationEnvironment environment;

	/**
	 * @param batchGenerationEnvironment
	 */
	public StepLight(BatchGenerationEnvironment batchGenerationEnvironment)
	{
		environment = batchGenerationEnvironment;
	}

	public final ChunkStatus STATUS = ChunkStatus.LIGHT;
	
	public void generateGroup(
			#if PRE_MC_1_17_1 LevelLightEngine lightEngine,
			#else LightEventListener lightEngine, #endif
			ArrayGridList<ChunkWrapper> chunkWrappers) {
		
		for (ChunkWrapper chunkWrapper : chunkWrappers)
		{
			ChunkAccess chunk = chunkWrapper.getChunk();
			if (chunk.getStatus().isOrAfter(STATUS)) continue;
			((ProtoChunk) chunk).setStatus(STATUS);
		}
		
		for (ChunkWrapper chunkWrapper : chunkWrappers)
		{
			ChunkAccess chunk = chunkWrapper.getChunk();
			boolean hasCorrectBlockLight = (chunk instanceof LevelChunk && chunk.isLightCorrect());
			try {
				if (lightEngine == null) {
					// Do nothing
				} else if (lightEngine instanceof WorldGenLevelLightEngine) {
					((WorldGenLevelLightEngine)lightEngine).lightChunk(chunk, !hasCorrectBlockLight);
				} else if (lightEngine instanceof ThreadedLevelLightEngine) {
					((ThreadedLevelLightEngine) lightEngine).lightChunk(chunk, !hasCorrectBlockLight).join();
				} else {
					assert(false);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			#if POST_MC_1_18_2 && PRE_MC_1_20_1
			if (chunk instanceof LevelChunk) ((LevelChunk)chunk).setClientLightReady(true);
			#elif POST_MC_1_20_1
			lightEngine.setLightEnabled(chunk.getPos(), true);
			#endif


			chunk.setLightCorrect(true);
		}
		#if PRE_MC_1_20_1
		lightEngine.runUpdates(Integer.MAX_VALUE, true, true);
		#else
		lightEngine.runLightUpdates();
		#endif
	}
}