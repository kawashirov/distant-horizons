package com.backsun.lod.builders;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.lwjgl.opengl.GL11;

import com.backsun.lod.objects.LodChunk;
import com.backsun.lod.objects.LodDimension;
import com.backsun.lod.objects.NearFarBuffer;
import com.backsun.lod.renderer.LodRenderer;
import com.backsun.lod.util.LodConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.common.WorldWorkerManager;

/**
 * This object is used to create NearFarBuffer objects.
 * 
 * @author James Seibel
 * @version 05-06-2021
 */
public class LodBufferBuilder
{
	private Minecraft mc;
	
	/** This holds the thread used to generate new LODs off the main thread. */
	private ExecutorService genThread = Executors.newSingleThreadExecutor();
	
	private LodBuilder lodBuilder;
	
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
	
	/** If this is greater than 0 no new chunk generation requests will be made
	 * this is to prevent chunks from being generated for a long time in an area
	 * the player is no longer in. */
	public int numberOfChunksWaitingToGenerate = 0;
	
	/** how many chunks to generate outside of the player's
	 * view distance at one time. (or more specifically how
	 * many requests to make at one time) */
	public int maxChunkGenRequests = 8;
	
	
	public LodBufferBuilder(LodBuilder newLodBuilder)
	{
		mc = Minecraft.getInstance();
		lodBuilder = newLodBuilder;
	}
	
	
	
	

	/**
	 * Create a thread to asynchronously generate LOD buffers
	 * centered around the given camera X and Z.
	 * <br>
	 * This method will write to the drawableNearBuffers and drawableFarBuffers.
	 * <br>
	 * After the buildable buffers have been generated they must be
	 * swapped with the drawable buffers in the LodRenderer to be drawn.
	 */
	public void generateLodBuffersAsync(LodRenderer renderer, LodDimension lodDim,
			double playerX, double playerZ, int numbChunksWide)
	{
		// only allow one generation process to happen at a time
		if (generatingBuffers)
			return;
		
		if (buildableNearBuffer == null || buildableFarBuffer == null)
			throw new IllegalStateException("generateLodBuffersAsync was called before the buildableNearBuffer and buildableFarBuffer were created.");
		
		
		
		generatingBuffers = true;
		
		
		
		// this seemingly useless math is required,
		// just using (int) playerX/Z doesn't work
		int playerXChunkOffset = ((int) playerX / LodChunk.WIDTH) * LodChunk.WIDTH;
		int playerZChunkOffset = ((int) playerZ / LodChunk.WIDTH) * LodChunk.WIDTH;
		// this is where we will start drawing squares
		// (exactly half the total width)
		int startX = (-LodChunk.WIDTH * (numbChunksWide / 2)) + playerXChunkOffset;
		int startZ = (-LodChunk.WIDTH * (numbChunksWide / 2)) + playerZChunkOffset;
		
		
		
		Thread t = new Thread(()->
		{
			// index of the chunk currently being added to the
			// generation list
			int chunkGenIndex = 0;
			
			ChunkPos[] chunksToGen = new ChunkPos[maxChunkGenRequests];
			int minChunkDist = Integer.MAX_VALUE;
			ChunkPos playerChunkPos = new ChunkPos((int)playerX / LodChunk.WIDTH, (int)playerZ / LodChunk.WIDTH);
			
			
			// generate our new buildable buffers
			buildableNearBuffer.begin(GL11.GL_QUADS, LodRenderer.LOD_VERTEX_FORMAT);
			buildableFarBuffer.begin(GL11.GL_QUADS, LodRenderer.LOD_VERTEX_FORMAT);
			
			// x axis
			for (int i = 0; i < numbChunksWide; i++)
			{
				// z axis
				for (int j = 0; j < numbChunksWide; j++)
				{
					// skip the middle
					// (As the player moves some chunks will overlap or be missing,
					// this is just how chunk loading/unloading works. This can hopefully
					// be hidden with careful use of fog)
					int middle = (numbChunksWide / 2);
					if (isCoordInCenterArea(i, j, middle))
					{
						continue;
					}
					
					
					// set where this square will be drawn in the world
					double xOffset = (LodChunk.WIDTH * i) + // offset by the number of LOD blocks
									startX; // offset so the center LOD block is centered underneath the player
					double yOffset = 0;
					double zOffset = (LodChunk.WIDTH * j) + startZ;
					
					int chunkX = i + (startX / LodChunk.WIDTH);
					int chunkZ = j + (startZ / LodChunk.WIDTH);
					
					LodChunk lod = lodDim.getLodFromCoordinates(chunkX, chunkZ);
					
					if (lod == null || lod.isLodEmpty())
					{
						// generate a new chunk if no chunk currently exists
						// and we aren't waiting on any other chunks to generate
						if (lod == null && numberOfChunksWaitingToGenerate == 0)
						{
							ChunkPos pos = new ChunkPos(chunkX, chunkZ);
							
							// determine if this position is closer to the player
							// than the previous
							int newDistance = playerChunkPos.getChessboardDistance(pos);
							
							if (newDistance < minChunkDist)
							{
								// this chunk is closer, clear any previous
								// positions and update the new minimum distance
								minChunkDist = newDistance;
								
								chunkGenIndex = 0;
								chunksToGen = new ChunkPos[maxChunkGenRequests];
								chunksToGen[chunkGenIndex] = pos;
								chunkGenIndex++;
							}
							else if (newDistance <= minChunkDist)
							{
								// this chunk position is as close or closers than the
								// minimum distance
								if(chunkGenIndex < maxChunkGenRequests)
								{
									// we are still under the number of chunks to generate
									// add this position to the list
									chunksToGen[chunkGenIndex] = pos;
									chunkGenIndex++;
								}
							}
							
						}
						// don't render this null chunk
						continue;
					}
					
					
					BufferBuilder currentBuffer = null;
					if (isCoordinateInNearFogArea(i, j, numbChunksWide / 2))
						currentBuffer = buildableNearBuffer;
					else
						currentBuffer = buildableFarBuffer;
					
					// get the desired LodTemplate and
					// add this LOD to the buffer
					LodConfig.CLIENT.lodTemplate.get().template.
					addLodToBuffer(currentBuffer, lodDim, lod, 
							xOffset, yOffset, zOffset, renderer.debugging);
				}
			}
			
			// TODO add a way for a server side mod to generate chunks requested here
			if(mc.isIntegratedServerRunning())
			{
				// start chunk generation
				for(ChunkPos chunkPos : chunksToGen)
				{
					if(chunkPos == null)
						break;
					
					numberOfChunksWaitingToGenerate++;
					
					LodChunkGenWorker genWorker = new LodChunkGenWorker(chunkPos, renderer, lodBuilder, this, lodDim);
					WorldWorkerManager.addWorker(genWorker);
				}
			}
			
			// finish the buffer building
			buildableNearBuffer.finishDrawing();
			buildableFarBuffer.finishDrawing();
			
			// mark that the buildable buffers as ready to swap
			generatingBuffers = false;
			switchBuffers = true;
		});
		
		genThread.execute(t);
		
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
		return (i >= centerCoordinate - mc.gameSettings.renderDistanceChunks
				&& i <= centerCoordinate + mc.gameSettings.renderDistanceChunks) 
				&& 
				(j >= centerCoordinate - mc.gameSettings.renderDistanceChunks
				&& j <= centerCoordinate + mc.gameSettings.renderDistanceChunks);
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