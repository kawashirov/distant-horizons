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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import com.seibel.lod.builders.lodTemplates.Box;
import com.seibel.lod.objects.*;
import org.lwjgl.opengl.GL11;

import com.seibel.lod.config.LodConfig;
import com.seibel.lod.proxy.ClientProxy;
import com.seibel.lod.render.LodRenderer;
import com.seibel.lod.util.LodThreadFactory;
import com.seibel.lod.util.LodUtil;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;


/**
 * This object is used to create NearFarBuffer objects.
 *
 * @author James Seibel
 * @version 8-24-2021
 */
public class LodBufferBuilder
{
	/**
	 * This holds the thread used to generate new LODs off the main thread.
	 */
	private ExecutorService mainGenThread = Executors.newSingleThreadExecutor(new LodThreadFactory(this.getClass().getSimpleName() + " - main"));
	/**
	 * This holds the threads used to generate buffers.
	 */
	private ExecutorService bufferBuilderThreads = Executors.newFixedThreadPool(LodConfig.CLIENT.threading.numberOfBufferBuilderThreads.get(), new LodThreadFactory(this.getClass().getSimpleName() + " - builder"));

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

	private static final int NUMBER_OF_DIRECTION = 4;
	//in order -x, +x, -z, +z
	private static final int[][] ADJ_DIRECTION = new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

	private volatile Box[][] boxCache;
	private volatile PosToRenderContainer[][] setsToRender;
	private volatile RegionPos center;

	/** This is the ChunkPos the player was at the last time the buffers were built.
	 * IE the center of the buffers last time they were built */
	private volatile ChunkPos drawableCenterChunkPos = new ChunkPos(0,0);
	private volatile ChunkPos buildableCenterChunkPos = new ChunkPos(0,0);



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


				ArrayList<Callable<Boolean>> nodeToRenderThreads = new ArrayList<>(lodDim.regions.length * lodDim.regions.length);

				startBuffers(fullRegen, lodDim);

				// =====================//
				//    RENDERING PART    //
				// =====================//

				RegionPos playerRegionPos = new RegionPos(playerChunkPos);
				if (center == null)
					center = playerRegionPos;

				if (setsToRender == null)
					setsToRender = new PosToRenderContainer[lodDim.regions.length][lodDim.regions.length];

				if (setsToRender.length != lodDim.regions.length)
					setsToRender = new PosToRenderContainer[lodDim.regions.length][lodDim.regions.length];

				if (boxCache == null)
					boxCache = new Box[lodDim.regions.length][lodDim.regions.length];

				if (boxCache.length != lodDim.regions.length)
					boxCache = new Box[lodDim.regions.length][lodDim.regions.length];

				// this will be the center of the VBOs once they have been built
				buildableCenterChunkPos = playerChunkPos;

