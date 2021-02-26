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

import com.backsun.lod.builders.BuildBufferThread;
import com.backsun.lod.handlers.ReflectionHandler;
import com.backsun.lod.objects.LodChunk;
import com.backsun.lod.objects.LodDimension;
import com.backsun.lod.objects.NearFarBuffer;
import com.backsun.lod.util.LodConfig;
import com.backsun.lod.util.enums.ColorDirection;
import com.backsun.lod.util.enums.FogDistance;
import com.backsun.lod.util.enums.FogQuality;
import com.backsun.lod.util.enums.LodCorner;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;

/**
 * @author James Seibel
 * @version 2-24-2021
 */
public class LodRenderer
{
	/** this is the light used when rendering the LODs,
	 * it should be something different than what is used by Minecraft */
	private static final int LOD_GL_LIGHT_NUMBER = GL11.GL_LIGHT2;

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
	
	private ReflectionHandler reflectionHandler;
	
	public LodDimension lodDimension = null;
	
	
	/** Total number of CPU cores available to the Java VM */
	private int maxNumbThreads = Runtime.getRuntime().availableProcessors();
	/** How many threads should be used for building render buffers */
	private int numbBufferThreads = maxNumbThreads;
	/** This stores all the BuildBufferThread objects for each CPU core */
	private ArrayList<BuildBufferThread> bufferThreads = new ArrayList<BuildBufferThread>();
	/** The buffers that are used to draw LODs using near fog */
	private volatile BufferBuilder[] drawableNearBuffers = null;
	/** The buffers that are used to draw LODs using far fog */
	private volatile BufferBuilder[] drawableFarBuffers = null;
	
	/** The buffers that are used to create LODs using near fog */
	private volatile BufferBuilder[] buildableNearBuffers = null;
	/** The buffers that are used to create LODs using far fog */
	private volatile BufferBuilder[] buildableFarBuffers = null;
	
	/** If we have more CPU cores than LOD rows to draw this tells
	 * which drawable buffers will and won't be used. */
	private boolean[] shouldDrawBuffer = new boolean[maxNumbThreads];
	
	/** This holds the threads used to generate the LOD buffers */
	private ExecutorService bufferThreadPool = Executors.newFixedThreadPool(maxNumbThreads);
	/** This holds the thread used to generate new LODs off the main thread. */
	private ExecutorService genThread = Executors.newSingleThreadExecutor();
	
	/** This is used to determine if the LODs should be regenerated */
	private int previousChunkRenderDistance = 0;
	/** This is used to determine if the LODs should be regenerated */
	private int prevChunkX = 0;
	/** This is used to determine if the LODs should be regenerated */
	private int prevChunkZ = 0;
	/** This is used to determine if the LODs should be regenerated */
	private FogDistance prevFogDistance = FogDistance.NEAR_AND_FAR;
	
