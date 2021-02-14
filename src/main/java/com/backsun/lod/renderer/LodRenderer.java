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
import org.lwjgl.util.glu.Project;

import com.backsun.lod.objects.LodChunk;
import com.backsun.lod.objects.LodDimension;
import com.backsun.lod.util.ReflectionHandler;
import com.backsun.lod.util.enums.ColorDirection;
import com.backsun.lod.util.enums.LodLocation;
import com.backsun.lod.util.fog.FogDistanceMode;
import com.backsun.lod.util.fog.FogQuality;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;

/**
 * @author James Seibel
 * @version 2-10-2021
 */
public class LodRenderer
{
	public boolean debugging = false;
	
	private Minecraft mc;
	private float farPlaneDistance;
	// make sure this is an even number, or else it won't align with the chunk grid
	/** this is the total width of the LODs (I.E the diameter, not the radius) */
	public static final int VIEW_DISTANCE_MULTIPLIER = 12;
	public static final int LOD_WIDTH = 16;
	public static final int MINECRAFT_CHUNK_WIDTH = 16;
	
	private Tessellator tessellator;
	private BufferBuilder bufferBuilder;
	
	/**
	 * This is an array of 0's used to clear old 
	 * ByteBuffers when they need to be rebuilt.
	 */
	byte[] clearBytes;
	
	private ReflectionHandler reflectionHandler;
	
	public LodDimension dimension = null;
	
	
	
	private int maxThreads = Runtime.getRuntime().availableProcessors();
	/** How many threads should be used for building the render buffer. */
	private int numbBufferThreads = maxThreads;
	private ArrayList<BuildBufferThread> bufferThreads = new ArrayList<BuildBufferThread>();
	private volatile ByteBuffer[] buffers = new ByteBuffer[maxThreads];
	private ExecutorService threadPool = Executors.newFixedThreadPool(maxThreads);
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
	
	/** if this is true the LODs should be regenerated */
	private boolean regen = false;
	
	
	
	
	
	public LodRenderer()
	{
		mc = Minecraft.getMinecraft();
		
		// for some reason "Tessellator.getInstance()" won't work here, we have to create a new one
		tessellator = new Tessellator(2097152);
		bufferBuilder = tessellator.getBuffer();
		
		reflectionHandler = new ReflectionHandler();
	}
	
	
	
