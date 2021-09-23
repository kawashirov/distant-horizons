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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL15C;

import com.seibel.lod.builders.lodTemplates.Box;
import com.seibel.lod.config.LodConfig;
import com.seibel.lod.enums.VerticalQuality;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.objects.LodRegion;
import com.seibel.lod.objects.PosToRenderContainer;
import com.seibel.lod.objects.RegionPos;
import com.seibel.lod.proxy.ClientProxy;
import com.seibel.lod.proxy.GlProxy;
import com.seibel.lod.proxy.GlProxy.GlProxyContext;
import com.seibel.lod.render.LodRenderer;
import com.seibel.lod.util.DataPointUtil;
import com.seibel.lod.util.DetailDistanceUtil;
import com.seibel.lod.util.LevelPosUtil;
import com.seibel.lod.util.LodThreadFactory;
import com.seibel.lod.util.LodUtil;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

/**
 * This object is used to create NearFarBuffer objects.
 *
 * @author James Seibel
 * @version 9-22-2021
 */
public class LodBufferBuilder
{
	/**
	 * This holds the thread used to generate new LODs off the main thread.
	 */
	public static ExecutorService mainGenThread = Executors.newSingleThreadExecutor(new LodThreadFactory(LodBufferBuilder.class.getSimpleName() + " - main"));
	/**
	 * This holds the threads used to generate buffers.
	 */
	public static ExecutorService bufferBuilderThreads = Executors.newFixedThreadPool(LodConfig.CLIENT.threading.numberOfBufferBuilderThreads.get(), new LodThreadFactory(LodBufferBuilder.class.getSimpleName() + " - builder"));
	
	/**
	 * The buffers that are used to create LODs using far fog
	 */
	public volatile BufferBuilder[][] buildableBuffers;
	
	/**
	 * Used when building new VBOs
	 */
	public volatile VertexBuffer[][] buildableVbos;
	
	/**
	 * VBOs that are sent over to the LodNodeRenderer
	 */
	public volatile VertexBuffer[][] drawableVbos;
	
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
	
	/**
	 * Size of the buffer builders in bytes last time we created them
	 */
	public int previousBufferSize = 0;
	
	/**
	 * Width of the dimension in regions last time we created the buffers
	 */
	public int previousRegionWidth = 0;
	
	/**
	 * this is used to prevent multiple threads creating, destroying, or using the buffers at the same time
	 */
	private ReentrantLock bufferLock = new ReentrantLock();
	
	//	private static final int NUMBER_OF_DIRECTION = 4;
	//in order -x, +x, -z, +z
	//	private static final int[][] ADJ_VECTOR = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
	
	private volatile Box[][] boxCache;
	private volatile PosToRenderContainer[][] setsToRender;
	private volatile RegionPos center;
	
