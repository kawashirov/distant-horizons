/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2021 Tom Lee (TomTheFurry) & James Seibel (Original code)
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

package com.seibel.lod.common.wrappers.worldGeneration;

import java.util.concurrent.TimeUnit;

import com.seibel.lod.common.wrappers.world.WorldWrapper;
import com.seibel.lod.common.wrappers.worldGeneration.WorldGenerationStep.Steps;
import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.enums.config.GenerationPriority;
import com.seibel.lod.core.objects.PosToGenerateContainer;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.util.LevelPosUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;
import com.seibel.lod.core.wrapperInterfaces.worldGeneration.AbstractExperimentalWorldGeneratorWrapper;

public class ExperimentalGenerator extends AbstractExperimentalWorldGeneratorWrapper {
	
	public static final boolean ENABLE_GENERATOR_STATS_LOGGING = false;
	
	private static final IMinecraftWrapper MC = SingletonHandler.get(IMinecraftWrapper.class);
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	public WorldGenerationStep generationGroup;
	public LodDimension targetLodDim;
	public static final int generationGroupSize = 4;
	public static final int generationGroupSizeFar = 0;
	public static int numberOfGenerationPoints = CONFIG.client().advanced().threading().getNumberOfWorldGenerationThreads();

	private int estimatedSampleNeeded = 128;

	public ExperimentalGenerator(LodBuilder newLodBuilder, LodDimension newLodDimension, IWorldWrapper worldWrapper) {
		super(newLodBuilder, newLodDimension, worldWrapper);
		MC.sendChatMessage("WARNING: You are currently using Distant Horizon's Experimental Chunk Pre-Generator!");
		MC.sendChatMessage("The generation mode: Feature mode is not recommended for < 8GB RAM");
		MC.sendChatMessage("Stuff may broke at any time!");
		generationGroup = new WorldGenerationStep(((WorldWrapper) worldWrapper).getServerWorld(), newLodBuilder,
				newLodDimension);
		ClientApi.LOGGER.info("1.18 Experimental Chunk Generator initialized");
	}
	
