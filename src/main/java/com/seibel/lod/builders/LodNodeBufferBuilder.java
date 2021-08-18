/*
 *    This file is part of the LOD Mod, licensed under the GNU GPL v3 License.
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
package com.seibel.lod.builders;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.seibel.lod.objects.LevelPos;
import com.seibel.lod.objects.LodDataPoint;
import com.seibel.lod.objects.LodDimension;
import org.lwjgl.opengl.GL11;

import com.seibel.lod.builders.worldGeneration.LodNodeGenWorker;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.enums.LodDetail;
import com.seibel.lod.handlers.LodConfig;
import com.seibel.lod.objects.RegionPos;
import com.seibel.lod.proxy.ClientProxy;
import com.seibel.lod.render.LodNodeRenderer;
import com.seibel.lod.util.LodThreadFactory;
import com.seibel.lod.util.LodUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.WorldWorkerManager;

/**
 * This object is used to create NearFarBuffer objects.
 * 
 * @author James Seibel
 * @version 8-17-2021
 */
public class LodNodeBufferBuilder
{
	private Minecraft mc;
	
	/** This holds the thread used to generate new LODs off the main thread. */
	private ExecutorService mainGenThread = Executors.newSingleThreadExecutor(new LodThreadFactory(this.getClass().getSimpleName() + " - main"));
	/** This holds the threads used to generate the buffers. */
	private ExecutorService bufferGenThreads = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new LodThreadFactory(this.getClass().getSimpleName() + " - buffer"));
	
	private LodNodeBuilder LodQuadTreeNodeBuilder;
	
	/** The buffers that are used to create LODs using far fog */
	public volatile BufferBuilder[][] buildableBuffers;
	
	/** Used when building new VBOs */
	public volatile VertexBuffer[][] buildableVbos;
	
	/** VBOs that are sent over to the LodNodeRenderer */
	public volatile VertexBuffer[][] drawableVbos;
	
	/** if this is true the LOD buffers are currently being
	 * regenerated. */
	public boolean generatingBuffers = false;
	
	/** if this is true new LOD buffers have been generated
	 * and are waiting to be swapped with the drawable buffers*/
	private boolean switchVbos = false;
	
	/** This keeps track of how many chunk generation requests are on going.
	 * This is to prevent chunks from being generated for a long time in an area
	 * the player is no longer in. */
	public AtomicInteger numberOfChunksWaitingToGenerate = new AtomicInteger(0);
	
	
	
	/** how many chunks to generate outside of the player's
	 * view distance at one time. (or more specifically how
	 * many requests to make at one time).
	 * I multiply by 8 to make sure there is always a buffer of chunk requests,
	 * to make sure the CPU is always busy and we can generate LODs as quickly as
	 * possible. */
	public int maxChunkGenRequests = LodConfig.CLIENT.numberOfWorldGenerationThreads.get() * 8;
	
	
	public LodNodeBufferBuilder(LodNodeBuilder newLodBuilder)
	{
		mc = Minecraft.getInstance();
		LodQuadTreeNodeBuilder = newLodBuilder;
	}
	
	
	private LodDimension previousDimension = null;
	
	
	/**
	 * Create a thread to asynchronously generate LOD buffers
	 * centered around the given camera X and Z.
	 * <br>
	 * This method will write to the drawable near and far buffers.
	 * <br>
	 * After the buildable buffers have been generated they must be
	 * swapped with the drawable buffers in the LodRenderer to be drawn.
	 */
	public void generateLodBuffersAsync(LodNodeRenderer renderer, LodDimension lodDim,
			BlockPos playerBlockPos, int numbChunksWide)
	{
		// only allow one generation process to happen at a time
		if (generatingBuffers)
			return;
		
		if (buildableBuffers == null)
			throw new IllegalStateException("\"generateLodBuffersAsync\" was called before the \"setupBuffers\" method was called.");
		
		if (previousDimension != lodDim)
		{
			previousDimension = lodDim;
		}
		
		
		// TODO
		maxChunkGenRequests = LodConfig.CLIENT.numberOfWorldGenerationThreads.get() * 8;
		
		
		generatingBuffers = true;
		
		
		// round the player's block position down to the nearest chunk BlockPos
		ChunkPos playerChunkPos = new ChunkPos(playerBlockPos);
		BlockPos playerBlockPosRounded = playerChunkPos.getWorldPosition();
		
		// this is where we will start drawing squares
		// (exactly half the total width)
		BlockPos startBlockPos = new BlockPos(-(numbChunksWide * 16 / 2) + playerBlockPosRounded.getX(), 0, -(numbChunksWide * 16 / 2) + playerBlockPosRounded.getZ());
		ChunkPos startChunkPos = new ChunkPos(startBlockPos);
		
		
		Thread thread = new Thread(() ->
		{
			try
			{
				long startTime = System.currentTimeMillis();
				
				// index of the chunk currently being added to the
				// generation list
				int chunkGenIndex = 0;
				
				ChunkPos[] chunksToGen = new ChunkPos[maxChunkGenRequests];
				// if we don't have a full number of chunks to generate in chunksToGen
				// we can top it off from the reserve
				ChunkPos[] chunksToGenReserve = new ChunkPos[maxChunkGenRequests];
				
				// Used when determining what detail level to use at what distance
				int maxBlockDistance = (numbChunksWide / 2) * 16;
				
				startBuffers();
				
				// used when determining which chunks are closer when queuing distance
				// generation
				int minChunkDist = Integer.MAX_VALUE;
				
				// x axis
				for (int i = 0; i < numbChunksWide; i++)
				{
					// z axis
					for (int j = 0; j < numbChunksWide; j++)
					{
						int chunkX = i + startChunkPos.x;
						int chunkZ = j + startChunkPos.z;
						
						// skip any chunks that Minecraft is going to render
						if (isCoordInCenterArea(i, j, (numbChunksWide / 2))
								&& renderer.vanillaRenderedChunks.contains(new ChunkPos(chunkX, chunkZ)))
						{
							continue;
						}
						
						// set where this square will be drawn in the world
						double xOffset = (LodUtil.CHUNK_WIDTH * i) + // offset by the number of LOD blocks
								startBlockPos.getX(); // offset so the center LOD block is centered underneath the player
						double yOffset = 0;
						double zOffset = (LodUtil.CHUNK_WIDTH * j) + startBlockPos.getZ();

						if (!lodDim.doesDataExist(new LevelPos((byte) LodUtil.CHUNK_DETAIL_LEVEL, chunkX, chunkZ)))
						{
							// generate a new chunk if no chunk currently exists
							// and we aren't waiting on any other chunks to generate
							if (numberOfChunksWaitingToGenerate.get() < maxChunkGenRequests)
							{
								ChunkPos pos = new ChunkPos(chunkX, chunkZ);
								
								// alternate determining logic that
								// can be used for debugging
//								if (chunksToGen == null)
//								{
//									chunkGenIndex = 0;
//									chunksToGen = new ChunkPos[maxChunkGenRequests];
//								}
//								
//								if (chunkGenIndex < maxChunkGenRequests)
//								{
//									chunksToGen[chunkGenIndex] = pos;
//									chunkGenIndex++;
//								}
								
								// determine if this position is closer to the player
								// than the previous
								int newDistance = playerChunkPos.getChessboardDistance(pos);
								
								// issue #40
								// TODO optimize this code,
								// using the purely optimized code above we can achieve close to
								// 100% CPU utilization, this code generally achieves 40 - 50%
								// after a certain point; and I'm sure there is a better data
								// structure for this.
								if (newDistance < minChunkDist)
								{
									// this chunk is closer, clear any previous
									// positions and update the new minimum distance
									minChunkDist = newDistance;
									
									// move all the old chunks into the reserve
									ChunkPos[] newReserve = new ChunkPos[maxChunkGenRequests];
									int oldToGenIndex = 0;
									int oldReserveIndex = 0;
									for (int tmpIndex = 0; tmpIndex < newReserve.length; tmpIndex++)
									{
										// we don't check if the boundaries are good since
										// the tmp array will always be the same length
										// as chunksToGen and chunksToGenReserve
										
										if (chunksToGen[oldToGenIndex] != null)
										{
											// add all the closest chunks...
											newReserve[tmpIndex] = chunksToGen[oldToGenIndex];
											oldToGenIndex++;
										}
										else if (chunksToGenReserve[oldReserveIndex] != null)
										{
											// ...then add all the previous reserve chunks
											// (which are farther away)
											newReserve[tmpIndex] = chunksToGenReserve[oldToGenIndex];
											oldReserveIndex++;
										}
										else
										{
											// we have moved all the items from
											// the old chunksToGen and reserve
											break;
										}
									}
									chunksToGenReserve = newReserve;
									
									chunkGenIndex = 0;
									chunksToGen = new ChunkPos[maxChunkGenRequests];
									chunksToGen[chunkGenIndex] = pos;
									chunkGenIndex++;
								}
								else if (newDistance <= minChunkDist)
								{
									// this chunk position is as close or closers than the
									// minimum distance
									if (chunkGenIndex < maxChunkGenRequests)
									{
										// we are still under the number of chunks to generate
										// add this position to the list
										chunksToGen[chunkGenIndex] = pos;
										chunkGenIndex++;
									}
								}
								
							} // lod null and can generate more chunks
							
							// don't render this null/empty chunk
							continue;
							
						} // lod null or empty
						
						BufferBuilder currentBuffer = null;
						try
						{
							// local position in the vbo and bufferBuilder arrays
							RegionPos regionArrayPos = new RegionPos(new ChunkPos(i, j));
							currentBuffer = buildableBuffers[regionArrayPos.x][regionArrayPos.z];
						}
						catch(Exception e)
						{
							e.printStackTrace();
							continue;
						}
						
						
						// determine detail level should this LOD be drawn at
						int distance = (int) Math.sqrt(Math.pow((playerBlockPosRounded.getX() - chunkX*16 + 8), 2) + Math.pow((playerBlockPosRounded.getZ() - chunkZ*16 + 8), 2));
						int posX;
						int posZ;
						LevelPos levelPos;
						LodDataPoint lodData;
						LodDetail detail = LodDetail.getDetailForDistance(LodConfig.CLIENT.maxDrawDetail.get(), distance, maxBlockDistance);

						for (int k = 0; k < detail.dataPointLengthCount * detail.dataPointLengthCount; k++)
						{
							// how much to offset this LOD by
							posX = (int) (xOffset + detail.startX[k]);
							posZ = (int) (zOffset + detail.startZ[k]);
							levelPos = new LevelPos((byte) 0, posX, posZ).convert((byte) detail.detailLevel);
							if (lodDim.hasThisPositionBeenGenerated(levelPos)) {
								lodData = lodDim.getData(levelPos);
							}else {
								lodData = lodDim.getData(levelPos);
							}
							// get the desired LodTemplate and
							// add this LOD to the buffer
							LodConfig.CLIENT.lodTemplate.get().
									template.addLodToBuffer(currentBuffer, lodDim, lodData,
									posX, yOffset, posZ, renderer.debugging, detail);

						}

					}
				}
				
				// issue #19
				// TODO add a way for a server side mod to generate chunks requested here
				if (mc.hasSingleplayerServer())
				{
					ServerWorld serverWorld = LodUtil.getServerWorldFromDimension(lodDim.dimension);
					
					// make sure we have as many chunks to generate as we are allowed
					if (chunkGenIndex < maxChunkGenRequests)
					{
						for (int i = chunkGenIndex, j = 0; i < maxChunkGenRequests; i++, j++)
						{
							chunksToGen[i] = chunksToGenReserve[j];
						}
					}
					
					// start chunk generation
					for (ChunkPos chunkPos : chunksToGen)
					{
						// don't add null chunkPos (which shouldn't happen anyway)
						// or add more to the generation queue
						if (chunkPos == null || numberOfChunksWaitingToGenerate.get() >= maxChunkGenRequests)
							break;
						
						// TODO add a list of locations we are waiting to generate so we don't add the
						// same position to the queue multiple times
						
						numberOfChunksWaitingToGenerate.addAndGet(1);
						
						LodNodeGenWorker genWorker = new LodNodeGenWorker(chunkPos, renderer, LodQuadTreeNodeBuilder, this, lodDim, serverWorld);
						WorldWorkerManager.addWorker(genWorker);
					}
				}
				
				// finish the buffer building
				closeBuffers();
				
				// upload the new buffers
				uploadBuffers();
				
				
				long endTime = System.currentTimeMillis();
				long buildTime = endTime - startTime;
				ClientProxy.LOGGER.info("Buffer Build time: " + buildTime + " ms");
				
				// mark that the buildable buffers as ready to swap
				switchVbos = true;
			}
			catch (Exception e)
			{
				ClientProxy.LOGGER.warn("\"LodNodeBufferBuilder.generateLodBuffersAsync\" ran into trouble: ");
				e.printStackTrace();
			}
			finally
			{
				// regardless of if we successfully created the buffers
				// we are done generating.
				generatingBuffers = false;
				
				
				// clean up any potentially open resources
				if (buildableBuffers != null)
					closeBuffers();
			}
			
		});
		
		mainGenThread.execute(thread);
		
		return;
	}
	
	
	
	
	
	
	
	
	
	
	//====================//
	// generation helpers //
	//====================//
	
	/**
	 * Returns if the given coordinate is in the loaded area of the world.
	 * @param centerCoordinate the center of the loaded world
	 */
	private boolean isCoordInCenterArea(int i, int j, int centerCoordinate)
	{
		return (i >= centerCoordinate - mc.options.renderDistance
				&& i <= centerCoordinate + mc.options.renderDistance) 
				&& 
				(j >= centerCoordinate - mc.options.renderDistance
				&& j <= centerCoordinate + mc.options.renderDistance);
	}
	
	
	
	
	
	//===============================//
	// BufferBuilder related methods //
	//===============================//
	
	
	/**
	 * Called from the LodRenderer to create the
	 * BufferBuilders.
	 */
	public void setupBuffers(int numbRegionsWide, int bufferMaxCapacity)
	{
		buildableBuffers = new BufferBuilder[numbRegionsWide][numbRegionsWide];
		
		buildableVbos = new VertexBuffer[numbRegionsWide][numbRegionsWide];
		drawableVbos = new VertexBuffer[numbRegionsWide][numbRegionsWide];
		
		for (int x = 0; x < numbRegionsWide; x++)
		{
			for (int z = 0; z < numbRegionsWide; z++)
			{
				buildableBuffers[x][z] = new BufferBuilder(bufferMaxCapacity);
				buildableVbos[x][z] = new VertexBuffer(LodNodeRenderer.LOD_VERTEX_FORMAT);
				drawableVbos[x][z] = new VertexBuffer(LodNodeRenderer.LOD_VERTEX_FORMAT);
			}
		}
	}
	
	/**
	 * Calls begin on each of the buildable BufferBuilders.
	 */
	public void startBuffers()
	{
		for (int x = 0; x < buildableBuffers.length; x++)
			for (int z = 0; z < buildableBuffers.length; z++)
				buildableBuffers[x][z].begin(GL11.GL_QUADS, LodNodeRenderer.LOD_VERTEX_FORMAT);
	}
	
	/**
	 * Calls end on each of the buildable BufferBuilders.
	 */
	public void closeBuffers()
	{
		for (int x = 0; x < buildableBuffers.length; x++)
			for (int z = 0; z < buildableBuffers.length; z++)
				if (buildableBuffers[x][z].building())
					buildableBuffers[x][z].end();
	}
	
	/**
	 * Called from the LodRenderer to create the
	 * BufferBuilders at the right size.
	 * 
	 * @param bufferMaxCapacity
	 */
	public void uploadBuffers()
	{
		for (int x = 0; x < buildableVbos.length; x++)
		{
			for (int z = 0; z < buildableVbos.length; z++)
			{
				buildableVbos[x][z].upload(buildableBuffers[x][z]);
			}
		}
	}
	
	
	/**
	 * Get the newly created VBOs
	 */
	public VertexBuffer[][] getVertexBuffers()
	{
		VertexBuffer[][] tmp = drawableVbos;
		drawableVbos = buildableVbos;
		buildableVbos = tmp;
		
		// the vbos have been swapped
		switchVbos = false;
		
		return drawableVbos;
	}
	
	/**
	 * If this is true the buildable near and far
	 * buffers have been generated and are ready to be
	 * sent to the LodRenderer. 
	 */
	public boolean newBuffersAvaliable() 
	{
		return switchVbos;
	}
	
	
	
	
}