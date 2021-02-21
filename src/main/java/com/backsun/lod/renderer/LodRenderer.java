package com.backsun.lod.renderer;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.lwjgl.opengl.GL11;

import com.backsun.lod.objects.LodChunk;
import com.backsun.lod.objects.LodDimension;
import com.backsun.lod.util.LodConfig;
import com.backsun.lod.util.ReflectionHandler;
import com.backsun.lod.util.enums.ColorDirection;
import com.backsun.lod.util.enums.FogDistance;
import com.backsun.lod.util.enums.FogQuality;
import com.backsun.lod.util.enums.LodLocation;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.chunk.Chunk;

/**
 * @author James Seibel
 * @version 2-13-2021
 */
public class LodRenderer
{
	/** If true the LODs colors will be replaced with
	 * a checkerboard, this can be used for debugging. */
	public boolean debugging = false;
	
	private Minecraft mc;
	private GameRenderer gameRender;
	private IProfiler profiler;
	private float farPlaneDistance;
	// make sure this is an even number, or else it won't align with the chunk grid
	/** this is the total width of the LODs (I.E the diameter, not the radius) */
	private static final int LOD_CHUNK_DISTANCE_RADIUS = 6;
	
	private Tessellator tessellator;
	private BufferBuilder bufferBuilder;
	
	/**
	 * This is an array of 0's used to clear old 
	 * ByteBuffers when they need to be rebuilt.
	 */
	byte[] clearBytes;
	
	private ReflectionHandler reflectionHandler;
	
	public LodDimension lodDimension = null;
	
	
	
	private int maxNumbThreads = Runtime.getRuntime().availableProcessors();
	/** How many threads should be used for building the render buffer. */
	private int numbBufferThreads = 1;
	private ArrayList<BuildBufferThread> bufferThreads = new ArrayList<BuildBufferThread>();
	private volatile BufferBuilder[] nearBuffers = new BufferBuilder[maxNumbThreads];
	private volatile BufferBuilder[] farBuffers = new BufferBuilder[maxNumbThreads];
	private ExecutorService bufferThreadPool = Executors.newFixedThreadPool(maxNumbThreads);
	/*
	 * this is the maximum number of bytes a buffer
	 * would ever have to hold at once (this prevents the buffer
	 * from having to resize and thus save performance)
	 */
	private int bufferMaxCapacity = 0;
	
	/** This is used to determine if the LODs should be regenerated */
	private int previousChunkRenderDistance = 0;
	/** This is used to determine if the LODs should be regenerated */
	private int prevChunkX = 0;
	/** This is used to determine if the LODs should be regenerated */
	private int prevChunkZ = 0;
	/** This is used to determine if the LODs should be regenerated */
	private FogDistance prevFogDistance = FogDistance.NEAR_AND_FAR;
	
	/** if this is true the LODs should be regenerated */
	private boolean regen = false;
	
	private volatile boolean regenerating = false;
	
	
	
	public LodRenderer()
	{
		mc = Minecraft.getInstance();
		gameRender = mc.gameRenderer;
		
		// for some reason "Tessellator.getInstance()" won't work here, we have to create a new one
		tessellator = new Tessellator(2097152);
		bufferBuilder = tessellator.getBuffer();
		
		reflectionHandler = new ReflectionHandler();
	}
	
