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

package com.seibel.lod.builders.bufferBuilding;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL45;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.seibel.lod.builders.bufferBuilding.lodTemplates.Box;
import com.seibel.lod.config.LodConfig;
import com.seibel.lod.enums.GlProxyContext;
import com.seibel.lod.enums.GpuUploadMethod;
import com.seibel.lod.enums.VanillaOverdraw;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.objects.LodRegion;
import com.seibel.lod.objects.PosToRenderContainer;
import com.seibel.lod.objects.RegionPos;
import com.seibel.lod.proxy.ClientProxy;
import com.seibel.lod.proxy.GlProxy;
import com.seibel.lod.render.LodRenderer;
import com.seibel.lod.util.DataPointUtil;
import com.seibel.lod.util.DetailDistanceUtil;
import com.seibel.lod.util.LevelPosUtil;
import com.seibel.lod.util.LodThreadFactory;
import com.seibel.lod.util.LodUtil;
import com.seibel.lod.util.ThreadMapUtil;
import com.seibel.lod.wrappers.MinecraftWrapper;
import com.seibel.lod.wrappers.Block.BlockPosWrapper;
import com.seibel.lod.wrappers.Chunk.ChunkPosWrapper;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LightLayer;

/**
 * This object is used to create NearFarBuffer objects.
 * @author James Seibel
 * @version 10-23-2021
 */
public class LodBufferBuilder
{
	private static final MinecraftWrapper mc = MinecraftWrapper.INSTANCE;
	
	/** The thread used to generate new LODs off the main thread. */
	public static final ExecutorService mainGenThread = Executors.newSingleThreadExecutor(new LodThreadFactory(LodBufferBuilder.class.getSimpleName() + " - main"));
	/** The threads used to generate buffers. */
	public static final ExecutorService bufferBuilderThreads = Executors.newFixedThreadPool(LodConfig.CLIENT.advancedModOptions.threading.numberOfBufferBuilderThreads.get(), new ThreadFactoryBuilder().setNameFormat("Buffer-Builder-%d").build());
	
	/**
	 * When uploading to a buffer that is too small,
	 * recreate it this many times bigger than the upload payload
	 */
	public static final double BUFFER_EXPANSION_MULTIPLIER = 1.5;
	
	/**
	 * When buffers are first created they are allocated to this size (in Bytes).
	 * This size will be too small, more than likely. The buffers will be expanded
	 * when need be to fit the larger sizes.
	 */
	public static final int DEFAULT_MEMORY_ALLOCATION = 1024;
	
	// TODO this should be moved to LodUtil
	public static int skyLightPlayer = 15;
	
	/**
	 * How many buffers there are for the given region. <Br>
	 * This is done because some regions may require more memory than
	 * can be directly allocated, so we split the regions into smaller sections. <Br>
	 * This keeps track of those sections.
	 */
	public volatile int[][] numberOfBuffersPerRegion;
	
	/** Stores the vertices when building the VBOs */
	public volatile BufferBuilder[][][] buildableBuffers;
	
	/** The OpenGL IDs of the storage buffers used by the buildableVbos */
	public int[][][] buildableStorageBufferIds;
	/** The OpenGL IDs of the storage buffers used by the drawableVbos */
	public int[][][] drawableStorageBufferIds;
	
	/** Used when building new VBOs */
	public volatile VertexBuffer[][][] buildableVbos;
	/** VBOs that are sent over to the LodNodeRenderer */
	public volatile VertexBuffer[][][] drawableVbos;
	
	/**
	 * if this is true the LOD buffers are currently being
	 * regenerated.
	 */
	public boolean generatingBuffers = false;
	
	/**
	 * if this is true new LOD buffers have been generated
	 * and are waiting to be swapped with the drawable buffers
	 */
	private boolean switchVbos = false;
	
	/** Size of the buffer builders in bytes last time we created them */
	public int previousBufferSize = 0;
	
	/** Width of the dimension in regions last time we created the buffers */
	public int previousRegionWidth = 0;
	
	/** this is used to prevent multiple threads creating, destroying, or using the buffers at the same time */
	private final ReentrantLock bufferLock = new ReentrantLock();
	
	private volatile Box[][] boxCache;
	private volatile PosToRenderContainer[][] setsToRender;
	private volatile RegionPos center;
	
	/**
	 * This is the ChunkPosWrapper the player was at the last time the buffers were built.
	 * IE the center of the buffers last time they were built
	 */
	private volatile ChunkPosWrapper drawableCenterChunkPos = new ChunkPosWrapper(0, 0);
	private volatile ChunkPosWrapper buildableCenterChunkPos = new ChunkPosWrapper(0, 0);
	
	
	
	
	
	
	public LodBufferBuilder()
	{
		
	}
	
