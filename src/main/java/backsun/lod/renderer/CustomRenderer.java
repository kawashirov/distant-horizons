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

/**
 * 
 * @author James Seibel
 * @version 04-19-2020
 */
public class CustomRenderer
{
	private Minecraft mc;
	private float farPlaneDistance;
	
	private Tessellator tessellator;
	private BufferBuilder worldRenderer;
	
	// make sure this is an even number, or else it won't align with the chunk grid
	public final int viewDistanceMultiplier = 12;
	private float fovModifierHandPrev;
	private float fovModifierHand;
	public boolean debugging = false;
	
	/**
	 * constructor
	 */
	public CustomRenderer()
	{
		mc = Minecraft.getMinecraft();
		
		tessellator = Tessellator.getInstance();
		worldRenderer = tessellator.getBuffer();
		
		// GL11.GL_MODELVIEW  //5888
		// GL11.GL_PROJECTION //5889
	}
	
	/**
	 * create a new projection matrix and send it over to the GPU
	 * @param partialTicks how many ticks into the frame we are
	 */
	private void setProjectionMatrix(float partialTicks)
	{
		// update the fov
		float fov = getFOVModifier(partialTicks, true);
		
		// create a new view frustum so that the squares can be drawn outside the normal view distance
		GlStateManager.matrixMode(GL11.GL_PROJECTION);
		GlStateManager.loadIdentity();
		// farPlaneDistance // 10 chunks = 160
		Project.gluPerspective(fov, (float) mc.displayWidth / (float) mc.displayHeight, 0.0125f, farPlaneDistance * viewDistanceMultiplier);
	}
	
	/**
	 * 
	 * @param mc
	 * @param partialTicks
	 */
	public void drawTest(Minecraft mc, float partialTicks)
	{
		farPlaneDistance = mc.gameSettings.renderDistanceChunks * 16;
		
		setupFog();
		
		// set the new model view matrix
		setProjectionMatrix(partialTicks);
		
		// used for debugging
		mc.world.profiler.startSection("lod setup");
		
		// set the required open GL settings
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glLineWidth(2.0f);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_CULL_FACE);
		
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
		GL11.glEnable(GL11.GL_BLEND);
		
		
		// get the camera location
		Entity entity = mc.player;
		double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
		double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
		double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
		
		int playerXChunkOffset = ((int) x / 16) * 16;
		int playerZChunkOffset = ((int) z / 16) * 16;
		
		Color grass = new Color(80, 104, 50, 255);
		Color black = Color.BLACK;
		Color white = Color.WHITE;
		
		// set how big the squares will be and how far they will go
		int totalLength = (int) farPlaneDistance * viewDistanceMultiplier;
		int squareSideLength = 16;
		int numbOfBoxesWide = totalLength / squareSideLength;
		//TODO start distance is about 2 * farPlaneDistance
		
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
		
		mc.world.profiler.endStartSection("lod setup");
		
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
				double xoffset = -x + (squareSideLength * i) + startX;
				double yoffset = -y;
				double zoffset = -z + (squareSideLength * j) + startZ;
				
				
				// add the new box to the array
				lodArray[i + (j * numbOfBoxesWide)] = new AxisAlignedBB(bbx, bby, bbz, 0, bby, 0).offset(xoffset, yoffset, zoffset);
				colorArray[i + (j * numbOfBoxesWide)] = grass;
				