	public void drawLODs(LodDimension newDimension, float partialTicks)
	{
		if (reflectionHandler.fovMethod == null)
		{
			// don't continue if we can't get the
			// user's FOV
			return;
		}
		
		if (reflectionHandler.fovMethod == null)
		{
			// we aren't able to get the user's
			// FOV, don't render anything
			return;
		}
		
		// should the LODs be regenerated?
		if ((int)Minecraft.getMinecraft().player.posX / LodChunk.WIDTH != prevChunkX ||
			(int)Minecraft.getMinecraft().player.posZ / LodChunk.WIDTH != prevChunkZ ||
			previousChunkRenderDistance != mc.gameSettings.renderDistanceChunks ||
			dimension != newDimension)
		{
			regen = true;
			
			prevChunkX = (int)Minecraft.getMinecraft().player.posX / LodChunk.WIDTH;
			prevChunkZ = (int)Minecraft.getMinecraft().player.posZ / LodChunk.WIDTH;
		}
		else
		{
			// nope, the player hasn't moved, the
			// render distance hasn't changed, and
			// the dimension is the same
			regen = false;
		}
		
		dimension = newDimension;
		if (dimension == null)
		{
			// if there aren't any loaded LodChunks
			// don't try drawing anything
			return;
		}
		
		
		
		
		
		
		// used for debugging and viewing how long different processes take
		mc.mcProfiler.endSection();
		mc.mcProfiler.startSection("LOD");
		mc.mcProfiler.startSection("LOD setup");
		@SuppressWarnings("unused")
		long startTime = System.nanoTime();
				
		
		
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
		Entity player = mc.player;
		double cameraX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
		double cameraY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
		double cameraZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
		
		int playerXChunkOffset = ((int) cameraX / MINECRAFT_CHUNK_WIDTH) * MINECRAFT_CHUNK_WIDTH;
		int playerZChunkOffset = ((int) cameraZ / MINECRAFT_CHUNK_WIDTH) * MINECRAFT_CHUNK_WIDTH;
		
		
		
		// determine how far the game's render distance is currently set
		int renderDistWidth = mc.gameSettings.renderDistanceChunks;
		farPlaneDistance = renderDistWidth * MINECRAFT_CHUNK_WIDTH;
		
		// set how big the LODs will be and how far they will go
		int totalLength = (int) farPlaneDistance * VIEW_DISTANCE_MULTIPLIER;
		int numbChunksWide = (totalLength / LOD_WIDTH);
		
		// this where we will start drawing squares
		// (exactly half the total width)
		int startX = (-LOD_WIDTH * (numbChunksWide / 2)) + playerXChunkOffset;
		int startZ = (-LOD_WIDTH * (numbChunksWide / 2)) + playerZChunkOffset;
		
		
		// this is where we store the LOD objects
		AxisAlignedBB lodArray[][] = new AxisAlignedBB[numbChunksWide][numbChunksWide];
		// this is where we store the color for each LOD object
		Color colorArray[][] = new Color[numbChunksWide][numbChunksWide];
		
		
		
		
		
		//=================//
		// create the LODs //
		//=================//
		
		// TODO create a worker thread to do this
		if (regen)
		{
			mc.mcProfiler.endStartSection("LOD generation");
			
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
					double xOffset = (LOD_WIDTH * i) + // offset by the number of LOD blocks
									startX; // offset so the center LOD block is centered underneath the player
					double yOffset = 0;
					double zOffset = (LOD_WIDTH * j) + startZ;
					
					int chunkX = i + (startX / MINECRAFT_CHUNK_WIDTH);
					int chunkZ = j + (startZ / MINECRAFT_CHUNK_WIDTH);
					
					LodChunk lod = dimension.getLodFromCoordinates(chunkX, chunkZ); // new LodChunk(); //   
					if (lod == null)
					{
						// note: for some reason if any color or lod object are set here
						// it causes the game to use 100% gpu, all of it undefined in the debug menu
						// and drop to ~6 fps.
//						colorArray[i][j] = null;
//						lodArray[i][j] = null;
						
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
					
					lodArray[i][j] = new AxisAlignedBB(0, bottomPoint, 0, LOD_WIDTH, topPoint, LOD_WIDTH).offset(xOffset, yOffset, zOffset);
				}
			}
		}
		
		
		
		
		//===========================//
		// GL settings for rendering //
		//===========================//
		