	/**
	 * Create a thread to asynchronously generate LOD buffers
	 * centered around the given camera X and Z.
	 * <br>
	 * This method will write to the drawable near and far buffers.
	 * <br>
	 * After the buildable buffers have been generated they must be
	 * swapped with the drawable buffers in the LodRenderer to be drawn.
	 */
	public void generateLodBuffersAsync(LodRenderer renderer, LodDimension lodDim,
			BlockPosWrapper playerBlockPos, boolean fullRegen)
	{
		
		// only allow one generation process to happen at a time
		if (generatingBuffers)
			return;
		
		if (buildableBuffers == null)
			// setupBuffers hasn't been called yet
			return;
		
		generatingBuffers = true;
		
		
		Thread thread = new Thread(() -> generateLodBuffersThread(renderer, lodDim, playerBlockPos, fullRegen));
		
		mainGenThread.execute(thread);
	}
	
	// this was pulled out as a separate method so that it could be
	// more easily edited by hot swapping. Because, As far as James is aware
	// you can't hot swap lambda expressions.
	private void generateLodBuffersThread(LodRenderer renderer, LodDimension lodDim,
			BlockPosWrapper playerBlockPos, boolean fullRegen)
	{
		bufferLock.lock();
		
		try
		{
			// round the player's block position down to the nearest chunk BlockPos
			ChunkPosWrapper playerChunkPos = new ChunkPosWrapper(playerBlockPos);
			BlockPosWrapper playerBlockPosRounded = playerChunkPos.getWorldPosition();
			
			
			long startTime = System.currentTimeMillis();
			
			ArrayList<Callable<Boolean>> nodeToRenderThreads = new ArrayList<>(lodDim.getWidth() * lodDim.getWidth());
			
			startBuffers(fullRegen, lodDim);
			
			
			RegionPos playerRegionPos = new RegionPos(playerChunkPos);
			if (center == null)
				center = playerRegionPos;
			
			if (setsToRender == null)
				setsToRender = new PosToRenderContainer[lodDim.getWidth()][lodDim.getWidth()];
			
			if (setsToRender.length != lodDim.getWidth())
				setsToRender = new PosToRenderContainer[lodDim.getWidth()][lodDim.getWidth()];
			
			if (boxCache == null)
				boxCache = new Box[lodDim.getWidth()][lodDim.getWidth()];
			
			if (boxCache.length != lodDim.getWidth())
				boxCache = new Box[lodDim.getWidth()][lodDim.getWidth()];
			
			// this will be the center of the VBOs once they have been built
			buildableCenterChunkPos = playerChunkPos;
			
			
			//================================//
			// create the nodeToRenderThreads //
			//================================//
			
			ClientLevel world = mc.getClientLevel();
			skyLightPlayer = world.getBrightness(LightLayer.SKY, playerBlockPos.getBlockPos());
			
			for (int xRegion = 0; xRegion < lodDim.getWidth(); xRegion++)
			{
				for (int zRegion = 0; zRegion < lodDim.getWidth(); zRegion++)
				{
					if (lodDim.doesRegionNeedBufferRegen(xRegion, zRegion) || fullRegen)
					{
						RegionPos regionPos = new RegionPos(
								xRegion + lodDim.getCenterRegionPosX() - lodDim.getWidth() / 2,
								zRegion + lodDim.getCenterRegionPosZ() - lodDim.getWidth() / 2);
						
						// local position in the vbo and bufferBuilder arrays
						BufferBuilder[] currentBuffers = buildableBuffers[xRegion][zRegion];
						LodRegion region = lodDim.getRegion(regionPos.x, regionPos.z);
						
						if (region == null)
							continue;
						
						// make sure the buffers weren't
						// changed while we were running this method
						if (currentBuffers == null || !currentBuffers[0].building())
							return;
						
						byte minDetail = region.getMinDetailLevel();
						
						
						final int xR = xRegion;
						final int zR = zRegion;
						
						//we create the Callable to use for the buffer builder creation
						Callable<Boolean> dataToRenderThread = () ->
						{
							//Variable initialization
							byte detailLevel;
							int posX;
							int posZ;
							int xAdj;
							int zAdj;
							int bufferIndex;
							boolean posNotInPlayerChunk;
							boolean adjPosInPlayerChunk;
							Box box = ThreadMapUtil.getBox();
							boolean[] adjShadeDisabled = ThreadMapUtil.getAdjShadeDisabledArray();
							
							// determine how many LODs we can stack vertically
							int maxVerticalData = DetailDistanceUtil.getMaxVerticalData((byte) 0);
							
							//we get or create the map that will contain the adj data
							Map<Direction, long[]> adjData = ThreadMapUtil.getAdjDataArray(maxVerticalData);
							
							//previous setToRender cache
							if (setsToRender[xR][zR] == null)
								setsToRender[xR][zR] = new PosToRenderContainer(minDetail, regionPos.x, regionPos.z);
							
							
							//We ask the lod dimension which block we have to render given the player position
							PosToRenderContainer posToRender = setsToRender[xR][zR];
							posToRender.clear(minDetail, regionPos.x, regionPos.z);
							
							lodDim.getPosToRender(
									posToRender,
									regionPos,
									playerBlockPosRounded.getX(),
									playerBlockPosRounded.getZ());
							
							
							
							// keep a local version, so we don't have to worry about indexOutOfBounds Exceptions
							// if it changes in the LodRenderer while we are working here
							boolean[][] vanillaRenderedChunks = renderer.vanillaRenderedChunks;
							short gameChunkRenderDistance = (short) (vanillaRenderedChunks.length / 2 - 1);
							
							
							
							for (int index = 0; index < posToRender.getNumberOfPos(); index++)
							{
								bufferIndex = index % currentBuffers.length;
								detailLevel = posToRender.getNthDetailLevel(index);
								posX = posToRender.getNthPosX(index);
								posZ = posToRender.getNthPosZ(index);
								
								int chunkXdist = LevelPosUtil.getChunkPos(detailLevel, posX) - playerChunkPos.getX();
								int chunkZdist = LevelPosUtil.getChunkPos(detailLevel, posZ) - playerChunkPos.getZ();
								
								//We don't want to render this fake block if
								//The block is inside the render distance with, is not bigger than a chunk and is positioned in a chunk set as vanilla rendered
								//
								//The block is in the player chunk or in a chunk adjacent to the player
								if(isThisPositionGoingToBeRendered(detailLevel, posX, posZ, playerChunkPos, vanillaRenderedChunks, gameChunkRenderDistance))
								{
									continue;
								}
								
								//we check if the block to render is not in player chunk
								posNotInPlayerChunk = !(chunkXdist == 0 && chunkZdist == 0);
										
																					// We extract the adj data in the four cardinal direction
								
								// we first reset the adjShadeDisabled. This is used to disable the shade on the border when we have transparent block like water or glass
								// to avoid having a "darker border" underground
								Arrays.fill(adjShadeDisabled, false);
								
								//We check every adj block in each direction
								for (Direction direction : Box.ADJ_DIRECTIONS)
								{
									
									xAdj = posX + Box.DIRECTION_NORMAL_MAP.get(direction).getX();
									zAdj = posZ + Box.DIRECTION_NORMAL_MAP.get(direction).getZ();
									long data;
									chunkXdist = LevelPosUtil.getChunkPos(detailLevel, xAdj) - playerChunkPos.getX();
									chunkZdist = LevelPosUtil.getChunkPos(detailLevel, zAdj) - playerChunkPos.getZ();
									adjPosInPlayerChunk = (chunkXdist == 0 && chunkZdist == 0);
									
									//If the adj block is rendered in the same region and with same detail
									// and is positioned in a place that is not going to be rendered by vanilla game
									// then we can set this position as adj
									// We avoid cases where the adjPosition is in player chunk while the position is not
									// to always have a wall underwater
									if(posToRender.contains(detailLevel, xAdj, zAdj)
										&& !isThisPositionGoingToBeRendered(detailLevel, xAdj, zAdj, playerChunkPos, vanillaRenderedChunks, gameChunkRenderDistance)
										&& !(posNotInPlayerChunk && adjPosInPlayerChunk))
									{
										for (int verticalIndex = 0; verticalIndex < lodDim.getMaxVerticalData(detailLevel, xAdj, zAdj); verticalIndex++)
										{
											data = lodDim.getData(detailLevel, xAdj, zAdj, verticalIndex);
											adjShadeDisabled[Box.DIRECTION_INDEX.get(direction)] = false;
											adjData.get(direction)[verticalIndex] = data;
										}
									}
									else
									{
										//Other wise we check if this position is
										data = lodDim.getSingleData(detailLevel, xAdj, zAdj);
										
										adjData.get(direction)[0] = DataPointUtil.EMPTY_DATA;
										
										if ((isThisPositionGoingToBeRendered(detailLevel, xAdj, zAdj, playerChunkPos, vanillaRenderedChunks, gameChunkRenderDistance) || (posNotInPlayerChunk && adjPosInPlayerChunk))
													&& !DataPointUtil.isVoid(data))
										{
											adjShadeDisabled[Box.DIRECTION_INDEX.get(direction)] = DataPointUtil.getAlpha(data) < 255;
										}
									}
								}
								
								
								// We render every vertical lod present in this position
								// We only stop when we find a block that is void or non existing block
								long data;
								for (int verticalIndex = 0; verticalIndex < lodDim.getMaxVerticalData(detailLevel, posX, posZ); verticalIndex++)
								{
									
									//we get the above block as adj UP
									if (verticalIndex > 0)
									{
										adjData.get(Direction.UP)[0] = lodDim.getData(detailLevel, posX, posZ, verticalIndex - 1);
									}
									else
									{
										adjData.get(Direction.UP)[0] = DataPointUtil.EMPTY_DATA;
									}
									
									
									//we get the below block as adj DOWN
									if (verticalIndex < lodDim.getMaxVerticalData(detailLevel, posX, posZ) - 1)
									{
										adjData.get(Direction.DOWN)[0] = lodDim.getData(detailLevel, posX, posZ, verticalIndex + 1);
									}
									else
									{
										adjData.get(Direction.DOWN)[0] = DataPointUtil.EMPTY_DATA;
									}
									
									//We extract the data to render
									data = lodDim.getData(detailLevel, posX, posZ, verticalIndex);
									
									//If the data is not renderable (Void or non existing) we stop since there is no data left in this position
									if (DataPointUtil.isVoid(data) || !DataPointUtil.doesItExist(data))
										break;
									
									//We send the call to create the vertices
									LodConfig.CLIENT.graphics.advancedGraphicsOption.lodTemplate.get().template.addLodToBuffer(currentBuffers[bufferIndex], playerBlockPosRounded, data, adjData,
											detailLevel, posX, posZ, box, renderer.previousDebugMode, adjShadeDisabled);
								}
								
								
							} // for pos to in list to render
							// the thread executed successfully
							return true;
						};
						
						
						nodeToRenderThreads.add(dataToRenderThread);
						
						
					}
				} // region z
			} // region z
			
			
			long executeStart = System.currentTimeMillis();
			// wait for all threads to finish
			List<Future<Boolean>> futuresBuffer = bufferBuilderThreads.invokeAll(nodeToRenderThreads);
			for (Future<Boolean> future : futuresBuffer)
			{
				// the future will be false if its thread failed
				if (!future.get())
				{
					ClientProxy.LOGGER.warn("LodBufferBuilder ran into trouble and had to start over.");
					break;
				}
			}
			long executeEnd = System.currentTimeMillis();
			
			
			long endTime = System.currentTimeMillis();
			@SuppressWarnings("unused")
			long buildTime = endTime - startTime;
			@SuppressWarnings("unused")
			long executeTime = executeEnd - executeStart;

//			ClientProxy.LOGGER.info("Thread Build time: " + buildTime + " ms" + '\n' +
//					                        "thread execute time: " + executeTime + " ms");
			
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
			// clean up any potentially open resources
			if (buildableBuffers != null)
				closeBuffers(fullRegen, lodDim);
			
			try
			{
				// upload the new buffers
				uploadBuffers(fullRegen, lodDim);
			}
			catch (Exception e)
			{
				ClientProxy.LOGGER.warn("\"LodNodeBufferBuilder.generateLodBuffersAsync\" was unable to upload the buffers to the GPU: " + e.getMessage());
				e.printStackTrace();
			}
			
			// regardless of whether we were able to successfully create
			// the buffers, we are done generating.
			generatingBuffers = false;
			bufferLock.unlock();
		}
	}
	
