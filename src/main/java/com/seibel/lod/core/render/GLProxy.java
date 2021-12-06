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

package com.seibel.lod.core.render;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLCapabilities;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.enums.config.GpuUploadMethod;
import com.seibel.lod.core.enums.rendering.GLProxyContext;
import com.seibel.lod.core.render.shader.LodShader;
import com.seibel.lod.core.render.shader.LodShaderProgram;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;

/**
 * A singleton that holds references to different openGL contexts
 * and GPU capabilities.
 *
 * <p>
 * Helpful OpenGL resources:
 * <p>
 * https://www.seas.upenn.edu/~pcozzi/OpenGLInsights/OpenGLInsights-AsynchronousBufferTransfers.pdf <br>
 * https://learnopengl.com/Advanced-OpenGL/Advanced-Data <br>
 * https://www.slideshare.net/CassEveritt/approaching-zero-driver-overhead <br><br>
 * 
 * https://gamedev.stackexchange.com/questions/91995/edit-vbo-data-or-create-a-new-one <br>
 * https://stackoverflow.com/questions/63509735/massive-performance-loss-with-glmapbuffer <br><br>
 * 
 * @author James Seibel
 * @version 12-1-2021
 */
public class GLProxy
{
	private static final IMinecraftWrapper MC = SingletonHandler.get(IMinecraftWrapper.class);
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	
	private static final ExecutorService workerThread = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(GLProxy.class.getSimpleName() + "-Worker-Thread").build());
	
	
	private static GLProxy instance = null;
	
	/** Minecraft's GLFW window */
	public final long minecraftGlContext;
	/** Minecraft's GL capabilities */
	public final GLCapabilities minecraftGlCapabilities;
	
	/** the LodBuilder's GLFW window */
	public final long lodBuilderGlContext;
	/** the LodBuilder's GL capabilities */
	public final GLCapabilities lodBuilderGlCapabilities;

	/** the proxyWorker's GLFW window */
	public final long proxyWorkerGlContext;
	/** the proxyWorker's GL capabilities */
	public final GLCapabilities proxyWorkerGlCapabilities;
	
	
	
	/** This program contains all shaders required when rendering LODs */
	public LodShaderProgram lodShaderProgram;
	/** This is the VAO that is used when rendering */
	public final int vertexArrayObjectId;
	
	
	/** Requires OpenGL 4.5, and offers the best buffer uploading */
	public final boolean bufferStorageSupported;
	
	/** Requires OpenGL 3.0 */
	public final boolean mapBufferRangeSupported;
	
	
	
	
	private GLProxy()
	{
		ClientApi.LOGGER.error("Creating " + GLProxy.class.getSimpleName() + "... If this is the last message you see in the log there must have been a OpenGL error.");
		
		// getting Minecraft's context has to be done on the render thread,
		// where the GL context is
		if (GLFW.glfwGetCurrentContext() == 0L)
			throw new IllegalStateException(GLProxy.class.getSimpleName() + " was created outside the render thread!");
		
		
		
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
		
		
		// create the LodBuilder context
		lodBuilderGlContext = GLFW.glfwCreateWindow(64, 48, "LOD Builder Window", 0L, minecraftGlContext);
		GLFW.glfwMakeContextCurrent(lodBuilderGlContext);
		lodBuilderGlCapabilities = GL.createCapabilities();
		
		
		// create the proxyWorker's context
		proxyWorkerGlContext = GLFW.glfwCreateWindow(64, 48, "LOD proxy worker Window", 0L, minecraftGlContext);
		GLFW.glfwMakeContextCurrent(proxyWorkerGlContext);
		proxyWorkerGlCapabilities = GL.createCapabilities();
		
		
		
		
		
		
		//==================================//
		// get any GPU related capabilities //
		//==================================//
		
		setGlContext(GLProxyContext.LOD_BUILDER);
		
		ClientApi.LOGGER.info("Lod Render OpenGL version [" + GL11.glGetString(GL11.GL_VERSION) + "].");
		
		// crash the game if the GPU doesn't support OpenGL 2.0
		if (!minecraftGlCapabilities.OpenGL20)
		{
			// Note: as of MC 1.17 this shouldn't happen since MC
			// requires OpenGL 3.3, but just in case.
			String errorMessage = ModInfo.READABLE_NAME + " was initializing " + GLProxy.class.getSimpleName() + " and discoverd this GPU doesn't support OpenGL 2.0 or greater.";
			MC.crashMinecraft(errorMessage + " Sorry I couldn't tell you sooner :(", new UnsupportedOperationException("This GPU doesn't support OpenGL 2.0 or greater."));
		}
		
		
		
		// get specific capabilities
		bufferStorageSupported = lodBuilderGlCapabilities.glBufferStorage != 0;
		mapBufferRangeSupported = lodBuilderGlCapabilities.glMapBufferRange != 0;
		
		// display the capabilities
		if (!bufferStorageSupported)
		{
			String fallBackVersion = mapBufferRangeSupported ? "3.0" : "1.5";  
			ClientApi.LOGGER.error("This GPU doesn't support Buffer Storage (OpenGL 4.5), falling back to OpenGL " + fallBackVersion + ". This may cause stuttering and reduced performance.");			
		}
		
		
		// if using AUTO gpuUpload
		// determine a good default for the GPU
		if (CONFIG.client().advanced().buffers().getGpuUploadMethod() == GpuUploadMethod.AUTO)
		{
			GpuUploadMethod uploadMethod;
			String vendor = GL15.glGetString(GL15.GL_VENDOR).toUpperCase(); // example return: "NVIDIA CORPORATION"
			if (vendor.contains("NVIDIA") || vendor.contains("GEFORCE"))
			{
				// NVIDIA card
				
				if (bufferStorageSupported)
				{
					uploadMethod = GpuUploadMethod.BUFFER_STORAGE;
				}
				else
				{
					uploadMethod = GpuUploadMethod.SUB_DATA;
				}
			}
			else
			{
				// AMD or Intel card
				
				if (mapBufferRangeSupported)
				{
					uploadMethod = GpuUploadMethod.BUFFER_MAPPING;
				}
				else
				{
					uploadMethod = GpuUploadMethod.DATA;
				}
			}
			
			CONFIG.client().advanced().buffers().setGpuUploadMethod(uploadMethod);
			ClientApi.LOGGER.info("GPU Vendor [" + vendor + "], Upload method set to [" + uploadMethod + "].");
		}
		
		
		
		
		//==============//
		// shader setup //
		//==============//
		
		setGlContext(GLProxyContext.MINECRAFT);
		
		createShaderProgram();
        
        // Note: VAO objects can not be shared between contexts,
        // this must be created on minecraft's render context to work correctly
        vertexArrayObjectId = GL30.glGenVertexArrays();
        
        
        
        
		
		//==========//
		// clean up //
		//==========//
		
		// Since this is created on the render thread, make sure the Minecraft context is used in the end
        setGlContext(GLProxyContext.MINECRAFT);
		
		
		// GLProxy creation success
		ClientApi.LOGGER.error(GLProxy.class.getSimpleName() + " creation successful. OpenGL smiles upon you this day.");
	}
	
	/** Creates all required shaders */
	public void createShaderProgram()
	{
		LodShader vertexShader = null;
		LodShader fragmentShader = null;
		
		try
		{
			// get the shaders from the resource folder
			vertexShader = LodShader.loadShader(GL20.GL_VERTEX_SHADER, "shaders" + File.separator + "standard.vert", false);
			fragmentShader = LodShader.loadShader(GL20.GL_FRAGMENT_SHADER, "shaders" + File.separator + "flat_shaded.frag", false);
			
			// this can be used when testing shaders, 
			// since we can't hot swap the files in the resource folder 
//			vertexShader = LodShader.loadShader(GL20.GL_VERTEX_SHADER, "C:/Users/James Seibel/Desktop/shaders/standard.vert", true);
//			fragmentShader = LodShader.loadShader(GL20.GL_FRAGMENT_SHADER, "C:/Users/James Seibel/Desktop/shaders/flat_shaded.frag", true);
			
			
			// create the shaders
			
			lodShaderProgram = new LodShaderProgram();
		    
		    // Attach the compiled shaders to the program
		    lodShaderProgram.attachShader(vertexShader);
		    lodShaderProgram.attachShader(fragmentShader);
		    
		    // activate the fragment shader output
		    GL30.glBindFragDataLocation(lodShaderProgram.id, 0, "fragColor");
		    
		    // attach the shader program to the OpenGL context
		    lodShaderProgram.link();
		    
		    // after the shaders have been attached to the program
		    // we don't need their OpenGL references anymore
		    GL20.glDeleteShader(vertexShader.id);
		    GL20.glDeleteShader(fragmentShader.id);
		}
		catch (Exception e)
		{
			ClientApi.LOGGER.error("Unable to compile shaders. Error: " + e.getMessage());
		}
	}
	
	
	/**
	 * A wrapper function to make switching contexts easier. <br>
	 * Does nothing if the calling thread is already using newContext.
	 */
	public void setGlContext(GLProxyContext newContext)
	{
		GLProxyContext currentContext = getGlContext();
		
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
		
		case MINECRAFT:
			contextPointer = minecraftGlContext;
			newGlCapabilities = minecraftGlCapabilities;
			break;
		
		case PROXY_WORKER:
			contextPointer = proxyWorkerGlContext;
			newGlCapabilities = proxyWorkerGlCapabilities;
			break;
			
		default: // default should never happen, it is just here to make the compiler happy
		case NONE:
			// 0L is equivalent to null
			contextPointer = 0L;
			break;
		}
		
		GLFW.glfwMakeContextCurrent(contextPointer);
		GL.setCapabilities(newGlCapabilities);
	}
	
	/** Returns this thread's OpenGL context. */
	public GLProxyContext getGlContext()
	{
		long currentContext = GLFW.glfwGetCurrentContext();
		
		
		if (currentContext == lodBuilderGlContext)
			return GLProxyContext.LOD_BUILDER;
		else if (currentContext == minecraftGlContext)
			return GLProxyContext.MINECRAFT;
		else if (currentContext == proxyWorkerGlContext)
			return GLProxyContext.PROXY_WORKER;
		else if (currentContext == 0L)
			return GLProxyContext.NONE;
		else
			// hopefully this shouldn't happen
			throw new IllegalStateException(Thread.currentThread().getName() + 
					" has a unknown OpenGl context: [" + currentContext + "]. "
					+ "Minecraft context [" + minecraftGlContext + "], "
					+ "LodBuilder context [" + lodBuilderGlContext + "], "
					+ "ProxyWorker context [" + proxyWorkerGlContext + "], "
					+ "no context [0].");
	}
	
	
	public static GLProxy getInstance()
	{
		if (instance == null)
			instance = new GLProxy();
		
		return instance;
	}
	
	
	
	
	
	

	
	/** 
	 * Asynchronously calls the given runnable on proxy's OpenGL context.
	 * Useful for creating/destroying OpenGL objects in a thread
	 * that doesn't normally have access to a OpenGL context. <br>
	 * No rendering can be done through this method.
	 */
	public void recordOpenGlCall(Runnable renderCall)
	{
		workerThread.execute(new Thread(() -> { runnableContainer(renderCall); }));
	}
	private void runnableContainer(Runnable renderCall)
	{
		try
		{
			// set up the context...
			setGlContext(GLProxyContext.PROXY_WORKER);
			// ...run the actual code...
			renderCall.run();
		}
		catch (Exception e)
		{
			ClientApi.LOGGER.error(Thread.currentThread().getName() + " ran into a issue: " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			// ...and make sure the context is released when the thread finishes
			setGlContext(GLProxyContext.NONE);	
		}
	}
	
	/** 
	 * If called from a legacy OpenGL context this will
	 * set the fog end to infinity with a density of 0. 
	 * Effectively removing the fog.
	 * <p>
	 * This only works with Legacy OpenGL because James hasn't
	 * looking into a way for it to work with Modern OpenGL.
	 */
	public void disableLegacyFog()
	{
		// make sure this is a legacy OpenGL context 
		if (minecraftGlCapabilities.glFogf != 0)
		{
			// glFogf should only have an address if the current OpenGL
			// context can call it, and it should only be able to call it in
			// legacy OpenGL contexts; since it is disabled in Modern
			// OpenGL.
			
			GL11.glFogf(GL11.GL_FOG_START, 0.0f);
			GL11.glFogf(GL11.GL_FOG_END, Float.MAX_VALUE);
			GL11.glFogf(GL11.GL_FOG_DENSITY, 0.0f);
		}
	}
	
	
}