				for (int xRegion = 0; xRegion < lodDim.regions.length; xRegion++)
				{
					for (int zRegion = 0; zRegion < lodDim.regions.length; zRegion++)
					{
						if (lodDim.regen[xRegion][zRegion] || fullRegen)
						{
							RegionPos regionPos = new RegionPos(
									xRegion + lodDim.getCenterX() - Math.floorDiv(lodDim.getWidth(), 2),
									zRegion + lodDim.getCenterZ() - Math.floorDiv(lodDim.getWidth(), 2));

							// local position in the vbo and bufferBuilder arrays
							BufferBuilder currentBuffer = buildableBuffers[xRegion][zRegion];
							LodRegion region = lodDim.getRegion(regionPos.x, regionPos.z);
							if (region == null) continue;
							byte minDetail = region.getMinDetailLevel();
							// make sure the buffers weren't
							// changed while we were running this method
							if (currentBuffer == null || (currentBuffer != null && !currentBuffer.building()))
								return;
							//previous setToRender chache
							final int xR = xRegion;
							final int zR = zRegion;
							Callable<Boolean> dataToRenderThread = () ->
							{

								//previous setToRender chache
								if (setsToRender[xR][zR] == null)
								{
									setsToRender[xR][zR] = new PosToRenderContainer(minDetail, regionPos.x, regionPos.z);
								}

								if (boxCache[xR][zR] == null)
								{
									boxCache[xR][zR] = new Box();
								}
								PosToRenderContainer posToRender = (PosToRenderContainer) setsToRender[xR][zR];
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
								short gameChunkRenderDistance = (short) (renderer.vanillaRenderedChunks.length / 2 - 1);
								long lodData;
								long[] adjData;
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
											    && renderer.vanillaRenderedChunks[chunkXdist + gameChunkRenderDistance + 1][chunkZdist + gameChunkRenderDistance + 1])
									{
										continue;
									}
									// skip any chunks that Minecraft is going to render
									try
									{
										if (lodDim.doesDataExist(detailLevel, posX, posZ))
										{
											lodData = lodDim.getData(detailLevel, posX, posZ);
											adjData = new long[NUMBER_OF_DIRECTION];
											for (int direction = 0; direction < NUMBER_OF_DIRECTION; direction++)
											{
												xAdj = posX + ADJ_DIRECTION[direction][0];
												zAdj = posZ + ADJ_DIRECTION[direction][1];
												chunkXdist = LevelPosUtil.getChunkPos(detailLevel,xAdj) - playerChunkPos.x;
												chunkZdist = LevelPosUtil.getChunkPos(detailLevel,xAdj) - playerChunkPos.z;

												if (gameChunkRenderDistance >= Math.abs(chunkXdist) && gameChunkRenderDistance >= Math.abs(chunkZdist))
												{

													if (!renderer.vanillaRenderedChunks[chunkXdist + gameChunkRenderDistance + 1][chunkZdist + gameChunkRenderDistance + 1]
															    && posToRender.contains(detailLevel, xAdj, zAdj))
													{
														adjData[direction]= lodDim.getData(detailLevel, xAdj, zAdj);
													}
												} else
												{
													if (posToRender.contains(detailLevel, xAdj, zAdj))
													{
														adjData[direction] = lodDim.getData(detailLevel, xAdj, zAdj);
													}
												}
											}
											LodConfig.CLIENT.graphics.lodTemplate.get().template.addLodToBuffer(currentBuffer, playerBlockPosRounded, lodData, adjData,
													detailLevel, posX, posZ, boxCache[xR][zR],renderer.previousDebugMode);
										}
									} catch (ArrayIndexOutOfBoundsException e)
									{
										e.printStackTrace();
										return false;
									}

								}// for pos to in list to render
								// the thread executed successfully
								return true;
							};
							nodeToRenderThreads.add(dataToRenderThread);
						}
					}// region z
				}// region z
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

		return;
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
	public void setupBuffers(int numbRegionsWide, int bufferMaxCapacity)
	{
		bufferLock.lock();

		previousRegionWidth = numbRegionsWide;
		previousBufferSize = bufferMaxCapacity;


		buildableBuffers = new BufferBuilder[numbRegionsWide][numbRegionsWide];

		buildableVbos = new VertexBuffer[numbRegionsWide][numbRegionsWide];
		drawableVbos = new VertexBuffer[numbRegionsWide][numbRegionsWide];

		for (int x = 0; x < numbRegionsWide; x++)
		{
			for (int z = 0; z < numbRegionsWide; z++)
			{
				buildableBuffers[x][z] = new BufferBuilder(bufferMaxCapacity);
				buildableVbos[x][z] = new VertexBuffer(LodRenderer.LOD_VERTEX_FORMAT);
				drawableVbos[x][z] = new VertexBuffer(LodRenderer.LOD_VERTEX_FORMAT);
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
			for (int z = 0; z < buildableBuffers.length; z++)
			{
				if (fullRegen || lodDim.regen[x][z])
				{
					buildableBuffers[x][z].begin(GL11.GL_QUADS, LodRenderer.LOD_VERTEX_FORMAT);
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

				if (buildableBuffers[x][z] != null && buildableBuffers[x][z].building() && (fullRegen || lodDim.regen[x][z]))
				{
					buildableBuffers[x][z].end();
				}
	}

	/**
	 * Called from the LodRenderer to create the
	 * BufferBuilders at the right size.
	 */
	private void uploadBuffers(boolean fullRegen, LodDimension lodDim)
	{
		for (int x = 0; x < buildableVbos.length; x++)
		{
			for (int z = 0; z < buildableVbos.length; z++)
			{
				if (fullRegen || lodDim.regen[x][z])
				{
					buildableVbos[x][z].upload(buildableBuffers[x][z]);
					lodDim.regen[x][z] = false;
				}
			}
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
			VertexBuffer[][] tmp = drawableVbos;
			drawableVbos = buildableVbos;
			buildableVbos = tmp;

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
