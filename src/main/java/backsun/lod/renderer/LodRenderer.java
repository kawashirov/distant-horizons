package backsun.lod.renderer;

import java.awt.Color;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * @author James Seibel
 * @version 08-30-2020
 */
public class LodRenderer
{
	private Minecraft mc;
	private float farPlaneDistance;
	
	private Tessellator tessellator;
	private BufferBuilder bufferBuilder;
	
	

	private enum FogMode{NEAR, FAR, NONE};
	
	// make sure this is an even number, or else it won't align with the chunk grid
	public final int viewDistanceMultiplier = 12;
	private float fovModifierHandPrev;
	private float fovModifierHand;
	private float fovModifier = 1.1f;
	private float fov = 70 * fovModifier;
	
	public int biomes[][];
	
	public boolean debugging = true;
	
	
	
	
	/**
	 * constructor
	 */
	public LodRenderer()
	{
		mc = Minecraft.getMinecraft();
		
		tessellator = Tessellator.getInstance();
		bufferBuilder = tessellator.getBuffer();
		
		biomes = new int[64][64];
		
		for(int i = 0; i < 64; i++)
		{
			for(int j = 0; j < 64; j++)
			{
				biomes[i][j] = -1;
			}
		}
		
		// GL11.GL_MODELVIEW  //5888
		// GL11.GL_PROJECTION //5889
	}
	
	
	
