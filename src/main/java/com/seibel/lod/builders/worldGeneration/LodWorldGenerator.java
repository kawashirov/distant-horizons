package com.seibel.lod.builders.worldGeneration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.seibel.lod.builders.GenerationRequest;
import com.seibel.lod.builders.LodBuilder;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.handlers.LodConfig;
import com.seibel.lod.objects.LevelPos;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.render.LodRenderer;
import com.seibel.lod.util.DetailDistanceUtil;
import com.seibel.lod.util.LodThreadFactory;
import com.seibel.lod.util.LodUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.WorldWorkerManager;

/**
 * A singleton that handles all long distance LOD world generation.
 * 
 * @author James Seibel
 * @version 8-24-2021
 */
public class LodWorldGenerator
{
	public Minecraft mc = Minecraft.getInstance();
	
	/** This holds the thread used to generate new LODs off the main thread. */
	private ExecutorService mainGenThread = Executors.newSingleThreadExecutor(new LodThreadFactory(this.getClass().getSimpleName() + " world generator"));
	
	/** we only want to queue up one generator thread at a time */
	private boolean generatorThreadRunning = false;
	
	/**
	 * how many chunks to generate outside of the player's view distance at one
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
	
	public Set<ChunkPos> positionWaitingToBeGenerated = new HashSet<>();
	
	/** Singleton copy of this object */
	public static final LodWorldGenerator INSTANCE = new LodWorldGenerator();
	
	private LodWorldGenerator()
	{
		
	}
	
