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

package com.seibel.lod.core.builders.bufferBuilding;

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

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL45;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.enums.config.GpuUploadMethod;
import com.seibel.lod.core.enums.config.VanillaOverdraw;
import com.seibel.lod.core.enums.rendering.GLProxyContext;
import com.seibel.lod.core.objects.Box;
import com.seibel.lod.core.objects.PosToRenderContainer;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.objects.lod.LodRegion;
import com.seibel.lod.core.objects.lod.RegionPos;
import com.seibel.lod.core.objects.opengl.LodBufferBuilder;
import com.seibel.lod.core.objects.opengl.LodVertexBuffer;
import com.seibel.lod.core.render.GLProxy;
import com.seibel.lod.core.render.LodRenderer;
import com.seibel.lod.core.util.DataPointUtil;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LevelPosUtil;
import com.seibel.lod.core.util.LodThreadFactory;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.util.ThreadMapUtil;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;

/**
 * This object creates the buffers that are
 * rendered by the LodRenderer.
 * 
 * @author James Seibel
 * @version 11-21-2021
 */
public class LodBufferBuilderFactory
{
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	private static final IMinecraftWrapper MC = SingletonHandler.get(IMinecraftWrapper.class);
	private static final IWrapperFactory WRAPPER_FACTORY = SingletonHandler.get(IWrapperFactory.class);
	
	/** The thread used to generate new LODs off the main thread. */
	public static final ExecutorService mainGenThread = Executors.newSingleThreadExecutor(new LodThreadFactory(LodBufferBuilderFactory.class.getSimpleName() + " - main"));
	/** The threads used to generate buffers. */
	public static final ExecutorService bufferBuilderThreads = Executors.newFixedThreadPool(CONFIG.client().advanced().threading().getNumberOfBufferBuilderThreads(), new ThreadFactoryBuilder().setNameFormat("Buffer-Builder-%d").build());
	
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
	
	public static int skyLightPlayer = 15;
	
	/**
	 * How many buffers there are for the given region. <Br>
	 * This is done because some regions may require more memory than
	 * can be directly allocated, so we split the regions into smaller sections. <Br>
	 * This keeps track of those sections.
	 */
	public volatile int[][] numberOfBuffersPerRegion;
	
	/** Stores the vertices when building the VBOs */
	public volatile LodBufferBuilder[][][] buildableBuffers;
	
	/** The OpenGL IDs of the storage buffers used by the buildableVbos */
	public int[][][] buildableStorageBufferIds;
	/** The OpenGL IDs of the storage buffers used by the drawableVbos */
	public int[][][] drawableStorageBufferIds;
	