	@SuppressWarnings("unused")
	@Override
	public void queueGenerationRequests(LodDimension lodDim, LodBuilder lodBuilder) {
		if (lodDim != targetLodDim) {
			stop();
			WorldWrapper dim = (WorldWrapper) LodUtil.getServerWorldFromDimension(lodDim.dimension);
			generationGroup = new WorldGenerationStep(dim.getServerWorld(), lodBuilder, lodDim);
			targetLodDim = lodDim;
			ClientApi.LOGGER.info("1.18 Experimental Chunk Generator reinitialized");
		}
		
		DistanceGenerationMode mode = CONFIG.client().worldGenerator().getDistanceGenerationMode();
		numberOfGenerationPoints = CONFIG.client().advanced().threading().getNumberOfWorldGenerationThreads();
		GenerationPriority priority = CONFIG.client().worldGenerator().getGenerationPriority();
		if (priority == GenerationPriority.AUTO)
			priority = MC.hasSinglePlayerServer() ? GenerationPriority.FAR_FIRST : GenerationPriority.NEAR_FIRST;
		
		generationGroup.updateAllFutures();
		if (mode == DistanceGenerationMode.NONE || !MC.hasSinglePlayerServer())
			return;
		int eventsCount = generationGroup.events.size();
		// If we still all jobs running, return.
		if (eventsCount >= numberOfGenerationPoints)
			return;

		final int targetToGenerate = numberOfGenerationPoints - eventsCount;
		int toGenerate = targetToGenerate;
		int positionGoneThough = 0;

		// round the player's block position down to the nearest chunk BlockPos
		int playerPosX = MC.getPlayerBlockPos().getX();
		int playerPosZ = MC.getPlayerBlockPos().getZ();

		// TODO: Make it so that lodDim allows feeding in a function to fast halt if
		// position generation is completed.
		PosToGenerateContainer posToGenerate = lodDim.getPosToGenerate(estimatedSampleNeeded, playerPosX, playerPosZ, priority, mode);

		//ClientApi.LOGGER.info("PosToGenerate: {}", posToGenerate);
		
		// Find the max number of iterations we need to go though.
		// We are checking one FarPos, and one NearPos per iterations. This ensure we
		// aren't just
		// always picking one or the other.
		Steps targetStep;
		switch (mode) {
		case NONE:
		case FULL:
			return;
		case BIOME_ONLY:
			targetStep = Steps.Biomes; //NOTE: No block. Require fake height in LodBuilder
			break;
		case BIOME_ONLY_SIMULATE_HEIGHT:
			targetStep = Steps.Noise; //NOTE: Stone only. Require fake surface
			break;
		case SURFACE:
			targetStep = Steps.Surface; //Carvers or Surface???
			break;
		case FEATURES:
			targetStep = Steps.Features;
			break;
		// TODO!
		default:
			assert false;
			return;
		}
		
		if (priority == GenerationPriority.FAR_FIRST) {
			int nearCount = posToGenerate.getNumberOfNearPos();
			int farCount = posToGenerate.getNumberOfFarPos();
			if (ENABLE_GENERATOR_STATS_LOGGING)
				ClientApi.LOGGER.info("WorldGen. Near:"+nearCount+" Far:"+farCount);
			int maxIteration = Math.max(nearCount, farCount);
			for (int i = 0; i < maxIteration; i++) {

				// We have farPos to go though
				if (i < farCount && posToGenerate.getNthDetail(i, false) != 0) {
					positionGoneThough++;
					// TODO: Add comment here on why theres a '-1'.
					// Not sure what's happening here. This is copied from previous codes.
					byte detailLevel = (byte) (posToGenerate.getNthDetail(i, false) - 1);
					int chunkX = LevelPosUtil.getChunkPos(detailLevel, posToGenerate.getNthPosX(i, false));
					int chunkZ = LevelPosUtil.getChunkPos(detailLevel, posToGenerate.getNthPosZ(i, false));
					if (generationGroup.tryAddPoint(chunkX, chunkZ, generationGroupSizeFar, targetStep)) {
						toGenerate--;
					}
				}
				
				// We have nearPos to go though
				if (i < nearCount && posToGenerate.getNthDetail(i, true) != 0) {
					positionGoneThough++;
					// TODO: Add comment here on why theres a '-1'.
					// Not sure what's happening here. This is copied from previous codes.
					byte detailLevel = (byte) (posToGenerate.getNthDetail(i, true) - 1);
					int chunkX = LevelPosUtil.getChunkPos(detailLevel, posToGenerate.getNthPosX(i, true));
					int chunkZ = LevelPosUtil.getChunkPos(detailLevel, posToGenerate.getNthPosZ(i, true));
					int genSize = detailLevel > LodUtil.CHUNK_DETAIL_LEVEL ? 0 : generationGroupSize;
					if (generationGroup.tryAddPoint(chunkX, chunkZ, genSize, targetStep)) {
						toGenerate--;
					}
				}
	
				if (toGenerate <= 0)
					break;
			}
		} else {
			int nearCount = posToGenerate.getNumberOfNearPos();
			for (int i = 0; i < nearCount; i++) {
				
				// We have nearPos to go though
				if (posToGenerate.getNthDetail(i, true) != 0) {
					positionGoneThough++;
					// TODO: Add comment here on why theres a '-1'.
					// Not sure what's happening here. This is copied from previous codes.
					byte detailLevel = (byte) (posToGenerate.getNthDetail(i, true) - 1);
					int chunkX = LevelPosUtil.getChunkPos(detailLevel, posToGenerate.getNthPosX(i, true));
					int chunkZ = LevelPosUtil.getChunkPos(detailLevel, posToGenerate.getNthPosZ(i, true));
					int genSize = detailLevel > LodUtil.CHUNK_DETAIL_LEVEL ? 0 : generationGroupSize;
					if (generationGroup.tryAddPoint(chunkX, chunkZ, genSize, targetStep)) {
						toGenerate--;
					}
					if (toGenerate <= 0)
						break;
				}
			}
			// Only do far gen if toGenerate is non 0 and that we have requested all samples we can get.
			if (toGenerate > 0 && estimatedSampleNeeded > posToGenerate.getNumberOfPos()) {
				int farCount = posToGenerate.getNumberOfFarPos();
				for (int i = 0; i < farCount; i++) {
					// We have farPos to go though
					if (posToGenerate.getNthDetail(i, false) != 0) {
						positionGoneThough++;
						// TODO: Add comment here on why theres a '-1'.
						// Not sure what's happening here. This is copied from previous codes.
						byte detailLevel = (byte) (posToGenerate.getNthDetail(i, false) - 1);
						int chunkX = LevelPosUtil.getChunkPos(detailLevel, posToGenerate.getNthPosX(i, false));
						int chunkZ = LevelPosUtil.getChunkPos(detailLevel, posToGenerate.getNthPosZ(i, false));
						if (generationGroup.tryAddPoint(chunkX, chunkZ, generationGroupSizeFar, targetStep)) {
							toGenerate--;
						}
					}
					if (toGenerate <= 0)
						break;
				}
			}
		}

		if (targetToGenerate != toGenerate && ENABLE_GENERATOR_STATS_LOGGING) {
			if (toGenerate <= 0) {
				ClientApi.LOGGER.info(
						"WorldGenerator: Sampled " + posToGenerate.getNumberOfPos() + " out of " + estimatedSampleNeeded
								+ " points, started all targeted " + targetToGenerate + " generations.");
			} else {
				ClientApi.LOGGER.info("WorldGenerator: Sampled " + posToGenerate.getNumberOfPos() + " out of "
						+ estimatedSampleNeeded + " points, started " + (targetToGenerate - toGenerate)
						+ " out of targeted " + targetToGenerate + " generations.");
			}
		}

		if (toGenerate > 0 && estimatedSampleNeeded <= posToGenerate.getNumberOfPos()) {
			// We failed to generate enough points from the samples.
			// Let's increase the estimatedSampleNeeded.
			estimatedSampleNeeded *= 1.3;
			// Ensure wee don't go to basically infinity
			if (estimatedSampleNeeded > 32768)
				estimatedSampleNeeded = 32768;
			if (ENABLE_GENERATOR_STATS_LOGGING)
				ClientApi.LOGGER.info("WorldGenerator: Increasing estimatedSampleNeeeded to " + estimatedSampleNeeded);

		} else if (toGenerate <= 0 && positionGoneThough * 1.5 < posToGenerate.getNumberOfPos()) {
			// We haven't gone though half of them and it's already enough.
			// Let's shink the estimatedSampleNeeded.
			estimatedSampleNeeded /= 1.2;
			// Ensure we don't go to near zero.
			if (estimatedSampleNeeded < 4)
				estimatedSampleNeeded = 4;
			if (ENABLE_GENERATOR_STATS_LOGGING)
				ClientApi.LOGGER.info("WorldGenerator: Decreasing estimatedSampleNeeeded to " + estimatedSampleNeeded);
		}

	}

	@Override
	public void stop() {
		ClientApi.LOGGER.info("1.18 Experimental Chunk Generator shutting down...");
		generationGroup.executors.shutdownNow();
		try {
			if (!generationGroup.executors.awaitTermination(10, TimeUnit.SECONDS)) {
				ClientApi.LOGGER.error("1.18 Experimental Chunk Generator shutdown failed! Ignoring child threads...");
			}
		} catch (InterruptedException e) {}
	}

}