	/**
	 * Queues up LodNodeGenWorkers for the given lodDimension.
	 * 
	 * @param renderer needed so the LodNodeGenWorkers can flag that the
	 * 					buffers need to be rebuilt.
	 */
	public void queueGenerationRequests(LodDimension lodDim, LodRenderer renderer, LodBuilder lodBuilder)
	{
		if (LodConfig.CLIENT.distanceGenerationMode.get() != DistanceGenerationMode.NONE 
				&& !generatorThreadRunning
				&& mc.hasSingleplayerServer())
		{
			// the thread is now running, don't queue up another thread
			generatorThreadRunning = true;
			
			// just in case the config is changed
			maxChunkGenRequests = LodConfig.CLIENT.numberOfWorldGenerationThreads.get() * 8;
			
			Thread generatorThread = new Thread(() ->
			{
				try
				{
					// round the player's block position down to the nearest chunk BlockPos
					ChunkPos playerChunkPos = new ChunkPos(mc.player.blockPosition());
					BlockPos playerBlockPosRounded = playerChunkPos.getWorldPosition();
					
					// used when determining which chunks are closer when queuing distance
					// generation
					int minChunkDist = Integer.MAX_VALUE;
					
					List<LevelPos> levelPosListToGen;
					List<GenerationRequest> generationRequestList = new ArrayList<>();
					
					ArrayList<GenerationRequest> chunksToGen = new ArrayList<>(maxChunkGenRequests);
					// if we don't have a full number of chunks to generate in chunksToGen
					// we can top it off from this reserve
					ArrayList<GenerationRequest> chunksToGenReserve = new ArrayList<>(maxChunkGenRequests);
					
					// how many level positions to 
					int requesting = maxChunkGenRequests;
					
					
					/** TODO can give a totally different generation */
					/*
					 * for (byte detail = LodUtil.BLOCK_DETAIL_LEVEL; detail <=
					 * LodUtil.REGION_DETAIL_LEVEL; detail++) { if (!posListToGenerate.isEmpty())
					 * break; for (byte detailGen = LodUtil.BLOCK_DETAIL_LEVEL; detailGen <=
					 * LodUtil.REGION_DETAIL_LEVEL; detailGen++) { if (!posListToGenerate.isEmpty())
					 * break; posListToGenerate.addAll(lodDim.getDataToGenerate(
					 * playerBlockPosRounded.getX(), playerBlockPosRounded.getZ(), (int)
					 * (distancesLinear[detailGen]*1.5), (int) (distancesLinear[detailGen+1]*1.5),
					 * (byte) distancesGenerators[detailGen].complexity, detail, 16));
					 * System.out.println("HERE"); } }
					 */
					
					
					//=======================================//
					// create the generation Request objects //
					//=======================================//
					
					// start by generating half-region sized blocks...
					int farRequesting = 0;

					//we firstly make sure that the world is filled with half region wide block

					for (byte detailGen = LodConfig.CLIENT.maxGenerationDetail.get().detailLevel; detailGen <= LodUtil.REGION_DETAIL_LEVEL; detailGen++)
					{
						if (farRequesting <= maxChunkGenRequests/2) break;
						levelPosListToGen = lodDim.getDataToGenerate(
								playerBlockPosRounded.getX(),
								playerBlockPosRounded.getZ(),
								DetailDistanceUtil.getDistanceGeneration(detailGen),
								DetailDistanceUtil.getDistanceGeneration(detailGen + 1),
								DetailDistanceUtil.getDistanceGenerationMode(detailGen).complexity,
								(byte) 7,
								farRequesting);
						for(LevelPos levelPos : levelPosListToGen){
							generationRequestList.add(new GenerationRequest(levelPos,DetailDistanceUtil.getDistanceGenerationMode(detailGen), DetailDistanceUtil.getLodDetail(detailGen)));
						}
						farRequesting = farRequesting + generationRequestList.size();

					}
					
					// ...then once the world is filled with half-region sized blocks
					// fill in the rest

					int nearRequesting = farRequesting;
					//we then fill the world with the rest of the block
					for (byte detailGen = LodConfig.CLIENT.maxGenerationDetail.get().detailLevel; detailGen <= LodUtil.REGION_DETAIL_LEVEL; detailGen++)
					{
						if (nearRequesting <= maxChunkGenRequests) break;
						levelPosListToGen = lodDim.getDataToGenerate(
								playerBlockPosRounded.getX(),
								playerBlockPosRounded.getZ(),
								DetailDistanceUtil.getDistanceGeneration(detailGen),
								DetailDistanceUtil.getDistanceGeneration(detailGen + 1),
								DetailDistanceUtil.getDistanceGenerationMode(detailGen).complexity,
								DetailDistanceUtil.getLodDetail(detailGen).detailLevel,
								nearRequesting);
						for(LevelPos levelPos : levelPosListToGen){
							generationRequestList.add(new GenerationRequest(levelPos,DetailDistanceUtil.getDistanceGenerationMode(detailGen), DetailDistanceUtil.getLodDetail(detailGen)));
						}
						nearRequesting = nearRequesting + generationRequestList.size();
					}
					
					
					
					//====================================//
					// get the closet generation requests //
					//====================================//
					
					// determine which points in the posListToGenerate
					// should actually be queued to generate
					for (GenerationRequest generationRequest : generationRequestList)
					{
						ChunkPos chunkPos = generationRequest.getChunkPos();
						
						if (numberOfChunksWaitingToGenerate.get() < maxChunkGenRequests)
						{
							// prevent generating the same chunk multiple times
							if (positionWaitingToBeGenerated.contains(chunkPos))
							{
								// ClientProxy.LOGGER.debug(pos + " asked to be generated again.");
								continue;
							}
							
							// determine if this position is closer to the player
							// than the previous
							int newDistance = playerChunkPos.getChessboardDistance(chunkPos);
							
							if (newDistance < minChunkDist)
							{
								// this chunk is closer, clear any previous
								// positions and update the new minimum distance
								minChunkDist = newDistance;
								
								// move all the old chunks into the reserve
								ArrayList<GenerationRequest> oldReserve = new ArrayList<>(chunksToGenReserve);
								chunksToGenReserve.clear();
								chunksToGenReserve.addAll(chunksToGen);
								// top off reserve with whatever was in oldReerve
								for (int i = 0; i < oldReserve.size(); i++)
								{
									if (chunksToGenReserve.size() < maxChunkGenRequests)
										chunksToGenReserve.add(oldReserve.get(i));
									else
										break;
								}
								
								chunksToGen.clear();
								chunksToGen.add(generationRequest);
							}
							else if (newDistance == minChunkDist)
							{
								// this chunk position as close as the minimum distance
								if (chunksToGen.size() < maxChunkGenRequests)
								{
									// we are still under the number of chunks to generate
									// add this position to the list
									chunksToGen.add(generationRequest);
								}
							}
							else
							{
								// this chunk is farther away than the minimum distance,
								// add it to the reserve to make sure we always have a full reserve
								chunksToGenReserve.add(generationRequest);
							}
							
						} // lod null and can generate more chunks
					} // positions to generate
					
					// fill up chunksToGen from the reserve if it isn't full
					// already
					if (chunksToGen.size() < maxChunkGenRequests)
					{
						Iterator<GenerationRequest> reserveIterator = chunksToGenReserve.iterator();
						while (chunksToGen.size() < maxChunkGenRequests && reserveIterator.hasNext())
						{
							chunksToGen.add(reserveIterator.next());
						}
					}
					
					
					
					
					//=============================//
					// start the LodNodeGenWorkers //
					//=============================//
					
					// issue #19
					// TODO add a way for a server side mod to generate chunks requested here
					ServerWorld serverWorld = LodUtil.getServerWorldFromDimension(lodDim.dimension);
					
					// start chunk generation
					for (GenerationRequest generationRequest : generationRequestList)
					{
						// don't add null chunkPos (which shouldn't happen anyway)
						// or add more to the generation queue
						ChunkPos chunkPos = generationRequest.getChunkPos();
						if (chunkPos == null || numberOfChunksWaitingToGenerate.get() >= maxChunkGenRequests)
							continue;
						
						positionWaitingToBeGenerated.add(chunkPos);
						numberOfChunksWaitingToGenerate.addAndGet(1);
						LodNodeGenWorker genWorker = new LodNodeGenWorker(chunkPos, generationRequest.generationMode, generationRequest.detail, renderer, lodBuilder, lodDim, serverWorld);
						WorldWorkerManager.addWorker(genWorker);
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
	}
	
}