		// set the required open GL settings
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glLineWidth(2.0f);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_CULL_FACE);
		
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
		
		GlStateManager.translate(-cameraX, -cameraY, -cameraZ);
		
		setProjectionMatrix(partialTicks);
		setupLighting(partialTicks);
		setupBufferThreads(lodArray);
		
		
		
		
		//===========//
		// rendering //
		//===========//
		
		mc.mcProfiler.endStartSection("LOD build buffer");
		if (regen)
			generateLodBuffers(lodArray, colorArray);
		
		mc.mcProfiler.endStartSection("LOD draw setup");
		setupFog(FogDistanceMode.NEAR, reflectionHandler.getFogQuality());
		sendLodsToGpuAndDraw();
		
		
		
		
		
		//=========//
		// cleanup //
		//=========//
		
		mc.mcProfiler.endStartSection("LOD cleanup");
		
		
		// this must be done otherwise other parts of the screen may be drawn with a fog effect
		// IE the GUI
		GlStateManager.disableFog();
		
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_LIGHT1);
		GL11.glDisable(GL11.GL_COLOR_MATERIAL);
		
		// change the perspective matrix back to prevent incompatibilities
		// with other mods that may render during forgeRenderLast
		Project.gluPerspective(reflectionHandler.getFov(mc, partialTicks, true), (float) this.mc.displayWidth / (float) this.mc.displayHeight, 0.05F, this.farPlaneDistance * MathHelper.SQRT_2);
		
		// this can't be called until after the buffers are built
		// because otherwise the buffers may be set to the wrong size
		previousChunkRenderDistance = mc.gameSettings.renderDistanceChunks;
		
		
		
		// This is about how long this whole process should take
		// 16 ms = 60 hz
		@SuppressWarnings("unused")
		long endTime = System.nanoTime();
		
		// end of profiler tracking
		mc.mcProfiler.endSection();
	}



	
	
	
	
	
	
	/**
	 * draw an array of cubes (or squares) with the given colors.
	 * @param lods bounding boxes to draw
	 * @param colors color of each box to draw
	 */
	private void generateLodBuffers(AxisAlignedBB[][] lods, Color[][] colors)
	{
		List<Future<ByteBuffer>> bufferFutures = new ArrayList<>();
		bufferMaxCapacity = (lods.length * lods.length * (6 * 4 * ((3 * 4) + (4 * 4)))) / numbBufferThreads;
		
		for(int i = 0; i < numbBufferThreads; i++)
		{
			if (buffers[i] == null || previousChunkRenderDistance != mc.gameSettings.renderDistanceChunks)
			{
				buffers[i] = ByteBuffer.allocateDirect(bufferMaxCapacity);
				buffers[i].order(ByteOrder.LITTLE_ENDIAN);
			}
			
			if (regen)
			{
				// this is the best way I could find to
				// overwrite the old data
				// (which needs to be done otherwise the old
				// LODs may be drawn)
				buffers[i].clear();
				buffers[i].put(clearBytes);
				buffers[i].clear();
			}
			
			int pos = bufferBuilder.getByteBuffer().position();
			buffers[i].position(pos);
			
			bufferThreads.get(i).setNewData(buffers[i], lods, colors, i, numbBufferThreads);
		}
		
		try
		{
			bufferFutures = threadPool.invokeAll(bufferThreads);
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
				buffers[i] = bufferFutures.get(i).get();
			}
			catch(CancellationException | ExecutionException| InterruptedException e)
			{
				// this should never happen, but just in case
				e.printStackTrace();
			}
		}
		
	}
	
	private void sendLodsToGpuAndDraw()
	{
		for(int i = 0; i < numbBufferThreads; i++)
		{
			int pos = bufferBuilder.getByteBuffer().position();
			buffers[i].position(pos);
			
			bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
			bufferBuilder.getByteBuffer().clear();
			bufferBuilder.putBulkData(buffers[i]);
			
			mc.mcProfiler.endStartSection("LOD draw");
			tessellator.draw();
			mc.mcProfiler.endStartSection("LOD draw setup");
			
			bufferBuilder.getByteBuffer().clear(); // this is required otherwise nothing is drawn
		}
	}
	
	
	
	
	
	
	
	//=================//
	// Setup Functions //
	//=================//
	
	private void setupFog(FogDistanceMode fogMode, FogQuality fogQuality)
	{
		if(fogQuality == FogQuality.OFF)
		{
			GlStateManager.disableFog();
			return;
		}

		// the multipliers are percentages
		// of the regular view distance.
		
		// TODO add the ability to change the fogDistanceMode 
		// in the mod settings
		if(fogMode == FogDistanceMode.NEAR)
		{
			// the reason that I wrote fogEnd then fogStart backwards
			// is because we are using fog backwards to how
			// it is normally used, with it hiding near objects
			// instead of far objects.
			
			if (fogQuality == FogQuality.FANCY || fogQuality == FogQuality.UNKNOWN)
			{
				GlStateManager.setFogEnd(farPlaneDistance * 2.0f);
				GlStateManager.setFogStart(farPlaneDistance * 2.5f);
			}
			else if(fogQuality == FogQuality.FAST)
			{
				// for the far fog of the normal chunks
				// to start right where the LODs' end use:
				// end = 0.8f, start = 1.5f
				
				GlStateManager.setFogEnd(farPlaneDistance * 2.0f);
				GlStateManager.setFogStart(farPlaneDistance * 3.5f);
			}
		}
		else if(fogMode == FogDistanceMode.FAR)
		{
			if (fogQuality == FogQuality.FANCY || fogQuality == FogQuality.UNKNOWN)
			{
				GlStateManager.setFogStart(farPlaneDistance * 0.5f * VIEW_DISTANCE_MULTIPLIER / 2.0f);
				GlStateManager.setFogEnd(farPlaneDistance * 1.0f * VIEW_DISTANCE_MULTIPLIER / 2.0f);
			}
			else if(fogQuality == FogQuality.FAST)
			{
				GlStateManager.setFogStart(farPlaneDistance * 0.5f * VIEW_DISTANCE_MULTIPLIER / 2.0f);
				GlStateManager.setFogEnd(farPlaneDistance * 0.8f * VIEW_DISTANCE_MULTIPLIER / 2.0f);
			}
		}
		
		GlStateManager.setFogDensity(0.1f);
		GlStateManager.enableFog();
	}
	

	/**
	 * create a new projection matrix and send it over to the GPU
	 * @param partialTicks how many ticks into the frame we are
	 * @return true if the matrix was successfully created and sent to the GPU, false otherwise
	 */
	private void setProjectionMatrix(float partialTicks)
	{
		// create a new view frustum so that the squares can be drawn outside the normal view distance
		GlStateManager.matrixMode(GL11.GL_PROJECTION);
		GlStateManager.loadIdentity();	
		
		// only continue if we can get the FOV
		if (reflectionHandler.fovMethod != null)
		{
			Project.gluPerspective(reflectionHandler.getFov(mc, partialTicks, true), (float) mc.displayWidth / (float) mc.displayHeight, 0.5F, farPlaneDistance * 12);
		}
		
		// we weren't able to set up the projection matrix
		return;
	}
	
	
	/**
	 * setup the lighting to be used for the LODs
	 */
	private void setupLighting(float partialTicks)
	{
		GL11.glEnable(GL11.GL_COLOR_MATERIAL); // set the color to be used as the material (this allows lighting to be enabled)
		
		float sunBrightness = mc.world.getSunBrightness(partialTicks) * mc.world.provider.getSunBrightnessFactor(partialTicks);
		float skyHasLight = mc.world.provider.hasSkyLight()? 1.0f : 0.15f;
		float gammaMultiplyer = (mc.gameSettings.gammaSetting * 0.5f + 0.5f);
		float lightStrength = sunBrightness * skyHasLight * gammaMultiplyer;
		float lightAmbient[] = {lightStrength, lightStrength, lightStrength, 1.0f};
        	
		ByteBuffer temp = ByteBuffer.allocateDirect(16);
		temp.order(ByteOrder.nativeOrder());
		GL11.glLight(GL11.GL_LIGHT1, GL11.GL_AMBIENT, (FloatBuffer) temp.asFloatBuffer().put(lightAmbient).flip());
		GL11.glEnable(GL11.GL_LIGHT1); // Enable the above lighting
		
		GlStateManager.enableLighting();
	}
	
	
	/**
	 * This is used for changing the number of buffer threads during runtime.
	 * This will only be used during testing and not during release.
	 */
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
			
			for(int i = 0; i < maxThreads; i++)
			{
				buffers[i] = ByteBuffer.allocateDirect(bufferMaxCapacity);
				buffers[i].order(ByteOrder.LITTLE_ENDIAN);
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
	
	
	
	
	
}