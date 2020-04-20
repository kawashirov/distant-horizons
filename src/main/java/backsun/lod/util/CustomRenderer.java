package backsun.lod.util;

import java.awt.Color;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.glu.Project;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
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
		float fov = mc.entityRenderer.getFOVModifier(partialTicks, true);
		
		// create a new view frustum so that the squares can be drawn outside the normal view distance
		GlStateManager.matrixMode(GL11.GL_PROJECTION);
		GlStateManager.loadIdentity();
		// farPlaneDistance // 10 chunks = 160
		Project.gluPerspective(fov, (float) this.mc.displayWidth / (float) this.mc.displayHeight, 0.0125f, farPlaneDistance * 12.0f);
	}
	
	/**
	 * 
	 * @param mc
	 * @param partialTicks
	 */
	public void drawTest(Minecraft mc, float partialTicks)
	{
		farPlaneDistance = this.mc.gameSettings.renderDistanceChunks * 16;
		
		// TODO add fog
		//setupFog();
		GlStateManager.disableFog();
		
		// set the new model view matrix
		setProjectionMatrix(partialTicks);
		
		// used for debugging
		mc.world.profiler.startSection("renderlod");
		
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
		
		Color grass = new Color(80, 104, 50, 255);
		Color black = Color.BLACK;
		Color white = Color.WHITE;
		
		// set how big the squares will be and how far they will go
		int totalLength = (int) farPlaneDistance * 9;
		int singleLength = 160;
		int numbOfBoxesWide = totalLength / singleLength;
		
		// size of a single square
		int bbx = singleLength;
		int bby = 0;
		int bbz = singleLength;
		
		// this where we will start drawing squares
		int startXZ = -singleLength * numbOfBoxesWide;
		
		
		AxisAlignedBB bb;
		
		// this is used to alternate the colors of the drawn squares
		boolean alternateColor = false;
		// x axis
		for (int i = 0; i < numbOfBoxesWide; i++)
		{
			// z axis
			for (int j = 0; j < numbOfBoxesWide; j++)
			{
				// update where this square will be drawn
				double xoffset = -x + (singleLength * i * 2) + startXZ;
				double yoffset = -y;
				double zoffset = -z + (singleLength * j * 2) + startXZ;
				
				// create a new bounding box to store our points
				bb = new AxisAlignedBB(bbx, bby, bbz, -bbx, bby, -bbz).offset(xoffset, yoffset, zoffset);
				
				// draw the squares as a black and white checker board
				Color c;
				if (alternateColor)
					c = white;
				else
					c = black;
				alternateColor = !alternateColor;
				
				// draw the square
				drawBox(bb, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			}
		}
		
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

	/**
	 * draw an cube (or square) with the given colors
	 * @param bb the cube to draw
	 * @param red
	 * @param green
	 * @param blue
	 * @param alpha
	 */
	private void drawBox(AxisAlignedBB bb, int red, int green, int blue, int alpha)
	{
		worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		worldRenderer.pos(bb.minX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
		worldRenderer.pos(bb.maxX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
		worldRenderer.pos(bb.maxX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
		worldRenderer.pos(bb.minX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
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
		
		tessellator.draw();
		//TODO are these needed?
		//	worldRenderer.finishDrawing();
		//	worldRenderer.reset();
	}
    
    
	/**
	 * Sets up the fog to be rendered. If the argument passed in is -1 the fog starts at 0 and goes to 80% of far plane
	 * distance and is used for sky rendering.
	 */
	private void setupFog()
	{
		GlStateManager.glNormal3f(0.0F, -1.0F, 0.0F);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		
		
		float f = this.farPlaneDistance * 15.0f; //15 linear
		GlStateManager.setFog(GlStateManager.FogMode.LINEAR);
		
		GlStateManager.setFogDensity(0.0f);
		GlStateManager.setFogStart(f * 0.7F); // 0.7
		GlStateManager.setFogEnd(f);
		
		if (GLContext.getCapabilities().GL_NV_fog_distance)
		{
			GlStateManager.glFogi(34138, 34139);
		}
		
		
		GlStateManager.enableColorMaterial();
		GlStateManager.enableFog();
		GlStateManager.colorMaterial(1028, 4608);
	}
}