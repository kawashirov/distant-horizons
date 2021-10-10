package com.seibel.lod.proxy;

import com.mojang.blaze3d.systems.RenderSystem;
import com.seibel.lod.enums.GlProxyContext;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

/**
 * A singleton that holds references to different openGL contexts
 * and GPU capabilities.
 *
 * <p>
 * Helpful OpenGL resources: <br><br>
 *
 * https://www.seas.upenn.edu/~pcozzi/OpenGLInsights/OpenGLInsights-AsynchronousBufferTransfers.pdf <br>
 * https://learnopengl.com/Advanced-OpenGL/Advanced-Data <br>
 * https://gamedev.stackexchange.com/questions/91995/edit-vbo-data-or-create-a-new-one <br><br>
 *
 *
 * @author James Seibel
 * @version 10-2-2021
 */
public class GlProxy
{
	private static GlProxy instance = null;
	
	/** Minecraft's GLFW window */
	public final long minecraftGlContext;
	/** Minecraft's GL context */
	public final GLCapabilities minecraftGlCapabilities;
	
	/** the LodBuilder's GLFW window */
	public final long lodBuilderGlContext;
	/** the LodBuilder's GL context */
	public final GLCapabilities lodBuilderGlCapabilities;
	
	/** This is just used for debugging, hopefully it can be removed once 
	 * the context switching is more stable. */
	public Thread lodBuilderOwnerThread = null;
	
	/** Does this computer's GPU support fancy fog? */
	public final boolean fancyFogAvailable;
	
	
	
	private GlProxy()
	{
		// getting Minecraft's context has to be done on the render thread,
		// where the GL context is
		if (!RenderSystem.isOnRenderThread())
			throw new IllegalStateException(GlProxy.class.getSimpleName() + " was created outside the render thread!");
		
		
		
		
		//============================//
		// create the builder context //
		//============================//
		
		// get Minecraft's context
		minecraftGlContext = GLFW.glfwGetCurrentContext();
		minecraftGlCapabilities = GL.getCapabilities();
		
		// create the LodBuilder's context
		
		// Hopefully this shouldn't cause any issues with other mods that need custom contexts
		// (although the number that do should be relatively few)
		GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
		
		// create an invisible window to hold the context
		lodBuilderGlContext = GLFW.glfwCreateWindow(640, 480, "LOD window", 0L, minecraftGlContext);
		GLFW.glfwMakeContextCurrent(lodBuilderGlContext);
		lodBuilderGlCapabilities = GL.createCapabilities();
		
		// Since this is called on the render thread, make sure the Minecraft context is used in the end
		GLFW.glfwMakeContextCurrent(minecraftGlContext);
		GL.setCapabilities(minecraftGlCapabilities);
		
		
		
		
		//==================================//
		// get any GPU related capabilities //
		//==================================//
		
		// see if this GPU can run fancy fog
		fancyFogAvailable = GL.getCapabilities().GL_NV_fog_distance;
		
		if (!fancyFogAvailable)
		{
			ClientProxy.LOGGER.info("This GPU does not support GL_NV_fog_distance. This means that the fancy fog option will not be available.");
		}
	}
	
	
	
	
	/**
	 * A wrapper function to make switching contexts easier. <br>
	 * Does nothing if the calling thread is already using newContext.
	 */
	public void setGlContext(GlProxyContext newContext)
	{
		GlProxyContext currentContext = getGlContext();
		
		// we don't have to change the context, we're already there.
		if (currentContext == newContext)
			return;
		
		
		long contextPointer = 0L;
		GLCapabilities newGlCapabilities = null;
		
		// get the pointer(s) for this context
		switch (newContext)
		{
		case LOD_BUILDER:
			contextPointer = lodBuilderGlContext;
			newGlCapabilities = lodBuilderGlCapabilities;
			break;
		
		case MINECRAFT:
			contextPointer = minecraftGlContext;
			newGlCapabilities = minecraftGlCapabilities;
			break;
		
		default: // default should never happen, it is just here to make the compiler happy
		case NONE:
			// 0L is equivalent to null
			break;
		}
		
		
		GLFW.glfwMakeContextCurrent(contextPointer);
		GL.setCapabilities(newGlCapabilities);
		
		
		
		// used for debugging
		if (newContext == GlProxyContext.LOD_BUILDER)
			lodBuilderOwnerThread = Thread.currentThread();
		else if (newContext == GlProxyContext.NONE && currentContext == GlProxyContext.LOD_BUILDER)
			lodBuilderOwnerThread = null;
	}
	
	
	
	
	
	/** Returns this thread's OpenGL context. */
	public GlProxyContext getGlContext()
	{
		long currentContext = GLFW.glfwGetCurrentContext();
		
		
		if (currentContext == lodBuilderGlContext)
			return GlProxyContext.LOD_BUILDER;
		else if (currentContext == minecraftGlContext)
			return GlProxyContext.MINECRAFT;
		else if (currentContext == 0L)
			return GlProxyContext.NONE;
		else
			// hopefully this shouldn't happen, but
			// at least now we will be notified if it does happen
			throw new IllegalStateException(Thread.currentThread().getName() + " has a unkown OpenGl context: [" + currentContext + "]. Minecraft context [" + minecraftGlContext + "], LodBuilder context [" + lodBuilderGlContext + "], no context [0].");
	}
	
	
	public static GlProxy getInstance()
	{
		try
		{
			if (instance == null)
				instance = new GlProxy();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return instance;
	}
	
	
	
	
	
	
	
	
}
