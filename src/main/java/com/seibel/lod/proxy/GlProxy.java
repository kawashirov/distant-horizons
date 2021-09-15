package com.seibel.lod.proxy;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.WGL;

import com.mojang.blaze3d.systems.RenderSystem;

/**
 * A singleton that holds references to different openGL contexts.
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
 * @version 9-14-2021
 */
public class GlProxy
{
	private static GlProxy instance = null;
	
	public final long deviceContext;
	
	public long minecraftGlContext;
	public GLCapabilities minecraftGlCapabilities;
	
	public long lodBuilderGlContext;
	public GLCapabilities lodBuilderGlCapabilities;
	public long lodRenderGlContext;
	public GLCapabilities lodRenderGlCapabilities;
	
	private GlProxy()
	{
		// getting Minecraft's context has to be done on the render thread,
		// where the GL context is
		if (!RenderSystem.isOnRenderThread())
			throw new IllegalStateException(GlProxy.class.getSimpleName() + " was created outside the render thread!");
		
		
		minecraftGlContext = WGL.wglGetCurrentContext();
		minecraftGlCapabilities = GL.getCapabilities();
		deviceContext = WGL.wglGetCurrentDC();
		
		lodBuilderGlContext = WGL.wglCreateContext(deviceContext);
		if (!WGL.wglShareLists(minecraftGlContext, lodBuilderGlContext))
			throw new IllegalStateException("Unable to share lists between Minecraft and builder contexts.");
		if (!WGL.wglMakeCurrent(deviceContext, lodBuilderGlContext))
			throw new IllegalStateException("Unable to change OpenGL contexts! tried to change to [" + GlProxyContext.ALPHA.toString() + "] from [" + GlProxyContext.MINECRAFT.toString() + "]");
		lodBuilderGlCapabilities = GL.createCapabilities();
		WGL.wglMakeCurrent(deviceContext, 0L);
			
		
		
		lodRenderGlContext = WGL.wglCreateContext(deviceContext);
		if (!WGL.wglShareLists(minecraftGlContext, lodRenderGlContext))
			throw new IllegalStateException("Unable to share lists between builder and render contexts.");
		if (!WGL.wglMakeCurrent(deviceContext, lodRenderGlContext))
			throw new IllegalStateException("Unable to change OpenGL contexts! tried to change to [" + GlProxyContext.ALPHA.toString() + "] from [" + GlProxyContext.BETA.toString() + "]");
		lodRenderGlCapabilities = GL.createCapabilities();
		
		
		// Since this is called on the render thread, make sure the Minecraft context is used in the end
		WGL.wglMakeCurrent(deviceContext, minecraftGlContext);
	}
	
	
	/**
	 * A simple wrapper function to make switching contexts easier
	 */
	public void setGlContext(GlProxyContext context)
	{
		GlProxyContext currentContext = getGlContext();
		
		long contextPointer = 0L;
		GLCapabilities newGlCapabilities = null;
		switch(context)
		{
		case ALPHA:
			contextPointer = lodBuilderGlContext;
			newGlCapabilities = lodBuilderGlCapabilities;
			break;
		case BETA:
			contextPointer = lodRenderGlContext;
			newGlCapabilities = lodRenderGlCapabilities;
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
			throw new IllegalStateException("Unable to change OpenGL contexts! tried to change to [" + context.toString() + "] from [" + currentContext.toString() + "]");
		
		GL.setCapabilities(newGlCapabilities);
	}
	public GlProxyContext getGlContext()
	{
		long currentContext = WGL.wglGetCurrentContext();
		if(currentContext == lodBuilderGlContext)
		{
			return GlProxyContext.ALPHA;
		}
		else if(currentContext == lodRenderGlContext)
		{
			return GlProxyContext.BETA;
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
		ALPHA,
		BETA,
		
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