				// if debugging draw the squares as a black and white checker board
				if (debugging)
				{
					Color c;
					if (alternateColor)
						c = white;
					else
						c = black;
					// draw the first square as red
					if (i == 0 && j == 0)
						c = Color.RED;
					
					colorArray[i + (j * numbOfBoxesWide)] = c;
					
					alternateColor = !alternateColor;
				}
			}
		}
		
		mc.world.profiler.endStartSection("lod pre draw");
		
		// draw the LODs
		drawBoxArray(lodArray, colorArray);
		
		// end the profile section (so we can see how long it took to draw the LODs)
		mc.world.profiler.endSection();
		
		// this must be done otherwise other parts of the screen may be drawn with a fog effect
		// IE the GUI
		GlStateManager.disableFog();
		
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_POLYGON_OFFSET_LINE);
		GL11.glPolygonOffset(-1.f, -1.f);
		
		
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
	}
	
	private void VBO()
	{
//		this.vboEnabled = OpenGlHelper.useVbo();
//		this.vertexBufferFormat = new VertexFormat();
//		this.vertexBufferFormat.addElement(new VertexFormatElement(0, VertexFormatElement.EnumType.FLOAT, VertexFormatElement.EnumUsage.POSITION, 3));
//		
//		if (this.skyVBO != null)
//		{
//			this.skyVBO.deleteGlBuffers();
//		}
//		if (this.vboEnabled)
//		{
//			starVBO = new VertexBuffer(this.vertexBufferFormat);
//			this.renderStars(bufferbuilder);
//			bufferbuilder.finishDrawing();
//			bufferbuilder.reset();
//			this.starVBO.bufferData(bufferbuilder.getByteBuffer());
//		}
//		
//		
//		this.skyVBO.bindBuffer();
//		GlStateManager.glEnableClientState(32884);
//		GlStateManager.glVertexPointer(3, 5126, 12, 0);
//		this.skyVBO.drawArrays(7);
//		this.skyVBO.unbindBuffer();
//		GlStateManager.glDisableClientState(32884);
//		
//		
//		//rendersky
//		bufferBuilderIn.begin(7, DefaultVertexFormats.POSITION);
//		bufferBuilderIn.pos((double) f, (double) posY, (double) l).endVertex();
	}
	
	/**
	 * TODO improve this method's speed
	 * 
	 * draw an array of cubes (or squares) with the given colors
	 * @param bb the cube to draw
	 * @param red
	 * @param green
	 * @param blue
	 * @param alpha
	 */
	private void drawBoxArray(AxisAlignedBB[] bbArray, Color[] colorArray)
	{
		worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		
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
			worldRenderer.pos(bb.minX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
			worldRenderer.pos(bb.maxX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
			worldRenderer.pos(bb.maxX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
			worldRenderer.pos(bb.minX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
			
			// only draw the other 5 sides if there is some thickness to this box
			if (bb.minY != bb.maxY)
			{
				worldRenderer.pos(bb.minX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
				worldRenderer.pos(bb.maxX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
				worldRenderer.pos(bb.maxX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
				worldRenderer.pos(bb.minX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
				
				worldRenderer.pos(bb.minX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
				worldRenderer.pos(bb.minX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
				worldRenderer.pos(bb.maxX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
				worldRenderer.pos(bb.maxX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
				
				worldRenderer.pos(bb.minX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
				worldRenderer.pos(bb.minX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
				worldRenderer.pos(bb.maxX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
				worldRenderer.pos(bb.maxX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
				
				worldRenderer.pos(bb.minX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
				worldRenderer.pos(bb.minX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
				worldRenderer.pos(bb.minX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
				worldRenderer.pos(bb.minX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
				
				worldRenderer.pos(bb.maxX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
				worldRenderer.pos(bb.maxX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
				worldRenderer.pos(bb.maxX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
				worldRenderer.pos(bb.maxX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
			}
			
			// so we can get the next color
			colorIndex++;
		}
		
		mc.world.profiler.endStartSection("lod draw");
		
		tessellator.draw();
	}
	
	/**
	 * Sets up and enables the fog to be rendered.
	 */
	private void setupFog()
	{
		float f = farPlaneDistance * (viewDistanceMultiplier * 0.5f);
		GlStateManager.setFogDensity(0.1f);
		GlStateManager.setFogStart(farPlaneDistance * (viewDistanceMultiplier * 0.25f));
		GlStateManager.setFogEnd(f);
		GlStateManager.enableFog();
	}
	
	/**
	 * Originally from Minecraft's EntityRenderer
	 * Changes the field of view of the player depending on if they are underwater or not
	 */
	public float getFOVModifier(float partialTicks, boolean useFOVSetting)
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
}