	/** Used when building new VBOs */
	public volatile LodVertexBuffer[][][] buildableVbos;
	/** VBOs that are sent over to the LodNodeRenderer */
	public volatile LodVertexBuffer[][][] drawableVbos;
	
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
	private volatile AbstractChunkPosWrapper drawableCenterChunkPos = WRAPPER_FACTORY.createChunkPos();
	private volatile AbstractChunkPosWrapper buildableCenterChunkPos = WRAPPER_FACTORY.createChunkPos();
	
	
	
	
	
	
	public LodBufferBuilderFactory()
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
			AbstractBlockPosWrapper playerBlockPos, boolean fullRegen)
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
			AbstractBlockPosWrapper playerBlockPos, boolean fullRegen)
	{
		bufferLock.lock();
		
		try
		{
			// round the player's block position down to the nearest chunk BlockPos
			AbstractChunkPosWrapper playerChunkPos = WRAPPER_FACTORY.createChunkPos(playerBlockPos);
			AbstractBlockPosWrapper playerBlockPosRounded = playerChunkPos.getWorldPosition();
			
			
			//long startTime = System.currentTimeMillis();
			
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
			
			skyLightPlayer = MC.getWrappedClientWorld().getSkyLight(playerBlockPos);
			
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
						LodBufferBuilder[] currentBuffers = buildableBuffers[xRegion][zRegion];
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
							Map<LodDirection, long[]> adjData = ThreadMapUtil.getAdjDataArray(maxVerticalData);
							
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
								for (LodDirection lodDirection : Box.ADJ_DIRECTIONS)
								{
									
									xAdj = posX + Box.DIRECTION_NORMAL_MAP.get(lodDirection).x;
									zAdj = posZ + Box.DIRECTION_NORMAL_MAP.get(lodDirection).z;
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
											adjShadeDisabled[Box.DIRECTION_INDEX.get(lodDirection)] = false;
											adjData.get(lodDirection)[verticalIndex] = data;
										}
									}
									else
									{
										//Otherwise, we check if this position is
										data = lodDim.getSingleData(detailLevel, xAdj, zAdj);
										
										adjData.get(lodDirection)[0] = DataPointUtil.EMPTY_DATA;
										
										if ((isThisPositionGoingToBeRendered(detailLevel, xAdj, zAdj, playerChunkPos, vanillaRenderedChunks, gameChunkRenderDistance) || (posNotInPlayerChunk && adjPosInPlayerChunk))
													&& !DataPointUtil.isVoid(data))
										{
											adjShadeDisabled[Box.DIRECTION_INDEX.get(lodDirection)] = DataPointUtil.getAlpha(data) < 255;
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
										adjData.get(LodDirection.UP)[0] = lodDim.getData(detailLevel, posX, posZ, verticalIndex - 1);
									else
										adjData.get(LodDirection.UP)[0] = DataPointUtil.EMPTY_DATA;
									
									
									//we get the below block as adj DOWN
									if (verticalIndex < lodDim.getMaxVerticalData(detailLevel, posX, posZ) - 1)
										adjData.get(LodDirection.DOWN)[0] = lodDim.getData(detailLevel, posX, posZ, verticalIndex + 1);
									else
										adjData.get(LodDirection.DOWN)[0] = DataPointUtil.EMPTY_DATA;
									
									//We extract the data to render
									data = lodDim.getData(detailLevel, posX, posZ, verticalIndex);
									
									//If the data is not renderable (Void or non existing) we stop since there is no data left in this position
									if (DataPointUtil.isVoid(data) || !DataPointUtil.doesItExist(data))
										break;
									
									//We send the call to create the vertices
									CONFIG.client().graphics().advancedGraphics().getLodTemplate().template.addLodToBuffer(currentBuffers[bufferIndex], playerBlockPosRounded, data, adjData,
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
			
			
			//long executeStart = System.currentTimeMillis();
			// wait for all threads to finish
			List<Future<Boolean>> futuresBuffer = bufferBuilderThreads.invokeAll(nodeToRenderThreads);
			for (Future<Boolean> future : futuresBuffer)
			{
				// the future will be false if its thread failed
				if (!future.get())
				{
					ClientApi.LOGGER.warn("LodBufferBuilder ran into trouble and had to start over.");
					break;
				}
			}
			//long executeEnd = System.currentTimeMillis();
			
			
			//long endTime = System.currentTimeMillis();
			//long buildTime = endTime - startTime;
			//long executeTime = executeEnd - executeStart;

//			ClientProxy.LOGGER.info("Thread Build time: " + buildTime + " ms" + '\n' +
//					                        "thread execute time: " + executeTime + " ms");
			
			// mark that the buildable buffers as ready to swap
			switchVbos = true;
		}
		catch (Exception e)
		{
			ClientApi.LOGGER.warn("\"LodNodeBufferBuilder.generateLodBuffersAsync\" ran into trouble: ");
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
				ClientApi.LOGGER.warn("\"LodNodeBufferBuilder.generateLodBuffersAsync\" was unable to upload the buffers to the GPU: " + e.getMessage());
				e.printStackTrace();
			}
			
			// regardless of whether we were able to successfully create
			// the buffers, we are done generating.
			generatingBuffers = false;
			bufferLock.unlock();
		}
	}
	
	private boolean isThisPositionGoingToBeRendered(byte detailLevel, int posX, int posZ, AbstractChunkPosWrapper playerChunkPos, boolean[][] vanillaRenderedChunks, int gameChunkRenderDistance){
		
		
		// skip any chunks that Minecraft is going to render
		int chunkXdist = LevelPosUtil.getChunkPos(detailLevel, posX) - playerChunkPos.getX();
		int chunkZdist = LevelPosUtil.getChunkPos(detailLevel, posZ) - playerChunkPos.getZ();

		// check if the chunk is on the border
		boolean isItBorderPos;
		if (CONFIG.client().graphics().advancedGraphics().getVanillaOverdraw() == VanillaOverdraw.BORDER)
			isItBorderPos = LodUtil.isBorderChunk(vanillaRenderedChunks, chunkXdist + gameChunkRenderDistance + 1, chunkZdist + gameChunkRenderDistance + 1);
		else
			isItBorderPos = false;
		
		
		//boolean smallRenderDistance = gameChunkRenderDistance <= LodUtil.MINIMUM_RENDER_DISTANCE_FOR_PARTIAL_OVERDRAW;
		
		// get the positions that will be rendered
		
		return (gameChunkRenderDistance >= Math.abs(chunkXdist)
				&& gameChunkRenderDistance >= Math.abs(chunkZdist)
				&& detailLevel <= LodUtil.CHUNK_DETAIL_LEVEL
				&& vanillaRenderedChunks[chunkXdist + gameChunkRenderDistance + 1][chunkZdist + gameChunkRenderDistance + 1])
				&& (!isItBorderPos);
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
		bufferLock.lock();
		int numbRegionsWide = lodDimension.getWidth();
		long regionMemoryRequired;
		int numberOfBuffers;
		
		GLProxy glProxy = GLProxy.getInstance();
		GLProxyContext oldContext = glProxy.getGlContext();
		glProxy.setGlContext(GLProxyContext.LOD_BUILDER);
		
		
		previousRegionWidth = numbRegionsWide;
		numberOfBuffersPerRegion = new int[numbRegionsWide][numbRegionsWide];
		buildableBuffers = new LodBufferBuilder[numbRegionsWide][numbRegionsWide][];
		
		buildableVbos = new LodVertexBuffer[numbRegionsWide][numbRegionsWide][];
		drawableVbos = new LodVertexBuffer[numbRegionsWide][numbRegionsWide][];
		
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
					numberOfBuffers = (int) regionMemoryRequired / LodUtil.MAX_ALLOCATABLE_DIRECT_MEMORY + 1;
					
					// TODO shouldn't this be determined with regionMemoryRequired?
					// always allocating the max memory is a bit expensive isn't it?
					regionMemoryRequired = LodUtil.MAX_ALLOCATABLE_DIRECT_MEMORY;
					numberOfBuffersPerRegion[x][z] = numberOfBuffers;
					buildableBuffers[x][z] = new LodBufferBuilder[numberOfBuffers];
					buildableVbos[x][z] = new LodVertexBuffer[numberOfBuffers];
					drawableVbos[x][z] = new LodVertexBuffer[numberOfBuffers];
					
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
					buildableBuffers[x][z] = new LodBufferBuilder[1];
					buildableVbos[x][z] = new LodVertexBuffer[1];
					drawableVbos[x][z] = new LodVertexBuffer[1];
					
					if (glProxy.bufferStorageSupported)
					{
						buildableStorageBufferIds[x][z] = new int[1];
						drawableStorageBufferIds[x][z] = new int[1];
					}
				}
				
				
				for (int i = 0; i < numberOfBuffersPerRegion[x][z]; i++)
				{
					buildableBuffers[x][z][i] = new LodBufferBuilder((int) regionMemoryRequired);
					
					buildableVbos[x][z][i] = new LodVertexBuffer();
					drawableVbos[x][z][i] = new LodVertexBuffer();
					
					
					// create the initial mapped buffers (system memory)
					GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buildableVbos[x][z][i].id);
					GL15.glBufferData(GL15.GL_ARRAY_BUFFER, regionMemoryRequired, GL15.GL_DYNAMIC_DRAW);
					GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
					
					GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, drawableVbos[x][z][i].id);
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
		
		glProxy.setGlContext(oldContext);
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
						
						// make sure the buffers are deleted in a openGL context
						GLProxy.getInstance().recordOpenGlCall(() ->
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
							buildableId = buildableVbos[i][j][k].id;
						else
							buildableId = 0;
						
						if (drawableVbos[i][j][k] != null)
							drawableId = drawableVbos[i][j][k].id;
						else
							drawableId = 0;
						
						
						GLProxy.getInstance().recordOpenGlCall(() ->
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
						
						buildableBuffers[x][z][i].begin(GL11.GL_QUADS, LodUtil.LOD_VERTEX_FORMAT);
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
		GLProxy glProxy = GLProxy.getInstance();
		
		try
		{
			// make sure we are uploading to the builder context,
			// this helps prevent interference (IE stuttering) with the Minecraft context.
			glProxy.setGlContext(GLProxyContext.LOD_BUILDER);
			
			// determine the upload method
			GpuUploadMethod uploadMethod = CONFIG.client().graphics().advancedGraphics().getGpuUploadMethod();
			if (!glProxy.bufferStorageSupported && uploadMethod == GpuUploadMethod.BUFFER_STORAGE)
			{
				// if buffer storage isn't supported
				// default to SUB_DATA
				CONFIG.client().graphics().advancedGraphics().setGpuUploadMethod(GpuUploadMethod.SUB_DATA);
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
							ByteBuffer uploadBuffer = buildableBuffers[x][z][i].getCleanedByteBuffer();
							int storageBufferId = 0;
							if (buildableStorageBufferIds != null)
								storageBufferId = buildableStorageBufferIds[x][z][i];
							
							vboUpload(buildableVbos[x][z][i], storageBufferId, uploadBuffer, true, uploadMethod);
							lodDim.setRegenRegionBufferByArrayIndex(x, z, false);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			// this doesn't appear to be necessary anymore, but just in case.
			ClientApi.LOGGER.error(LodBufferBuilderFactory.class.getSimpleName() + " - UploadBuffers failed: " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			GL15.glFinish();
			
			// close the context so it can be re-used later.
			// I'm guessing we can't just leave it because the executor service
			// does something that invalidates the OpenGL context.
			glProxy.setGlContext(GLProxyContext.NONE);
		}
	}
	
	/** Uploads the uploadBuffer so the GPU can use it. */
	private void vboUpload(LodVertexBuffer vbo, int storageBufferId, ByteBuffer uploadBuffer,
			boolean allowBufferExpansion, GpuUploadMethod uploadMethod)
	{
		// this shouldn't happen, but just to be safe
		if (vbo.id != -1 && GLProxy.getInstance().getGlContext() == GLProxyContext.LOD_BUILDER)
		{
			// this is how many points will be rendered
			vbo.vertexCount = (uploadBuffer.capacity() / ((Float.BYTES * 3) + (Byte.BYTES * 4))); // TODO make this change with the LodTemplate
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo.id);
			try
			{
				// if possible use the faster buffer storage route
				if (uploadMethod == GpuUploadMethod.BUFFER_STORAGE && storageBufferId != 0)
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
						GL45.glCopyNamedBufferSubData(vbo.id, storageBufferId, 0, 0, uploadBuffer.capacity());
						
						// alternative way that doesn't require GL45
//						GL15.glBindBuffer(GL45.GL_COPY_WRITE_BUFFER, storageBufferId);
//						GL45.glCopyBufferSubData(GL15.GL_ARRAY_BUFFER, GL45.GL_COPY_WRITE_BUFFER, 0, 0, uploadBuffer.capacity());
//						GL15.glBindBuffer(GL45.GL_COPY_WRITE_BUFFER, 0);
					}
				}
				else if (uploadMethod == GpuUploadMethod.BUFFER_MAPPING)
				{
					// no stuttering but high GPU usage
					// stores everything in system memory instead of GPU memory
					// making rendering much slower.
					// Unless the user is running integrated graphics,
					// in that case this will actually work better than SUB_DATA.
					
					
					ByteBuffer vboBuffer;
					
					// map buffer range is better since it can be explicitly unsynchronized 
					if (GLProxy.getInstance().mapBufferRangeSupported)
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
				else if (uploadMethod == GpuUploadMethod.DATA)
				{
					// hybrid bufferData //
					// high stutter, low GPU usage
					// But simplest/most compatible
					
					GL15.glBufferData(GL15.GL_ARRAY_BUFFER, uploadBuffer, GL15.GL_DYNAMIC_DRAW);
				}
				else
				{
					// hybrid subData/bufferData //
					// less stutter, low GPU usage
					
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
				ClientApi.LOGGER.error("vboUpload failed: " + e.getClass().getSimpleName());
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
			LodVertexBuffer[][][] tmpVbo = drawableVbos;
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
		public final LodVertexBuffer[][][] vbos;
		public final int[][][] storageBufferIds;
		public final AbstractChunkPosWrapper drawableCenterChunkPos;
		
		public VertexBuffersAndOffset(LodVertexBuffer[][][] newVbos, int[][][] newStorageBufferIds, AbstractChunkPosWrapper newDrawableCenterChunkPos)
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
