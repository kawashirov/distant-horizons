package backsun.lod.renderer;

import java.awt.Color;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.biome.Biome;

/**
 * @author James Seibel
 * @version 08-30-2020
 */
public class CustomRenderer
{
	private Minecraft mc;
	private float farPlaneDistance;
	
	private Tessellator tessellator;
	private BufferBuilder bufferBuilder;
	
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
	public CustomRenderer()
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
	 * 
	 * @param mc
	 * @param partialTicks
	 */
	public void drawTest(Minecraft mc, float partialTicks)
	{
		// used for debugging and viewing how long different processes take
		mc.world.profiler.startSection("LOD setup");
		
		long startTime = System.nanoTime();
		
		// determine how far the game's render distance is currently set
		farPlaneDistance = mc.gameSettings.renderDistanceChunks * 16;
		
		// enable the fog
//		setupFog();
		
		// set the new model view matrix
		setProjectionMatrix(partialTicks);
		
		// set the required open GL settings
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glLineWidth(2.0f);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_CULL_FACE);
		
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
		GL11.glEnable(GL11.GL_BLEND);
		
		
		// get the camera location
		Entity entity = mc.player;
		double cameraX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
		double cameraY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
		double cameraZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
		
		int playerXChunkOffset = ((int) cameraX / 16) * 16;
		int playerZChunkOffset = ((int) cameraZ / 16) * 16;
		
//		System.out.println(mc.world.getBiome(new BlockPos(cameraX, cameraY, cameraZ)));
		
		
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
		// this is used to draw a checkerboard
		boolean alternateColor = false;
		boolean evenWidth = false;
		if (debugging && numbOfBoxesWide % 2 == 0)
			evenWidth = true;
		
		
		
		
		
		
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
						startX; // offset so the center LOD block is underneath the player
				
				double zoffset = -cameraZ + (squareSideLength * j) + startZ;
				
				
				int chunkX = ((squareSideLength * j) + startX) / 16;
				int chunkZ = ((squareSideLength * i) + startZ) / 16;
				
				
//				if (distanceToPlayer(chunkX * 16, 70, chunkZ * 16, cameraX, 70, cameraZ) < farPlaneDistance * 2)
//				{
//					// near to player
//					c = black;
//				}
//				else
//				{
//					// far from player
//					c = white;
////					System.out.println("loading");
////					mc.world.getChunkProvider().loadChunk(chunkX, chunkZ);
//				}
				
				Color c;
				Biome biome = null;
				try
				{
					biome = Biome.getBiome(biomes[chunkX+32][chunkZ+32]);
				}
				catch(IndexOutOfBoundsException e)
				{
					c = white;
				}
				
				double yoffset = -cameraY + mc.world.provider.getAverageGroundLevel();
				
				
//				if (i == (numbOfBoxesWide / 2) && j == i)
//					System.out.println("\nx " + (int)cameraX + " z " + (int)cameraZ + "\nx " + chunkX  * 16 + " z " + chunkZ * 16 + "\t" + biome.getBiomeName());
				
				
				// TODO fix this so that chunks outside of the view distance are loaded and the biomes are read
				// TODO fix so that the chunks within view distance aren't always plains
				if(biome != null)
				{
					// add the color
					switch (Biome.getIdForBiome(biome))
					{
						case -1: // unloaded
							c = red;
							break;
						case 0: // ocean
							c = water;
							break;
						case 24: // deep ocean
							c = water;
							break;
						case 7: // river
							c = water;
							break;
						case 1: // plains
//							c = invisible;
							c = grass;
							break;
						case 6: // swamp
							c = swamp;
							break;
						case 3: //extreme hills
							c = mountain;
							break;
						case 34: // extreme hills with trees
							c = mountain;
							break;
						default:
							c = error;
							break;
					}
				}
				else
				{
					c = black;
				}
				
				
				
				// if debugging draw the squares as a black and white checker board
//				if (debugging)
//				{
//					if (alternateColor)
//						c = white;
//					else
//						c = black;
//					// draw the first square as red
//					if (i == 0 && j == 0)
//						c = Color.RED;
//					
//					colorArray[i + (j * numbOfBoxesWide)] = c;
//					
//					alternateColor = !alternateColor;
//				}
				
				// add the color to the array
				colorArray[i + (j * numbOfBoxesWide)] = c;
				
