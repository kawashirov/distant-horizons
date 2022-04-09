/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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

import com.seibel.lod.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.lod.common.wrappers.worldGeneration.ThreadedParameters;
import com.seibel.lod.common.wrappers.worldGeneration.mimicObject.LightedWorldGenRegion;
import com.seibel.lod.core.util.gridList.ArrayGridList;

import net.minecraft.ReportedException;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;
#if POST_MC_1_18_1
import net.minecraft.world.level.levelgen.blending.Blender;
#endif

public final class StepFeatures {
	/**
	 * 
	 */
	private final BatchGenerationEnvironment environment;

	/**
	 * @param batchGenerationEnvironment
	 */
	public StepFeatures(BatchGenerationEnvironment batchGenerationEnvironment)
	{
		environment = batchGenerationEnvironment;
	}

	public final ChunkStatus STATUS = ChunkStatus.FEATURES;

	public void generateGroup(ThreadedParameters tParams, LightedWorldGenRegion worldGenRegion,
			ArrayGridList<ChunkAccess> chunks) {
		ArrayList<ChunkAccess> chunksToDo = new ArrayList<ChunkAccess>();
		
		for (ChunkAccess chunk : chunks) {
			if (chunk.getStatus().isOrAfter(STATUS)) continue;
			((ProtoChunk) chunk).setStatus(STATUS);
			chunksToDo.add(chunk);
		}
		
		for (ChunkAccess chunk : chunksToDo) {
			try {
				#if PRE_MC_1_18_1
				worldGenRegion.setOverrideCenter(chunk.getPos());
				Heightmap.primeHeightmaps(chunk, STATUS.heightmapsAfter());
				environment.params.generator.applyBiomeDecoration(worldGenRegion, tParams.structFeat);
				#else
				Heightmap.primeHeightmaps(chunk, STATUS.heightmapsAfter());
				environment.params.generator.applyBiomeDecoration(worldGenRegion, chunk,
						tParams.structFeat.forWorldGenRegion(worldGenRegion));
				#endif
			} catch (ReportedException e) {
				e.printStackTrace();
				// FIXME: Features concurrent modification issue. Something about cocobeans might just
				// error out. For now just retry.
			}
		}
	}
}