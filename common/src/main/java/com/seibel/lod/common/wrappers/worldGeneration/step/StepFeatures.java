package com.seibel.lod.common.wrappers.worldGeneration.step;

import java.util.ArrayList;
import java.util.EnumSet;

import com.seibel.lod.common.wrappers.worldGeneration.ThreadedParameters;
import com.seibel.lod.common.wrappers.worldGeneration.mimicObject.LightedWorldGenRegion;
import com.seibel.lod.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.lod.core.util.GridList;

import net.minecraft.ReportedException;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;

public final class StepFeatures {
	/**
	 * 
	 */
	private final BatchGenerationEnvironment envionment;

	/**
	 * @param worldGenerationEnvironment
	 */
	public StepFeatures(BatchGenerationEnvironment worldGenerationEnvironment) {
		envionment = worldGenerationEnvironment;
	}

	public final ChunkStatus STATUS = ChunkStatus.FEATURES;

	public void generateGroup(ThreadedParameters tParams, LightedWorldGenRegion worldGenRegion, GridList<ChunkAccess> chunks) {
		ArrayList<ChunkAccess> chunksToDo = new ArrayList<ChunkAccess>();

		for (ChunkAccess chunk : chunks) {
			if (chunk.getStatus().isOrAfter(STATUS))
				continue;
			((ProtoChunk) chunk).setStatus(STATUS);
			chunksToDo.add(chunk);
		}

		for (ChunkAccess chunk : chunksToDo) {
			try {
				worldGenRegion.setOverrideCenter(chunk.getPos());
				Heightmap.primeHeightmaps(chunk, STATUS.heightmapsAfter());
				envionment.params.generator.applyBiomeDecoration(worldGenRegion, tParams.structFeat);
			} catch (ReportedException e) {
				e.printStackTrace();
			}
		}
		worldGenRegion.setOverrideCenter(null);
	}
}