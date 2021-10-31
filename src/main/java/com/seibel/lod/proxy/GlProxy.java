/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.proxy;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;

import com.mojang.blaze3d.systems.RenderSystem;
import com.seibel.lod.ModInfo;
import com.seibel.lod.enums.GlProxyContext;
import com.seibel.lod.wrappers.MinecraftWrapper;

/**
 * A singleton that holds references to different openGL contexts
 * and GPU capabilities.
 *
 * <p>
 * Helpful OpenGL resources: <br><br>
 * <p>
 * https://www.seas.upenn.edu/~pcozzi/OpenGLInsights/OpenGLInsights-AsynchronousBufferTransfers.pdf <br>
 * https://learnopengl.com/Advanced-OpenGL/Advanced-Data <br>
 * https://gamedev.stackexchange.com/questions/91995/edit-vbo-data-or-create-a-new-one <br><br>
 * 
 * @author James Seibel
 * @version 10-31-2021
 */
public class GlProxy
{
	private static GlProxy instance = null;
	
	private static MinecraftWrapper mc = MinecraftWrapper.INSTANCE;
	
	/** Minecraft's GLFW window */
	public final long minecraftGlContext;
	/** Minecraft's GL capabilities */
	public final GLCapabilities minecraftGlCapabilities;
	
	/** the LodBuilder's GLFW window */
	public final long lodBuilderGlContext;
	/** the LodBuilder's GL capabilities */
	public final GLCapabilities lodBuilderGlCapabilities;
	
	/** the LodRender's GLFW window */
	public final long lodRenderGlContext;
	/** the LodRender's GL capabilities */
	public final GLCapabilities lodRenderGlCapabilities;
	
	/**
	 * This is just used for debugging, hopefully it can be removed once
	 * the context switching is more stable.
	 */
	public Thread lodBuilderOwnerThread = null;
	
	/** Does this computer's GPU support fancy fog? */
	public final boolean fancyFogAvailable;
	
	/** Requires OpenGL 4.5, and offers the best buffer uploading */
	public final boolean bufferStorageSupported;
	
	/** Requires OpenGL 3.0 */
	public final boolean mapBufferRangeSupported;
	
	
	private GlProxy()
	{
		ClientProxy.LOGGER.error("Creating " + GlProxy.class.getSimpleName() + "... If this is the last message you see in the log there must have been a OpenGL error.");
		
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
		
		
		// context creation setup
		GLFW.glfwDefaultWindowHints();
		// make the context window invisible
		GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
		// by default the context should get the highest available OpenGL version
		// but this can be explicitly set for testing
//		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
//		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 5);
		
		
		// create the LodRender context 
		lodRenderGlContext = GLFW.glfwCreateWindow(64, 48, "LOD Render Window", 0L, 0L); // create a window to hold the context
		GLFW.glfwMakeContextCurrent(lodRenderGlContext);
		lodRenderGlCapabilities = GL.createCapabilities();
		
		// create the LodBuilder context
		lodBuilderGlContext = GLFW.glfwCreateWindow(64, 48, "LOD Builder Window", 0L, lodRenderGlContext);
		GLFW.glfwMakeContextCurrent(lodBuilderGlContext);
		lodBuilderGlCapabilities = GL.createCapabilities();
		
		
		
		
		
		//==================================//
		// get any GPU related capabilities //
		//==================================//
		
		ClientProxy.LOGGER.info("Lod Render OpenGL version [" + GL11.glGetString(GL11.GL_VERSION) + "].");
		
		// crash the game if the GPU doesn't support OpenGL 1.5
		if (!minecraftGlCapabilities.OpenGL15)
		{
			// Note: as of MC 1.17 this shouldn't happen since MC
			// requires OpenGL 3.3, but just in case.
			String errorMessage = ModInfo.READABLE_NAME + " was initializing " + GlProxy.class.getSimpleName() + " and discoverd this GPU doesn't support OpenGL 1.5 or greater.";
			mc.crashMinecraft(errorMessage + " Sorry I couldn't tell you sooner :(", new UnsupportedOperationException("This GPU doesn't support OpenGL 1.5 or greater."));
		}
		
		
		
		// get specific capabilities
		bufferStorageSupported = lodBuilderGlCapabilities.glBufferStorage != 0;
		mapBufferRangeSupported = lodBuilderGlCapabilities.glMapBufferRange != 0;
		fancyFogAvailable = minecraftGlCapabilities.GL_NV_fog_distance;
		
		
		// display the capabilities
		if (!bufferStorageSupported)
		{
			String fallBackVersion = mapBufferRangeSupported ? "3.0" : "1.5";  
			ClientProxy.LOGGER.error("This GPU doesn't support Buffer Storage (OpenGL 4.5), falling back to OpenGL " + fallBackVersion + ". This may cause stuttering and reduced performance.");			
		}
		
		if (!fancyFogAvailable)
			ClientProxy.LOGGER.info("This GPU does not support GL_NV_fog_distance. This means that the fancy fog option will not be available.");
		
		
		
		
		
		//==========//
		// clean up //
		//==========//
		
		// Since this is created on the render thread, make sure the Minecraft context is used in the end
		GLFW.glfwMakeContextCurrent(minecraftGlContext);
		GL.setCapabilities(minecraftGlCapabilities);
		
		
		// GlProxy creation success
		ClientProxy.LOGGER.error(GlProxy.class.getSimpleName() + " creation successful. OpenGL smiles upon you this day.");
	}
	
	
	
	
	/**
	 * A wrapper function to make switching contexts easier. <br>
	 * Does nothing if the calling thread is already using newContext.
	 */
	public void setGlContext(GlProxyContext newContext)
	{
		GlProxyContext currentContext = getGlContext();
		
		// we don't have to change the context, we are already there.
		if (currentContext == newContext)
			return;
		
		
		long contextPointer;
		GLCapabilities newGlCapabilities = null;
		
		// get the pointer(s) for this context
		switch (newContext)
		{
		case LOD_BUILDER:
			contextPointer = lodBuilderGlContext;
			newGlCapabilities = lodBuilderGlCapabilities;
			break;
			
		case LOD_RENDER:
			contextPointer = lodRenderGlContext;
			newGlCapabilities = lodRenderGlCapabilities;
			break;
		
		case MINECRAFT:
			contextPointer = minecraftGlContext;
			newGlCapabilities = minecraftGlCapabilities;
			break;
		
		default: // default should never happen, it is just here to make the compiler happy
		case NONE:
			// 0L is equivalent to null
			contextPointer = 0L;
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
		else if (currentContext == lodRenderGlContext)
			return GlProxyContext.LOD_RENDER;
		else if (currentContext == minecraftGlContext)
			return GlProxyContext.MINECRAFT;
		else if (currentContext == 0L)
			return GlProxyContext.NONE;
		else
			// hopefully this shouldn't happen
			throw new IllegalStateException(Thread.currentThread().getName() + 
					" has a unknown OpenGl context: [" + currentContext + "]. "
					+ "Minecraft context [" + minecraftGlContext + "], "
					+ "LodBuilder context [" + lodBuilderGlContext + "], "
					+ "LodRender context [" + lodRenderGlContext + "], no context [0].");
	}
	
	
	public static GlProxy getInstance()
	{
		if (instance == null)
			instance = new GlProxy();
		
		return instance;
	}
	
	
	
	
	
	
	
	
}
