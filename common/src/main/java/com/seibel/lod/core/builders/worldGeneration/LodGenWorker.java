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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;
import com.seibel.lod.core.wrapperInterfaces.worldGeneration.AbstractWorldGeneratorWrapper;

/**
 * This is used to generate a LodChunk at a given ChunkPos.
 * 
 * @author James Seibel
 * @version 11-20-2021
 */
public class LodGenWorker
{
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	private static final IWrapperFactory FACTORY = SingletonHandler.get(IWrapperFactory.class);
	
	public static ExecutorService genThreads = Executors.newFixedThreadPool(CONFIG.client().advanced().threading().getNumberOfWorldGenerationThreads(), new ThreadFactoryBuilder().setNameFormat("Gen-Worker-Thread-%d").build());
	
	private final LodChunkGenThread thread;
	
	
	
	public LodGenWorker(AbstractChunkPosWrapper newPos, DistanceGenerationMode newGenerationMode,
			LodBuilder newLodBuilder,
			LodDimension newLodDimension, IWorldWrapper serverWorld)
	{
		// just a few sanity checks
		if (newPos == null)
			throw new IllegalArgumentException("LodChunkGenWorker must have a non-null ChunkPos");
		
		if (newLodBuilder == null)
			throw new IllegalArgumentException("LodChunkGenThread requires a non-null LodChunkBuilder");
		
		if (newLodDimension == null)
			throw new IllegalArgumentException("LodChunkGenThread requires a non-null LodDimension");
		
		if (serverWorld == null)
			throw new IllegalArgumentException("LodChunkGenThread requires a non-null ServerWorld");
		
		
		
		thread = new LodChunkGenThread(newPos, newGenerationMode,
				newLodBuilder,
				newLodDimension, serverWorld);
	}
	
	public void queueWork()
	{
		if (CONFIG.client().worldGenerator().getDistanceGenerationMode() == DistanceGenerationMode.FULL)
		{
			// if we are using FULL generation there is no reason
			// to queue up a bunch of generation requests,
			// because MC's internal server (as of 1.16.5) only
			// responds with a single thread. And we don't
			// want to cause more lag then necessary or queue up
			// requests that may end up being unneeded.
			thread.run();
		}
		else
		{
			// Every other method can
			// be done asynchronously
			genThreads.execute(thread);
		}
		
		// useful for debugging
//    	ClientProxy.LOGGER.info(thread.lodDim.getNumberOfLods());
//    	ClientProxy.LOGGER.info(genThreads.toString());
	}
	
	
	
	
	private static class LodChunkGenThread implements Runnable
	{
		private AbstractWorldGeneratorWrapper worldGenWrapper; 
		
		public final LodDimension lodDim;
		public final DistanceGenerationMode generationMode;
		
		private final AbstractChunkPosWrapper pos;
		
		public LodChunkGenThread(AbstractChunkPosWrapper newPos, DistanceGenerationMode newGenerationMode,
				LodBuilder newLodBuilder,
				LodDimension newLodDimension, IWorldWrapper worldWrapper)
		{
			worldGenWrapper = FACTORY.createWorldGenerator(newLodBuilder, newLodDimension, worldWrapper);
			
			pos = newPos;
			generationMode = newGenerationMode;
			lodDim = newLodDimension;
		}
		
		@Override
		public void run()
		{
			try
			{
				// only generate LodChunks if they can
				// be added to the current LodDimension
				
				if (lodDim.regionIsInRange(pos.getX() / LodUtil.REGION_WIDTH_IN_CHUNKS, pos.getZ() / LodUtil.REGION_WIDTH_IN_CHUNKS))
				{					
					switch (generationMode)
					{
					case NONE:
						// don't generate
						break;
					case BIOME_ONLY:
					case BIOME_ONLY_SIMULATE_HEIGHT:
						// fastest
						worldGenWrapper.generateBiomesOnly(pos, generationMode);
						break;
					case SURFACE:
						// faster
						worldGenWrapper.generateSurface(pos);
						break;
					case FEATURES:
						// fast
						worldGenWrapper.generateFeatures(pos);
						break;
					case FULL:
						// very slow
						worldGenWrapper.generateFull(pos);
						break;
					}
					

//					boolean dataExistence = lodDim.doesDataExist(new LevelPos((byte) 3, pos.x, pos.z));
//					if (dataExistence)
//						ClientProxy.LOGGER.info(pos.x + " " + pos.z + " Success!");
//					else
//						ClientProxy.LOGGER.info(pos.x + " " + pos.z);

					// shows the pool size, active threads, queued tasks and completed tasks
//					ClientProxy.LOGGER.info(genThreads.toString());

//					long endTime = System.currentTimeMillis();
//					System.out.println(endTime - startTime);
					
				}// if in range
			}
			catch (Exception e)
			{
				ClientApi.LOGGER.error(LodChunkGenThread.class.getSimpleName() + ": ran into an error: " + e.getMessage());
				e.printStackTrace();
			}
			finally
			{
				// decrement how many threads are running
				LodWorldGenerator.INSTANCE.numberOfChunksWaitingToGenerate.addAndGet(-1);
				
				// this position is no longer being generated
				LodWorldGenerator.INSTANCE.positionsWaitingToBeGenerated.remove(pos);
			}
		}// run
		
		
	}
	
	
	/**
	 * Stops the current genThreads if they are running
	 * and then recreates the Executor service. <br><br>
	 * <p>
	 * This is done to clear any outstanding tasks
	 * that may exist after the player leaves their current world.
	 * If this isn't done unfinished tasks may be left in the queue
	 * preventing new LodChunks form being generated.
	 */
	public static void restartExecutorService()
	{
		if (genThreads != null && !genThreads.isShutdown())
		{
			genThreads.shutdownNow();
		}
		genThreads = Executors.newFixedThreadPool(CONFIG.client().advanced().threading().getNumberOfWorldGenerationThreads(), new ThreadFactoryBuilder().setNameFormat("Gen-Worker-Thread-%d").build());
	}
	
}
