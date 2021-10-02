package com.seibel.lod.proxy;

import org.apache.commons.lang3.SystemUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.CGL;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLX;
import org.lwjgl.opengl.GLX14;
import org.lwjgl.opengl.WGL;
import org.lwjgl.system.linux.X11;
import org.lwjgl.system.linux.XVisualInfo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.seibel.lod.enums.GlProxyContext;

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
 * @version 10-1-2021
 */
public class GlProxy
{
	private static GlProxy instance = null;
	
	
	// windows context variables
	private long windowsDeviceContext;
	
	// mac context variables
	private long macPixelFormat;
	
	// linux context variables
	long linuxDisplayID;
	
	long linuxMcDrawable;
	long linuxMcReadDrawable;
	
	long linuxLodBuilderDrawable;
	long linuxLodBuilderReadDrawable;
	
	
	
	public final long minecraftGlContext;
	public final GLCapabilities minecraftGlCapabilities;
	
	public final long lodBuilderGlContext;
	public final GLCapabilities lodBuilderGlCapabilities;
	
	/** This is just used for debugging, hopefuly it can be removed once 
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
		
		
		minecraftGlCapabilities = GL.getCapabilities();
		
		// run OS specific context creation code
		if (SystemUtils.IS_OS_WINDOWS)
		{
			ClientProxy.LOGGER.info(GlProxy.class.getSimpleName() + " Running Windows setup.");
			
			// get Minecraft variables
			minecraftGlContext = WGL.wglGetCurrentContext();
			windowsDeviceContext = WGL.wglGetCurrentDC();
			
			// setup the lodBuilder variables
			lodBuilderGlContext = WGL.wglCreateContext(windowsDeviceContext);
			if (!WGL.wglShareLists(minecraftGlContext, lodBuilderGlContext))
				throw new IllegalStateException("Unable to share lists between Minecraft and builder contexts.");
			if (!WGL.wglMakeCurrent(windowsDeviceContext, lodBuilderGlContext))
				throw new IllegalStateException("Unable to change OpenGL contexts! tried to change to [" + GlProxyContext.LOD_BUILDER.toString() + "] from [" + GlProxyContext.MINECRAFT.toString() + "]");
			lodBuilderGlCapabilities = GL.createCapabilities();
		}
		else if (SystemUtils.IS_OS_MAC)
		{
			ClientProxy.LOGGER.info(GlProxy.class.getSimpleName() + " Running Mac setup.");
			
			// get Minecraft variables
			minecraftGlContext = CGL.CGLGetCurrentContext();
			macPixelFormat = CGL.CGLGetPixelFormat(minecraftGlContext);
			
			// setup the lodBuilder variables
			lodBuilderGlContext = CGL.CGLCreateContext(macPixelFormat, minecraftGlContext, null);
			lodBuilderGlCapabilities = GL.createCapabilities();
		}
		else
		{
			// if we can't determine the OS, default to linux
			if (SystemUtils.IS_OS_LINUX)
				ClientProxy.LOGGER.info(GlProxy.class.getSimpleName() + " Running Linux setup.");
			else
				ClientProxy.LOGGER.info(GlProxy.class.getSimpleName() + " Unkown OS: [" + SystemUtils.OS_NAME + "] Running Linux setup.");
			
			
			// get Minecraft variables //
			
			minecraftGlContext = GLX14.glXGetCurrentContext();
			linuxDisplayID = GLX14.glXGetCurrentDisplay();
			
			
			// setup the lodBuilder variables //
			
			linuxMcDrawable = GLX14.glXGetCurrentDrawable();
			linuxMcReadDrawable = GLX14.glXGetCurrentReadDrawable();
			
			// single buffered example config
//			int attributeList[] = {	GLX.GLX_RGBA,
//					GLX.GLX_RED_SIZE, 1,
//					GLX.GLX_GREEN_SIZE, 1,
//					GLX.GLX_BLUE_SIZE, 1,
//					GLX.GLX_DEPTH_SIZE, 12,
//					X11.None };
			
			// use the defaults for all config options
			int attributeList[] = { X11.None };
			
			PointerBuffer config = GLX14.glXChooseFBConfig(linuxDisplayID, 0, attributeList);
			XVisualInfo visualInfo = GLX14.glXGetVisualFromFBConfig(linuxDisplayID, config.address());
			lodBuilderGlContext = GLX14.glXCreateContext(linuxDisplayID, visualInfo, minecraftGlContext, false);
			lodBuilderGlCapabilities = GL.createCapabilities();
			
			linuxLodBuilderDrawable = GLX14.glXGetCurrentDrawable();
			linuxLodBuilderReadDrawable = GLX14.glXGetCurrentReadDrawable();
		}
		
		
		// Since this is called on the render thread, make sure the Minecraft context is used in the end
		setGlContext(GlProxyContext.MINECRAFT);
		
		
		
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
	 * 
	 * @throws IllegalStateException if unable to change to newContext. <br>
	 * 								 This exception should never be thrown if 
	 * 								 switching to GlProxyContext.NONE
	 */
	public void setGlContext(GlProxyContext newContext)
	{
		GlProxyContext currentContext = getGlContext();
		
		// we don't have to change the context, we're already there.
		if (currentContext == newContext)
			return;
		
		
		long contextPointer = 0L;
		long linuxDrawable = 0L; 
		long linuxReadDrawable = 0L;
		GLCapabilities newGlCapabilities = null;
		
		// get the pointer(s) for this context
		switch(newContext)
		{
		case LOD_BUILDER:
			linuxDrawable = linuxLodBuilderDrawable;
			linuxReadDrawable = linuxLodBuilderReadDrawable;
			
			contextPointer = lodBuilderGlContext;
			newGlCapabilities = lodBuilderGlCapabilities;
			break;
		
		case MINECRAFT:
			linuxDrawable = linuxMcDrawable;
			linuxReadDrawable = linuxMcReadDrawable;
			
			contextPointer = minecraftGlContext;
			newGlCapabilities = minecraftGlCapabilities;
			break;
		
		default: // default should never happen, it is just here to make the compiler happy
		case NONE:
			// 0L is equivalent to null
			linuxDrawable = 0L;
			linuxReadDrawable = 0L;
			
			contextPointer = 0L;
			newGlCapabilities = null;
			break;
		}
		
		
		String contextSwitchError = "Unable to change OpenGL contexts! tried to change to [" + newContext.toString() + "] from [" + currentContext.toString() + "] on thread: [" + Thread.currentThread().getName() + "] lod builder owner thread: " + (lodBuilderOwnerThread != null ? lodBuilderOwnerThread.getName() : "null");
		
		// run the OS specific context switching code
		if (SystemUtils.IS_OS_WINDOWS)
		{
			if (!WGL.wglMakeCurrent(windowsDeviceContext, contextPointer))
				throw new IllegalStateException(contextSwitchError);	
		}
		else if (SystemUtils.IS_OS_MAC)
		{
			if (CGL.CGLSetCurrentContext(contextPointer) != CGL.kCGLNoError)
				throw new IllegalStateException(contextSwitchError);
		}
		else //if (SystemUtils.IS_OS_LINUX)
		{
			// default to linux
			if (!GLX14.glXMakeContextCurrent(linuxDisplayID, linuxDrawable, linuxReadDrawable, contextPointer))
				throw new IllegalStateException(contextSwitchError);
		}
		
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
		long currentContext;
		
		if (SystemUtils.IS_OS_WINDOWS)
			currentContext = WGL.wglGetCurrentContext();
		else if (SystemUtils.IS_OS_MAC)
			currentContext = CGL.CGLGetCurrentContext();
		else //if (SystemUtils.IS_OS_LINUX) // default to Linux
			currentContext = GLX.glXGetCurrentContext();
		
		
		if(currentContext == lodBuilderGlContext)
			return GlProxyContext.LOD_BUILDER;
		else if(currentContext == minecraftGlContext)
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
		}catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return instance;
	}
	
	
	
	
	
	
	
	
	
}