	private ExecutorService genThread = Executors.newSingleThreadExecutor();
	private ExecutorService loadQueueThread = Executors.newSingleThreadExecutor();
	
	
	public void drawLODs(LodDimension newDimension, float partialTicks, IProfiler newProfiler)
	{
		// should the LODs be regenerated?
		if ((int)mc.player.getPosX() / LodChunk.WIDTH != prevChunkX ||
			(int)mc.player.getPosZ() / LodChunk.WIDTH != prevChunkZ ||
			previousChunkRenderDistance != mc.gameSettings.renderDistanceChunks ||
			prevFogDistance != LodConfig.COMMON.fogDistance.get() ||
			lodDimension != newDimension)
		{
			regen = true;
			
			prevChunkX = (int)mc.player.getPosX() / LodChunk.WIDTH;
			prevChunkZ = (int)mc.player.getPosZ() / LodChunk.WIDTH;
			prevFogDistance = LodConfig.COMMON.fogDistance.get();
		}
		else
		{
			// nope, the player hasn't moved, the
			// render distance hasn't changed, and
			// the dimension is the same
			regen = false;
		}
		
		profiler = newProfiler;
		lodDimension = newDimension;
		if (lodDimension == null)
		{
			// if there aren't any loaded LodChunks
			// don't try drawing anything
			return;
		}
		
		
		
		
		
		
		// used for debugging and viewing how long different processes take
		profiler.startSection("LOD_setup");
		
		@SuppressWarnings("unused")
		long startTime = System.nanoTime();
		if (LodConfig.COMMON.drawCheckerBoard.get())
		{
			if (debugging != LodConfig.COMMON.drawCheckerBoard.get())
				regen = true;
			debugging = true;
		}
		else
		{
			if (debugging != LodConfig.COMMON.drawCheckerBoard.get())
				regen = true;
			debugging = false;
		}
		
		
		// color setup
		int alpha = 255; // 0 - 255
		
		Color red = new Color(255, 0, 0, alpha);
		Color black = new Color(0, 0, 0, alpha);
		Color white = new Color(255, 255, 255, alpha);
		@SuppressWarnings("unused")
		Color invisible = new Color(0,0,0,0);
		@SuppressWarnings("unused")
		Color error = new Color(255, 0, 225, alpha); // bright pink
		
		
		
		// get the camera location
		ActiveRenderInfo renderInfo = mc.gameRenderer.getActiveRenderInfo();
        Vector3d projectedView = renderInfo.getProjectedView();
		double cameraX = projectedView.x;
		double cameraY = projectedView.y;
		double cameraZ = projectedView.z;

		

		
		// determine how far the game's render distance is currently set
		int renderDistWidth = mc.gameSettings.renderDistanceChunks;
		farPlaneDistance = renderDistWidth * LodChunk.WIDTH;
		
		// set how big the LODs will be and how far they will go
		int totalLength = (int) farPlaneDistance * LOD_CHUNK_DISTANCE_RADIUS * 2;
		int numbChunksWide = (totalLength / LodChunk.WIDTH);
		
		// this seemingly useless math is required,
		// just using (int) camera doesn't work
		int playerXChunkOffset = ((int) cameraX / LodChunk.WIDTH) * LodChunk.WIDTH;
		int playerZChunkOffset = ((int) cameraZ / LodChunk.WIDTH) * LodChunk.WIDTH;
		// this where we will start drawing squares
		// (exactly half the total width)
		int startX = (-LodChunk.WIDTH * (numbChunksWide / 2)) + playerXChunkOffset;
		int startZ = (-LodChunk.WIDTH * (numbChunksWide / 2)) + playerZChunkOffset;
		
		
		// this is where we store the LOD objects
		AxisAlignedBB lodArray[][] = new AxisAlignedBB[numbChunksWide][numbChunksWide];
		// this is where we store the color for each LOD object
		Color colorArray[][] = new Color[numbChunksWide][numbChunksWide];
		
		
		
		
		//=================//
		// create the LODs //
		//=================//
		
		
		profiler.endStartSection("LOD_generation");
		
		if (regen)
		{
			Thread t = new Thread(()->
			{
				int numbChunksGen = 0; 
				int maxChunkGen = 640;
				
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
						if (RenderUtil.isCoordinateInLoadedArea(i, j, middle))
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
						
						LodChunk lod = lodDimension.getLodFromCoordinates(chunkX, chunkZ);
						if (lod == null)
						{
							// note: for some reason if any color or lod object are set here
							// it causes the game to use 100% gpu, all of it undefined in the debug menu
							// and drop to ~6 fps.
	//						colorArray[i][j] = null;
	//						lodArray[i][j] = null;
							
							// TODO this partially works, but not fully
							if (numbChunksGen < maxChunkGen)
							{
//								if (lod == null)
//								{
//									LodChunk placeholder = new LodChunk();
//									placeholder.x = chunkX;
//									placeholder.z = chunkZ;
//									placeholder.colors[ColorDirection.TOP.value] = error;
//									lodDimension.addLod(placeholder);
//								}
								
	//							Thread loadT = new Thread(()-> {
								Chunk newChunk = mc.world.getChunk(chunkX, chunkZ);
								lodDimension.addLod(new LodChunk(newChunk, newChunk.getWorld()));
								System.out.println(chunkX + "," + chunkZ + "\t" + numbChunksGen);
	//							});
	//							loadQueueThread.execute(loadT);
								numbChunksGen++;
							}
							
							continue;
						}
						
						Color c = new Color(
								(lod.colors[ColorDirection.TOP.value].getRed()),
								(lod.colors[ColorDirection.TOP.value].getGreen()),
								(lod.colors[ColorDirection.TOP.value].getBlue()),
								lod.colors[ColorDirection.TOP.value].getAlpha());
						
						
						
						if (!debugging)
						{
							// add the color to the array
							colorArray[i][j] = c;
						}
						else
						{
							// if debugging draw the squares as a black and white checker board
							if ((chunkX + chunkZ) % 2 == 0)
								c = white;
							else
								c = black;
							// draw the first square as red
							if (i == 0 && j == 0)
								c = red;
							
							colorArray[i][j] = c;
						}
						
						
						// add the new box to the array
						int topPoint = getLodHeightPoint(lod.top);
						int bottomPoint = getLodHeightPoint(lod.bottom);
						
						// don't draw an LOD if it is empty
						if (topPoint == -1 && bottomPoint == -1)
							continue;
						
						lodArray[i][j] = new AxisAlignedBB(0, bottomPoint, 0, LodChunk.WIDTH, topPoint, LodChunk.WIDTH).offset(xOffset, yOffset, zOffset);
					}
				}
			});
			t.run();
//			genThread.execute(t);
		}
		
		
		
		
		
		//===========================//
		// GL settings for rendering //
		//===========================//
		
		profiler.endStartSection("LOD_setup");
		// set the required open GL settings
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_COLOR_MATERIAL);
		
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
		
		RenderSystem.pushMatrix();
        RenderSystem.rotatef(renderInfo.getPitch(), 1, 0, 0); // Fixes camera rotation.
        RenderSystem.rotatef(renderInfo.getYaw() + 180, 0, 1, 0); // Fixes camera rotation.
        RenderSystem.translated(-cameraX, -cameraY, -cameraZ);
		
