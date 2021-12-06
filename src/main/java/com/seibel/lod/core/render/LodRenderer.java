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

import java.awt.Color;
import java.util.HashSet;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import com.seibel.lod.core.api.ApiShared;
import com.seibel.lod.core.builders.bufferBuilding.LodBufferBuilderFactory;
import com.seibel.lod.core.builders.bufferBuilding.LodBufferBuilderFactory.VertexBuffersAndOffset;
import com.seibel.lod.core.enums.config.GpuUploadMethod;
import com.seibel.lod.core.enums.rendering.DebugMode;
import com.seibel.lod.core.enums.rendering.FogColorMode;
import com.seibel.lod.core.enums.rendering.FogDistance;
import com.seibel.lod.core.enums.rendering.FogDrawMode;
import com.seibel.lod.core.handlers.IReflectionHandler;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.objects.lod.RegionPos;
import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.objects.math.Vec3d;
import com.seibel.lod.core.objects.math.Vec3f;
import com.seibel.lod.core.objects.opengl.LodVertexBuffer;
import com.seibel.lod.core.objects.rending.LodFogConfig;
import com.seibel.lod.core.render.shader.LodShaderProgram;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LevelPosUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IProfilerWrapper;

/**
 * This is where all the magic happens. <br>
 * This is where LODs are draw to the world.
 * 
 * @author James Seibel
 * @version 11-27-2021
 */
public class LodRenderer
{
	private static final IMinecraftWrapper MC = SingletonHandler.get(IMinecraftWrapper.class);
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonHandler.get(IMinecraftRenderWrapper.class);
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	private static final IReflectionHandler REFLECTION_HANDLER = SingletonHandler.get(IReflectionHandler.class);
	private static final IWrapperFactory FACTORY = SingletonHandler.get(IWrapperFactory.class);
	
	
	/**
	 * If true the LODs colors will be replaced with
	 * a checkerboard, this can be used for debugging.
	 */
	public DebugMode previousDebugMode = DebugMode.OFF;
	
	private int farPlaneBlockDistance;
	
	
	/** This is used to generate the buildable buffers */
	private final LodBufferBuilderFactory lodBufferBuilderFactory;
	
	/** Each VertexBuffer represents 1 region */
	private LodVertexBuffer[][][] vbos;
	/**
	 * the OpenGL IDs for the vbos of the same indices.
	 * These have to be separate because we can't override the
	 * buffers in the VBOs (and we don't want to)
	 */
	@SuppressWarnings("unused")
	private int[][][] storageBufferIds;
	
	private AbstractChunkPosWrapper vbosCenter = FACTORY.createChunkPos();
	
	
	/** This is used to determine if the LODs should be regenerated */
	private int[] previousPos = new int[] { 0, 0, 0 };
	
	// these variables are used to determine if the buffers should be rebuilt
	private float prevSkyBrightness = 0;
	private double prevBrightness = 0;
	private int prevRenderDistance = 0;
	private long prevPlayerPosTime = 0;
	private long prevVanillaChunkTime = 0;
	private long prevChunkTime = 0;
	
	
	/** This is used to determine if the LODs should be regenerated */
	private FogDistance prevFogDistance = FogDistance.NEAR_AND_FAR;
	
	/**
	 * if this is true the LOD buffers should be regenerated,
	 * provided they aren't already being regenerated.
	 */
	private volatile boolean partialRegen = false;
	private volatile boolean fullRegen = true;
	
	/**
	 * This HashSet contains every chunk that Vanilla Minecraft
	 * is going to render
	 */
	public boolean[][] vanillaRenderedChunks;
	public boolean vanillaRenderedChunksChanged;
	public boolean vanillaRenderedChunksEmptySkip = false;
	public int vanillaBlockRenderedDistance;
	
	
	
	
	public LodRenderer(LodBufferBuilderFactory newLodNodeBufferBuilder)
	{
		lodBufferBuilderFactory = newLodNodeBufferBuilder;
	}
	
	
	
	
	
	
	
