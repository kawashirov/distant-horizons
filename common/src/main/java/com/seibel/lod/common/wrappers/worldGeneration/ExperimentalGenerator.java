package com.seibel.lod.common.wrappers.worldGeneration;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.seibel.lod.common.wrappers.world.WorldWrapper;
import com.seibel.lod.common.wrappers.worldGeneration.WorldGenerationStep.Steps;
import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.objects.PosToGenerateContainer;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.util.LevelPosUtil;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.IVersionConstants;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;
import com.seibel.lod.core.wrapperInterfaces.worldGeneration.AbstractExperimentalWorldGeneratorWrapper;

import net.minecraft.world.level.ChunkPos;

public class ExperimentalGenerator extends AbstractExperimentalWorldGeneratorWrapper {
	private static final IMinecraftWrapper MC = SingletonHandler.get(IMinecraftWrapper.class);
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	private static final IWrapperFactory WRAPPER_FACTORY = SingletonHandler.get(IWrapperFactory.class);
	private static final IVersionConstants VERSION_CONSTANTS = SingletonHandler.get(IVersionConstants.class);
	public WorldGenerationStep generationGroup;
	public LodDimension targetLodDim;
	private boolean generatorThreadRunning = false;

	public ExperimentalGenerator(LodBuilder newLodBuilder, LodDimension newLodDimension, IWorldWrapper worldWrapper) {
		super(newLodBuilder, newLodDimension, worldWrapper);
		System.out.println("================ExperimentalGenerator_INITING=============");
		generationGroup = new WorldGenerationStep(((WorldWrapper) worldWrapper).getServerWorld(), newLodBuilder, newLodDimension);
	}
	
	private static boolean isFarEnough(int genRange, int cax, int cay, int cbx, int cby) {
		int dist = Math.min(Math.abs(cax-cbx), Math.abs(cay-cby));
		// return true;
		return dist > genRange*2;
	}

	@Override
	public void queueGenerationRequests(LodDimension lodDim, LodBuilder lodBuilder) {
		ExecutorService executor = generationGroup.executors;
		DistanceGenerationMode mode = CONFIG.client().worldGenerator().getDistanceGenerationMode();

		if (mode != DistanceGenerationMode.NONE
				&& !generatorThreadRunning
				&& MC.hasSinglePlayerServer())
		{
			generatorThreadRunning = true;
			
			Runnable runner = () -> {
				System.out.println("================ExperimentalGenerator_Run=============");
				try
				{
					int maxSamples = 512;
					int genRange = 8;
					int points = 8;
					// round the player's block position down to the nearest chunk BlockPos
					int playerPosX = MC.getPlayerBlockPos().getX();
					int playerPosZ = MC.getPlayerBlockPos().getZ();
					
					//=======================================//
					// fill in positionsWaitingToBeGenerated //
					//=======================================//
					
					ArrayList<ChunkPos> genPos = new ArrayList<ChunkPos>(points);
					
					PosToGenerateContainer posToGenerate = lodDim.getPosToGenerate(
							maxSamples, playerPosX, playerPosZ);
					
					byte detailLevel;
					int posX;
					int posZ;
					int nearIndex = 0;
					int farIndex = 0;
					
					for (int i = 0; i < posToGenerate.getNumberOfPos(); i++)
					{
						
						// add the near positions
						if (nearIndex < posToGenerate.getNumberOfNearPos() && posToGenerate.getNthDetail(nearIndex, true) != 0)
						{
							detailLevel = (byte) (posToGenerate.getNthDetail(nearIndex, true) - 1);
							posX = posToGenerate.getNthPosX(nearIndex, true);
							posZ = posToGenerate.getNthPosZ(nearIndex, true);
							nearIndex++;
							ChunkPos chunkPos = new ChunkPos(LevelPosUtil.getChunkPos(detailLevel, posX), LevelPosUtil.getChunkPos(detailLevel, posZ));
							
							boolean tooClose = false;
							for (ChunkPos pos : genPos) {
								if (!isFarEnough(genRange, pos.x, pos.z, chunkPos.x, chunkPos.z)) {
									tooClose = true;
									break;
								}
							}
							if (tooClose) continue;
							genPos.add(chunkPos);
							if (genPos.size() >= points) break;
						}
						
						// add the far positions
						if (farIndex < posToGenerate.getNumberOfFarPos() && posToGenerate.getNthDetail(farIndex, false) != 0)
						{
							detailLevel = (byte) (posToGenerate.getNthDetail(farIndex, false) - 1);
							posX = posToGenerate.getNthPosX(farIndex, false);
							posZ = posToGenerate.getNthPosZ(farIndex, false);
							farIndex++;

							ChunkPos chunkPos = new ChunkPos(LevelPosUtil.getChunkPos(detailLevel, posX), LevelPosUtil.getChunkPos(detailLevel, posZ));
							
							boolean tooClose = false;
							for (ChunkPos pos : genPos) {
								if (!isFarEnough(genRange, pos.x, pos.z, chunkPos.x, chunkPos.z)) {
									tooClose = true;
									break;
								}
							}
							if (tooClose) continue;
							genPos.add(chunkPos);
							if (genPos.size() >= points) break;
						}
					}
					System.out.println("WorldGenerator: "+genPos.size()+" number of positions queried.");
					
					ArrayList<Future<?>> futures = new ArrayList<Future<?>>();
					
					for (ChunkPos pos : genPos) {
						futures.add(executor.submit(() -> {
							generationGroup.generateLodFromList(pos, genRange, Steps.Features);
						}));
					}
					
					for (Future<?> f : futures) {
						try {
							f.get(30, TimeUnit.SECONDS);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (ExecutionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (TimeoutException e) {
							f.cancel(true);
						}
					}
				}
				catch (RuntimeException e)
				{
					// this shouldn't ever happen, but just in case
					e.printStackTrace();
				}
				finally
				{
					generatorThreadRunning = false;
				}
			};
			executor.execute(runner);
		};
	}

	@Override
	public void stop() {
		generationGroup.executors.shutdownNow();
	}
	
}