	private boolean isThisPositionGoingToBeRendered(byte detailLevel, int posX, int posZ, ChunkPosWrapper playerChunkPos, boolean[][] vanillaRenderedChunks, int gameChunkRenderDistance){
		
		
		// skip any chunks that Minecraft is going to render
		int chunkXdist = LevelPosUtil.getChunkPos(detailLevel, posX) - playerChunkPos.getX();
		int chunkZdist = LevelPosUtil.getChunkPos(detailLevel, posZ) - playerChunkPos.getZ();
		
		// check if the chunk is on the border
		boolean isItBorderPos;
		if (LodConfig.CLIENT.graphics.advancedGraphicsOption.vanillaOverdraw.get() == VanillaOverdraw.BORDER)
			isItBorderPos = LodUtil.isBorderChunk(vanillaRenderedChunks, chunkXdist + gameChunkRenderDistance + 1, chunkZdist + gameChunkRenderDistance + 1);
		else
			isItBorderPos = false;
		
		
		//boolean smallRenderDistance = gameChunkRenderDistance <= LodUtil.MINIMUM_RENDER_DISTANCE_FOR_PARTIAL_OVERDRAW;
		
		// get the positions that will be rendered
		
		boolean vanillaRenderedPosition = gameChunkRenderDistance >= Math.abs(chunkXdist)
												  && gameChunkRenderDistance >= Math.abs(chunkZdist)
												  && detailLevel <= LodUtil.CHUNK_DETAIL_LEVEL
												  && vanillaRenderedChunks[chunkXdist + gameChunkRenderDistance + 1][chunkZdist + gameChunkRenderDistance + 1];
		
		
		return (vanillaRenderedPosition && (!(isItBorderPos)));
	}
	
	
	
	
	
	
	//===============================//
	// BufferBuilder related methods //
	//===============================//
	
