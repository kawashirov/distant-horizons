package backsun.lod.util;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;

public class CustomRenderer
{
	public static void drawTest(Minecraft mc, float partialTicks)
	{
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(2.0f);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
		
		// get the camera location
//		Entity entity = mc.getRenderViewEntity();
		Entity entity = mc.player;
		double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
		double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
		double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
		
		int bbx = 9;
		int bby = 9;
		int bbz = 9;
		// x y z
		AxisAlignedBB bb = new AxisAlignedBB(bbx, bby, bbz, bbx-1, bby-1, bbz-1).offset(-x, -y, -z);
		
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
		GL11.glEnable(GL11.GL_BLEND);
		
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder worldRenderer = tessellator.getBuffer();
		
		worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		worldRenderer.pos(bb.minX, bb.minY, bb.minZ).color(255, 255, 255, 255).endVertex();
		worldRenderer.pos(bb.maxX, bb.minY, bb.minZ).color(255, 255, 255, 255).endVertex();
		worldRenderer.pos(bb.maxX, bb.minY, bb.maxZ).color(255, 255, 255, 255).endVertex();
		worldRenderer.pos(bb.minX, bb.minY, bb.maxZ).color(255, 255, 255, 255).endVertex();
		if (bb.minY != bb.maxY)
		{
			
			worldRenderer.pos(bb.minX, bb.maxY, bb.minZ).color(255, 255, 255, 255).endVertex();
			worldRenderer.pos(bb.maxX, bb.maxY, bb.minZ).color(255, 255, 255, 255).endVertex();
			worldRenderer.pos(bb.maxX, bb.maxY, bb.maxZ).color(255, 255, 255, 255).endVertex();
			worldRenderer.pos(bb.minX, bb.maxY, bb.maxZ).color(255, 255, 255, 255).endVertex();
			
			worldRenderer.pos(bb.minX, bb.minY, bb.maxZ).color(255, 255, 255, 255).endVertex();
			worldRenderer.pos(bb.minX, bb.maxY, bb.maxZ).color(255, 255, 255, 255).endVertex();
			worldRenderer.pos(bb.maxX, bb.maxY, bb.maxZ).color(255, 255, 255, 255).endVertex();
			worldRenderer.pos(bb.maxX, bb.minY, bb.maxZ).color(255, 255, 255, 255).endVertex();
			
			worldRenderer.pos(bb.minX, bb.minY, bb.minZ).color(255, 255, 255, 255).endVertex();
			worldRenderer.pos(bb.minX, bb.maxY, bb.minZ).color(255, 255, 255, 255).endVertex();
			worldRenderer.pos(bb.maxX, bb.maxY, bb.minZ).color(255, 255, 255, 255).endVertex();
			worldRenderer.pos(bb.maxX, bb.minY, bb.minZ).color(255, 255, 255, 255).endVertex();
			
			worldRenderer.pos(bb.minX, bb.minY, bb.minZ).color(255, 255, 255, 255).endVertex();
			worldRenderer.pos(bb.minX, bb.minY, bb.maxZ).color(255, 255, 255, 255).endVertex();
			worldRenderer.pos(bb.minX, bb.maxY, bb.maxZ).color(255, 255, 255, 255).endVertex();
			worldRenderer.pos(bb.minX, bb.maxY, bb.minZ).color(255, 255, 255, 255).endVertex();
			
			worldRenderer.pos(bb.maxX, bb.minY, bb.minZ).color(255, 255, 255, 255).endVertex();
			worldRenderer.pos(bb.maxX, bb.minY, bb.maxZ).color(255, 255, 255, 255).endVertex();
			worldRenderer.pos(bb.maxX, bb.maxY, bb.maxZ).color(255, 255, 255, 255).endVertex();
			worldRenderer.pos(bb.maxX, bb.maxY, bb.minZ).color(255, 255, 255, 255).endVertex();
		}
		
		tessellator.draw();
		
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_POLYGON_OFFSET_LINE);
		GL11.glPolygonOffset(-1.f, -1.f);
		
		
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
		
	}
}
