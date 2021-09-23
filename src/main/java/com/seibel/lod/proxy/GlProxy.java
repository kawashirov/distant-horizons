package com.seibel.lod.proxy;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.WGL;

import com.mojang.blaze3d.systems.RenderSystem;

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
 * @version 9-15-2021
 */
public class GlProxy
{
	private static GlProxy instance = null;
	
	public final long deviceContext;
	
	public long minecraftGlContext;
	public GLCapabilities minecraftGlCapabilities;
	
	public long lodBuilderGlContext;
	public GLCapabilities lodBuilderGlCapabilities;
	/** This is just used for debugging, hopefuly it can be removed once 
	 * the context switching is more stable. */
	public Thread lodBuilderOwnerThread = null;
	
	/**
	 * Does this computer's GPU support fancy fog?
	 */
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
		
		minecraftGlContext = WGL.wglGetCurrentContext();
		minecraftGlCapabilities = GL.getCapabilities();
		deviceContext = WGL.wglGetCurrentDC();
		
		lodBuilderGlContext = WGL.wglCreateContext(deviceContext);
		if (!WGL.wglShareLists(minecraftGlContext, lodBuilderGlContext))
			throw new IllegalStateException("Unable to share lists between Minecraft and builder contexts.");
		if (!WGL.wglMakeCurrent(deviceContext, lodBuilderGlContext))
			throw new IllegalStateException("Unable to change OpenGL contexts! tried to change to [" + GlProxyContext.LOD_BUILDER.toString() + "] from [" + GlProxyContext.MINECRAFT.toString() + "]");
		lodBuilderGlCapabilities = GL.createCapabilities();
		WGL.wglMakeCurrent(deviceContext, 0L);
		
		
		// Since this is called on the render thread, make sure the Minecraft context is used in the end
		WGL.wglMakeCurrent(deviceContext, minecraftGlContext);
		
		
		
		
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
	 * A simple wrapper function to make switching contexts easier
	 * 
	 * @throws IllegalStateException if unable to change to newContext. <br>
	 * 								 This expection should never be thrown if 
	 * 								 switching to GlProxyContext.NONE
	 */
	public void setGlContext(GlProxyContext newContext)
	{
		GlProxyContext currentContext = getGlContext();
		
		// we don't have to change the context, we're already there.
		if (currentContext == newContext)
			return;
		
		
		long contextPointer = 0L;
		GLCapabilities newGlCapabilities = null;
		switch(newContext)
		{
		case LOD_BUILDER:
			contextPointer = lodBuilderGlContext;
			newGlCapabilities = lodBuilderGlCapabilities;
			break;
		case MINECRAFT:
			contextPointer = minecraftGlContext;
			newGlCapabilities = minecraftGlCapabilities;
			break;
		case NONE:
			contextPointer = 0L; // equivalent to null
			newGlCapabilities = null;
			break;
			
		default:
			// should never happen, here to make the compiler happy
		}
		
		if (!WGL.wglMakeCurrent(deviceContext, contextPointer))
			throw new IllegalStateException("Unable to change OpenGL contexts! tried to change to [" + newContext.toString() + "] from [" + currentContext.toString() + "] on thread: [" + Thread.currentThread().getName() + "] lod builder owner thread: " + (lodBuilderOwnerThread != null ? lodBuilderOwnerThread.getName() : "null"));
		
		if (newContext == GlProxyContext.LOD_BUILDER)
		{
			lodBuilderOwnerThread = Thread.currentThread();
		}
		else if (newContext == GlProxyContext.NONE && currentContext == GlProxyContext.LOD_BUILDER)
		{
			lodBuilderOwnerThread = null;
		}
		
		GL.setCapabilities(newGlCapabilities);
	}
	
	/**
	 * Returns this thread's OpenGL context.
	 */
	public GlProxyContext getGlContext()
	{
		long currentContext = WGL.wglGetCurrentContext();
		if(currentContext == lodBuilderGlContext)
		{
			return GlProxyContext.LOD_BUILDER;
		}
		else if(currentContext == minecraftGlContext)
		{
			return GlProxyContext.MINECRAFT;
		}
		else 
		{
			return GlProxyContext.NONE;
		}
	}
	
	/** Minecraft, Alpha, Beta, None */
	public enum GlProxyContext
	{
		MINECRAFT,
		LOD_BUILDER,
		
		/** used to un-bind threads */
		NONE,
	}
	
	public static GlProxy getInstance()
	{
		if (instance == null)
			instance = new GlProxy();
		
		return instance;
	}
}
