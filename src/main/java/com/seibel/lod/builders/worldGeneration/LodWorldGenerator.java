package com.seibel.lod.builders.worldGeneration;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.seibel.lod.builders.GenerationRequest;
import com.seibel.lod.builders.LodBuilder;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.handlers.LodConfig;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.objects.LevelPos.LevelPos;
import com.seibel.lod.render.LodRenderer;
import com.seibel.lod.util.DetailDistanceUtil;
import com.seibel.lod.util.LodThreadFactory;
import com.seibel.lod.util.LodUtil;

import javafx.collections.transformation.SortedList;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.WorldWorkerManager;
import org.apache.commons.lang3.mutable.MutableBoolean;

/**
 * A singleton that handles all long distance LOD world generation.
 *
 * @author James Seibel
 * @version 8-24-2021
 */
public class LodWorldGenerator
{
	public Minecraft mc = Minecraft.getInstance();

	/**
	 * This holds the thread used to generate new LODs off the main thread.
	 */
	private ExecutorService mainGenThread = Executors.newSingleThreadExecutor(new LodThreadFactory(this.getClass().getSimpleName() + " world generator"));

	/**
	 * we only want to queue up one generator thread at a time
	 */
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

	/**
	 * Singleton copy of this object
	 */
	public static final LodWorldGenerator INSTANCE = new LodWorldGenerator();

	public volatile ConcurrentMap<LevelPos, MutableBoolean> nodeToGenerate;

	SortedSet<LevelPos> nodeToGenerateListNear;
	SortedSet<LevelPos> nodeToGenerateListFar;

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
		if (LodConfig.CLIENT.distanceGenerationMode.get() != DistanceGenerationMode.NONE
				    && !generatorThreadRunning
				    && mc.hasSingleplayerServer())
		{
			// the thread is now running, don't queue up another thread
			generatorThreadRunning = true;

			// just in case the config changed
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

					ArrayList<GenerationRequest> chunksToGen = new ArrayList<>(maxChunkGenRequests);
					// if we don't have a full number of chunks to generate in chunksToGen
					// we can top it off from this reserve


					//=======================================//
					// create the generation Request objects //
					//=======================================//
					List<GenerationRequest> generationRequestList = new ArrayList<>(maxChunkGenRequests);

					if (nodeToGenerate == null)
						nodeToGenerate = new ConcurrentHashMap<>();


					Comparator<LevelPos> posNearComparator = LevelPos.getPosComparator(
							playerBlockPosRounded.getX(),
							playerBlockPosRounded.getZ());
					Comparator<LevelPos> posFarComparator = LevelPos.getPosAndDetailComparator(
							playerBlockPosRounded.getX(),
							playerBlockPosRounded.getZ());
					nodeToGenerateListNear = new TreeSet(posNearComparator);
					nodeToGenerateListFar = new TreeSet(posFarComparator);

					lodDim.getDataToGenerate(
							nodeToGenerate,
							playerBlockPosRounded.getX(),
							playerBlockPosRounded.getZ());


					//here we prepare two sorted set
					//the first contains the near pos to render
					//the second contain the far pos to render
					byte farDetail = (byte) 7;
					for (LevelPos pos : nodeToGenerate.keySet())
					{
						if (!nodeToGenerate.get(pos).booleanValue())
						{
							nodeToGenerate.remove(pos);
						} else
						{
							if (pos.detailLevel > farDetail){
								nodeToGenerateListFar.add(pos);
							}
							nodeToGenerateListNear.add(pos);
							nodeToGenerate.get(pos).setFalse();
						}
					}

					int maxDistance;
					byte circle;
					LevelPos levelPos;
					int requesting = maxChunkGenRequests;
					int requestingFar = maxChunkGenRequests / 4;
					while (requesting > 0 && !nodeToGenerateListNear.isEmpty())
					{
						levelPos = nodeToGenerateListNear.first();
						System.out.println(levelPos);
						nodeToGenerate.remove(levelPos);
						nodeToGenerateListNear.remove(levelPos);

						maxDistance = levelPos.maxDistance(	playerBlockPosRounded.getX(), playerBlockPosRounded.getZ());
						circle = DetailDistanceUtil.getDistanceGenerationInverse(maxDistance);
						generationRequestList.add(new GenerationRequest(levelPos, DetailDistanceUtil.getDistanceGenerationMode(circle), DetailDistanceUtil.getLodDetail(circle)));
						requesting--;
						if (requestingFar > 0 && !nodeToGenerateListFar.isEmpty())
						{
							levelPos = nodeToGenerateListFar.first();
							if (levelPos.detailLevel >= farDetail)
							{
								maxDistance = levelPos.maxDistance(	playerBlockPosRounded.getX(), playerBlockPosRounded.getZ());
								circle = DetailDistanceUtil.getDistanceGenerationInverse(maxDistance);
								generationRequestList.add(new GenerationRequest(levelPos, DetailDistanceUtil.getDistanceGenerationMode(circle), DetailDistanceUtil.getLodDetail(circle)));
								requestingFar--;
								requesting--;
							}
						}
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
								continue;
							}
							chunksToGen.add(generationRequest);

						} // lod null and can generate more chunks
					} // positions to generate


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

				} catch (Exception e)
				{
					// this shouldn't ever happen, but just in case
					e.printStackTrace();
				} finally
				{
					generatorThreadRunning = false;
				}
			});

			mainGenThread.execute(generatorThread);
		} // if distanceGenerationMode != DistanceGenerationMode.NONE && !generatorThreadRunning
	}

}
