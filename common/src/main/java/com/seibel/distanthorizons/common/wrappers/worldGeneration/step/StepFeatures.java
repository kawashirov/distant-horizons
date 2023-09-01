/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2023 James Seibel
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

import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.ThreadedParameters;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.mimicObject.DhLitWorldGenRegion;
import com.seibel.distanthorizons.core.util.gridList.ArrayGridList;

import net.minecraft.ReportedException;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;
#if POST_MC_1_18_2
#endif

public final class StepFeatures
{
	public static final ChunkStatus STATUS = ChunkStatus.FEATURES;
	
	private final BatchGenerationEnvironment environment;
	
	
	
	public StepFeatures(BatchGenerationEnvironment batchGenerationEnvironment) { this.environment = batchGenerationEnvironment; }
	
	
	
	public void generateGroup(
			ThreadedParameters tParams, DhLitWorldGenRegion worldGenRegion,
			ArrayGridList<ChunkWrapper> chunkWrappers)
	{
		ArrayList<ChunkAccess> chunksToDo = new ArrayList<ChunkAccess>();
		
		for (ChunkWrapper chunkWrapper : chunkWrappers)
		{
			ChunkAccess chunk = chunkWrapper.getChunk();
			if (chunk.getStatus().isOrAfter(STATUS)) continue;
			((ProtoChunk) chunk).setStatus(STATUS);
			chunksToDo.add(chunk);
		}
		
		for (ChunkAccess chunk : chunksToDo)
		{
			try
			{
				#if PRE_MC_1_18_2
				worldGenRegion.setOverrideCenter(chunk.getPos());
				environment.params.generator.applyBiomeDecoration(worldGenRegion, tParams.structFeat);
				#else
				environment.params.generator.applyBiomeDecoration(worldGenRegion, chunk,
						tParams.structFeat.forWorldGenRegion(worldGenRegion));
				#endif
				
				Heightmap.primeHeightmaps(chunk, STATUS.heightmapsAfter());
				BatchGenerationEnvironment.clearDistantGenerationMixinData();
			}
			catch (ReportedException e)
			{
				e.printStackTrace();
				// FIXME: Features concurrent modification issue. Something about cocobeans might just
				// error out. For now just retry.
			}
		}
	}
	
}