	/**
	 * Draw Level of Details in the world.
	 * @param mc 
	 * @param partialTicks
	 */
	public void drawLODs(Minecraft mc, float partialTicks)
	{
		// color setup
		int alpha = 255;
		Color error = new Color(255, 0, 225, alpha); // bright pink
		Color grass = new Color(80, 104, 50, alpha);
		Color water = new Color(37, 51, 174, alpha);
		Color swamp = new Color(60, 63, 32, alpha);
		Color mountain = new Color(100, 100, 100, alpha);
		
		Color red = Color.RED;
		Color black = Color.BLACK;
		Color white = Color.WHITE;
		Color invisible = new Color(0,0,0,0);
		
		
		
		// used for debugging and viewing how long different processes take
		mc.world.profiler.startSection("LOD setup");
		
		long startTime = System.nanoTime();
		
		
		
		// get the camera location
		Entity player = mc.player;
		double cameraX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
		double cameraY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
		double cameraZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
		
		int playerXChunkOffset = ((int) cameraX / 16) * 16;
		int playerZChunkOffset = ((int) cameraZ / 16) * 16;
		
		
		
		
		// set how big the squares will be and how far they will go
		int totalLength = (int) farPlaneDistance * viewDistanceMultiplier;
		int squareSideLength = 16;
		int numbOfBoxesWide = (totalLength / squareSideLength);
		//XXX start distance is about 2 * farPlaneDistance
		
		// size of a single square
		int bbx = squareSideLength;
		int bby = 0;
		int bbz = squareSideLength;
		
		// this where we will start drawing squares
		// (exactly half the total width)
		int startX = (-squareSideLength * (numbOfBoxesWide / 2)) + playerXChunkOffset;
		int startZ = (-squareSideLength * (numbOfBoxesWide / 2)) + playerZChunkOffset;
		
		
		// this is where we store the LOD objects
		AxisAlignedBB lodArray[] = new AxisAlignedBB[numbOfBoxesWide * numbOfBoxesWide];
		// this is where we store the color for each LOD object
		Color colorArray[] = new Color[numbOfBoxesWide * numbOfBoxesWide];
		
		
		// used for debugging
		// and drawing a checkerboard
		boolean alternateColor = false;
		boolean evenWidth = false;
		if (debugging && numbOfBoxesWide % 2 == 0)
			evenWidth = true;
		
		
		
		
		//=================//
		// create the LODs //
		//=================//
		
		mc.world.profiler.endStartSection("LOD generation");
		
		// x axis
		for (int i = 0; i < numbOfBoxesWide; i++)
		{
			// this is so that the debug pattern will be a checkerboard instead of stripes
			if (debugging && evenWidth)
				alternateColor = !alternateColor;
			
			// z axis
			for (int j = 0; j < numbOfBoxesWide; j++)
			{
				// update where this square will be drawn
				double xoffset = -cameraX + // start at x = 0
						(squareSideLength * i) + // offset by the number of LOD blocks
						startX; // offset so the center LOD block is centered underneath the player
				
				double zoffset = -cameraZ + (squareSideLength * j) + startZ;
				
				
				int chunkX = ((squareSideLength * j) + startX) / 16;
				int chunkZ = ((squareSideLength * i) + startZ) / 16;
				
								
				Color c = null;
//				Biome biome = null;
//				try
//				{
//					biome = Biome.getBiome(biomes[chunkX+32][chunkZ+32]);
//				}
//				catch(IndexOutOfBoundsException e)
//				{
//					c = white;
//				}
//				
				double yoffset = -cameraY + mc.world.provider.getAverageGroundLevel();
				
				// TODO fix this so that chunks outside of the view distance are loaded and the biomes are read
				// TODO fix so that the chunks within view distance aren't always plains
//				if(biome != null)
//				{
//					// add the color
//					switch (Biome.getIdForBiome(biome))
//					{
//						case -1: // unloaded
//							c = red;
//							break;
//						case 0: // ocean
//							c = water;
//							break;
//						case 24: // deep ocean
//							c = water;
//							break;
//						case 7: // river
//							c = water;
//							break;
//						case 1: // plains
////							c = invisible;
//							c = grass;
//							break;
//						case 6: // swamp
//							c = swamp;
//							break;
//						case 3: //extreme hills
//							c = mountain;
//							break;
//						case 34: // extreme hills with trees
//							c = mountain;
//							break;
//						default:
//							c = error;
//							break;
//					}
//				}
//				else
//				{
//					c = black;
//				}
				
				
				
				// if debugging draw the squares as a black and white checker board
				if (debugging)
				{
					if (alternateColor)
						c = white;
					else
						c = black;
					// draw the first square as red
					if (i == 0 && j == 0)
						c = red;
					
					colorArray[i + (j * numbOfBoxesWide)] = c;
					
					alternateColor = !alternateColor;
				}
				
				// add the color to the array
				colorArray[i + (j * numbOfBoxesWide)] = c;
				
				// add the new box to the array
				lodArray[i + (j * numbOfBoxesWide)] = new AxisAlignedBB(0, bby, 0, bbx, bby, bbz).offset(xoffset, yoffset, zoffset);
			}
		}
		
		
		
		
		
		
		
		
		
		//===========================//
		// GL settings for rendering //
		//===========================//

		// determine how far the game's render distance is currently set
		farPlaneDistance = mc.gameSettings.renderDistanceChunks * 16;
		
		// enable the fog
//		setupFog(FogMode.FAR);
		
		// set the new model view matrix
		setProjectionMatrix(partialTicks);
		
		// set the required open GL settings
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glLineWidth(2.0f);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_CULL_FACE);
		
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
		GL11.glEnable(GL11.GL_BLEND);
		
		
		
		
		//===========//
		// rendering //
		//===========//

		mc.world.profiler.endStartSection("LOD pre draw");
		
		// send the LODs over to the GPU
		sendDataToGPU(lodArray, colorArray, cameraX, cameraY ,cameraZ);
		
		
		
		
		
		
		
		
		//=========//
		// cleanup //
		//=========//
		
