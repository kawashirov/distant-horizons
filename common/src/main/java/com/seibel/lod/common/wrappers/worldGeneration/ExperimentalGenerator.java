package com.seibel.lod.common.wrappers.worldGeneration;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.seibel.lod.common.wrappers.world.WorldWrapper;
import com.seibel.lod.common.wrappers.worldGeneration.WorldGenerationStep.Steps;
import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.objects.PosToGenerateContainer;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.util.LevelPosUtil;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;
import com.seibel.lod.core.wrapperInterfaces.worldGeneration.AbstractExperimentalWorldGeneratorWrapper;

import net.minecraft.world.level.ChunkPos;

public class ExperimentalGenerator extends AbstractExperimentalWorldGeneratorWrapper {
	private static final IMinecraftWrapper MC = SingletonHandler.get(IMinecraftWrapper.class);
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	public WorldGenerationStep generationGroup;
	public LodDimension targetLodDim;
	public static final int generationGroupSize = 8;
	public static int numberOfGenerationPoints = 8;
	private int generationFutureDebugIDs = 0;

	private int estimatedSampleNeeded = 128;

	private LinkedList<Future<?>> futures = new LinkedList<Future<?>>();
	private LinkedList<ChunkPos> futuresChunkPos = new LinkedList<ChunkPos>();

	public ExperimentalGenerator(LodBuilder newLodBuilder, LodDimension newLodDimension, IWorldWrapper worldWrapper) {
		super(newLodBuilder, newLodDimension, worldWrapper);
		System.out.println("================ExperimentalGenerator INIT=============");
		generationGroup = new WorldGenerationStep(((WorldWrapper) worldWrapper).getServerWorld(), newLodBuilder,
				newLodDimension);
	}

	private static boolean isFarEnough(int genRange, int cax, int cay, int cbx, int cby) {
		int dist = Math.min(Math.abs(cax - cbx), Math.abs(cay - cby));
		// return true;
		// TODO: tune this value
		return dist > genRange * 2 + 8;
	}

	private boolean checkIfPositionIsValid(int chunkX, int chunkZ) {
		for (ChunkPos pos : futuresChunkPos) {
			if (!isFarEnough(generationGroupSize, pos.x, pos.z, chunkX, chunkZ)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void queueGenerationRequests(LodDimension lodDim, LodBuilder lodBuilder) {
		ExecutorService executor = generationGroup.executors;
		DistanceGenerationMode mode = CONFIG.client().worldGenerator().getDistanceGenerationMode();

		if (mode == DistanceGenerationMode.NONE || !MC.hasSinglePlayerServer())
			return;

		// Update all current out standing jobs
		Iterator<Future<?>> iter = futures.iterator();
		Iterator<ChunkPos> posIter = futuresChunkPos.iterator();
		while (iter.hasNext()) {
			posIter.next();
			Future<?> future = iter.next();
			if (future.isDone()) {
				try {
					future.get();
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				} finally {
					iter.remove();
					posIter.remove();
				}
			}
		}

		// If we still all jobs running, return.
		if (futures.size() >= numberOfGenerationPoints)
			return;

		final int targetToGenerate = numberOfGenerationPoints - futures.size();
		int toGenerate = targetToGenerate;
		int positionGoneThough = 0;

		// round the player's block position down to the nearest chunk BlockPos
		int playerPosX = MC.getPlayerBlockPos().getX();
		int playerPosZ = MC.getPlayerBlockPos().getZ();

		// TODO: Make it so that lodDim allows feeding in a function to fast halt if
		// position generation is completed.
		PosToGenerateContainer posToGenerate = lodDim.getPosToGenerate(estimatedSampleNeeded, playerPosX, playerPosZ);

		// Find the max number of iterations we need to go though.
		// We are checking one FarPos, and one NearPos per iterations. This ensure we
		// aren't just
		// always picking one or the other.
		int nearCount = posToGenerate.getNumberOfNearPos();
		int farCount = posToGenerate.getNumberOfFarPos();
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
				if (checkIfPositionIsValid(chunkX, chunkZ)) {
					ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
					futuresChunkPos.add(chunkPos);
					futures.add(executor.submit(() -> {
						generationGroup.generateLodFromList(generationFutureDebugIDs++, chunkPos, generationGroupSize,
								Steps.Features);
					}));
					toGenerate--;
				}
			}
			if (toGenerate <= 0)
				break;
			

			// We have nearPos to go though
			if (i < nearCount && posToGenerate.getNthDetail(i, true) != 0) {
				positionGoneThough++;
				// TODO: Add comment here on why theres a '-1'.
				// Not sure what's happening here. This is copied from previous codes.
				byte detailLevel = (byte) (posToGenerate.getNthDetail(i, true) - 1);
				int chunkX = LevelPosUtil.getChunkPos(detailLevel, posToGenerate.getNthPosX(i, true));
				int chunkZ = LevelPosUtil.getChunkPos(detailLevel, posToGenerate.getNthPosZ(i, true));
				if (checkIfPositionIsValid(chunkX, chunkZ)) {
					ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
					futuresChunkPos.add(chunkPos);
					futures.add(executor.submit(() -> {
						generationGroup.generateLodFromList(generationFutureDebugIDs++, chunkPos, generationGroupSize,
								Steps.Features);
					}));
					toGenerate--;
				}
			}
			if (toGenerate <= 0)
				break;
			
		}
		if (targetToGenerate != toGenerate) {
			if (toGenerate == 0) {
				System.out.println(
						"WorldGenerator: Sampled " + posToGenerate.getNumberOfPos() + " out of " + estimatedSampleNeeded
								+ " points, started all targeted " + targetToGenerate + " generations.");
			} else {
				System.out.println("WorldGenerator: Sampled " + posToGenerate.getNumberOfPos() + " out of "
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
			System.out.println("WorldGenerator: Increasing estimatedSampleNeeeded to " + estimatedSampleNeeded);

		} else if (toGenerate == 0 && positionGoneThough * 1.5 < posToGenerate.getNumberOfPos()) {
			// We haven't gone though half of them and it's already enough.
			// Let's shink the estimatedSampleNeeded.
			estimatedSampleNeeded /= 1.2;
			// Ensure we don't go to near zero.
			if (estimatedSampleNeeded < 4)
				estimatedSampleNeeded = 4;
			System.out.println("WorldGenerator: Decreasing estimatedSampleNeeeded to " + estimatedSampleNeeded);
		}

	}

	@Override
	public void stop() {
		System.out.println("================ExperimentalGenerator SHUTDOWN=============");
		generationGroup.executors.shutdownNow();
	}

}