//		setupProjectionMatrix(partialTicks);
//		setupLighting(partialTicks);
		setupBufferThreads(lodArray);
		
		
		
		
		
		
		
		
		
		
		
		//===========//
		// rendering //
		//===========//
		
		profiler.endStartSection("LOD build buffer");
		if (regen)
			generateLodBuffers(lodArray, colorArray, LodConfig.COMMON.fogDistance.get());
		
		profiler.endStartSection("LOD draw");
		
		switch(LodConfig.COMMON.fogDistance.get())
		{
		case NEAR_AND_FAR:
			profiler.startSection("LOD draw near");
			setupFog(FogDistance.NEAR, reflectionHandler.getFogQuality());
			sendLodsToGpuAndDraw(nearBuffers);
			profiler.endSection();
			
//			profiler.startSection("LOD draw far");
//			setupFog(FogDistance.FAR, reflectionHandler.getFogQuality());
//			sendLodsToGpuAndDraw(farBuffers);
//			profiler.endSection();
			break;
		case NEAR:
			profiler.endStartSection("LOD draw near");
			setupFog(FogDistance.NEAR, reflectionHandler.getFogQuality());
			sendLodsToGpuAndDraw(nearBuffers);
			break;
		case FAR:
			profiler.endStartSection("LOD draw far");
			setupFog(FogDistance.FAR, reflectionHandler.getFogQuality());
			sendLodsToGpuAndDraw(farBuffers);
			break;
		}
		
		
		
		
		//=========//
		// cleanup //
		//=========//
		
		profiler.endStartSection("LOD_cleanup");
		
		
		// this must be done otherwise other parts of the screen may be drawn with a fog effect
		// IE the GUI
		RenderSystem.disableFog();
		
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_LIGHT2);
		GL11.glDisable(GL11.GL_COLOR_MATERIAL);
		GL11.glDisable(GL11.GL_CULL_FACE);
		
		// undo any projection matrix changes we did to prevent other renders
		// from being corrupted
		RenderSystem.popMatrix();
		
		
		// this can't be called until after the buffers are built
		// because otherwise the buffers may be set to the wrong size
		previousChunkRenderDistance = mc.gameSettings.renderDistanceChunks;
		
		
		// This is about how long this whole process should take
		// 16 ms = 60 hz
		@SuppressWarnings("unused")
		long endTime = System.nanoTime();
	}


	
	
	
	
	
	
	
	









	/**
	 * draw an array of cubes (or squares) with the given colors.
	 * @param lods bounding boxes to draw
	 * @param colors color of each box to draw
	 */
	private void generateLodBuffers(AxisAlignedBB[][] lods, Color[][] colors, FogDistance fogDistance)
	{
		List<Future<NearFarBuffer>> bufferFutures = new ArrayList<>();
		// TODO this should change based on whether we are using near/far or both fog settings
		bufferMaxCapacity = (lods.length * lods.length * (6 * 4 * ((3 * 4) + (4 * 4)))) / numbBufferThreads; 
		
		for(int i = 0; i < numbBufferThreads; i++)
		{
			if (nearBuffers[i] == null || previousChunkRenderDistance != mc.gameSettings.renderDistanceChunks)
			{
				nearBuffers[i] = new BufferBuilder(bufferMaxCapacity); //ByteBuffer.allocateDirect(bufferMaxCapacity);
//				nearBuffers[i].order(ByteOrder.LITTLE_ENDIAN);
				
				farBuffers[i] = new BufferBuilder(bufferMaxCapacity); //ByteBuffer.allocateDirect(bufferMaxCapacity);
//				farBuffers[i].order(ByteOrder.LITTLE_ENDIAN);
				
				clearBytes = new byte[bufferMaxCapacity];
			}
			
			if (regen)
			{
				// this is the best way I could find to
				// overwrite the old data
				// (which needs to be done otherwise old
				// LODs may be drawn)
				nearBuffers[i].byteBuffer.clear();
				nearBuffers[i].byteBuffer.put(clearBytes);
				nearBuffers[i].byteBuffer.clear();
				
				farBuffers[i].byteBuffer.clear();
				farBuffers[i].byteBuffer.put(clearBytes);
				farBuffers[i].byteBuffer.clear();
			}
			
//			int pos = bufferBuilder.byteBuffer.position();
//			nearBuffers[i].byteBuffer.position(pos);
//			farBuffers[i].byteBuffer.position(pos);
			
			bufferThreads.get(i).setNewData(nearBuffers[i], farBuffers[i], fogDistance, lods, colors, i, numbBufferThreads);
		}
		
		try
		{
			bufferFutures = bufferThreadPool.invokeAll(bufferThreads);
		}
		catch (InterruptedException e)
		{
			// this should never happen, but just in case
			e.printStackTrace();
		}
		
		for(int i = 0; i < numbBufferThreads; i++)
		{
			try
			{
				nearBuffers[i] = bufferFutures.get(i).get().nearBuffer;
				farBuffers[i] = bufferFutures.get(i).get().farBuffer;
				
				nearBuffers[i].finishDrawing();
				farBuffers[i].finishDrawing();
			}
			catch(CancellationException | ExecutionException| InterruptedException e)
			{
				// this should never happen, but just in case
				e.printStackTrace();
			}
		}
		
	}
	
	private void sendLodsToGpuAndDraw(BufferBuilder[] nearBuffers)
	{
		for(int i = 0; i < numbBufferThreads; i++)
		{
			profiler.startSection("LOD setup");
//			int pos = bufferBuilder.byteBuffer.position();
//			nearBuffers[i].byteBuffer.position(pos);
			
			bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
			bufferBuilder.byteBuffer.clear();
			bufferBuilder.putBulkData(nearBuffers[i].byteBuffer);
			
			profiler.endStartSection("LOD draw");
			tessellator.draw();
			
			bufferBuilder.byteBuffer.clear(); // this is required otherwise nothing is drawn
			profiler.endSection();
		}
	}
	
	
	
	
	
	
	
	//=================//
	// Setup Functions //
	//=================//
	
	@SuppressWarnings("deprecation")
	private void setupFog(FogDistance fogDistance, FogQuality fogQuality)
	{
		if(fogQuality == FogQuality.OFF)
		{
			RenderSystem.disableFog();
			return;
		}
		
		if(fogDistance == FogDistance.NEAR_AND_FAR)
		{
			throw new IllegalArgumentException("setupFog only accepts NEAR or FAR fog distances.");
		}

		// the multipliers are percentages
		// of the regular view distance.
		
		if(fogDistance == FogDistance.NEAR)
		{
			// the reason that I wrote fogEnd then fogStart backwards
			// is because we are using fog backwards to how
			// it is normally used, with it hiding near objects
			// instead of far objects.
			
			if (fogQuality == FogQuality.FANCY)
			{
				RenderSystem.fogEnd(farPlaneDistance * 0.3f * LOD_CHUNK_DISTANCE_RADIUS);
				RenderSystem.fogStart(farPlaneDistance * 0.35f * LOD_CHUNK_DISTANCE_RADIUS);
			}
			else if(fogQuality == FogQuality.FAST)
			{
				// for the far fog of the normal chunks
				// to start right where the LODs' end use:
				// end = 0.8f, start = 1.5f
				
				RenderSystem.fogEnd(farPlaneDistance * 1.5f);
				RenderSystem.fogStart(farPlaneDistance * 2.0f);
			}
		}
		else if(fogDistance == FogDistance.FAR)
		{
			if (fogQuality == FogQuality.FANCY)
			{
				RenderSystem.fogStart(farPlaneDistance * 0.78f * LOD_CHUNK_DISTANCE_RADIUS);
				RenderSystem.fogEnd(farPlaneDistance * 1.0f * LOD_CHUNK_DISTANCE_RADIUS);
			}
			else if(fogQuality == FogQuality.FAST)
			{
				RenderSystem.fogStart(farPlaneDistance * 0.5f * LOD_CHUNK_DISTANCE_RADIUS);
				RenderSystem.fogEnd(farPlaneDistance * 0.75f * LOD_CHUNK_DISTANCE_RADIUS);
			}
		}
		
		RenderSystem.fogDensity(0.1f);
		RenderSystem.enableFog();
	}
	

	
	/**
	 * create a new projection matrix and send it over to the GPU
	 * @param partialTicks how many ticks into the frame we are
	 * @return true if the matrix was successfully created and sent to the GPU, false otherwise
	 */
	private void setupProjectionMatrix(float partialTicks)
	{
//		Project.gluPerspective(getFov(partialTicks, true), (float) mc.currentScreen.width / (float) mc.currentScreen.height, 0.5F, farPlaneDistance * 12);
		gameRender.resetProjectionMatrix(getCustomProjectionMatrix(partialTicks, false));
		
		return;
	}
	/**
	 * Almost an exact copy of what is in GameRenderer
	 */
	public Matrix4f getCustomProjectionMatrix(float partialTicks, boolean useFovSetting)
	{
		ActiveRenderInfo activeRenderInfoIn = mc.gameRenderer.getActiveRenderInfo();
		
		return Matrix4f.perspective(
				gameRender.getFOVModifier(activeRenderInfoIn, partialTicks, useFovSetting), 
				(float)this.mc.getMainWindow().getFramebufferWidth() / (float)this.mc.getMainWindow().getFramebufferHeight(), 
				0.5F, 
				this.farPlaneDistance * LOD_CHUNK_DISTANCE_RADIUS * 2);
   }
	
	
	/**
	 * setup the lighting to be used for the LODs
	 */
	private void setupLighting(float partialTicks)
	{
		GL11.glEnable(GL11.GL_COLOR_MATERIAL); // set the color to be used as the material (this allows lighting to be enabled)
		
		// this isn't perfect right now, but it looks pretty good at 50% brightness
		float sunBrightness = mc.world.getSunBrightness(partialTicks);
		float skyHasLight = 1.0f; //mc.world.provider.hasSkyLight()? 1.0f : 0.15f;
		float gammaMultiplyer = ((float)mc.gameSettings.gamma * 0.5f + 0.5f);
		float lightStrength = sunBrightness * skyHasLight * gammaMultiplyer;
		float lightAmbient[] = {lightStrength, lightStrength, lightStrength, 1.0f};
        	
		ByteBuffer temp = ByteBuffer.allocateDirect(16);
		temp.order(ByteOrder.nativeOrder());
		GL11.glLightfv(GL11.GL_LIGHT2, GL11.GL_AMBIENT, (FloatBuffer) temp.asFloatBuffer().put(lightAmbient).flip());
		GL11.glEnable(GL11.GL_LIGHT2); // Enable the above lighting
		
		RenderSystem.enableLighting();
	}
	
	
	private void setupBufferThreads(AxisAlignedBB[][] lods)
	{
		if (numbBufferThreads != bufferThreads.size())
		{
			bufferMaxCapacity = (lods.length * lods.length * (6 * 4 * ((3 * 4) + (4 * 4)))) / numbBufferThreads;
			clearBytes = new byte[bufferMaxCapacity];
			
			bufferThreads.clear();
			for(int i = 0; i < numbBufferThreads; i++)
				bufferThreads.add(new BuildBufferThread());
			regen = true;
			
			for(int i = 0; i < maxNumbThreads; i++)
			{
				nearBuffers[i] = new BufferBuilder(bufferMaxCapacity); //ByteBuffer.allocateDirect(bufferMaxCapacity);
//				nearBuffers[i].order(ByteOrder.LITTLE_ENDIAN);
				
				farBuffers[i] = new BufferBuilder(bufferMaxCapacity); //ByteBuffer.allocateDirect(bufferMaxCapacity);
//				farBuffers[i].order(ByteOrder.LITTLE_ENDIAN);
			}
		}
	}
	
	
	
	
	
	
	/**
	 * Returns -1 if there are no valid points
	 */
	private int getLodHeightPoint(short[] heightPoints)
	{
		if (heightPoints[LodLocation.NE.value] != -1)
			return heightPoints[LodLocation.NE.value];
		if (heightPoints[LodLocation.NW.value] != -1)
			return heightPoints[LodLocation.NW.value];
		if (heightPoints[LodLocation.SE.value] != -1)
			return heightPoints[LodLocation.NE.value];
		return heightPoints[LodLocation.NE.value];
	}
	
	
	public double getFov(float partialTicks, boolean useFovSetting)
	{
		return mc.gameRenderer.getFOVModifier(mc.gameRenderer.getActiveRenderInfo(), partialTicks, useFovSetting);
	}
	
	
	
}