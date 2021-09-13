package com.seibel.lod.proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.WGL;

import com.mojang.blaze3d.systems.RenderSystem;
import com.seibel.lod.builders.LodBufferBuilder;

/**
 * A singleton that holds references to different openGL contexts.
 * 
 * @author James Seibel
 * @version 9-9-2021
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
		GLFWErrorCallback errorfun = GLFWErrorCallback.createPrint();
		GLFW.glfwSetErrorCallback(errorfun);
		
		
		// getting Minecraft's context has to be done on the render thread,
		// where the GL context is
		if (!RenderSystem.isOnRenderThread())
			throw new IllegalStateException(GlProxy.class.getSimpleName() + " was created outside the render thread!");
		
		minecraftGlContext = WGL.wglGetCurrentContext();
		minecraftGlCapabilities = GL.getCapabilities();
		deviceContext = WGL.wglGetCurrentDC();
		
		
		Callable<Void> callable = () ->
		{
			lodBuilderGlContext = WGL.wglCreateContext(deviceContext);
//			if (!WGL.wglShareLists(minecraftGlContext, lodBuilderGlContext))
//				throw new IllegalStateException("Unable to share lists between Minecraft and builder contexts.");
			if (!WGL.wglMakeCurrent(deviceContext, lodBuilderGlContext))
				throw new IllegalStateException("Unable to change OpenGL contexts! tried to change to [" + GlProxyContext.BUILDER.toString() + "] from [" + GlProxyContext.MINECRAFT.toString() + "]");
			lodBuilderGlCapabilities = GL.createCapabilities();
			WGL.wglMakeCurrent(deviceContext, 0L);
			
			return null;
		};
		
		ArrayList<Callable<Void>> list = new ArrayList<Callable<Void>>();
		list.add(callable);
		try
		{
			List<Future<Void>> futuresBuffer = LodBufferBuilder.mainGenThread.invokeAll(list);
			
			for (Future<Void> future : futuresBuffer)
				if (!future.isDone())
					ClientProxy.LOGGER.error("GLProxy failed to setup.");
		}
		catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		lodRenderGlContext = WGL.wglCreateContext(deviceContext);
//		if (!WGL.wglShareLists(lodBuilderGlContext, lodRenderGlContext))
//			throw new IllegalStateException("Unable to share lists between builder and render contexts.");
		if (!WGL.wglMakeCurrent(deviceContext, lodRenderGlContext))
			throw new IllegalStateException("Unable to change OpenGL contexts! tried to change to [" + GlProxyContext.BUILDER.toString() + "] from [" + GlProxyContext.RENDER.toString() + "]");
		lodRenderGlCapabilities = GL.createCapabilities();
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
		case BUILDER:
			contextPointer = lodBuilderGlContext;
			newGlCapabilities = lodBuilderGlCapabilities;
			break;
		case RENDER:
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
			return GlProxyContext.BUILDER;
		}
		else if(currentContext == lodRenderGlContext)
		{
			return GlProxyContext.RENDER;
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
		BUILDER,
		RENDER,
		
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
