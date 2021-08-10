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

import com.seibel.lod.enums.LodDetail;
import org.lwjgl.opengl.GL11;

import com.seibel.lod.builders.worldGeneration.LodNodeGenWorker;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.handlers.LodConfig;
import com.seibel.lod.objects.LodQuadTreeDimension;
import com.seibel.lod.objects.LodQuadTreeNode;
import com.seibel.lod.objects.NearFarBuffer;
import com.seibel.lod.render.LodNodeRenderer;
import com.seibel.lod.util.LodUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.WorldWorkerManager;

/**
 * This object is used to create NearFarBuffer objects.
 * 
 * @author James Seibel
 * @version 8-9-2021
 */
public class LodNodeBufferBuilder
{
	private Minecraft mc;

	/** This holds the thread used to generate new LODs off the main thread. */
	private ExecutorService genThread = Executors.newSingleThreadExecutor();

	private LodNodeBuilder LodQuadTreeNodeBuilder;

	/** The buffers that are used to create LODs using near fog */
	public volatile BufferBuilder buildableNearBuffer;
	/** The buffers that are used to create LODs using far fog */
	public volatile BufferBuilder buildableFarBuffer;

	/** if this is true the LOD buffers are currently being
	 * regenerated. */
	public volatile boolean generatingBuffers = false;

	/** if this is true new LOD buffers have been generated
	 * and are waiting to be swapped with the drawable buffers*/
	private volatile boolean switchBuffers = false;

	/** This keeps track of how many chunk generation requests are on going.
	 * This is to prevent chunks from being generated for a long time in an area
	 * the player is no longer in. */
	public volatile AtomicInteger numberOfChunksWaitingToGenerate = new AtomicInteger(0);
	
	

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
	
	
	private BiomeContainer biomeContainer = null;
	private LodQuadTreeDimension previousDimension = null;
	

