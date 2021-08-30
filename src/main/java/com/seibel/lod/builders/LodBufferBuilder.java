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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.lwjgl.opengl.GL11;

import com.seibel.lod.handlers.LodConfig;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.objects.RegionPos;
import com.seibel.lod.objects.LevelPos.LevelPos;
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
	private ExecutorService bufferBuilderThreads = Executors.newFixedThreadPool(LodConfig.CLIENT.numberOfBufferBuilderThreads.get(), new LodThreadFactory(this.getClass().getSimpleName() + " - builder"));

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

	private Object[][] setsToRender;
	private RegionPos center;

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
					setsToRender = new Object[lodDim.regions.length][lodDim.regions.length];

				if (setsToRender.length != lodDim.regions.length)
					setsToRender = new Object[lodDim.regions.length][lodDim.regions.length];


				RegionPos worldRegionOffset = new RegionPos(playerRegionPos.x - lodDim.getCenterX(), playerRegionPos.z - lodDim.getCenterZ());
				if (worldRegionOffset.x != 0 || worldRegionOffset.z != 0)
				{
					move(worldRegionOffset, Math.floorDiv(lodDim.getWidth(), 2));
				}

				for (int xRegion = 0; xRegion < lodDim.regions.length; xRegion++)
				{
					for (int zRegion = 0; zRegion < lodDim.regions.length; zRegion++)
					{
						//if (lodDim.regen[xRegion][zRegion])
						//	ClientProxy.LOGGER.debug("Rendering region " + xRegion + " " + zRegion);
						RegionPos regionPos = new RegionPos(
								xRegion + lodDim.getCenterX() - Math.floorDiv(lodDim.getWidth(), 2),
								zRegion + lodDim.getCenterZ() - Math.floorDiv(lodDim.getWidth(), 2));

						// local position in the vbo and bufferBuilder arrays
						BufferBuilder currentBuffer = buildableBuffers[xRegion][zRegion];

						// make sure the buffers weren't
						// changed while we were running this method
						if (currentBuffer == null || (currentBuffer != null && !currentBuffer.building()))
							return;

						if (setsToRender[xRegion][zRegion] == null)
						{
							setsToRender[xRegion][zRegion] = new ConcurrentHashMap<LevelPos, MutableBoolean>();
						}
						ConcurrentMap<LevelPos, MutableBoolean> nodeToRender = (ConcurrentMap<LevelPos, MutableBoolean>) setsToRender[xRegion][zRegion];
						final boolean regen = fullRegen;
						final boolean regenReg = lodDim.regen[xRegion][zRegion];
						Callable<Boolean> dataToRenderThread = () ->
						{

							if (regen || regenReg)
							{
								lodDim.getDataToRender(
										nodeToRender,
										regionPos,
										playerBlockPosRounded.getX(),
										playerBlockPosRounded.getZ());
							}

							int posX;
							int posZ;
							byte detailLevel;
							for (LevelPos posToRender : nodeToRender.keySet())
							{
								if (!nodeToRender.get(posToRender).booleanValue())
								{
									nodeToRender.remove(posToRender);
									continue;
								}
								nodeToRender.get(posToRender).setFalse();
								// skip any chunks that Minecraft is going to render

								if (renderer.vanillaRenderedChunks.contains(posToRender.getChunkPos()))
								{
									continue;
								}
								posX = posToRender.posX;
								posZ = posToRender.posZ;
								detailLevel = posToRender.detailLevel;

								LevelPos chunkPos = posToRender.getConvertedLevelPos(LodUtil.CHUNK_DETAIL_LEVEL);
								// skip any chunks that Minecraft is going to render

								if (renderer.vanillaRenderedChunks.contains(new ChunkPos(chunkPos.posX, chunkPos.posZ)))
								{
									continue;
								}

								try
								{
									boolean disableFix = false;
									if (lodDim.doesDataExist(posToRender.clone()))
									{
										short[] lodData = lodDim.getData(posToRender);
										short[][][] adjData = new short[2][2][];
										for (int x : new int[]{0, 1})
										{
											posToRender.changeParameters(detailLevel, posX + x * 2 - 1, posZ);
											if (!renderer.vanillaRenderedChunks.contains(posToRender.getChunkPos())
													    && (nodeToRender.containsKey(posToRender) || disableFix))
												adjData[0][x] = lodDim.getData(posToRender);
										}

										for (int z : new int[]{0, 1})
										{
											posToRender.changeParameters(detailLevel, posX, posZ + z * 2 - 1);
											if (!renderer.vanillaRenderedChunks.contains(posToRender.getChunkPos())
													    && (nodeToRender.containsKey(posToRender) || disableFix))
												adjData[1][z] = lodDim.getData(posToRender);
										}
										posToRender.changeParameters(detailLevel, posX, posZ);

										LodConfig.CLIENT.lodTemplate.get().template.addLodToBuffer(currentBuffer, playerBlockPos, lodData, adjData,
												posToRender, renderer.previousDebugMode);
									}
								} catch (ArrayIndexOutOfBoundsException e)
								{
									return false;
								}

							}// for pos to in list to render

							// the thread executed successfully
							return true;
						};// buffer builder worker thread


						nodeToRenderThreads.add(dataToRenderThread);
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
			} catch (
					  Exception e)

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
				uploadBuffers();
				bufferLock.unlock();
			}

		});

		mainGenThread.execute(thread);

		return;
	}


	/**
	 * Move the center of this LodDimension and move all owned
	 * regions over by the given x and z offset. <br><br>
	 * <p>
	 * Synchronized to prevent multiple moves happening on top of each other.
	 */
	public synchronized void move(RegionPos regionOffset, int width)
	{
		int xOffset = regionOffset.x;
		int zOffset = regionOffset.z;

		// if the x or z offset is equal to or greater than
		// the total size, just delete the current data
		// and update the centerX and/or centerZ
		if (Math.abs(xOffset) >= width || Math.abs(zOffset) >= width)
		{
			for (int x = 0; x < width; x++)
			{
				for (int z = 0; z < width; z++)
				{
					setsToRender[x][z] = null;
				}
			}

			// update the new center
			center.x += xOffset;
			center.z += zOffset;

			return;
		}


		// X
		if (xOffset > 0)
		{
			// move everything over to the left (as the center moves to the right)
			for (int x = 0; x < width; x++)
			{
				for (int z = 0; z < width; z++)
				{
					if (x + xOffset < width)
						setsToRender[x][z] = setsToRender[x + xOffset][z];
					else
						setsToRender[x][z] = null;
				}
			}
		} else
		{
			// move everything over to the right (as the center moves to the left)
			for (int x = width - 1; x >= 0; x--)
			{
				for (int z = 0; z < width; z++)
				{
					if (x + xOffset >= 0)
						setsToRender[x][z] = setsToRender[x + xOffset][z];
					else
						setsToRender[x][z] = null;
				}
			}
		}


		// Z
		if (zOffset > 0)
		{
			// move everything up (as the center moves down)
			for (int x = 0; x < width; x++)
			{
				for (int z = 0; z < width; z++)
				{
					if (z + zOffset < width)
						setsToRender[x][z] = setsToRender[x][z + zOffset];
					else
						setsToRender[x][z] = null;
				}
			}
		} else
		{
			// move everything down (as the center moves up)
			for (int x = 0; x < width; x++)
			{
				for (int z = width - 1; z >= 0; z--)
				{
					if (z + zOffset >= 0)
						setsToRender[x][z] = setsToRender[x][z + zOffset];
					else
						setsToRender[x][z] = null;
				}
			}
		}


		// update the new center
		center.x += xOffset;
		center.z += zOffset;
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
				//if (fullRegen || lodDim.regen[x][z])
				//{
				//if (lodDim.regen[x][z])
				//	ClientProxy.LOGGER.debug("Starting region " + x + " " + z);
				buildableBuffers[x][z].begin(GL11.GL_QUADS, LodRenderer.LOD_VERTEX_FORMAT);
				//}
			}
	}

	/**
	 * Calls end on each of the buildable BufferBuilders.
	 */
	private void closeBuffers(boolean fullRegen, LodDimension lodDim)
	{
		for (int x = 0; x < buildableBuffers.length; x++)
			for (int z = 0; z < buildableBuffers.length; z++)

				if (buildableBuffers[x][z] != null && buildableBuffers[x][z].building() /*&& (fullRegen || lodDim.regen[x][z])*/)
				{
					//if(lodDim.regen[x][z])
					//	ClientProxy.LOGGER.debug("Closing region " + x + " " + z);
					lodDim.regen[x][z] = false;
					buildableBuffers[x][z].end();
				}
	}

	/**
	 * Called from the LodRenderer to create the
	 * BufferBuilders at the right size.
	 */
	private void uploadBuffers()
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
		// don't wait for the lock to open
		// since this is called on the main render thread
		if (bufferLock.tryLock())
		{
			VertexBuffer[][] tmp = drawableVbos;
			drawableVbos = buildableVbos;
			buildableVbos = tmp;

			// the vbos have been swapped
			switchVbos = false;
			bufferLock.unlock();
		}

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