	/**
	 * Besides drawing the LODs this method also starts
	 * the async process of generating the Buffers that hold those LODs.
	 * @param lodDim The dimension to draw, if null doesn't replace the current dimension.
	 * @param mcModelViewMatrix This matrix stack should come straight from MC's renderChunkLayer (or future equivalent) method
	 * @param mcProjectionMatrix 
	 * @param partialTicks how far into the current tick this method was called.
	 */
	public void drawLODs(LodDimension lodDim, Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks, IProfilerWrapper profiler)
	{
		//=================================//
		// determine if LODs should render //
		//=================================//
		
		if (lodDim == null)
		{
			// if there aren't any loaded LodChunks
			// don't try drawing anything
			return;
		}
		
		if (MC_RENDER.playerHasBlindnessEffect())
		{
			// if the player is blind, don't render LODs,
			// and don't change minecraft's fog
			// which blindness relies on.
			return;
		}
		
		if (CONFIG.client().graphics().fogQuality().getDisableVanillaFog())
			GLProxy.getInstance().disableLegacyFog();
		
		
		
		
		// TODO move the buffer regeneration logic into its own class (probably called in the client api instead)
		// starting here...
		determineIfLodsShouldRegenerate(lodDim, partialTicks);
		
		//=================//
		// create the LODs //
		//=================//
		
		// only regenerate the LODs if:
		// 1. we want to regenerate LODs
		// 2. we aren't already regenerating the LODs
		// 3. we aren't waiting for the build and draw buffers to swap
		//		(this is to prevent thread conflicts)
		if ((partialRegen || fullRegen) && !lodBufferBuilderFactory.generatingBuffers && !lodBufferBuilderFactory.newBuffersAvailable())
		{
			// generate the LODs on a separate thread to prevent stuttering or freezing
			lodBufferBuilderFactory.generateLodBuffersAsync(this, lodDim, MC.getPlayerBlockPos(), true);
			
			// the regen process has been started,
			// it will be done when lodBufferBuilder.newBuffersAvailable()
			// is true
			fullRegen = false;
			partialRegen = false;
		}
		
		// TODO move the buffer regeneration logic into its own class (probably called in the client api instead)
		// ...ending here
		
		if (lodBufferBuilderFactory.newBuffersAvailable())
		{
			swapBuffers();
		}
		
		
		
		
		
		//===============//
		// initial setup //
		//===============//
		
		profiler.push("LOD setup");
		
		GLProxy glProxy = GLProxy.getInstance();
		
		
		
		// set the required open GL settings
		
		if (CONFIG.client().advanced().debugging().getDebugMode() == DebugMode.SHOW_DETAIL_WIREFRAME)
			GL15.glPolygonMode(GL15.GL_FRONT_AND_BACK, GL15.GL_LINE);
		else
			GL15.glPolygonMode(GL15.GL_FRONT_AND_BACK, GL15.GL_FILL);
		
		GL15.glEnable(GL15.GL_CULL_FACE);
		GL15.glEnable(GL15.GL_DEPTH_TEST);
		
		// enable transparent rendering
		GL15.glBlendFunc(GL15.GL_SRC_ALPHA, GL15.GL_ONE_MINUS_SRC_ALPHA);
		GL15.glEnable(GL15.GL_BLEND);
		
		// get MC's shader program
		int currentProgram = GL20.glGetInteger(GL20.GL_CURRENT_PROGRAM);
		
		
		Mat4f modelViewMatrix = translateModelViewMatrix(mcModelViewMatrix, partialTicks);
		vanillaBlockRenderedDistance = MC_RENDER.getRenderDistance() * LodUtil.CHUNK_WIDTH;
		// required for setupFog and setupProjectionMatrix
		if (MC.getWrappedClientWorld().getDimensionType().hasCeiling())
			farPlaneBlockDistance = Math.min(CONFIG.client().graphics().quality().getLodChunkRenderDistance(), LodUtil.CEILED_DIMENSION_MAX_RENDER_DISTANCE) * LodUtil.CHUNK_WIDTH;
		else
			farPlaneBlockDistance = CONFIG.client().graphics().quality().getLodChunkRenderDistance() * LodUtil.CHUNK_WIDTH;
		
		
		Mat4f projectionMatrix = createProjectionMatrix(mcProjectionMatrix, vanillaBlockRenderedDistance);
		
		LodFogConfig fogSettings = determineFogConfig();
		
		
		
		
		
		
		if (vbos != null)
		{
			//==============//
			// shader setup //
			//==============//
			
			// can be used when testing shaders
			// glProxy.createShaderProgram();
			
			
			LodShaderProgram shaderProgram = glProxy.lodShaderProgram;
			shaderProgram.use();
			
			
			// determine the VertexArrayObject's element positions
	        int posAttrib = shaderProgram.getAttributeLocation("vPosition");
	        shaderProgram.enableVertexAttribute(posAttrib);
	        int colAttrib = shaderProgram.getAttributeLocation("color");
	        shaderProgram.enableVertexAttribute(colAttrib);
	        
	        
	        // global uniforms
	        int mvmUniform = shaderProgram.getUniformLocation("modelViewMatrix");
	        shaderProgram.setUniform(mvmUniform, modelViewMatrix);
			int projUniform = shaderProgram.getUniformLocation("projectionMatrix");
			shaderProgram.setUniform(projUniform, projectionMatrix);
			int cameraUniform = shaderProgram.getUniformLocation("cameraPos");
			shaderProgram.setUniform(cameraUniform, getTranslatedCameraPos());
			int fogColorUniform = shaderProgram.getUniformLocation("fogColor");
			shaderProgram.setUniform(fogColorUniform, getFogColor());
			
			
			// region dependent uniforms
			int fogEnabledUniform = shaderProgram.getUniformLocation("fogEnabled");
			int nearFogEnabledUniform = shaderProgram.getUniformLocation("nearFogEnabled");
			int farFogEnabledUniform = shaderProgram.getUniformLocation("farFogEnabled");
			// near
			int nearFogStartUniform = shaderProgram.getUniformLocation("nearFogStart");
			int nearFogEndUniform = shaderProgram.getUniformLocation("nearFogEnd");
			// far
			int farFogStartUniform = shaderProgram.getUniformLocation("farFogStart");
			int farFogEndUniform = shaderProgram.getUniformLocation("farFogEnd");
			
			
			
			
			
			//===========//
			// rendering //
			//===========//
			
			profiler.popPush("LOD draw");
			
			boolean cullingDisabled = CONFIG.client().graphics().advancedGraphics().getDisableDirectionalCulling();
			boolean renderBufferStorage = CONFIG.client().advanced().buffers().getGpuUploadMethod() == GpuUploadMethod.BUFFER_STORAGE && glProxy.bufferStorageSupported;
			
			// where the center of the buffers is (needed when culling regions)
			RegionPos vboCenterRegionPos = new RegionPos(vbosCenter);
			RegionPos vboPos = new RegionPos();
			
			
			// render each of the buffers
			for (int x = 0; x < vbos.length; x++)
			{
				for (int z = 0; z < vbos.length; z++)
				{
					vboPos.x = x + vboCenterRegionPos.x - (lodDim.getWidth() / 2);
					vboPos.z = z + vboCenterRegionPos.z - (lodDim.getWidth() / 2);
					
					if (cullingDisabled || RenderUtil.isRegionInViewFrustum(MC_RENDER.getCameraBlockPosition(), MC_RENDER.getLookAtVector(), vboPos.blockPos()))
					{
						// fog may be different from region to region
						applyFog(shaderProgram, 
								fogSettings, fogEnabledUniform, nearFogEnabledUniform, farFogEnabledUniform, 
								nearFogStartUniform, nearFogEndUniform, farFogStartUniform, farFogEndUniform);
						
						
						// actual rendering
						int bufferId = 0;
						for (int i = 0; i < vbos[x][z].length; i++)
						{
							bufferId = (storageBufferIds != null && renderBufferStorage) ? storageBufferIds[x][z][i] : vbos[x][z][i].id;
							drawArrays(bufferId, vbos[x][z][i].vertexCount, posAttrib, colAttrib);
						}
						
					}
				}
			}
			
			
			
			//================//
			// render cleanup //
			//================//
			
			// if this cleanup isn't done MC may crash
			// when trying to render its own terrain
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
			GL30.glBindVertexArray(0);
			
			GL20.glDisableVertexAttribArray(posAttrib);
			GL20.glDisableVertexAttribArray(colAttrib);
		}
		
		
		
		
		
		//=========//
		// cleanup //
		//=========//
		
		profiler.popPush("LOD cleanup");
		
		GL15.glPolygonMode(GL15.GL_FRONT_AND_BACK, GL15.GL_FILL);
		GL15.glDisable(GL15.GL_BLEND); // TODO: what should this be reset to?
		
		GL20.glUseProgram(currentProgram);
		
		// clear the depth buffer so everything is drawn
		// over the LODs
		GL15.glClear(GL15.GL_DEPTH_BUFFER_BIT);
		
		
		 
		// end of internal LOD profiling
		profiler.pop();
	}






	
	
