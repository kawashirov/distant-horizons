package com.backsun.lod.renderer;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;

/**
 * @author James Seibel
 * @version 1-20-2021
 */
public class LodRenderer
{
	public boolean debugging = false;
	
	private Minecraft mc;
	private float farPlaneDistance;
	// make sure this is an even number, or else it won't align with the chunk grid
	public static final int VIEW_DISTANCE_MULTIPLIER = 12;
	public static final int LOD_WIDTH = 16;
	public static final int MINECRAFT_CHUNK_WIDTH = 16;
	
	public int defaultLodHeight = 0;
	
	private Tessellator tessellator;
	private BufferBuilder bufferBuilder;
	
	private ReflectionHandler reflectionHandler;
	
	public LodDimension dimension = null;
	
	
	
	public LodRenderer()
	{
		mc = Minecraft.getMinecraft();
		
		// for some reason "Tessellator.getInstance()" won't work here, we have to create a new one
		tessellator = new Tessellator(2097152);
		bufferBuilder = tessellator.getBuffer();
		
		reflectionHandler = new ReflectionHandler();
	}
	
	
	
	public void drawLODs(LodDimension newDimension, Minecraft mc, float partialTicks)
	{
		if (reflectionHandler.fovMethod == null)
		{
			// don't continue if we can't get the
			// user's FOV
			return;
		}
		
		dimension = newDimension;
		if (dimension == null)
		{
			// if there aren't any loaded LodChunks
			// don't try drawing anything
			return;
		}
		
		if (reflectionHandler.fovMethod == null)
		{
			// we aren't able to get the user's
			// FOV, don't render anything
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
		float sunBrightness = mc.world.getSunBrightness(partialTicks); // TODO this only works in dimensions with day night cycles
		
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
				int middle = (numbChunksWide / 2) - 1;
				if (isCoordinateInLoadedArea(i, j, middle))
				{
					continue;
//					colorArray[i][j] = null;
//					lodArray[i][j] = null;
				}
				
				
				// set where this square will be drawn in the world
				double xOffset = -cameraX + // start at x = 0
							(LOD_WIDTH * i) + // offset by the number of LOD blocks
							startX; // offset so the center LOD block is centered underneath the player
				double yOffset = -cameraY;
				double zOffset = -cameraZ + (LOD_WIDTH * j) + startZ;
				
				int chunkX = i + (startX / MINECRAFT_CHUNK_WIDTH);
				int chunkZ = j + (startZ / MINECRAFT_CHUNK_WIDTH);
				
				LodChunk lod = dimension.getLodFromCoordinates(chunkX, chunkZ); // new LodChunk(); //   
				if (lod == null)
				{
					// note: for some reason if any color or lod object are set here
					// it causes the game to use 100% gpu, all of it undefined in the debug menu
					// and drop to ~6 fps.
//					colorArray[i][j] = null;
//					lodArray[i][j] = null;
					
					continue;
				}
				
				Color c = new Color(
						(int) (lod.colors[ColorDirection.TOP.value].getRed() * sunBrightness),
						(int) (lod.colors[ColorDirection.TOP.value].getGreen() * sunBrightness),
						(int) (lod.colors[ColorDirection.TOP.value].getBlue() * sunBrightness),
						lod.colors[ColorDirection.TOP.value].getAlpha());
				
				
				// if debugging draw the squares as a black and white checker board
				if (debugging)
				{
					if ((chunkX + chunkZ) % 2 == 0)
						c = white;
					else
						c = black;
					// draw the first square as red
					if (i == 0 && j == 0)
						c = red;
					
					colorArray[i][j] = c; // TODO does this work? if so why?
				}
				
				
				// add the color to the array
				colorArray[i][j] = c;
				
				// add the new box to the array
				lodArray[i][j] = new AxisAlignedBB(0, lod.bottom[LodLocation.NE.value], 0, LOD_WIDTH, lod.top[LodLocation.NE.value], LOD_WIDTH).offset(xOffset, yOffset, zOffset);
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
		GL11.glEnable(GL11.GL_BLEND);
		
		setProjectionMatrix(partialTicks);
		setupFog(FogDistanceMode.NEAR, reflectionHandler.getFogType());
		
		
		
		
		
		
		
		//===========//
		// rendering //
		//===========//
		
		mc.mcProfiler.endStartSection("LOD build buffer");
		// send the LODs over to the GPU
		sendToGPUAndDraw(lodArray, colorArray);
		
		
		
		
		
		//=========//
		// cleanup //
		//=========//
		
		mc.mcProfiler.endStartSection("LOD cleanup");
		
		
		// this must be done otherwise other parts of the screen may be drawn with a fog effect
		// IE the GUI
		GlStateManager.disableFog();
		
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		
		// change the perspective matrix back to prevent incompatibilities
		// with other mods that may render during forgeRenderLast
		Project.gluPerspective(reflectionHandler.getFov(mc, partialTicks, true), (float) this.mc.displayWidth / (float) this.mc.displayHeight, 0.05F, this.farPlaneDistance * MathHelper.SQRT_2);
		
		
		
		
		// This is about how long this whole process should take
		// 16 ms = 60 hz
		@SuppressWarnings("unused")
		long endTime = System.nanoTime();
		
		// end of profiler tracking
		mc.mcProfiler.endSection();
	}
	
	
	
	
	
	
	//private ExecutorService threadPool = Executors.newFixedThreadPool(8);
	
	private int numbThreads = 1;
	private BuildBufferThread[] threads = new BuildBufferThread[numbThreads];
	private ByteBuffer[] buffers = new ByteBuffer[numbThreads];
	
	/**
	 * draw an array of cubes (or squares) with the given colors.
	 * @param lods bounding boxes to draw
	 * @param colors color of each box to draw
	 */
	private void sendToGPUAndDraw(AxisAlignedBB[][] lods, Color[][] colors)
	{
		for(int i = 0; i < numbThreads; i++)
		{
			if (buffers[i] == null)
			{
				buffers[i] = ByteBuffer.allocateDirect(bufferBuilder.getByteBuffer().capacity()); // 8388608
				buffers[i].order(ByteOrder.LITTLE_ENDIAN);
			}
			int pos = bufferBuilder.getByteBuffer().position();
			buffers[i].position(pos);
			
			
//			System.out.println(bufferBuilder.getByteBuffer() + "\t\t" + buffers[i].toString());
			BuildBufferThread thread = new BuildBufferThread(buffers[i], lods, colors, i, numbThreads);
			thread.run();
			threads[i] = thread;
			try
			{ threads[i].join(); }
			catch(Exception e)
			{ e.printStackTrace(); }
		}
//		for(int i = 0; i < 8; i++)
//		{
//			try
//			{ threads[i].join(); }
//			catch(Exception e)
//			{ e.printStackTrace(); }
//		}
		
//		ByteBuffer tessBuffer = bufferBuilder.getByteBuffer();
//		ByteBuffer demoBuffer = buffers[0];
//		
//		boolean same = true;
//		for(int i = 0; i < tessBuffer.capacity(); i++)
//		{
//			int testIndex = i;
//			byte demoVal = demoBuffer.get(testIndex);
//			byte tessVal = tessBuffer.get(testIndex);
//			if(demoVal != tessVal)
//			{
//				System.out.println("ye-no " + i);
//				same = false;
//				break;
//			}
//		}
//		if (same)
//			System.out.println("they are the same jim");
		
		
		mc.mcProfiler.endStartSection("LOD draw");
		for(int i = 0; i < numbThreads; i++)
		{
			bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
			bufferBuilder.putBulkData(buffers[i]);
			tessellator.draw();
			
			// this is required otherwise nothing is drawn
			bufferBuilder.getByteBuffer().clear();
		}
	}
	private class BuildBufferThread extends Thread
	{
		ByteBuffer buffer;
		AxisAlignedBB[][] lods;
		Color[][] colors;
		
		int start = 0;
		int end = -1;
		
		BuildBufferThread(ByteBuffer newByteBuffer, AxisAlignedBB[][] newLods, Color[][] newColors, int threadCount, int totalThreads)
		{
			buffer = newByteBuffer;
			lods = newLods;
			colors = newColors;
			
			int numbChunksWide = lods.length;
			int rowsToRender = numbChunksWide / totalThreads;
			start = threadCount * rowsToRender;
			end = (threadCount + 1) * rowsToRender;
			
			vertexCount = 0;
			vertexFormat = DefaultVertexFormats.POSITION_COLOR;
			vertexFormatIndex = 0;
			vertexFormatElement = vertexFormat.getElement(this.vertexFormatIndex); 
		}
		
		@Override
		public void run()
		{
			int numbChunksWide = lods.length;
			
			AxisAlignedBB bb;
			int red;
			int green;
			int blue;
			int alpha;
			
			// x axis
			for (int i = start; i < end; i++)
			{
				// z axis
				for (int j = 0; j < numbChunksWide; j++)
				{
					// skip the middle
					// (As the player moves some chunks will overlap or be missing,
					// this is just how chunk loading/unloading works. This can hopefully
					// be hidden with careful use of fog)
					int middle = (numbChunksWide / 2) - 1;
					if (isCoordinateInLoadedArea(i, j, middle))
					{
						continue;
					}
					
					
					if (lods[i][j] == null || colors[i][j] == null)
						continue;
					
					bb = lods[i][j];
					
					// get the color of this LOD object
					red = colors[i][j].getRed();
					green = colors[i][j].getGreen();
					blue = colors[i][j].getBlue();
					alpha = colors[i][j].getAlpha();
					
					// only draw all 6 sides if there is some thickness to the box
					if (bb.minY != bb.maxY)
					{
						// top (facing up)
						addPosAndColor(buffer, bb.minX, bb.maxY, bb.minZ, red, green, blue, alpha);
						addPosAndColor(buffer, bb.minX, bb.maxY, bb.maxZ, red, green, blue, alpha);
						addPosAndColor(buffer, bb.maxX, bb.maxY, bb.maxZ, red, green, blue, alpha);
						addPosAndColor(buffer, bb.maxX, bb.maxY, bb.minZ, red, green, blue, alpha);
						// bottom (facing down)
						addPosAndColor(buffer, bb.maxX, bb.minY, bb.minZ, red, green, blue, alpha);
						addPosAndColor(buffer, bb.maxX, bb.minY, bb.maxZ, red, green, blue, alpha);
						addPosAndColor(buffer, bb.minX, bb.minY, bb.maxZ, red, green, blue, alpha);
						addPosAndColor(buffer, bb.minX, bb.minY, bb.minZ, red, green, blue, alpha);

						// south (facing -Z) 
						addPosAndColor(buffer, bb.maxX, bb.minY, bb.maxZ, red, green, blue, alpha);
						addPosAndColor(buffer, bb.maxX, bb.maxY, bb.maxZ, red, green, blue, alpha);
						addPosAndColor(buffer, bb.minX, bb.maxY, bb.maxZ, red, green, blue, alpha);
						addPosAndColor(buffer, bb.minX, bb.minY, bb.maxZ, red, green, blue, alpha);
						// north (facing +Z)
						addPosAndColor(buffer, bb.minX, bb.minY, bb.minZ, red, green, blue, alpha);
						addPosAndColor(buffer, bb.minX, bb.maxY, bb.minZ, red, green, blue, alpha);
						addPosAndColor(buffer, bb.maxX, bb.maxY, bb.minZ, red, green, blue, alpha);
						addPosAndColor(buffer, bb.maxX, bb.minY, bb.minZ, red, green, blue, alpha);

						// west (facing -X)
						addPosAndColor(buffer, bb.minX, bb.minY, bb.minZ, red, green, blue, alpha);
						addPosAndColor(buffer, bb.minX, bb.minY, bb.maxZ, red, green, blue, alpha);
						addPosAndColor(buffer, bb.minX, bb.maxY, bb.maxZ, red, green, blue, alpha);
						addPosAndColor(buffer, bb.minX, bb.maxY, bb.minZ, red, green, blue, alpha);
						// east (facing +X)
						addPosAndColor(buffer, bb.maxX, bb.maxY, bb.minZ, red, green, blue, alpha);
						addPosAndColor(buffer, bb.maxX, bb.maxY, bb.maxZ, red, green, blue, alpha);
						addPosAndColor(buffer, bb.maxX, bb.minY, bb.maxZ, red, green, blue, alpha);
						addPosAndColor(buffer, bb.maxX, bb.minY, bb.minZ, red, green, blue, alpha);
					}
					else
					{
						// bottom (facing up)
//						buffer.pos(bb.minX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
//						buffer.pos(bb.minX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
//						buffer.pos(bb.maxX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
//						buffer.pos(bb.maxX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
						
						// top (facing up)
//						bufferBuilder.pos(bb.minX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
//						bufferBuilder.pos(bb.minX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
//						bufferBuilder.pos(bb.maxX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
//						bufferBuilder.pos(bb.maxX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
					}
					
				} // z axis
			} // x axis
		}
		
		private void addPosAndColor(ByteBuffer buffer, double x, double y, double z, int red, int green, int blue, int alpha)
		{
			addPos(buffer, x, y, z);
			addColor(buffer, red, green, blue, alpha);
			endVertex();
		}
		
		private int vertexCount = 0;
		private VertexFormat vertexFormat = null;
		private int vertexFormatIndex = 0;
		private VertexFormatElement vertexFormatElement = null; 
		
		private void addPos(ByteBuffer byteBuffer, double x, double y, double z)
		{
			int i = this.vertexCount * this.vertexFormat.getNextOffset() + this.vertexFormat.getOffset(this.vertexFormatIndex);

	        switch (this.vertexFormatElement.getType())
	        {
	            case FLOAT: // This is the one currently used
	                byteBuffer.putFloat(i, (float)(x));
	                byteBuffer.putFloat(i + 4, (float)(y));
	                byteBuffer.putFloat(i + 8, (float)(z));
	                break;
	            case UINT:
	            case INT:
	                byteBuffer.putInt(i, Float.floatToRawIntBits((float)(x)));
	                byteBuffer.putInt(i + 4, Float.floatToRawIntBits((float)(y)));
	                byteBuffer.putInt(i + 8, Float.floatToRawIntBits((float)(z)));
	                break;
	            case USHORT:
	            case SHORT:
	                byteBuffer.putShort(i, (short)((int)(x)));
	                byteBuffer.putShort(i + 2, (short)((int)(y)));
	                byteBuffer.putShort(i + 4, (short)((int)(z)));
	                break;
	            case UBYTE:
	            case BYTE:
	                byteBuffer.put(i, (byte)((int)(x)));
	                byteBuffer.put(i + 1, (byte)((int)(y)));
	                byteBuffer.put(i + 2, (byte)((int)(z)));
	        }
	        
	        nextVertexFormatIndex();
		}
		
		private void addColor(ByteBuffer byteBuffer, int red, int green, int blue, int alpha)
	    {
            int i = this.vertexCount * this.vertexFormat.getNextOffset() + this.vertexFormat.getOffset(this.vertexFormatIndex);

            switch (this.vertexFormatElement.getType())
            {
                case FLOAT:
                	byteBuffer.putFloat(i, red / 255.0F);
                	byteBuffer.putFloat(i + 4, green / 255.0F);
                	byteBuffer.putFloat(i + 8, blue / 255.0F);
                	byteBuffer.putFloat(i + 12, alpha / 255.0F);
                    break;
                case UINT:
                case INT:
                	byteBuffer.putFloat(i, red);
                	byteBuffer.putFloat(i + 4, green);
                	byteBuffer.putFloat(i + 8, blue);
                	byteBuffer.putFloat(i + 12, alpha);
                    break;
                case USHORT:
                case SHORT:
                	byteBuffer.putShort(i, (short)red);
                	byteBuffer.putShort(i + 2, (short)green);
                	byteBuffer.putShort(i + 4, (short)blue);
                	byteBuffer.putShort(i + 6, (short)alpha);
                    break;
                case UBYTE:
                case BYTE:
                	
                    if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN)
                    {
                    	// this is the one used currently
                        byteBuffer.put(i, (byte)red);
                        byteBuffer.put(i + 1, (byte)green);
                        byteBuffer.put(i + 2, (byte)blue);
                        byteBuffer.put(i + 3, (byte)alpha);
                    }
                    else
                    {
                        byteBuffer.put(i, (byte)alpha);
                        byteBuffer.put(i + 1, (byte)blue);
                        byteBuffer.put(i + 2, (byte)green);
                        byteBuffer.put(i + 3, (byte)red);
                    }
            }

            nextVertexFormatIndex();
	    }
		
		private void nextVertexFormatIndex()
	    {
	        ++this.vertexFormatIndex;
	        this.vertexFormatIndex %= this.vertexFormat.getElementCount();
	        this.vertexFormatElement = this.vertexFormat.getElement(this.vertexFormatIndex);

	        if (this.vertexFormatElement.getUsage() == VertexFormatElement.EnumUsage.PADDING)
	        {
	            this.nextVertexFormatIndex();
	        }
	    }
		
		private void endVertex()
	    {
	        ++this.vertexCount;
	        growBuffer(this.vertexFormat.getNextOffset());
	    }
		
		private void growBuffer(int p_181670_1_)
	    {
	        //if (MathHelper.roundUp(p_181670_1_, 4) / 4 > this.rawIntBuffer.remaining() || this.vertexCount * this.vertexFormat.getNextOffset() + p_181670_1_ > this.byteBuffer.capacity())
			if (this.vertexCount * this.vertexFormat.getNextOffset() + p_181670_1_ > buffer.capacity())
	        {
	            int i = buffer.capacity();
	            int j = i + MathHelper.roundUp(p_181670_1_, 2097152);
//	            int k = this.rawIntBuffer.position();
	            ByteBuffer directBytebuffer = GLAllocation.createDirectByteBuffer(j);
	            buffer.position(0);
	            directBytebuffer.put(buffer);
	            directBytebuffer.rewind();
	            buffer = directBytebuffer;
//	            this.rawFloatBuffer = buffer.asFloatBuffer().asReadOnlyBuffer();
//	            this.rawIntBuffer = buffer.asIntBuffer();
//	            this.rawIntBuffer.position(k);
//	            this.rawShortBuffer = buffer.asShortBuffer();
//	            this.rawShortBuffer.position(k << 1);
	        }
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
		
		// TODO have fog change based on height
		// when higher up have it end up farther away
		// (to hide the boarders of the LODs)
		if(fogMode == FogDistanceMode.NEAR)
		{
			// the multipliers are percentages
			// of the normal view distance.
			
			// the reason that I wrote fogEnd then fogStart backwards
			// is because we are using fog backwards to how
			// it is normally used, with it hiding near objects
			// instead of far objects.
			
			if (fogQuality == FogQuality.FANCY || fogQuality == FogQuality.UNKNOWN)
			{
				GlStateManager.setFogEnd(farPlaneDistance * 2.0f);
				GlStateManager.setFogStart(farPlaneDistance * 2.25f);
			}
			else //if(fogType == FogType.FAST)
			{
				// for the far fog of the normal chunks
				// to start right where the LODs' end use:
				// end = 0.8f, start = 1.5f
				
				GlStateManager.setFogEnd(farPlaneDistance * 2.0f);
				GlStateManager.setFogStart(farPlaneDistance * 3.5f);
			}
		}
		else //if(fogMode == FogMode.FAR) or Both
		{
			// the multipliers are percentages of
			// the LOD view distance.
			
			if (fogQuality == FogQuality.FANCY || fogQuality == FogQuality.UNKNOWN)
			{
				GlStateManager.setFogStart(farPlaneDistance * 0.5f * VIEW_DISTANCE_MULTIPLIER / 2.0f);
				GlStateManager.setFogEnd(farPlaneDistance * 1.0f * VIEW_DISTANCE_MULTIPLIER / 2.0f);
			}
			else //if(fogType == FogType.FAST)
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
	
	
	
	
	//==================//
	// Helper Functions //
	//==================//
	
	
	/**
	 * Returns if the given coordinate is in the loaded area of the world.
	 * @param middle the center of the loaded world
	 */
	private boolean isCoordinateInLoadedArea(int x, int z, int middle)
	{
		return (x >= middle - mc.gameSettings.renderDistanceChunks 
				&& x <= middle + mc.gameSettings.renderDistanceChunks) 
				&& 
				(z >= middle - mc.gameSettings.renderDistanceChunks 
				&& z <= middle + mc.gameSettings.renderDistanceChunks);
	}
	
	
	
	
	
}