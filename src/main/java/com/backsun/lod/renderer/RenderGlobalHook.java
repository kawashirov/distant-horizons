package com.backsun.lod.renderer;


import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.RenderType;

/**
 * This code is used for drawing 
 * to the stencil buffer.
 * 
 * @author James Seibel
 * @version 02-17-2021
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
	 * This method tells OpenGL to start drawing everything to the stencil.
	 * This is done to prevent LODs from being rendered on top of the world.
	 * <br><br>
	 * Called in the order (as of minecraft 1.16.4): <br>
	 * RenderType.getSolid() <br>
	 * RenderType.getCutoutMipped() <br>
	 * RenderType.getCutout() <br>
	 * RenderType.getTranslucent() <br>
	 * RenderType.getTripwire() <br>
	 */
	public static void startRenderingStencil(RenderType blockLayerIn)
	{
		// we only enable drawing to the stencil once since
		// we want to skip the rendering of the out of world fog
		// but catch everything else
		if (blockLayerIn == RenderType.getSolid())
		{
			// solid is the first layer rendered
			// clear the buffer so we can start fresh.
			// if this isn't cleared first we will have overlap with the fog
			// outside the world
			GL11.glClearStencil(0);
			GL11.glStencilMask(0x11111111);
			GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
			
			GL11.glEnable(GL11.GL_STENCIL_TEST);
			GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0x11111111);
			GL11.glStencilMask(0b11111111);
			GL11.glStencilOp(GL11.GL_KEEP, // this doesn't mater since GL_ALWAYS is being used
					GL11.GL_KEEP,  // stencil test passes
					GL11.GL_REPLACE); // stencil + depth pass
		}
	}
	
	
	/** 
	 * this variable should be the same as the method name below.
	 * It is used when transforming the RenderGlobal class' 
	 * renderBlockLayer method.
	 */
	public static final String END_STENCIL_METHOD_NAME = "endRenderingStencil";
	
	/**
	 * Currently this method isn't used in any transformations since we end
	 * the stencil drawing in the ClientProxy right before we draw the LODs.
	 */
	public static void endRenderingStencil(RenderType blockLayerIn)
	{
		GL11.glStencilOp(GL11.GL_KEEP, // this doesn't mater since GL_ALWAYS is being used
				GL11.GL_KEEP,  // stencil test passes
				GL11.GL_KEEP); // stencil + depth pass
	}
	
	public static void endRenderingStencil()
	{
		endRenderingStencil(null);
	}
}