	/**
	 * Called from the LodRenderer to create the
	 * BufferBuilders. <br><br>
	 * <p>
	 * May have to wait for the bufferLock to open.
	 */
	public void setupBuffers(LodDimension lodDimension)
	{
		GlProxy glProxy = GlProxy.getInstance();
		
		bufferLock.lock();
		int numbRegionsWide = lodDimension.getWidth();
		long regionMemoryRequired;
		int numberOfBuffers;
		
		previousRegionWidth = numbRegionsWide;
		numberOfBuffersPerRegion = new int[numbRegionsWide][numbRegionsWide];
		buildableBuffers = new BufferBuilder[numbRegionsWide][numbRegionsWide][];
		
		buildableVbos = new VertexBuffer[numbRegionsWide][numbRegionsWide][];
		drawableVbos = new VertexBuffer[numbRegionsWide][numbRegionsWide][];
		
		if (glProxy.bufferStorageSupported)
		{
			buildableStorageBufferIds = new int[numbRegionsWide][numbRegionsWide][];
			drawableStorageBufferIds = new int[numbRegionsWide][numbRegionsWide][];
		}
		
		for (int x = 0; x < numbRegionsWide; x++)
		{
			for (int z = 0; z < numbRegionsWide; z++)
			{
				regionMemoryRequired = DEFAULT_MEMORY_ALLOCATION;
				
				// if the memory required is greater than the max buffer 
				// capacity, divide the memory across multiple buffers
				if (regionMemoryRequired > LodUtil.MAX_ALLOCATABLE_DIRECT_MEMORY)
				{
					numberOfBuffers = (int) Math.ceil(regionMemoryRequired / LodUtil.MAX_ALLOCATABLE_DIRECT_MEMORY) + 1;
					
					// TODO shouldn't this be determined with regionMemoryRequired?
					// always allocating the max memory is a bit expensive isn't it?
					regionMemoryRequired = LodUtil.MAX_ALLOCATABLE_DIRECT_MEMORY;
					numberOfBuffersPerRegion[x][z] = numberOfBuffers;
					buildableBuffers[x][z] = new BufferBuilder[numberOfBuffers];
					buildableVbos[x][z] = new VertexBuffer[numberOfBuffers];
					drawableVbos[x][z] = new VertexBuffer[numberOfBuffers];
					
					if (glProxy.bufferStorageSupported)
					{
						buildableStorageBufferIds[x][z] = new int[numberOfBuffers];
						drawableStorageBufferIds[x][z] = new int[numberOfBuffers];
					}
				}
				else
				{
					// we only need one buffer for this region
					numberOfBuffersPerRegion[x][z] = 1;
					buildableBuffers[x][z] = new BufferBuilder[1];
					buildableVbos[x][z] = new VertexBuffer[1];
					drawableVbos[x][z] = new VertexBuffer[1];
					
					if (glProxy.bufferStorageSupported)
					{
						buildableStorageBufferIds[x][z] = new int[1];
						drawableStorageBufferIds[x][z] = new int[1];
					}
				}
				
				
				for (int i = 0; i < numberOfBuffersPerRegion[x][z]; i++)
				{
					buildableBuffers[x][z][i] = new BufferBuilder((int) regionMemoryRequired);
					
					buildableVbos[x][z][i] = new VertexBuffer();
					drawableVbos[x][z][i] = new VertexBuffer();
					
					
					// create the initial mapped buffers (system memory)
					GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buildableVbos[x][z][i].vertextBufferId);
					GL15.glBufferData(GL15.GL_ARRAY_BUFFER, regionMemoryRequired, GL15.GL_DYNAMIC_DRAW);
					GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
					
					GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, drawableVbos[x][z][i].vertextBufferId);
					GL15.glBufferData(GL15.GL_ARRAY_BUFFER, regionMemoryRequired, GL15.GL_DYNAMIC_DRAW);
					GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
					
					
					if (glProxy.bufferStorageSupported)
					{
						// create the buffer storage (GPU memory)
						buildableStorageBufferIds[x][z][i] = GL15.glGenBuffers();
						GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buildableStorageBufferIds[x][z][i]);
						GL45.glBufferStorage(GL15.GL_ARRAY_BUFFER, regionMemoryRequired, 0); // the 0 flag means to create the storage in the GPUs memory
						GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
						
						drawableStorageBufferIds[x][z][i] = GL15.glGenBuffers();
						GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, drawableStorageBufferIds[x][z][i]);
						GL45.glBufferStorage(GL15.GL_ARRAY_BUFFER, regionMemoryRequired, 0);
						GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);	
					}
				}
			}
		}
		
		bufferLock.unlock();
	}
	
	
	
	/**
	 * Sets the buffers and Vbos to null, forcing them to be recreated <br>
	 * and destroys any bound OpenGL objects. <br><br>
	 * <p>
	 * May have to wait for the bufferLock to open.
	 */
	public void destroyBuffers()
	{
		bufferLock.lock();
		
		
		// destroy the buffer storages if they aren't already
		if (buildableStorageBufferIds != null)
		{
			for (int x = 0; x < buildableStorageBufferIds.length; x++)
			{
				for (int z = 0; z < buildableStorageBufferIds.length; z++)
				{
					for (int i = 0; i < buildableStorageBufferIds[x][z].length; i++)
					{
						int buildableId = buildableStorageBufferIds[x][z][i];
						int drawableId = drawableStorageBufferIds[x][z][i];
						
						// Send this over to the render thread, if this is being
						// called we aren't worried about stuttering anyway.
						// This way we don't have to worry about what context this
						// was called from (if any).
						RenderSystem.recordRenderCall(() ->
						{
							GL15.glDeleteBuffers(buildableId);
							GL15.glDeleteBuffers(drawableId);
						});
					}
				}
			}
		}
		
		buildableStorageBufferIds = null;
		drawableStorageBufferIds = null;
		
		
		
		
		// destroy the VBOs if they aren't already
		if (buildableVbos != null)
		{
			for (int i = 0; i < buildableVbos.length; i++)
			{
				for (int j = 0; j < buildableVbos.length; j++)
				{
					for (int k = 0; k < buildableVbos[i][j].length; k++)
					{
						int buildableId;
						int drawableId;
						
						// variables passed into a lambda expression
						// need to be effectively final, so we have
						// to use an else statement here
						if (buildableVbos[i][j][k] != null)
							buildableId = buildableVbos[i][j][k].vertextBufferId;
						else
							buildableId = 0;
						
						if (drawableVbos[i][j][k] != null)
							drawableId = drawableVbos[i][j][k].vertextBufferId;
						else
							drawableId = 0;
						
						
						RenderSystem.recordRenderCall(() ->
						{
							if (buildableId != 0)
								GL15.glDeleteBuffers(buildableId);
							if (drawableId != 0)
								GL15.glDeleteBuffers(drawableId);
						});
					}
				}
			}
		}
		
		buildableVbos = null;
		drawableVbos = null;
		
		
		// these don't contain any OpenGL objects, so
		// they don't require any special clean-up
		buildableBuffers = null;
		
		bufferLock.unlock();
	}
	
	/** Calls begin on each of the buildable BufferBuilders. */
	private void startBuffers(boolean fullRegen, LodDimension lodDim)
	{
		for (int x = 0; x < buildableBuffers.length; x++)
		{
			for (int z = 0; z < buildableBuffers.length; z++)
			{
				if (fullRegen || lodDim.doesRegionNeedBufferRegen(x, z))
				{
					for (int i = 0; i < buildableBuffers[x][z].length; i++)
					{
						// for some reason BufferBuilder.vertexCounts
						// isn't reset unless this is called, which can cause
						// a false indexOutOfBoundsException
						buildableBuffers[x][z][i].discard();
						
						buildableBuffers[x][z][i].begin(VertexFormat.Mode.QUADS, LodUtil.LOD_VERTEX_FORMAT);
					}
				}
			}
		}
	}
	
	/** Calls end on each of the buildable BufferBuilders. */
	private void closeBuffers(boolean fullRegen, LodDimension lodDim)
	{
		for (int x = 0; x < buildableBuffers.length; x++)
			for (int z = 0; z < buildableBuffers.length; z++)
				for (int i = 0; i < buildableBuffers[x][z].length; i++)
					if (buildableBuffers[x][z][i] != null && buildableBuffers[x][z][i].building() && (fullRegen || lodDim.doesRegionNeedBufferRegen(x, z)))
						buildableBuffers[x][z][i].end();
	}
	
	
	/** Upload all buildableBuffers to the GPU. */
	private void uploadBuffers(boolean fullRegen, LodDimension lodDim)
	{
		GlProxy glProxy = GlProxy.getInstance();
		
		try
		{
			// make sure we are uploading to the builder context,
			// this helps prevent interference (IE stuttering) with the Minecraft context.
			glProxy.setGlContext(GlProxyContext.LOD_BUILDER);
			
			// determine the upload method
			GpuUploadMethod uploadMethod = LodConfig.CLIENT.graphics.advancedGraphicsOption.gpuUploadMethod.get();
			if (!glProxy.bufferStorageSupported && uploadMethod == GpuUploadMethod.BUFFER_STORAGE)
			{
				// if buffer storage isn't supported
				// default to SUB_DATA
				LodConfig.CLIENT.graphics.advancedGraphicsOption.gpuUploadMethod.set(GpuUploadMethod.SUB_DATA);
				uploadMethod = GpuUploadMethod.SUB_DATA;
			}
			
			// actually upload the buffers
			for (int x = 0; x < buildableVbos.length; x++)
			{
				for (int z = 0; z < buildableVbos.length; z++)
				{
					if (fullRegen || lodDim.doesRegionNeedBufferRegen(x, z))
					{
						for (int i = 0; i < buildableBuffers[x][z].length; i++)
						{
							ByteBuffer uploadBuffer = buildableBuffers[x][z][i].popNextBuffer().getSecond();
							vboUpload(buildableVbos[x][z][i], buildableStorageBufferIds[x][z][i], uploadBuffer, true, uploadMethod);
							lodDim.setRegenRegionBufferByArrayIndex(x, z, false);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			// this doesn't appear to be necessary anymore, but just in case.
			ClientProxy.LOGGER.error(LodBufferBuilder.class.getSimpleName() + " - UploadBuffers failed: " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			GL15.glFinish();
			
			// close the context so it can be re-used later.
			// I'm guessing we can't just leave it because the executor service
			// does something that invalidates the OpenGL context.
			glProxy.setGlContext(GlProxyContext.NONE);
		}
	}
	
	/** Uploads the uploadBuffer so the GPU can use it. 
	 * @param uploadMethod */
	private void vboUpload(VertexBuffer vbo, int storageBufferId, ByteBuffer uploadBuffer,
			boolean allowBufferExpansion, GpuUploadMethod uploadMethod)
	{
		// this shouldn't happen, but just to be safe
		if (vbo.vertextBufferId != -1 && GlProxy.getInstance().getGlContext() == GlProxyContext.LOD_BUILDER)
		{
			// this is how many points will be rendered
			// TODO double check that 4 * 6 is correct for the number of points
			vbo.indexCount = (uploadBuffer.capacity() / (4 * 6));
			
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo.vertextBufferId);
			try
			{
				// if possible use the faster buffer storage route
				if (uploadMethod == GpuUploadMethod.BUFFER_STORAGE)
				{
					// get a pointer to the buffer in system memory
					ByteBuffer vboBuffer = GL30.glMapBufferRange(GL15.GL_ARRAY_BUFFER, 0, uploadBuffer.capacity(), GL30.GL_MAP_WRITE_BIT | GL30.GL_MAP_UNSYNCHRONIZED_BIT);
					if (vboBuffer == null)
					{
						int previousCapacity = uploadBuffer.capacity();
						
						// only expand the buffers if the uploadBuffer actually
						// has something in it and expansion is allowed
						if (previousCapacity != 0 && allowBufferExpansion)
						{
							// the buffer(s) aren't big enough, expand them.
							// This does cause lag/stuttering, so it should be avoided!
							
							// expand the buffer in system memory
							GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (int) (uploadBuffer.capacity() * BUFFER_EXPANSION_MULTIPLIER), GL15.GL_DYNAMIC_DRAW);
							GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, uploadBuffer);
							
							// un-bind the system memory buffer 
							GL15.glUnmapBuffer(GL15.GL_ARRAY_BUFFER);
							GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
							
							// expand the buffer storage
							GL15.glDeleteBuffers(storageBufferId);
							GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, storageBufferId);
							GL45.glBufferStorage(GL15.GL_ARRAY_BUFFER, (int) (uploadBuffer.capacity() * BUFFER_EXPANSION_MULTIPLIER), 0);
							GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
							
							
							// recursively try to upload into the newly created buffer storage
							// but don't recurse again if that fails
							// (we don't want an infinitely expanding buffer!)
							vboUpload(vbo, storageBufferId, uploadBuffer, false, uploadMethod);
						}
					}
					else
					{
						// upload the buffer into system memory...
						vboBuffer.put(uploadBuffer);
						GL15.glUnmapBuffer(GL15.GL_ARRAY_BUFFER);
						
						// ...then upload into GPU memory
						// (uploading into GPU memory directly can only be done 
						// through the glCopyBufferSubData/glCopyNamed... methods)
						GL45.glCopyNamedBufferSubData(vbo.vertextBufferId, storageBufferId, 0, 0, uploadBuffer.capacity());
					}
				}
				else if (uploadMethod == GpuUploadMethod.BUFFER_MAPPING)
				{
					// no stuttering but high GPU usage
					// stores everything in system memory instead of GPU memory
					// making rendering much slower.
					// Unless the user is running integrated graphics,
					// in that case this will actually work better than SUB_DATA.
					
					
					ByteBuffer vboBuffer = null;
					
					// map buffer range is better since it can be explicitly unsynchronized 
					if (GlProxy.getInstance().mapBufferRangeSupported)
						vboBuffer = GL30.glMapBufferRange(GL30.GL_ARRAY_BUFFER, 0, uploadBuffer.capacity(), GL30.GL_MAP_WRITE_BIT | GL30.GL_MAP_UNSYNCHRONIZED_BIT);
					else
						vboBuffer = GL15.glMapBuffer(GL30.GL_ARRAY_BUFFER, uploadBuffer.capacity());
					
					
					if (vboBuffer == null)
					{
						GL15.glBufferData(GL45.GL_ARRAY_BUFFER, (int) (uploadBuffer.capacity() * BUFFER_EXPANSION_MULTIPLIER), GL15.GL_DYNAMIC_DRAW);
						GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, uploadBuffer);
					}
					else
					{
						vboBuffer.put(uploadBuffer);
					}
				}
				else
				{
					// hybrid subData/bufferData //
					// less stutter, low GPU usage
					
					//long size = GL31.glGetBufferParameteri64(GL15.GL_ARRAY_BUFFER, GL15.GL_BUFFER_SIZE); // hopefully just a int should be long enough
					long size = GL15.glGetBufferParameteri(GL15.GL_ARRAY_BUFFER, GL15.GL_BUFFER_SIZE);
					if (size < uploadBuffer.capacity() * BUFFER_EXPANSION_MULTIPLIER)
					{
						GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (int) (uploadBuffer.capacity() * BUFFER_EXPANSION_MULTIPLIER), GL15.GL_DYNAMIC_DRAW);
					}
					GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, uploadBuffer);
				}
			}
			catch (Exception e)
			{
				ClientProxy.LOGGER.error("vboUpload failed: " + e.getClass().getSimpleName());
				e.printStackTrace();
			}
			finally
			{
				if (uploadMethod == GpuUploadMethod.BUFFER_MAPPING)
					GL15.glUnmapBuffer(GL15.GL_ARRAY_BUFFER);
				
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
			}
			
		}//if vbo exists and in correct GL context
	}//vboUpload
	
	/** Get the newly created VBOs */
	public VertexBuffersAndOffset getVertexBuffers()
	{
		// don't wait for the lock to open,
		// since this is called on the main render thread
		if (bufferLock.tryLock())
		{
			VertexBuffer[][][] tmpVbo = drawableVbos;
			drawableVbos = buildableVbos;
			buildableVbos = tmpVbo;
			
			int[][][] tmpStorage = drawableStorageBufferIds;
			drawableStorageBufferIds = buildableStorageBufferIds;
			buildableStorageBufferIds = tmpStorage;
			
			drawableCenterChunkPos = buildableCenterChunkPos;
			
			// the vbos have been swapped
			switchVbos = false;
			bufferLock.unlock();
		}
		
		return new VertexBuffersAndOffset(drawableVbos, drawableStorageBufferIds, drawableCenterChunkPos);
	}
	
	/** A simple container to pass multiple objects back in the getVertexBuffers method. */
	public static class VertexBuffersAndOffset
	{
		public final VertexBuffer[][][] vbos;
		public final int[][][] storageBufferIds;
		public final ChunkPosWrapper drawableCenterChunkPos;
		
		public VertexBuffersAndOffset(VertexBuffer[][][] newVbos, int[][][] newStorageBufferIds, ChunkPosWrapper newDrawableCenterChunkPos)
		{
			vbos = newVbos;
			storageBufferIds = newStorageBufferIds;
			drawableCenterChunkPos = newDrawableCenterChunkPos;
		}
	}
	
	/**
	 * If this is true the buildable near and far
	 * buffers have been generated and are ready to be
	 * sent to the LodRenderer.
	 */
	public boolean newBuffersAvailable()
	{
		return switchVbos;
	}
}