	/**
	 * This is the ChunkPos the player was at the last time the buffers were built.
	 * IE the center of the buffers last time they were built
	 */
	private volatile ChunkPos drawableCenterChunkPos = new ChunkPos(0, 0);
	private volatile ChunkPos buildableCenterChunkPos = new ChunkPos(0, 0);
	private volatile boolean firstSetup = true;
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
			BlockPos playerBlockPos, boolean fullRegen)
	{
		
		// only allow one generation process to happen at a time
		if (generatingBuffers)
			return;
		
		if (buildableBuffers == null)
			// setupBuffers hasn't been called yet
			return;
		
		generatingBuffers = true;
		
		// round the player's block position down to the nearest chunk BlockPos
		ChunkPos playerChunkPos = new ChunkPos(playerBlockPos);
		BlockPos playerBlockPosRounded = playerChunkPos.getWorldPosition();
		
		Thread thread = new Thread(() ->
		{
			bufferLock.lock();
			
			try
			{
				long treeStart = System.currentTimeMillis();
				long treeEnd = System.currentTimeMillis();
				
				long startTime = System.currentTimeMillis();
				
				ArrayList<Callable<Boolean>> nodeToRenderThreads = new ArrayList<>(lodDim.getWidth() * lodDim.getWidth());
				
				//setupBuffers(lodDim);
				startBuffers(fullRegen, lodDim);
				
				// =====================//
				//    RENDERING PART    //
				// =====================//
				
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
				
				for (int xRegion = 0; xRegion < lodDim.getWidth(); xRegion++)
				{
					for (int zRegion = 0; zRegion < lodDim.getWidth(); zRegion++)
					{
						if (lodDim.isRegionToRegen(xRegion, zRegion) || fullRegen)
						{
							RegionPos regionPos = new RegionPos(
									xRegion + lodDim.getCenterX() - Math.floorDiv(lodDim.getWidth(), 2),
									zRegion + lodDim.getCenterZ() - Math.floorDiv(lodDim.getWidth(), 2));
							
							// local position in the vbo and bufferBuilder arrays
							BufferBuilder currentBuffer = buildableBuffers[xRegion][zRegion];
							LodRegion region = lodDim.getRegion(regionPos.x, regionPos.z);
							
							if (region == null)
								continue;
							
							// make sure the buffers weren't
							// changed while we were running this method
							if (currentBuffer == null || !currentBuffer.building())
								return;
							
							byte minDetail = region.getMinDetailLevel();
							
							
							final int xR = xRegion;
							final int zR = zRegion;
							Callable<Boolean> dataToRenderThread = () ->
							{
								Map<Direction, long[]> adjData = new HashMap<>();
								
								// determine how many LODs we can stack vertically
								int maxVerticalData = 1;
								if (LodConfig.CLIENT.worldGenerator.lodQualityMode.get() == VerticalQuality.MULTI_LOD)
								{
									maxVerticalData = DetailDistanceUtil.getMaxVerticalData(LodUtil.BLOCK_DETAIL_LEVEL);
								}
								
								// create adjData's arrays
								for (Direction direction : Box.ADJ_DIRECTIONS)
								{
									adjData.put(direction, new long[maxVerticalData]);
								}
								
								//previous setToRender cache
								if (setsToRender[xR][zR] == null)
								{
									setsToRender[xR][zR] = new PosToRenderContainer(minDetail, regionPos.x, regionPos.z);
								}
								
								if (boxCache[xR][zR] == null)
								{
									boxCache[xR][zR] = new Box();
								}
								PosToRenderContainer posToRender = setsToRender[xR][zR];
								posToRender.clear(minDetail, regionPos.x, regionPos.z);
								
								lodDim.getDataToRender(
										posToRender,
										regionPos,
										playerBlockPosRounded.getX(),
										playerBlockPosRounded.getZ());
								
								byte detailLevel;
								int posX;
								int posZ;
								int xAdj;
								int zAdj;
								int chunkXdist;
								int chunkZdist;
								
								// keep a local version so we don't have to worry about indexOutOfBounds Exceptions
								// if it changes in the LodRenderer while we are working here
								boolean[][] vanillaRenderedChunks = renderer.vanillaRenderedChunks;
								short gameChunkRenderDistance = (short) (vanillaRenderedChunks.length / 2 - 1);
								
								for (int index = 0; index < posToRender.getNumberOfPos(); index++)
								{
									detailLevel = posToRender.getNthDetailLevel(index);
									posX = posToRender.getNthPosX(index);
									posZ = posToRender.getNthPosZ(index);
									
									// skip any chunks that Minecraft is going to render
									chunkXdist = LevelPosUtil.getChunkPos(detailLevel, posX) - playerChunkPos.x;
									chunkZdist = LevelPosUtil.getChunkPos(detailLevel, posZ) - playerChunkPos.z;
									
									if (gameChunkRenderDistance >= Math.abs(chunkXdist)
											&& gameChunkRenderDistance >= Math.abs(chunkZdist)
											&& detailLevel <= LodUtil.CHUNK_DETAIL_LEVEL
											&& vanillaRenderedChunks[chunkXdist + gameChunkRenderDistance + 1][chunkZdist + gameChunkRenderDistance + 1])
									{
										continue;
									}
									
									// skip any chunks that Minecraft is going to render
									for (Direction direction : Box.ADJ_DIRECTIONS)
									{
										xAdj = posX + direction.getNormal().getX();
										zAdj = posZ + direction.getNormal().getZ();
										chunkXdist = LevelPosUtil.getChunkPos(detailLevel, xAdj) - playerChunkPos.x;
										chunkZdist = LevelPosUtil.getChunkPos(detailLevel, zAdj) - playerChunkPos.z;
										boolean performFaceCulling = true;
										if (performFaceCulling
												&& posToRender.contains(detailLevel, xAdj, zAdj)
												&& (gameChunkRenderDistance < Math.abs(chunkXdist)
														|| gameChunkRenderDistance < Math.abs(chunkZdist)
														|| !vanillaRenderedChunks[chunkXdist + gameChunkRenderDistance + 1][chunkZdist + gameChunkRenderDistance + 1]))
										{
											if (!adjData.containsKey(direction) || adjData.get(direction) == null)
												adjData.put(direction, new long[maxVerticalData]);
											for (int verticalIndex = 0; verticalIndex < lodDim.getMaxVerticalData(detailLevel, xAdj, zAdj); verticalIndex++)
											{
												long data = lodDim.getData(detailLevel, xAdj, zAdj, verticalIndex);
												adjData.get(direction)[verticalIndex] = data;
											}
										} else
										{
											adjData.put(direction, null);
										}
									}
									
									long data;
									for (int verticalIndex = 0; verticalIndex < lodDim.getMaxVerticalData(detailLevel, posX, posZ); verticalIndex++)
									{
										data = lodDim.getData(detailLevel, posX, posZ, verticalIndex);
										if (DataPointUtil.isItVoid(data) || !DataPointUtil.doesItExist(data))
											break;
										LodConfig.CLIENT.graphics.lodTemplate.get().template.addLodToBuffer(currentBuffer, playerBlockPosRounded, data, adjData,
												detailLevel, posX, posZ, boxCache[xR][zR], renderer.previousDebugMode, renderer.lightMap);
									}
									
									
								} // for pos to in list to render
								// the thread executed successfully
								return true;
							};
							nodeToRenderThreads.add(dataToRenderThread);
						}
					} // region z
				} // region z
				long renderStart = System.currentTimeMillis();
				// wait for all threads to finish
				List<Future<Boolean>> futuresBuffer = bufferBuilderThreads.invokeAll(nodeToRenderThreads);
				for (Future<Boolean> future : futuresBuffer)
				{
					// the future will be false if its thread failed
					if (!future.get())
					{
						ClientProxy.LOGGER.warn("LodBufferBuilder ran into trouble and had to start over.");
						closeBuffers(fullRegen, lodDim);
						return;
					}
				}
				long renderEnd = System.currentTimeMillis();
				
				long endTime = System.currentTimeMillis();
				@SuppressWarnings("unused")
				long buildTime = endTime - startTime;
				@SuppressWarnings("unused")
				long treeTime = treeEnd - treeStart;
				@SuppressWarnings("unused")
				long renderingTime = renderEnd - renderStart;
				
				//				ClientProxy.LOGGER.info("Buffer Build time: " + buildTime + " ms" + '\n' +
				//						                        "Tree cutting time: " + treeTime + " ms" + '\n' +
				//						                        "Rendering time: " + renderingTime + " ms");
				
				// mark that the buildable buffers as ready to swap
				switchVbos = true;
			} catch (Exception e)
			{
				ClientProxy.LOGGER.warn("\"LodNodeBufferBuilder.generateLodBuffersAsync\" ran into trouble: ");
				e.printStackTrace();
			} finally
			{
				// regardless of if we successfully created the buffers
				// we are done generating.
				generatingBuffers = false;
				
				// clean up any potentially open resources
				if (buildableBuffers != null)
					closeBuffers(fullRegen, lodDim);
				
				// upload the new buffers
				uploadBuffers(fullRegen, lodDim);
				bufferLock.unlock();
			}
			
		});
		
		mainGenThread.execute(thread);
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
		int bufferMaxCapacity;
		
		//if(previousRegionWidth != numbRegionsWide || firstSetup)
		//{
		firstSetup = false;
		previousRegionWidth = numbRegionsWide;
		buildableBuffers = new BufferBuilder[numbRegionsWide][numbRegionsWide];
		
		buildableVbos = new VertexBuffer[numbRegionsWide][numbRegionsWide];
		drawableVbos = new VertexBuffer[numbRegionsWide][numbRegionsWide];
		//}
		for (int x = 0; x < numbRegionsWide; x++)
		{
			for (int z = 0; z < numbRegionsWide; z++)
			{
				//if(lodDimension.isBufferToSetup(x,z))
				//{
				bufferMaxCapacity = lodDimension.getMemoryRequired(x, z, LodConfig.CLIENT.graphics.lodTemplate.get());
				buildableBuffers[x][z] = new BufferBuilder(bufferMaxCapacity);
				
				buildableVbos[x][z] = new VertexBuffer(LodRenderer.LOD_VERTEX_FORMAT);
				drawableVbos[x][z] = new VertexBuffer(LodRenderer.LOD_VERTEX_FORMAT);
				//}
			}
		}
		
		bufferLock.unlock();
	}
	
	/**
	 * sets the buffers and Vbos to null, forcing them to be recreated. <br><br>
	 * <p>
	 * May have to wait for the bufferLock to open.
	 */
	public void destroyBuffers()
	{
		bufferLock.lock();
		
		buildableBuffers = null;
		buildableVbos = null;
		drawableVbos = null;
		
		bufferLock.unlock();
	}
	
	/**
	 * Calls begin on each of the buildable BufferBuilders.
	 */
	private void startBuffers(boolean fullRegen, LodDimension lodDim)
	{
		for (int x = 0; x < buildableBuffers.length; x++)
		{
			for (int z = 0; z < buildableBuffers.length; z++)
			{
				if (fullRegen || lodDim.isRegionToRegen(x, z))
				{
					buildableBuffers[x][z].begin(GL11.GL_QUADS, LodRenderer.LOD_VERTEX_FORMAT);
				}
			}
		}
	}
	
	/**
	 * Calls end on each of the buildable BufferBuilders.
	 */
	private void closeBuffers(boolean fullRegen, LodDimension lodDim)
	{
		for (int x = 0; x < buildableBuffers.length; x++)
			for (int z = 0; z < buildableBuffers.length; z++)
				if (buildableBuffers[x][z] != null && buildableBuffers[x][z].building() && (fullRegen || lodDim.isRegionToRegen(x, z)))
					buildableBuffers[x][z].end();
	}
	
	/**
	 * Upload all buildableBuffers to the GPU.
	 */
	private void uploadBuffers(boolean fullRegen, LodDimension lodDim)
	{
		GlProxy glProxy = GlProxy.getInstance();
		
		try
		{
			// make sure we are uploading to a different OpenGL context,
			// to prevent interference (IE stuttering) with the Minecraft context.
			glProxy.setGlContext(GlProxyContext.LOD_BUILDER);
			
			for (int x = 0; x < buildableVbos.length; x++)
			{
				for (int z = 0; z < buildableVbos.length; z++)
				{
					if (fullRegen || lodDim.isRegionToRegen(x, z))
					{
						ByteBuffer builderBuffer = buildableBuffers[x][z].popNextBuffer().getSecond();
						vboUpload(buildableVbos[x][z], builderBuffer);
						lodDim.setRegenByArrayIndex(x, z, false);
					}
				}
			}
		}
		catch (IllegalStateException e)
		{
			ClientProxy.LOGGER.error(LodBufferBuilder.class.getSimpleName() + " - UploadBuffers failed: " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			// make sure no buffer is bound
			if (glProxy.getGlContext() == GlProxyContext.LOD_BUILDER)
			{
				GL15C.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
			}
			
			// make sure the context is disabled
			glProxy.setGlContext(GlProxyContext.NONE);
		}
	}
	
	/**
	 * Uploads the uploadBuffer into the VBO in GPU memory.
	 */
	private void vboUpload(VertexBuffer vbo, ByteBuffer uploadBuffer)
	{
		// this shouldn't happen, but just to be safe
		if (vbo.id != -1 && GlProxy.getInstance().getGlContext() == GlProxyContext.LOD_BUILDER)
		{
			// this is how many points will be rendered
			vbo.vertexCount = (uploadBuffer.remaining() / vbo.format.getVertexSize());
			
			GL15C.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo.id);
			
			// subData only works if the memory is allocated beforehand.
			GL15C.glBufferData(GL15.GL_ARRAY_BUFFER, uploadBuffer.remaining(), GL15C.GL_DYNAMIC_DRAW);
			
			// interestingly bufferSubData renders faster than glMapBuffer
			// even though OpenGLInsights-AsynchronousBufferTransfers says glMapBuffer
			// is faster for transferring data. They must put the data in different memory
			// or something.
			GL15C.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, uploadBuffer);
			
			GL15C.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		}
	}
	
	/**
	 * Get the newly created VBOs
	 */
	public VertexBuffersAndOffset getVertexBuffers()
	{
		// don't wait for the lock to open
		// since this is called on the main render thread
		if (bufferLock.tryLock())
		{
			VertexBuffer[][] tmpVbo = drawableVbos;
			drawableVbos = buildableVbos;
			buildableVbos = tmpVbo;
			
			drawableCenterChunkPos = buildableCenterChunkPos;
			
			// the vbos have been swapped
			switchVbos = false;
			bufferLock.unlock();
		}
		
		return new VertexBuffersAndOffset(drawableVbos, drawableCenterChunkPos);
	}
	
	/**
	 * A simple container to pass multiple objects back in the getVertexBuffers method.
	 */
	public class VertexBuffersAndOffset
	{
		public VertexBuffer[][] vbos;
		public ChunkPos drawableCenterChunkPos;
		
		public VertexBuffersAndOffset(VertexBuffer[][] newVbos, ChunkPos newDrawableCenterChunkPos)
		{
			vbos = newVbos;
			drawableCenterChunkPos = newDrawableCenterChunkPos;
		}
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