		mc.world.profiler.endStartSection("LOD cleanup");
		
		
		// this must be done otherwise other parts of the screen may be drawn with a fog effect
		// IE the GUI
		GlStateManager.disableFog();
		
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_POLYGON_OFFSET_LINE);
		GL11.glPolygonOffset(-1.f, -1.f);
		
		
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		
		
		// This is about how long this whole process should take
		// 16 ms = 60 hz
		long endTime = System.nanoTime();
		
		// end of profiler tracking
		mc.world.profiler.endSection();
	}
	

	/**
	 * create a new projection matrix and send it over to the GPU
	 * @param partialTicks how many ticks into the frame we are
	 */
	private void setProjectionMatrix(float partialTicks)
	{
		// create a new view frustum so that the squares can be drawn outside the normal view distance
		GlStateManager.matrixMode(GL11.GL_PROJECTION);
		GlStateManager.loadIdentity();
		// farPlaneDistance // 10 chunks = 160											0.0125f
		Project.gluPerspective(fov, (float) mc.displayWidth / (float) mc.displayHeight, 0.0125f, farPlaneDistance * viewDistanceMultiplier);
	}
	

	/**
	 * Sets up and enables the fog to be rendered.
	 */
	private void setupFog(FogMode fogMode)
	{
		if(fogMode == FogMode.NONE)
		{
			GlStateManager.disableFog();
			return;
		}
		
		
		
		if(fogMode == FogMode.NEAR)
		{
			// 2.0f
			// 2.25f
			GlStateManager.setFogEnd(farPlaneDistance * 2.0f);
			GlStateManager.setFogStart(farPlaneDistance * 2.25f);
			
		}
		else //if(fogMode == FogMode.FAR)
		{
			// 0.25f
			// 0.5f
			GlStateManager.setFogStart(farPlaneDistance * (viewDistanceMultiplier * 0.25f));
			GlStateManager.setFogEnd(farPlaneDistance * (viewDistanceMultiplier * 0.5f));
		}
		
		GlStateManager.setFogDensity(0.1f);
//		GlStateManager.enableFog();
	}
	
	
	/**
	 * draw an array of cubes (or squares) with the given colors
	 * 
	 * @param bbArray
	 * @param colorArray
	 */
	private void sendDataToGPU(AxisAlignedBB[] bbArray, Color[] colorArray, double cameraX, double cameraY, double cameraZ)
	{		
		int red;
		int green;
		int blue;
		int alpha;
		
		//TODO this is the part that needs optimization
		int colorIndex = 0;
		for (AxisAlignedBB bb : bbArray)
		{
			mc.world.profiler.endStartSection("LOD pre draw");
			bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
			
			// get the color of this LOD object
			red = colorArray[colorIndex].getRed();
			green = colorArray[colorIndex].getGreen();
			blue = colorArray[colorIndex].getBlue();
			alpha = colorArray[colorIndex].getAlpha();
			// TODO which side is this?
			bufferBuilder.pos(bb.minX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
			bufferBuilder.pos(bb.maxX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
			bufferBuilder.pos(bb.maxX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
			bufferBuilder.pos(bb.minX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
			
			// only draw the other 5 sides if there is some thickness to this box
			if (bb.minY != bb.maxY)
			{
				bufferBuilder.pos(bb.minX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
				bufferBuilder.pos(bb.maxX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
				bufferBuilder.pos(bb.maxX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
				bufferBuilder.pos(bb.minX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
				
				bufferBuilder.pos(bb.minX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
				bufferBuilder.pos(bb.minX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
				bufferBuilder.pos(bb.maxX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
				bufferBuilder.pos(bb.maxX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
				
				bufferBuilder.pos(bb.minX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
				bufferBuilder.pos(bb.minX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
				bufferBuilder.pos(bb.maxX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
				bufferBuilder.pos(bb.maxX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
				
				bufferBuilder.pos(bb.minX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
				bufferBuilder.pos(bb.minX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
				bufferBuilder.pos(bb.minX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
				bufferBuilder.pos(bb.minX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
				
				bufferBuilder.pos(bb.maxX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
				bufferBuilder.pos(bb.maxX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
				bufferBuilder.pos(bb.maxX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
				bufferBuilder.pos(bb.maxX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
			}
			
			// so we can get the next color
			colorIndex++;
			
			
			// depending on how far away this bounding box is, either
			// have it use near or far fog
			double nearDist = Math.pow(farPlaneDistance * viewDistanceMultiplier * 0.5f, 2);
			double bbDist = Math.pow(bb.minX, 2) + Math.pow(bb.minZ, 2);
			
			if (bbDist < nearDist)
				 setupFog(FogMode.NEAR);
			else
				 setupFog(FogMode.FAR);
			
			
			mc.world.profiler.endStartSection("LOD draw");
			// draw the LODs
			tessellator.draw();
		}
	}
	
	
	
	/**
	 * 
	 * @param newfov
	 */
	public void updateFOVModifier(float newFov)
	{
		fovModifier = newFov;
		
		// update the fov
		fov = mc.gameSettings.fovSetting * fovModifier;
	}
	
	
}