				// add the new box to the array
				lodArray[i + (j * numbOfBoxesWide)] = new AxisAlignedBB(bbx, bby, bbz, 0, bby, 0).offset(xoffset, yoffset, zoffset);
				
				
			}
		}
		
		
		
		
		
		mc.world.profiler.endStartSection("LOD pre draw");
		
		// send the LODs over to the GPU
		sendDataToGPU(lodArray, colorArray);
		
		
		
		
		
		mc.world.profiler.endStartSection("LOD draw");
		// draw the LODs
		tessellator.draw();
		
		
		
		
		
		
		
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
		
		
		// 7 - 5  ms
		// 16 ms = 60 hz
		long endTime = System.nanoTime();
//		System.out.println((endTime - startTime) + " :ns");
		
		// end of profiler tracking
		mc.world.profiler.endSection();
	}
	
	
	public double distanceToPlayer(int x, int y, int z, double cameraX, double cameraY, double cameraZ)
	{
		if(cameraY == y)
			return Math.sqrt(Math.pow((x - cameraX),2) + Math.pow((z - cameraZ),2));
					
		return Math.sqrt(Math.pow((x - cameraX),2) + Math.pow((y - cameraY),2) + Math.pow((z - cameraZ),2));
	}
	
	/**
	 * draw an array of cubes (or squares) with the given colors
	 * 
	 * @param bbArray
	 * @param colorArray
	 */
	private void sendDataToGPU(AxisAlignedBB[] bbArray, Color[] colorArray)
	{
		bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		
		
		int red;
		int green;
		int blue;
		int alpha;
		
		//TODO this is the part that needs optimization
		int colorIndex = 0;
		for (AxisAlignedBB bb : bbArray)
		{
			
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
		}
	}
	
	
	/**
	 * Sets up and enables the fog to be rendered.
	 */
	private void setupFog()
	{
		GlStateManager.setFogDensity(0.1f);
		// near fog
		GlStateManager.setFogEnd(farPlaneDistance * 2.0f);
		GlStateManager.setFogStart(farPlaneDistance * 2.25f);
		
		// far fog
//		GlStateManager.setFogStart(farPlaneDistance * (viewDistanceMultiplier * 0.25f));
//		GlStateManager.setFogEnd(farPlaneDistance * (viewDistanceMultiplier * 0.5f));
		GlStateManager.enableFog();
	}
	
	/**
	 * possible replacement :  mc.gameSettings.fovSetting * fovModifier;
	 * 
	 * Originally from Minecraft's EntityRenderer
	 * Changes the field of view of the player depending on if they are underwater or not
	 */
	public float getFOV(float partialTicks, boolean useFOVSetting)
	{
		Entity entity = mc.getRenderViewEntity();
		float f = 70.0F;
		
		updateFovModifierHand();
		
		if (useFOVSetting)
		{
			f = mc.gameSettings.fovSetting;
			f = f * (fovModifierHandPrev + (fovModifierHand - fovModifierHandPrev) * partialTicks);
		}
		
		if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).getHealth() <= 0.0F)
		{
			float f1 = ((EntityLivingBase) entity).deathTime + partialTicks;
			f /= (1.0F - 500.0F / (f1 + 500.0F)) * 2.0F + 1.0F;
		}
		
		IBlockState iblockstate = ActiveRenderInfo.getBlockStateAtEntityViewpoint(mc.world, entity, partialTicks);
		
		if (iblockstate.getMaterial() == Material.WATER)
		{
			f = f * 60.0F / 70.0F;
		}
		
		return f;
	}
	
	/**
	 * originally from Minecraft's EntityRenderer
	 * Update FOV modifier hand
	 */
	private void updateFovModifierHand()
	{
		float f = 1.0F;
		
		if (mc.getRenderViewEntity() instanceof AbstractClientPlayer)
		{
			AbstractClientPlayer abstractclientplayer = (AbstractClientPlayer) mc.getRenderViewEntity();
			f = abstractclientplayer.getFovModifier();
		}
		
		fovModifierHandPrev = fovModifierHand;
		fovModifierHand += (f - fovModifierHand) * 0.5F;
		
		if (fovModifierHand > 1.5F)
		{
			fovModifierHand = 1.5F;
		}
		
		if (fovModifierHand < 0.1F)
		{
			fovModifierHand = 0.1F;
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
		fov = mc.gameSettings.fovSetting * fovModifier;//getFOV(partialTicks, true);
	}
	
	
}