	/**
	 * Create a thread to asynchronously generate LOD buffers
	 * centered around the given camera X and Z.
	 * <br>
	 * This method will write to the drawable near and far buffers.
	 * <br>
	 * After the buildable buffers have been generated they must be
	 * swapped with the drawable buffers in the LodRenderer to be drawn.
	 */
	public void generateLodBuffersAsync(LodNodeRenderer renderer, LodQuadTreeDimension lodDim,
										double playerX, double playerZ, int numbChunksWide)
	{
		// only allow one generation process to happen at a time
		if (generatingBuffers)
			return;
		
		if (buildableNearBuffer == null || buildableFarBuffer == null)
			throw new IllegalStateException("generateLodBuffersAsync was called before the buildableNearBuffer and buildableFarBuffer were created.");
		
		if (previousDimension != lodDim)
		{
			biomeContainer = LodUtil.getServerWorldFromDimension(lodDim.dimension).getChunk(0, 0, ChunkStatus.EMPTY).getBiomes();
			previousDimension = lodDim;
		}
		
		
		
		generatingBuffers = true;
		
		
		
		// this seemingly useless math is required,
		// just using (int) playerX/Z doesn't work
		int playerXChunkOffset = ((int) playerX / LodQuadTreeNode.CHUNK_WIDTH) * LodQuadTreeNode.CHUNK_WIDTH;
		int playerZChunkOffset = ((int) playerZ / LodQuadTreeNode.CHUNK_WIDTH) * LodQuadTreeNode.CHUNK_WIDTH;
		// this is where we will start drawing squares
		// (exactly half the total width)
		int startX = (-LodQuadTreeNode.CHUNK_WIDTH * (numbChunksWide / 2)) + playerXChunkOffset;
		int startZ = (-LodQuadTreeNode.CHUNK_WIDTH * (numbChunksWide / 2)) + playerZChunkOffset;
		
		
		Thread thread = new Thread(()->
		{
			// index of the chunk currently being added to the
			// generation list
			int chunkGenIndex = 0;
			
			ChunkPos[] chunksToGen = new ChunkPos[maxChunkGenRequests];
			// if we don't have a full number of chunks to generate in chunksToGen
			// we can top it off from the reserve
			ChunkPos[] chunksToGenReserve = new ChunkPos[maxChunkGenRequests];
			int minChunkDist = Integer.MAX_VALUE;
			ChunkPos playerChunkPos = new ChunkPos((int)playerX / LodQuadTreeNode.CHUNK_WIDTH, (int)playerZ / LodQuadTreeNode.CHUNK_WIDTH);
			
			
			// generate our new buildable buffers
			buildableNearBuffer.begin(GL11.GL_QUADS, LodNodeRenderer.LOD_VERTEX_FORMAT);
			buildableFarBuffer.begin(GL11.GL_QUADS, LodNodeRenderer.LOD_VERTEX_FORMAT);

			// x axis
			for (int i = 0; i < numbChunksWide; i++)
			{
				// z axis
				for (int j = 0; j < numbChunksWide; j++) {
					int chunkX = i + Math.floorDiv(startX, LodQuadTreeNode.CHUNK_WIDTH);
					int chunkZ = j + Math.floorDiv(startZ, LodQuadTreeNode.CHUNK_WIDTH);

					// skip any chunks that Minecraft is going to render
					if (isCoordInCenterArea(i, j, (numbChunksWide / 2))
							&& renderer.vanillaRenderedChunks.contains(new ChunkPos(chunkX, chunkZ))) {
						continue;
					}


					// set where this square will be drawn in the world
					double xOffset = (LodQuadTreeNode.CHUNK_WIDTH * i) + // offset by the number of LOD blocks
							startX + // offset so the center LOD block is centered underneath the player
							8; // I'm not sure what this is correcting exactly but with it the chunks line up.
					// 8 works for LODs the size of chunks
					double yOffset = 0;
					double zOffset = (LodQuadTreeNode.CHUNK_WIDTH * j) + startZ + 8;

					LodQuadTreeNode lod = lodDim.getLodFromCoordinates(new ChunkPos(chunkX, chunkZ), LodQuadTreeNode.CHUNK_LEVEL);

					if (lod == null || lod.complexity == DistanceGenerationMode.NONE) {
						// generate a new chunk if no chunk currently exists
						// and we aren't waiting on any other chunks to generate
						if (lod == null && numberOfChunksWaitingToGenerate.get() < maxChunkGenRequests) {
							ChunkPos pos = new ChunkPos(chunkX, chunkZ);

							// alternate determining logic that
							// can be used for debugging
//							if (chunksToGen == null)
//							{
//								chunkGenIndex = 0;
//								chunksToGen = new ChunkPos[maxChunkGenRequests];
//							}
//							
//							if (chunkGenIndex < maxChunkGenRequests)
//							{
//								chunksToGen[chunkGenIndex] = pos;
//								chunkGenIndex++;
//							}


							// determine if this position is closer to the player
							// than the previous
							int newDistance = playerChunkPos.getChessboardDistance(pos);

							// issue #40
							// TODO optimize this code, 
							// using the purely optimized code above we can achieve close to
							// 100% CPU utilization, this code generally achieves 40 - 50%
							// after a certain point; and I'm sure there is a better data
							// structure for this.
							if (newDistance < minChunkDist) {
								// this chunk is closer, clear any previous
								// positions and update the new minimum distance
								minChunkDist = newDistance;

								// move all the old chunks into the reserve
								ChunkPos[] newReserve = new ChunkPos[maxChunkGenRequests];
								int oldToGenIndex = 0;
								int oldReserveIndex = 0;
								for (int tmpIndex = 0; tmpIndex < newReserve.length; tmpIndex++) {
									// we don't check if the boundaries are good since
									// the tmp array will always be the same length
									// as chunksToGen and chunksToGenReserve

									if (chunksToGen[oldToGenIndex] != null) {
										// add all the closest chunks...
										newReserve[tmpIndex] = chunksToGen[oldToGenIndex];
										oldToGenIndex++;
									} else if (chunksToGenReserve[oldReserveIndex] != null) {
										// ...then add all the previous reserve chunks
										// (which are farther away)
										newReserve[tmpIndex] = chunksToGenReserve[oldToGenIndex];
										oldReserveIndex++;
									} else {
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
							} else if (newDistance <= minChunkDist) {
								// this chunk position is as close or closers than the
								// minimum distance
								if (chunkGenIndex < maxChunkGenRequests) {
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
					if (isCoordinateInNearFogArea(i, j, numbChunksWide / 2))
						currentBuffer = buildableNearBuffer;
					else
						currentBuffer = buildableFarBuffer;

					// get the desired LodTemplate and
					// add this LOD to the buffer
					int distance = (int) Math.sqrt(Math.pow((mc.player.getX() - lod.startBlockPos.getX()),2) + Math.pow((mc.player.getZ() - lod.startBlockPos.getZ()),2));
					LodDetail detail;
					/*
					if(distance < 100){
						detail = LodDetail.HALF;
					}else */if(distance < 200){
						detail = LodDetail.QUAD;
					}else if(distance < 400){
						detail = LodDetail.DOUBLE;
					}else{
						detail = LodDetail.SINGLE;
					}
					//detail = LodDetail.SINGLE;
					LodConfig.CLIENT.lodTemplate.get().
							template.addLodToBuffer(currentBuffer, lodDim, lod,
							xOffset , yOffset, zOffset, renderer.debugging, detail);
					/*
					LodDetail detail = LodConfig.CLIENT.lodDetail.get();
					for(int x = 0; x < detail.dataPointLengthCount; x++){
						for(int z = 0; z < detail.dataPointLengthCount; z++) {
							int posX = LodUtil.convertLevelPos(lod.startBlockPos.getX() + (x*detail.dataPointWidth), 0, detail.detailLevel);
							int posZ = LodUtil.convertLevelPos(lod.startBlockPos.getZ() + (z*detail.dataPointWidth), 0, detail.detailLevel);
							LodQuadTreeNode newLod = lodDim.getLodFromCoordinates(posX, posZ, detail.detailLevel);
							System.out.print("printing ");
							System.out.println(newLod);
							if(newLod != null) {
								LodConfig.CLIENT.lodTemplate.get().
										template.addLodToBuffer(currentBuffer, lodDim, newLod,
										xOffset + (x*detail.dataPointWidth), yOffset, zOffset + (z*detail.dataPointWidth), renderer.debugging);
							}
						}
					}
					 */
				}
			}
			
			//ClientProxy.LOGGER.info(numberOfChunksWaitingToGenerate.get());
			
			// issue #19
			// TODO add a way for a server side mod to generate chunks requested here
			if(mc.hasSingleplayerServer())
			{
				ServerWorld serverWorld = LodUtil.getServerWorldFromDimension(lodDim.dimension);
				
				// make sure we have as many chunks to generate as we are allowed
				if (chunkGenIndex < maxChunkGenRequests)
				{
					for(int i = chunkGenIndex, j = 0; i < maxChunkGenRequests; i++, j++)
					{
						chunksToGen[i] = chunksToGenReserve[j];
					}
				}

				// start chunk generation
				for(ChunkPos chunkPos : chunksToGen)
				{
					// don't add null chunkPos (which shouldn't happen anyway)
					// or add more to the generation queue 
					if(chunkPos == null || numberOfChunksWaitingToGenerate.get() >= maxChunkGenRequests)
						break;
					
					// TODO add a list of locations we are waiting to generate so we don't add the same position to the queue multiple times
					
					numberOfChunksWaitingToGenerate.addAndGet(1);
					
					LodNodeGenWorker genWorker = new LodNodeGenWorker(chunkPos, renderer, LodQuadTreeNodeBuilder, this, lodDim, serverWorld, biomeContainer);
					WorldWorkerManager.addWorker(genWorker);
				}
			}

			// finish the buffer building
			buildableNearBuffer.end();
			buildableFarBuffer.end();
			
			// mark that the buildable buffers as ready to swap
			generatingBuffers = false;
			switchBuffers = true;
		});
		
		genThread.execute(thread);
		
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
	
	
	/**
	 * Find the coordinates that are in the center half of the given
	 * 2D matrix, starting at (0,0) and going to (2 * lodRadius, 2 * lodRadius).
	 */
	private static boolean isCoordinateInNearFogArea(int chunkX, int chunkZ, int lodRadius)
	{
		int halfRadius = lodRadius / 2;
		
		return (chunkX >= lodRadius - halfRadius 
				&& chunkX <= lodRadius + halfRadius) 
				&& 
				(chunkZ >= lodRadius - halfRadius
				&& chunkZ <= lodRadius + halfRadius);
	}
	
	
	
	
	
	//===============================//
	// BufferBuilder related methods //
	//===============================//
	
	
	/**
	 * Called from the LodRenderer to create the
	 * BufferBuilders at the right size.
	 * 
	 * @param bufferMaxCapacity
	 */
	public void setupBuffers(int bufferMaxCapacity)
	{
		buildableNearBuffer = new BufferBuilder(bufferMaxCapacity);
		buildableFarBuffer = new BufferBuilder(bufferMaxCapacity);
	}
	
	/**
	 * Swap the drawable and buildable buffers and return
	 * the old drawable buffers.
	 * @param drawableNearBuffer
	 * @param drawableFarBuffer
	 */
	public NearFarBuffer swapBuffers(BufferBuilder drawableNearBuffer, BufferBuilder drawableFarBuffer)
	{
		// swap the BufferBuilders
		BufferBuilder tmp = buildableNearBuffer;
		buildableNearBuffer = drawableNearBuffer;
		drawableNearBuffer = tmp;
		
		tmp = buildableFarBuffer;
		buildableFarBuffer = drawableFarBuffer;
		drawableFarBuffer = tmp;
		
		
		// the buffers have been swapped
		switchBuffers = false;
		
		return new NearFarBuffer(drawableNearBuffer, drawableFarBuffer);
	}
	
	/**
	 * If this is true the buildable near and far
	 * buffers have been generated and are ready to be
	 * sent to the LodRenderer. 
	 */
	public boolean newBuffersAvaliable() 
	{
		return switchBuffers;
	}
	
	
	
	
}