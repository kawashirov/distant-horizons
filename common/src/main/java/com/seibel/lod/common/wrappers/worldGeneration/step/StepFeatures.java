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
#if MC_VERSION_1_18_2 || MC_VERSION_1_18_1
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
				#if MC_VERSION_1_18_2 || MC_VERSION_1_18_1
				environment.params.generator.applyBiomeDecoration(worldGenRegion, chunk,
						tParams.structFeat.forWorldGenRegion(worldGenRegion));
				Blender.generateBorderTicks(worldGenRegion, chunk);
				#elif MC_VERSION_1_17_1 || MC_VERSION_1_16_5
				worldGenRegion.setOverrideCenter(chunk.getPos());
				Heightmap.primeHeightmaps(chunk, STATUS.heightmapsAfter());
				environment.params.generator.applyBiomeDecoration(worldGenRegion, tParams.structFeat);
				#endif
			} catch (ReportedException e) {
				e.printStackTrace();
				// FIXME: Features concurrent modification issue. Something about cocobeans just
				// aren't happy
				// For now just retry.
			}
		}/*
		for (ChunkAccess chunk : chunks) {
			Heightmap.primeHeightmaps(chunk,
					EnumSet.of(Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
							Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE));
		}*/
	}
}