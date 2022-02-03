package com.seibel.lod.common.wrappers.worldGeneration.step;

import com.seibel.lod.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.lod.common.wrappers.worldGeneration.mimicObject.WorldGenLevelLightEngine;
import com.seibel.lod.core.util.GridList;

import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;

public final class StepLight {
	/**
	 * 
	 */
	private final BatchGenerationEnvironment envionment;

	/**
	 * @param worldGenerationEnvironment
	 */
	public StepLight(BatchGenerationEnvironment worldGenerationEnvironment) {
		envionment = worldGenerationEnvironment;
	}

	public final ChunkStatus STATUS = ChunkStatus.LIGHT;

	public void generateGroup(LevelLightEngine lightEngine, GridList<ChunkAccess> chunks) {
		// ArrayList<ChunkAccess> chunksToDo = new ArrayList<ChunkAccess>();

		for (ChunkAccess chunk : chunks) {
			if (chunk.getStatus().isOrAfter(STATUS))
				continue;
			((ProtoChunk) chunk).setStatus(STATUS);
		}

		for (ChunkAccess chunk : chunks) {
			boolean hasCorrectBlockLight = (chunk instanceof LevelChunk && chunk.isLightCorrect());
			try {
				if (lightEngine == null) {
					// Do nothing
				} else if (lightEngine instanceof WorldGenLevelLightEngine) {
					((WorldGenLevelLightEngine) lightEngine).lightChunk(chunk, !hasCorrectBlockLight);
				} else if (lightEngine instanceof ThreadedLevelLightEngine) {
					((ThreadedLevelLightEngine) lightEngine).lightChunk(chunk, !hasCorrectBlockLight).join();
				} else {
					assert (false);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			chunk.setLightCorrect(true);
		}
		lightEngine.runUpdates(Integer.MAX_VALUE, true, true);
	}
}