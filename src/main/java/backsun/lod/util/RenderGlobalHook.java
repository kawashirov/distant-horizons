package backsun.lod.util;

import org.lwjgl.opengl.GL11;

import net.minecraft.util.BlockRenderLayer;

/**
 * 
 * @author James Seibel
 * @version 02-07-2021
 */
public class RenderGlobalHook
{
	/** 
	 * this variable should be the same as the method name below.
	 * It is used when transforming the RenderGlobal class' 
	 * renderBlockLayer method.
	 */
	public static final String START_STENCIL_METHOD_NAME = "startRenderingStencil";
	
	/**
	 * Start drawing to the stencil
	 * <br><br>
	 * called in the order: <br>
	 * BlockRenderLayer.SOLID <br>
	 * BlockRenderLayer.CUTOUT_MIPPED <br>
	 * BlockRenderLayer.CUTOUT <br>
	 * BlockRenderLayer.TRANSLUCENT <br>
	 */
	public static void startRenderingStencil(BlockRenderLayer blockLayerIn)
	{
		// solid is the first layer rendered
		// clear the buffer so we can start fresh.
		// if this isn't cleared first we will have overlap with the fog
		// outside the world
		if (blockLayerIn == BlockRenderLayer.SOLID)
		{
			GL11.glClearStencil(0);
			GL11.glStencilMask(0xFF); //255
			GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
		}
		
		GL11.glEnable(GL11.GL_STENCIL_TEST);
		GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0x11111111); // the 2 numbers here don't matter since GL_ALWAYS is being used
		GL11.glStencilMask(0b11111111);
		GL11.glStencilOp(GL11.GL_KEEP, // this doesn't mater since GL_ALWAYS is being used
				GL11.GL_KEEP,  // stencil test passes
				GL11.GL_REPLACE); // stencil + depth pass
	}
	
	
	/** 
	 * this variable should be the same as the method name below.
	 * It is used when transforming the RenderGlobal class' 
	 * renderBlockLayer method.
	 */
	public static final String END_STENCIL_METHOD_NAME = "endRenderingStencil";
	
	public static void endRenderingStencil(BlockRenderLayer blockLayerIn)
	{
		GL11.glStencilOp(GL11.GL_KEEP, // this doesn't mater since GL_ALWAYS is being used
				GL11.GL_KEEP,  // stencil test passes
				GL11.GL_KEEP); // stencil + depth pass
	}
}
