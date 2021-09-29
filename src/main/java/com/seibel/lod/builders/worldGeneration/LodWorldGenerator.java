package com.seibel.lod.builders.worldGeneration;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.seibel.lod.builders.lodBuilding.LodBuilder;
import com.seibel.lod.config.LodConfig;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.objects.PosToGenerateContainer;
import com.seibel.lod.render.LodRenderer;
import com.seibel.lod.util.DetailDistanceUtil;
import com.seibel.lod.util.LevelPosUtil;
import com.seibel.lod.util.LodThreadFactory;
import com.seibel.lod.util.LodUtil;
import com.seibel.lod.wrappers.MinecraftWrapper;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.WorldWorkerManager;

/**
 * A singleton that handles all long distance LOD world generation.
 *
 * @author James Seibel
 * @version 9-25-2021
 */
public class LodWorldGenerator
{
	public MinecraftWrapper mc = MinecraftWrapper.INSTANCE;
	
	/** This holds the thread used to generate new LODs off the main thread. */
	private ExecutorService mainGenThread = Executors.newSingleThreadExecutor(new LodThreadFactory(this.getClass().getSimpleName() + " world generator"));
	
	/** we only want to queue up one generator thread at a time */
	private boolean generatorThreadRunning = false;
	
	/**
	 * How many chunks to generate outside of the player's view distance at one
	 * time. (or more specifically how many requests to make at one time). I
	 * multiply by 8 to make sure there is always a buffer of chunk requests, to
	 * make sure the CPU is always busy and we can generate LODs as quickly as
	 * possible.
	 */
	public int maxChunkGenRequests;
	
	/**
	 * This keeps track of how many chunk generation requests are on going. This is
	 * to limit how many chunks are queued at once. To prevent chunks from being
	 * generated for a long time in an area the player is no longer in.
	 */
	public AtomicInteger numberOfChunksWaitingToGenerate = new AtomicInteger(0);
	
	public Set<ChunkPos> positionsWaitingToBeGenerated = new HashSet<>();
	
	/**
	 * Singleton copy of this object
	 */
	public static final LodWorldGenerator INSTANCE = new LodWorldGenerator();
	
	
	private LodWorldGenerator()
	{
		
	}
	
	/**
	 * Queues up LodNodeGenWorkers for the given lodDimension.
	 *
	 * @param renderer needed so the LodNodeGenWorkers can flag that the
	 *                 buffers need to be rebuilt.
	 */
	public void queueGenerationRequests(LodDimension lodDim, LodRenderer renderer, LodBuilder lodBuilder)
	{
		if (LodConfig.CLIENT.worldGenerator.distanceGenerationMode.get() != DistanceGenerationMode.NONE
				&& !generatorThreadRunning
				&& mc.hasSingleplayerServer())
		{
			// the thread is now running, don't queue up another thread
			generatorThreadRunning = true;
			
			// just in case the config changed
			maxChunkGenRequests = LodConfig.CLIENT.threading.numberOfWorldGenerationThreads.get() * 8;
			
			Thread generatorThread = new Thread(() ->
			{
				try
				{
					// round the player's block position down to the nearest chunk BlockPos
					int playerPosX = mc.getPlayer().blockPosition().getX();
					int playerPosZ = mc.getPlayer().blockPosition().getZ();
					
					
					//=======================================//
					// fill in positionsWaitingToBeGenerated //
					//=======================================//
					
					ServerWorld serverWorld = LodUtil.getServerWorldFromDimension(lodDim.dimension);
					
					PosToGenerateContainer posToGenerate = lodDim.getDataToGenerate(
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
						// a easy way to do so.
						
						// add the near positions
						if (posToGenerate.getNthDetail(nearIndex, true) != 0 && nearIndex < posToGenerate.getNumberOfNearPos())
						{
							detailLevel = (byte) (posToGenerate.getNthDetail(nearIndex, true) - 1);
							posX = posToGenerate.getNthPosX(nearIndex, true);
							posZ = posToGenerate.getNthPosZ(nearIndex, true);
							nearIndex++;
							
							ChunkPos chunkPos = new ChunkPos(LevelPosUtil.getChunkPos(detailLevel, posX), LevelPosUtil.getChunkPos(detailLevel, posZ));
							if (numberOfChunksWaitingToGenerate.get() < maxChunkGenRequests)
							{
								// prevent generating the same chunk multiple times
								if (positionsWaitingToBeGenerated.contains(chunkPos))
									continue;
							}
							
							// don't add more to the generation queue then allowed
							if (numberOfChunksWaitingToGenerate.get() >= maxChunkGenRequests)
								break;
							
							positionsWaitingToBeGenerated.add(chunkPos);
							numberOfChunksWaitingToGenerate.addAndGet(1);
							LodNodeGenWorker genWorker = new LodNodeGenWorker(chunkPos, DetailDistanceUtil.getDistanceGenerationMode(detailLevel), lodBuilder, lodDim, serverWorld);
							WorldWorkerManager.addWorker(genWorker);
						}
						
						
						// add the far positions
						if (posToGenerate.getNthDetail(farIndex, false) != 0 && farIndex < posToGenerate.getNumberOfFarPos())
						{
							detailLevel = (byte) (posToGenerate.getNthDetail(farIndex, false) - 1);
							posX = posToGenerate.getNthPosX(farIndex, false);
							posZ = posToGenerate.getNthPosZ(farIndex, false);
							farIndex++;
							
							ChunkPos chunkPos = new ChunkPos(LevelPosUtil.getChunkPos(detailLevel, posX), LevelPosUtil.getChunkPos(detailLevel, posZ));
							if (numberOfChunksWaitingToGenerate.get() < maxChunkGenRequests)
							{
								// prevent generating the same chunk multiple times
								if (positionsWaitingToBeGenerated.contains(chunkPos))
									continue;
							}
							
							// don't add more to the generation queue then allowed
							if (numberOfChunksWaitingToGenerate.get() >= maxChunkGenRequests)
								break;
							
							positionsWaitingToBeGenerated.add(chunkPos);
							numberOfChunksWaitingToGenerate.addAndGet(1);
							LodNodeGenWorker genWorker = new LodNodeGenWorker(chunkPos, DetailDistanceUtil.getDistanceGenerationMode(detailLevel), lodBuilder, lodDim, serverWorld);
							WorldWorkerManager.addWorker(genWorker);
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