	/** if this is true the LOD buffers should be regenerated,
	 * provided they aren't already being regenerated. */
	private boolean regen = false;
	/** if this is true the LOD buffers are currently being
	 * regenerated. */
	private volatile boolean regenerating = false;
	/** if this is true new LOD buffers have been generated 
	 * and are waiting to be swapped with the drawable buffers*/
	private volatile boolean switchBuffers = false;
	
	
	
	
	
	
	public LodRenderer()
	{
		mc = Minecraft.getInstance();
		gameRender = mc.gameRenderer;
		
		// for some reason "Tessellator.getInstance()" won't work here, we have to create a new one
		tessellator = new Tessellator(2097152); // the number here is what is used by the default Tessellator
		bufferBuilder = tessellator.getBuffer();
		
		reflectionHandler = new ReflectionHandler();
	}
	
	
	/**
	 * Besides drawing the LODs this method also starts
	 * the async process of generating the Buffers that hold those LODs.
	 * 
	 * @param newDimension The dimension to draw, if null doesn't replace the current dimension.
	 * @param partialTicks how far into the current tick this method was called.
	 */
	public void drawLODs(LodDimension newDimension, float partialTicks, IProfiler newProfiler)
	{		
		if (lodDimension == null && newDimension == null)
		{
			// if there aren't any loaded LodChunks
			// don't try drawing anything
			return;
		}
		
		
		
		
		
		//===============//
		// initial setup //
		//===============//
		
		
		// used for debugging and viewing how long different processes take
		mc.getProfiler().endSection();
		mc.getProfiler().startSection("LOD");
		mc.getProfiler().startSection("LOD setup");
		
		ClientPlayerEntity player = mc.player;
		
		// should LODs be regenerated?
		if ((int)player.getPosX() / LodChunk.WIDTH != prevChunkX ||
			(int)player.getPosZ() / LodChunk.WIDTH != prevChunkZ ||
			previousChunkRenderDistance != mc.gameSettings.renderDistanceChunks ||
			prevFogDistance != LodConfig.COMMON.fogDistance.get() ||
			lodDimension != newDimension)
		{
			// yes
			regen = true;
			
			prevChunkX = (int)player.getPosX() / LodChunk.WIDTH;
			prevChunkZ = (int)player.getPosZ() / LodChunk.WIDTH;
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
		
		
		
		
		
		
		//=================//
		// create the LODs //
		//=================//
		
		// only regenerate the LODs if:
		// 1. we want to regenerate LODs
		// 2. we aren't already regenerating the LODs
		// 3. we aren't waiting for the build and draw buffers to swap
		//		(this is to prevent thread conflicts)
		if (regen && !regenerating && !switchBuffers)
		{
			mc.getProfiler().endStartSection("LOD generation");
			regenerating = true;
			
			// this will only be called once, unless the numbBufferThreads changes
			if (numbBufferThreads != bufferThreads.size())
				setupBufferThreads();
			
			// this will mainly happen when the view distance is changed
			if (drawableNearBuffers == null || drawableFarBuffers == null || 
				previousChunkRenderDistance != mc.gameSettings.renderDistanceChunks)
				setupBuffers(numbChunksWide);
			
			// generate the LODs on a separate thread to prevent stuttering or freezing
			genThread.execute(createLodBufferGenerationThread(player.getPosX(), player.getPosZ(), numbChunksWide));
		}
		
		// replace the buffers used to draw and build,
		// this is only done when the createLodBufferGenerationThread
		// has finished executing on a parallel thread.
		if (switchBuffers)
		{
			swapBuffers();
			switchBuffers = false;
		}
		
		
		
		
		
		//===========================//
		// GL settings for rendering //
		//===========================//
		
		// set the required open GL settings
//		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
//		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
//		GL11.glDisable(GL11.GL_TEXTURE_2D);
//		GL11.glEnable(GL11.GL_CULL_FACE);
//		GL11.glEnable(GL11.GL_COLOR_MATERIAL);
//		
//		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
		
		RenderSystem.pushMatrix();
        RenderSystem.rotatef(renderInfo.getPitch(), 1, 0, 0); // Fixes camera rotation.
        RenderSystem.rotatef(renderInfo.getYaw() + 180, 0, 1, 0); // Fixes camera rotation.
        RenderSystem.translated(-cameraX, -cameraY, -cameraZ);
		
//		setupProjectionMatrix(partialTicks);
//		setupLighting(partialTicks);
		
		
		
		
		
		
		
		
		
		
		//===========//
		// rendering //
		//===========//
		
		switch(LodConfig.COMMON.fogDistance.get())
		{
		case NEAR_AND_FAR:
			// when drawing NEAR_AND_FAR fog we need 2 draw
			// calls since fog can only go in one direction at a time
			
			mc.getProfiler().endStartSection("LOD draw");
			setupFog(FogDistance.NEAR, reflectionHandler.getFogQuality());
			sendLodsToGpuAndDraw(drawableNearBuffers);
			
			mc.getProfiler().endStartSection("LOD draw");
			setupFog(FogDistance.FAR, reflectionHandler.getFogQuality());
			sendLodsToGpuAndDraw(drawableFarBuffers);
			break;
			
		case NEAR:
			mc.getProfiler().endStartSection("LOD draw");
			setupFog(FogDistance.NEAR, reflectionHandler.getFogQuality());
			sendLodsToGpuAndDraw(drawableNearBuffers);
			break;
			
		case FAR:
			mc.getProfiler().endStartSection("LOD draw");
			setupFog(FogDistance.FAR, reflectionHandler.getFogQuality());
			sendLodsToGpuAndDraw(drawableFarBuffers);
			break;
		}
		
		
		
		
		//=========//
		// cleanup //
		//=========//
		
		mc.getProfiler().endStartSection("LOD cleanup");
		
		// this must be done otherwise other parts of the screen may be drawn with a fog effect
		// IE the GUI
		RenderSystem.disableFog();
		
//		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
//		GL11.glEnable(GL11.GL_TEXTURE_2D);
//		GL11.glDisable(LOD_GL_LIGHT_NUMBER);
//		GL11.glDisable(GL11.GL_COLOR_MATERIAL);
		
		// undo any projection matrix changes we did to prevent other renders
		// from being corrupted
		RenderSystem.popMatrix();
		
		
		// this can't be called until after the buffers are built
		// because otherwise the buffers may be set to the wrong size
		previousChunkRenderDistance = mc.gameSettings.renderDistanceChunks;
		
		
		// end of profiler tracking
		mc.getProfiler().endSection();
	}
	
	
	/**
	 * This is where the actual drawing happens.
	 * 
	 * @param buffers the buffers sent to the GPU to draw
	 */
	private void sendLodsToGpuAndDraw(BufferBuilder[] buffers)
	{
		for(int i = 0; i < numbBufferThreads; i++)
		{
			if (shouldDrawBuffer[i])
			{
				int pos = bufferBuilder.byteBuffer.position();
				buffers[i].byteBuffer.position(pos);
				
				bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
				bufferBuilder.byteBuffer.clear();
				// replace the data in bufferBuilder with the data from the given buffer
				bufferBuilder.putBulkData(buffers[i].byteBuffer);
				
				tessellator.draw();
				
				bufferBuilder.byteBuffer.clear(); // this is required otherwise nothing is drawn
			}
		}
	}
	
	
	
	
	
	
	
	//=================//
	// Setup Functions //
	//=================//
	
	private void setupFog(FogDistance fogDistance, FogQuality fogQuality)
	{
		if(fogQuality == FogQuality.OFF)
		{
			RenderSystem.disableFog();
			return;
		}
		
		if(fogDistance == FogDistance.NEAR_AND_FAR)
		{
			throw new IllegalArgumentException("setupFog doesn't accept the NEAR_AND_FAR fog distance.");
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
		
		RenderSystem.enableFog();
	}
	

	/**
	 * create a new projection matrix and send it over to the GPU
	 * @param partialTicks how many ticks into the frame we are
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
		
		// FIXME
		// this isn't perfect right now, but it looks pretty good at 50% brightness
		float sunBrightness = mc.world.getSunBrightness(partialTicks); // * mc.world.provider.getSunBrightnessFactor(partialTicks);
		float skyHasLight = 1.0f; //mc.world.provider.hasSkyLight()? 1.0f : 0.15f;
		float gammaMultiplyer = (float) mc.gameSettings.gamma * 0.5f + 0.5f;
		float lightStrength = sunBrightness * skyHasLight * gammaMultiplyer;
		float lightAmbient[] = {lightStrength, lightStrength, lightStrength, 1.0f};
        
		ByteBuffer temp = ByteBuffer.allocateDirect(16);
		temp.order(ByteOrder.nativeOrder());
		GL11.glLightfv(LOD_GL_LIGHT_NUMBER, GL11.GL_AMBIENT, (FloatBuffer) temp.asFloatBuffer().put(lightAmbient).flip());
		GL11.glEnable(LOD_GL_LIGHT_NUMBER); // Enable the above lighting
		
		RenderSystem.enableLighting();
	}
	
	/**
	 * create the BuildBufferThreads
	 */
	private void setupBufferThreads()
	{
		bufferThreads.clear();
		for(int i = 0; i < numbBufferThreads; i++)
			bufferThreads.add(new BuildBufferThread());
	}
	
	/**
	 * Create all buffers that will be used.
	 */
	private void setupBuffers(int numbChunksWide)
	{
		drawableNearBuffers = new BufferBuilder[numbBufferThreads];
		drawableFarBuffers = new BufferBuilder[numbBufferThreads];
		
		buildableNearBuffers = new BufferBuilder[numbBufferThreads];
		buildableFarBuffers = new BufferBuilder[numbBufferThreads];
		
		
		// calculate how many chunks wide, at most
		// any thread will have to generate
		int biggestWidth = -1;
		int[] loads = calculateCpuLoadBalance(numbChunksWide, numbBufferThreads);
		for(int i : loads)
			if (i > biggestWidth)
				biggestWidth = i;
		
		
		// calculate the max amount of storage needed (in bytes)
		// by any singular buffer
		// NOTE: most buffers won't use the full amount, but this should prevent
		//		them from needing to allocate more memory (which is a slow progress)
		int bufferMaxCapacity = (numbChunksWide * biggestWidth * (6 * 4 * ((3 * 4) + (4 * 4))));
		
		for(int i = 0; i < numbBufferThreads; i++)
		{
			// TODO complain or do something when memory is too low
			// currently the VM will just crash and complain there is no more memory
			// issue #4
			drawableNearBuffers[i] = new BufferBuilder(bufferMaxCapacity);
			drawableFarBuffers[i] = new BufferBuilder(bufferMaxCapacity);
			
			buildableNearBuffers[i] = new BufferBuilder(bufferMaxCapacity);
			buildableFarBuffers[i] = new BufferBuilder(bufferMaxCapacity);
		}
	}
	
	
	
	
	
	
	
	//======================//
	// Other Misc Functions // 
	//======================//
	
	
	/**
	 * @Returns -1 if there are no valid points
	 */
	private int getValidHeightPoint(short[] heightPoints)
	{
		if (heightPoints[LodCorner.NE.value] != -1)
			return heightPoints[LodCorner.NE.value];
		if (heightPoints[LodCorner.NW.value] != -1)
			return heightPoints[LodCorner.NW.value];
		if (heightPoints[LodCorner.SE.value] != -1)
			return heightPoints[LodCorner.NE.value];
		return heightPoints[LodCorner.NE.value];
	}
	
	
	/**
	 * Create a thread to asynchronously generate LOD buffers
	 * centered around the given camera X and Z.
	 * <br>
	 * This thread will write to the drawableNearBuffers and drawableFarBuffers.
	 * <br>
	 * After the buildable buffers have been generated they must be
	 * swapped with the drawable buffers to be drawn.
	 */
	private Thread createLodBufferGenerationThread(double playerX, double playerZ,
			int numbChunksWide)
	{
		// this is where we store the points for each LOD object
		AxisAlignedBB lodArray[][] = new AxisAlignedBB[numbChunksWide][numbChunksWide];
		// this is where we store the color for each LOD object
		Color colorArray[][] = new Color[numbChunksWide][numbChunksWide];
		
		int alpha = 255; // 0 - 255
		Color red = new Color(255, 0, 0, alpha);
		Color black = new Color(0, 0, 0, alpha);
		Color white = new Color(255, 255, 255, alpha);
		@SuppressWarnings("unused")
		Color invisible = new Color(0,0,0,0);
		@SuppressWarnings("unused")
		Color error = new Color(255, 0, 225, alpha); // bright pink
		
		// this seemingly useless math is required,
		// just using (int) camera doesn't work
		int playerXChunkOffset = ((int) playerX / LodChunk.WIDTH) * LodChunk.WIDTH;
		int playerZChunkOffset = ((int) playerZ / LodChunk.WIDTH) * LodChunk.WIDTH;
		// this is where we will start drawing squares
		// (exactly half the total width)
		int startX = (-LodChunk.WIDTH * (numbChunksWide / 2)) + playerXChunkOffset;
		int startZ = (-LodChunk.WIDTH * (numbChunksWide / 2)) + playerZChunkOffset;
		
		Thread t = new Thread(()->
		{
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
					
					LodChunk lod = lodDimension.getLodFromCoordinates(chunkX, chunkZ);
					if (lod == null)
					{
						// note: for some reason if any color or lod objects are set here
						// it causes the game to use 100% gpu; 
						// undefined in the debug menu
						// and drop to ~6 fps.
						colorArray[i][j] = null;
						lodArray[i][j] = null;
						
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
					int topPoint = getValidHeightPoint(lod.top);
					int bottomPoint = getValidHeightPoint(lod.bottom);
					
					// don't draw an LOD if it is empty
					if (topPoint == -1 && bottomPoint == -1)
						continue;
					
					lodArray[i][j] = new AxisAlignedBB(0, bottomPoint, 0, LodChunk.WIDTH, topPoint, LodChunk.WIDTH).offset(xOffset, yOffset, zOffset);
				}
			}
			
			generateLodBuffers(lodArray, colorArray, LodConfig.COMMON.fogDistance.get());
			
			regenerating = false;
			switchBuffers = true;
		});
		return t;
	}
	
	/**
	 * draw an array of boxes with the given colors.
	 * <br><br>
	 * Currently only one color per box is supported.
	 * 
	 * @param lods bounding boxes to draw
	 * @param colors color of each box to draw
	 */
	private void generateLodBuffers(AxisAlignedBB[][] lods, Color[][] colors, FogDistance fogDistance)
	{
		List<Future<NearFarBuffer>> bufferFutures = new ArrayList<>();
		ArrayList<BuildBufferThread> threadsToRun = new ArrayList<>();
		
		int indexToStart = 0;
		int[] threadLoads = calculateCpuLoadBalance(lods.length, numbBufferThreads);
		
		// update the information that the bufferThreads are using
		for(int i = 0; i < numbBufferThreads; i++)
		{
			// if we have more threads than LOD rows to generate
			// don't send the threads to the CPU
			if (threadLoads[i] != 0)
			{
				// update this thread with the latest information
				bufferThreads.get(i).
				setNewData(buildableNearBuffers[i], buildableFarBuffers[i], 
						fogDistance, lods, colors, indexToStart, threadLoads[i]);
				indexToStart += threadLoads[i];
				
				// add this thread to the list of threads we are going to run
				threadsToRun.add(bufferThreads.get(i));
				
				shouldDrawBuffer[i] = true;
			}
			else
			{
				shouldDrawBuffer[i] = false;
			}
		}
		
		// run all the bufferThreads and get their results
		try
		{
			bufferFutures = bufferThreadPool.invokeAll(threadsToRun);
		}
		catch (InterruptedException e)
		{
			// this should never happen, but just in case
			e.printStackTrace();
		}
		
		// update our buildable buffers
		for(int i = 0; i < numbBufferThreads; i++)
		{
			// only replace buffers that actually generated something
			if (threadLoads[i] != 0)
			{
				try
				{
					buildableNearBuffers[i] = bufferFutures.get(i).get().nearBuffer;
					buildableFarBuffers[i] = bufferFutures.get(i).get().farBuffer;
				}
				catch(CancellationException | ExecutionException| InterruptedException e)
				{
					// this should never happen, but just in case
					e.printStackTrace();
				}
			}
		}
	}
	
	
	/**
	 * Swap buildable and drawable buffers.
	 */
	private void swapBuffers()
	{
		for(int i = 0; i < buildableNearBuffers.length; i++)
		{				
			try
			{
				BufferBuilder tmp = buildableNearBuffers[i];
				buildableNearBuffers[i] = drawableNearBuffers[i];
				drawableNearBuffers[i] = tmp;
				
				tmp = buildableFarBuffers[i];
				buildableFarBuffers[i] = drawableFarBuffers[i];
				drawableFarBuffers[i] = tmp;
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	
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
	 * This is a simple implementation of the pigeon hole
	 * principle to try and give each BuildBufferThread a balanced load.
	 * 
	 * @returns an array of ints where each int is how many rows 
	 * that BuildBufferThread should generate
	 */
	private int[] calculateCpuLoadBalance(int numbOfItems, int numbOfThreads)
	{
		int[] cpuLoad = new int[numbOfThreads];
		
		for(int i = 0; i < numbOfItems; i++)
			cpuLoad[i % numbOfThreads]++;
		
		return cpuLoad;
	}
	
	
	public double getFov(float partialTicks, boolean useFovSetting)
	{
		return mc.gameRenderer.getFOVModifier(mc.gameRenderer.getActiveRenderInfo(), partialTicks, useFovSetting);
	}
	
	
	
}