/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
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

package com.seibel.lod.core.builders.worldGeneration;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.objects.PosToGenerateContainer;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.render.LodRenderer;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LevelPosUtil;
import com.seibel.lod.core.util.LodThreadFactory;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;

/**
 * A singleton that handles all long distance LOD world generation.
 * @author Leonardo Amato
 * @author James Seibel
 * @version 9-25-2021
 */
public class LodWorldGenerator
{
	private static final IMinecraftWrapper MC = SingletonHandler.get(IMinecraftWrapper.class);
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	private static final IWrapperFactory WRAPPER_FACTORY = SingletonHandler.get(IWrapperFactory.class);
	
	
	/** This holds the thread used to create LOD generation requests off the main thread. */
	private final ExecutorService mainGenThread = Executors.newSingleThreadExecutor(new LodThreadFactory(this.getClass().getSimpleName() + " world generator"));
	
	/** we only want to queue up one generator thread at a time */
	private boolean generatorThreadRunning = false;
	
	/**
	 * How many chunks to generate outside the player's view distance at one
	 * time. (or more specifically how many requests to make at one time). I
	 * multiply by 8 to make sure there is always a buffer of chunk requests, to
	 * make sure the CPU is always busy, and we can generate LODs as quickly as
	 * possible.
	 */
	public int maxChunkGenRequests;
	
	/**
	 * This keeps track of how many chunk generation requests are on going. This is
	 * to limit how many chunks are queued at once. To prevent chunks from being
	 * generated for a long time in an area the player is no longer in.
	 */
	public final AtomicInteger numberOfChunksWaitingToGenerate = new AtomicInteger(0);
	
	public final Set<AbstractChunkPosWrapper> positionsWaitingToBeGenerated = new HashSet<>();
	
	/**
	 * Singleton copy of this object
	 */
	public static final LodWorldGenerator INSTANCE = new LodWorldGenerator();
	
	
	
	private LodWorldGenerator()
	{
		
	}
	
	/**
	 * Queues up LodNodeGenWorkers for the given lodDimension.
	 * @param renderer needed so the LodNodeGenWorkers can flag that the
	 * buffers need to be rebuilt.
	 */
	public void queueGenerationRequests(LodDimension lodDim, LodRenderer renderer, LodBuilder lodBuilder)
	{
		if (CONFIG.client().worldGenerator().getDistanceGenerationMode() != DistanceGenerationMode.NONE
				&& !generatorThreadRunning
				&& MC.hasSinglePlayerServer())
		{
			// the thread is now running, don't queue up another thread
			generatorThreadRunning = true;
			
			// just in case the config changed
			maxChunkGenRequests = CONFIG.client().advanced().threading().getNumberOfWorldGenerationThreads() * 8;
			
			Thread generatorThread = new Thread(() ->
			{
				try
				{
					// round the player's block position down to the nearest chunk BlockPos
					int playerPosX = MC.getPlayerBlockPos().getX();
					int playerPosZ = MC.getPlayerBlockPos().getZ();
					
					
					//=======================================//
					// fill in positionsWaitingToBeGenerated //
					//=======================================//
					
					IWorldWrapper serverWorld = LodUtil.getServerWorldFromDimension(lodDim.dimension);
					
					PosToGenerateContainer posToGenerate = lodDim.getPosToGenerate(
							maxChunkGenRequests,
							playerPosX,
							playerPosZ);
					
					
					byte detailLevel;
					int posX;
					int posZ;
					int nearIndex = 0;
					int farIndex = 0;
					
					for (int i = 0; i < posToGenerate.getNumberOfPos(); i++)
					{
						// I wish there was a way to compress this code, but I'm not aware of 
						// an easy way to do so.
						
						// add the near positions
						if (posToGenerate.getNthDetail(nearIndex, true) != 0 && nearIndex < posToGenerate.getNumberOfNearPos())
						{
							detailLevel = (byte) (posToGenerate.getNthDetail(nearIndex, true) - 1);
							posX = posToGenerate.getNthPosX(nearIndex, true);
							posZ = posToGenerate.getNthPosZ(nearIndex, true);
							nearIndex++;
							
							AbstractChunkPosWrapper chunkPos = WRAPPER_FACTORY.createChunkPos(LevelPosUtil.getChunkPos(detailLevel, posX), LevelPosUtil.getChunkPos(detailLevel, posZ));
							
							// prevent generating the same chunk multiple times
							if (positionsWaitingToBeGenerated.contains(chunkPos))
								continue;
							
							// don't add more to the generation queue then allowed
							if (numberOfChunksWaitingToGenerate.get() >= maxChunkGenRequests)
								break;
							
							positionsWaitingToBeGenerated.add(chunkPos);
							numberOfChunksWaitingToGenerate.addAndGet(1);
							LodGenWorker genWorker = new LodGenWorker(chunkPos, DetailDistanceUtil.getDistanceGenerationMode(detailLevel), lodBuilder, lodDim, serverWorld);
							genWorker.queueWork();
						}
						
						
						// add the far positions
						if (posToGenerate.getNthDetail(farIndex, false) != 0 && farIndex < posToGenerate.getNumberOfFarPos())
						{
							detailLevel = (byte) (posToGenerate.getNthDetail(farIndex, false) - 1);
							posX = posToGenerate.getNthPosX(farIndex, false);
							posZ = posToGenerate.getNthPosZ(farIndex, false);
							farIndex++;
							
							AbstractChunkPosWrapper chunkPos = WRAPPER_FACTORY.createChunkPos(LevelPosUtil.getChunkPos(detailLevel, posX), LevelPosUtil.getChunkPos(detailLevel, posZ));
							
							// don't add more to the generation queue then allowed
							if (numberOfChunksWaitingToGenerate.get() >= maxChunkGenRequests)
								continue;
							//break;
							
							// prevent generating the same chunk multiple times
							if (positionsWaitingToBeGenerated.contains(chunkPos))
								continue;
							
							positionsWaitingToBeGenerated.add(chunkPos);
							numberOfChunksWaitingToGenerate.addAndGet(1);
							LodGenWorker genWorker = new LodGenWorker(chunkPos, DetailDistanceUtil.getDistanceGenerationMode(detailLevel), lodBuilder, lodDim, serverWorld);
							genWorker.queueWork();
						}
					}
					
				}
				catch (Exception e)
				{
					// this shouldn't ever happen, but just in case
					e.printStackTrace();
				}
				finally
				{
					generatorThreadRunning = false;
				}
			});
			
			mainGenThread.execute(generatorThread);
		} // if distanceGenerationMode != DistanceGenerationMode.NONE && !generatorThreadRunning
	} // queueGenerationRequests
	
}