	/** This is where the actual drawing happens. */
	private void drawArrays(int glBufferId, int vertexCount, int posAttrib, int colAttrib)
	{
		if (glBufferId == 0)
			return;
		
		// can be used to check for OpenGL errors
//		int error = GL15.glGetError();
//		ClientProxy.LOGGER.info(Integer.toHexString(error));
		
		
        // bind the buffer we are going to draw
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glBufferId);
		GL30.glBindVertexArray(GLProxy.getInstance().vertexArrayObjectId);
        
		// let OpenGL know how our buffer is set up
		int vertexByteCount = (Float.BYTES * 3) + (Byte.BYTES * 4);
        GL20.glEnableVertexAttribArray(posAttrib);
        GL20.glVertexAttribPointer(posAttrib, 3, GL15.GL_FLOAT, false, vertexByteCount, 0);
        GL20.glEnableVertexAttribArray(colAttrib);
        GL20.glVertexAttribPointer(colAttrib, 4, GL15.GL_UNSIGNED_BYTE, true, vertexByteCount, Float.BYTES * 3);
        
        
        // draw the LODs
		GL30.glDrawArrays(GL30.GL_TRIANGLES, 0, vertexCount);
	}
	
	
	
	
	
	
	//=================//
	// Setup Functions //
	//=================//
	

	/** Create all buffers that will be used. */
	public void setupBuffers(LodDimension lodDim)
	{
		lodBufferBuilderFactory.setupBuffers(lodDim);
	}
	
	
	
	
	/** Return what fog settings should be used when rendering. */
	private LodFogConfig determineFogConfig()
	{
		LodFogConfig fogConfig = new LodFogConfig();
		
		
		fogConfig.fogDrawMode = CONFIG.client().graphics().fogQuality().getFogDrawMode();
		if (fogConfig.fogDrawMode == FogDrawMode.USE_OPTIFINE_SETTING)
			fogConfig.fogDrawMode = REFLECTION_HANDLER.getFogDrawMode();
		
		
		// how different distances are drawn depends on the quality set
		fogConfig.fogDistance = CONFIG.client().graphics().fogQuality().getFogDistance();
		
		
		
		
		
		// far fog //
		
		if (CONFIG.client().graphics().fogQuality().getFogDistance() == FogDistance.NEAR_AND_FAR)
			fogConfig.farFogStart = farPlaneBlockDistance * 1.6f * 0.9f;
		else
			// for more realistic fog when using FAR
			fogConfig.farFogStart = Math.min(vanillaBlockRenderedDistance * 1.5f, farPlaneBlockDistance * 0.9f * 1.6f);
		
		fogConfig.farFogEnd = farPlaneBlockDistance * 1.6f;
	
		
		// near fog //
		
		// the reason that I wrote fogEnd then fogStart backwards
		// is because we are using fog backwards to how
		// it is normally used, hiding near objects
		// instead of far objects.
		fogConfig.nearFogEnd = vanillaBlockRenderedDistance * 1.41f;
		fogConfig.nearFogStart = vanillaBlockRenderedDistance * 1.6f;
		
		
		return fogConfig;
	}
	
	private Color getFogColor()
	{
		Color fogColor;
		
		if (CONFIG.client().graphics().fogQuality().getFogColorMode() == FogColorMode.USE_SKY_COLOR)
			fogColor = MC_RENDER.getSkyColor();
		else
			fogColor = MC_RENDER.getFogColor();
		
		return fogColor;
	}
	
	/**
	 * Translate the camera relative to the LodDimension's center,
	 * this is done since all LOD buffers are created in world space
	 * instead of object space.
	 * (since AxisAlignedBoundingBoxes (LODs) use doubles and thus have a higher
	 * accuracy vs the model view matrix, which only uses floats)
	 */
	private Mat4f translateModelViewMatrix(Mat4f mcModelViewMatrix, float partialTicks)
	{
		// get all relevant camera info
		Vec3d projectedView = MC_RENDER.getCameraExactPosition();
		
		// translate the camera relative to the regions' center
		// (AxisAlignedBoundingBoxes (LODs) use doubles and thus have a higher
		// accuracy vs the model view matrix, which only uses floats)
		AbstractBlockPosWrapper bufferPos = vbosCenter.getWorldPosition();
		double xDiff = projectedView.x - bufferPos.getX();
		double zDiff = projectedView.z - bufferPos.getZ();
		mcModelViewMatrix.multiplyTranslationMatrix(-xDiff, -projectedView.y, -zDiff);
		
		return mcModelViewMatrix;
	}
	
	/** 
	 * Similar to translateModelViewMatrix (above),
	 * but for the camera position
	 */
	private Vec3f getTranslatedCameraPos()
	{
		AbstractBlockPosWrapper worldCenter = vbosCenter.getWorldPosition();
		Vec3d cameraPos = MC_RENDER.getCameraExactPosition();
		return new Vec3f((float)cameraPos.x - worldCenter.getX(), (float)cameraPos.y, (float)cameraPos.z - worldCenter.getZ());
	}

	/**
	 * create and return a new projection matrix based on MC's projection matrix
	 * @param currentProjectionMatrix this is Minecraft's current projection matrix
	 * @param vanillaBlockRenderedDistance Minecraft's vanilla far plane distance
	 */
	private Mat4f createProjectionMatrix(Mat4f currentProjectionMatrix, float vanillaBlockRenderedDistance)
	{
		//Create a copy of the current matrix, so the current matrix isn't modified.
		Mat4f lodProj = currentProjectionMatrix.copy();

		//Set new far and near clip plane values.
		lodProj.setClipPlanes(
				CONFIG.client().graphics().advancedGraphics().getUseExtendedNearClipPlane() ? vanillaBlockRenderedDistance / 5 : 1,
				farPlaneBlockDistance * LodUtil.CHUNK_WIDTH / 2);

		return lodProj;
	}
	
	private void applyFog(LodShaderProgram shaderProgram, 
			LodFogConfig fogSettings, int fogEnabledUniform, int nearFogEnabledUniform, int farFogEnabledUniform, 
			int nearFogStartUniform, int nearFogEndUniform, int farFogStartUniform, int farFogEndUniform)
	{
		if (fogSettings.fogDrawMode != FogDrawMode.FOG_DISABLED)
		{
			shaderProgram.setUniform(fogEnabledUniform, true);
			shaderProgram.setUniform(nearFogEnabledUniform, fogSettings.fogDistance != FogDistance.FAR);
			shaderProgram.setUniform(farFogEnabledUniform, fogSettings.fogDistance != FogDistance.NEAR);
			
			// near
			shaderProgram.setUniform(nearFogStartUniform, fogSettings.nearFogStart);
			shaderProgram.setUniform(nearFogEndUniform, fogSettings.nearFogEnd);
			// far
			shaderProgram.setUniform(farFogStartUniform, fogSettings.farFogStart);
			shaderProgram.setUniform(farFogEndUniform, fogSettings.farFogEnd);
		}
		else
		{
			shaderProgram.setUniform(fogEnabledUniform, false);
		}
	}
	
	
	
	
	
	//======================//
	// Other Misc Functions //
	//======================//
	
	
	/**
	 * If this is called then the next time "drawLODs" is called
	 * the LODs will be regenerated; the same as if the player moved.
	 */
	public void regenerateLODsNextFrame()
	{
		fullRegen = true;
	}
	
	/**
	 * Replace the current Vertex Buffers with the newly
	 * created buffers from the lodBufferBuilder. <br><br>
	 * <p>
	 * For some reason this has to be called after the frame has been rendered,
	 * otherwise visual stuttering/rubber banding may happen. I'm not sure why...
	 */
	private void swapBuffers()
	{
		// replace the drawable buffers with
		// the newly created buffers from the lodBufferBuilder
		VertexBuffersAndOffset result = lodBufferBuilderFactory.getVertexBuffers();
		vbos = result.vbos;
		storageBufferIds = result.storageBufferIds;
		vbosCenter = result.drawableCenterChunkPos;
	}
	
	/** Calls the BufferBuilder's destroyBuffers method. */
	public void destroyBuffers()
	{
		lodBufferBuilderFactory.destroyBuffers();
	}
	
	
	
	/** Determines if the LODs should have a fullRegen or partialRegen */
	private void determineIfLodsShouldRegenerate(LodDimension lodDim, float partialTicks)
	{
		short chunkRenderDistance = (short) MC_RENDER.getRenderDistance();
		int vanillaRenderedChunksWidth = chunkRenderDistance * 2 + 2;
		
		//=============//
		// full regens //
		//=============//
		
		// check if the view distance changed
		if (ApiShared.previousLodRenderDistance != CONFIG.client().graphics().quality().getLodChunkRenderDistance()
					|| chunkRenderDistance != prevRenderDistance
					|| prevFogDistance != CONFIG.client().graphics().fogQuality().getFogDistance())
		{
			
			vanillaRenderedChunks = new boolean[vanillaRenderedChunksWidth][vanillaRenderedChunksWidth];
			DetailDistanceUtil.updateSettings();
			fullRegen = true;
			previousPos = LevelPosUtil.createLevelPos((byte) 4, MC.getPlayerChunkPos().getZ(), MC.getPlayerChunkPos().getZ());
			prevFogDistance = CONFIG.client().graphics().fogQuality().getFogDistance();
			prevRenderDistance = chunkRenderDistance;
		}
		
		// did the user change the debug setting?
		if (CONFIG.client().advanced().debugging().getDebugMode() != previousDebugMode)
		{
			previousDebugMode = CONFIG.client().advanced().debugging().getDebugMode();
			fullRegen = true;
		}
		
		
		long newTime = System.currentTimeMillis();
		
		// check if the player has moved
		if (newTime - prevPlayerPosTime > CONFIG.client().advanced().buffers().getRebuildTimes().playerMoveTimeout)
		{
			if (LevelPosUtil.getDetailLevel(previousPos) == 0
						|| Math.abs(MC.getPlayerChunkPos().getX() - LevelPosUtil.getPosX(previousPos)) > CONFIG.client().advanced().buffers().getRebuildTimes().playerMoveDistance
						|| Math.abs(MC.getPlayerChunkPos().getZ() - LevelPosUtil.getPosZ(previousPos)) > CONFIG.client().advanced().buffers().getRebuildTimes().playerMoveDistance)
			{
				vanillaRenderedChunks = new boolean[vanillaRenderedChunksWidth][vanillaRenderedChunksWidth];
				fullRegen = true;
				previousPos = LevelPosUtil.createLevelPos((byte) 4, MC.getPlayerChunkPos().getX(), MC.getPlayerChunkPos().getZ());
			}
			prevPlayerPosTime = newTime;
		}
		
		
		
		// determine how far the lighting has to 
		// change in order to rebuild the buffers
		
		// the max brightness is 1 and the minimum is 0.2
		float skyBrightness = lodDim.dimension.hasSkyLight() ? MC.getSkyDarken(partialTicks) : 0.2f;
		float minLightingDifference;
		switch (CONFIG.client().advanced().buffers().getRebuildTimes())
		{
		case FREQUENT:
			minLightingDifference = 0.025f;
			break;
		case NORMAL:
			minLightingDifference = 0.05f;
			break;
		default:
		case RARE:
			minLightingDifference = 0.1f;
			break;
		}
		
		// check if the lighting changed
		if (Math.abs(skyBrightness - prevSkyBrightness) > minLightingDifference
					// make sure the lighting gets to the max/minimum value
					// (just in case the minLightingDifference is too large to notice the change)
					|| (skyBrightness == 1.0f && prevSkyBrightness != 1.0f) // noon
					|| (skyBrightness == 0.2f && prevSkyBrightness != 0.2f) // midnight
					|| MC_RENDER.getGamma() != prevBrightness)
		{
			fullRegen = true;
			prevBrightness = MC_RENDER.getGamma();
			prevSkyBrightness = skyBrightness;
		}
		
		/*if (lightMap != lastLightMap)
		{
			fullRegen = true;
			lastLightMap = lightMap;
		}*/
		
		//================//
		// partial regens //
		//================//
		
		
		// check if the vanilla rendered chunks changed
		if (newTime - prevVanillaChunkTime > CONFIG.client().advanced().buffers().getRebuildTimes().renderedChunkTimeout)
		{
			if (vanillaRenderedChunksChanged)
			{
				partialRegen = true;
				vanillaRenderedChunksChanged = false;
			}
			prevVanillaChunkTime = newTime;
		}
		
		
		// check if there is any newly generated terrain to show
		if (newTime - prevChunkTime > CONFIG.client().advanced().buffers().getRebuildTimes().chunkChangeTimeout)
		{
			if (lodDim.regenDimensionBuffers)
			{
				partialRegen = true;
				lodDim.regenDimensionBuffers = false;
			}
			prevChunkTime = newTime;
		}
		
		
		
		//==============//
		// LOD skipping //
		//==============//
		
		// determine which LODs should not be rendered close to the player
		HashSet<AbstractChunkPosWrapper> chunkPosToSkip = LodUtil.getNearbyLodChunkPosToSkip(lodDim, MC.getPlayerBlockPos());
		int xIndex;
		int zIndex;
		for (AbstractChunkPosWrapper pos : chunkPosToSkip)
		{
			vanillaRenderedChunksEmptySkip = false;
			
			xIndex = (pos.getX() - MC.getPlayerChunkPos().getX()) + (chunkRenderDistance + 1);
			zIndex = (pos.getZ() - MC.getPlayerChunkPos().getZ()) + (chunkRenderDistance + 1);
			
			// sometimes we are given chunks that are outside the render distance,
			// This prevents index out of bounds exceptions
			if (xIndex >= 0 && zIndex >= 0
						&& xIndex < vanillaRenderedChunks.length
						&& zIndex < vanillaRenderedChunks.length)
			{
				if (!vanillaRenderedChunks[xIndex][zIndex])
				{
					vanillaRenderedChunks[xIndex][zIndex] = true;
					vanillaRenderedChunksChanged = true;
					lodDim.markRegionBufferToRegen(pos.getRegionX(), pos.getRegionZ());
				}
			}
		}
		
		
		// if the player is high enough, draw all LODs
		if (chunkPosToSkip.isEmpty() && MC.getPlayerBlockPos().getY() > 256 && !vanillaRenderedChunksEmptySkip)
		{
			vanillaRenderedChunks = new boolean[vanillaRenderedChunksWidth][vanillaRenderedChunksWidth];
			vanillaRenderedChunksChanged = true;
			vanillaRenderedChunksEmptySkip = true;
		}
		
		vanillaRenderedChunks = new boolean[vanillaRenderedChunksWidth][vanillaRenderedChunksWidth];
	}
	
}
