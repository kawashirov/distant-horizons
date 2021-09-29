/*
 *    This file is part of the LOD Mod, licensed under the GNU GPL v3 License.
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
package com.seibel.lod.render;

import java.util.HashSet;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.NVFogDistance;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.seibel.lod.builders.LodBufferBuilder;
import com.seibel.lod.builders.LodBufferBuilder.VertexBuffersAndOffset;
import com.seibel.lod.config.LodConfig;
import com.seibel.lod.enums.DebugMode;
import com.seibel.lod.enums.DetailDropOff;
import com.seibel.lod.enums.FogDistance;
import com.seibel.lod.enums.FogDrawOverride;
import com.seibel.lod.enums.FogQuality;
import com.seibel.lod.handlers.ReflectionHandler;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.objects.NearFarFogSettings;
import com.seibel.lod.objects.RegionPos;
import com.seibel.lod.proxy.ClientProxy;
import com.seibel.lod.proxy.GlProxy;
import com.seibel.lod.util.DetailDistanceUtil;
import com.seibel.lod.util.LevelPosUtil;
import com.seibel.lod.util.LodUtil;
import com.seibel.lod.wrappers.MinecraftWrapper;

import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;


/**
 * This is where all the magic happens. <br>
 * This is where LODs are draw to the world.
 *
 * @author James Seibel
 * @version 9-23-2021
 */
public class LodRenderer
{
	/**
	 * this is the light used when rendering the LODs,
	 * it should be something different than what is used by Minecraft
	 */
	private static final int LOD_GL_LIGHT_NUMBER = GL11.GL_LIGHT2;
	
	/**
	 * If true the LODs colors will be replaced with
	 * a checkerboard, this can be used for debugging.
	 */
	public DebugMode previousDebugMode = DebugMode.OFF;

	private MinecraftWrapper mc;
	private GameRenderer gameRender;
	private IProfiler profiler;
	private int farPlaneBlockDistance;


	/**
	 * This is used to generate the buildable buffers
	 */
	private LodBufferBuilder lodBufferBuilder;

	/**
	 * Each VertexBuffer represents 1 region
	 */
	private VertexBuffer[][] vbos;
	public static final VertexFormat LOD_VERTEX_FORMAT = DefaultVertexFormats.POSITION_COLOR;
	private ChunkPos vbosCenter = new ChunkPos(0,0);

	/**
	 * This is used to determine if the LODs should be regenerated
	 */
	private int[] previousPos = new int[]{0,0,0};
	
	public NativeImage lightMap = null;
	
	// these variables are used to determine if the buffers should be rebuilt
	private long prevDayTime = 0;
	private double prevBrightness = 0;
	private int prevRenderDistance = 0;
	private long prevPlayerPosTime = 0;
	private long prevVanillaChunkTime = 0;
	private long prevChunkTime = 0;


	/**
	 * This is used to determine if the LODs should be regenerated
	 */
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


	public LodRenderer(LodBufferBuilder newLodNodeBufferBuilder)
	{
		mc = MinecraftWrapper.INSTANCE;
		gameRender = mc.getGameRenderer();

		lodBufferBuilder = newLodNodeBufferBuilder;
	}


	/**
	 * Besides drawing the LODs this method also starts
	 * the async process of generating the Buffers that hold those LODs.
	 *
	 * @param lodDim       The dimension to draw, if null doesn't replace the current dimension.
	 * @param mcMatrixStack This matrix stack should come straight from MC's renderChunkLayer (or future equivalent) method
	 * @param partialTicks how far into the current tick this method was called.
	 */
	public void drawLODs(LodDimension lodDim, MatrixStack mcMatrixStack, float partialTicks, IProfiler newProfiler)
	{
		if (lodDim == null)
		{
			// if there aren't any loaded LodChunks
			// don't try drawing anything
			return;
		}


		//===============//
		// initial setup //
		//===============//

		profiler = newProfiler;
		profiler.push("LOD setup");
		
		
		// TODO move the buffer regeneration logic into its own class (probably called in the client proxy instead)
		// starting here...
		determineIfLodsShouldRegenerate(lodDim);

		//=================//
		// create the LODs //
		//=================//

		// only regenerate the LODs if:
		// 1. we want to regenerate LODs
		// 2. we aren't already regenerating the LODs
		// 3. we aren't waiting for the build and draw buffers to swap
		//		(this is to prevent thread conflicts)
		if ((partialRegen || fullRegen) && !lodBufferBuilder.generatingBuffers && !lodBufferBuilder.newBuffersAvaliable())
		{
			// generate the LODs on a separate thread to prevent stuttering or freezing
			lodBufferBuilder.generateLodBuffersAsync(this, lodDim, mc.getPlayer().blockPosition(), true);

			// the regen process has been started,
			// it will be done when lodBufferBuilder.newBuffersAvaliable()
			// is true
			fullRegen = false;
			partialRegen = false;
		}

		// TODO move the buffer regeneration logic into its own class (probably called in the client proxy instead)
		// ...ending here
		
		if (lodBufferBuilder.newBuffersAvaliable())
		{
			swapBuffers();
		}
		


		//===========================//
		// GL settings for rendering //
		//===========================//

		// set the required open GL settings
		
		if (LodConfig.CLIENT.debugging.debugMode.get() == DebugMode.SHOW_DETAIL_WIREFRAME)
			GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
		else
			GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);

		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_COLOR_MATERIAL);
		GL11.glEnable(GL11.GL_DEPTH_TEST);

		// disable the lights Minecraft uses
		GL11.glDisable(GL11.GL_LIGHT0);
		GL11.glDisable(GL11.GL_LIGHT1);

		// get the default projection matrix so we can
		// reset it after drawing the LODs
		float[] mcProjMatrixRaw = new float[16];
		GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, mcProjMatrixRaw);
		Matrix4f mcProjectionMatrix = new Matrix4f(mcProjMatrixRaw);
		// OpenGl outputs their matricies in col,row form instead of row,col
		// (or maybe vice versa I have no idea :P)
		mcProjectionMatrix.transpose();
		
		Matrix4f modelViewMatrix = offsetTheModelViewMatrix(mcMatrixStack, partialTicks);

		// required for setupFog and setupProjectionMatrix
		farPlaneBlockDistance = LodConfig.CLIENT.graphics.lodChunkRenderDistance.get() * LodUtil.CHUNK_WIDTH;

		setupProjectionMatrix(mcProjectionMatrix, partialTicks);
		// commented out until we can add shaders to handle lighting
		//setupLighting(lodDim, partialTicks);

		NearFarFogSettings fogSettings = determineFogSettings();

		// determine the current fog settings so they can be
		// reset after drawing the LODs
		float defaultFogStartDist = GL11.glGetFloat(GL11.GL_FOG_START);
		float defaultFogEndDist = GL11.glGetFloat(GL11.GL_FOG_END);
		int defaultFogMode = GL11.glGetInteger(GL11.GL_FOG_MODE);
		int defaultFogDistance = GL11.glGetInteger(NVFogDistance.GL_FOG_DISTANCE_MODE_NV);


		//===========//
		// rendering //
		//===========//
		
		profiler.popPush("LOD draw");
		
		if (vbos != null)
		{
			ActiveRenderInfo renderInfo = mc.getGameRenderer().getMainCamera();
			Vector3d cameraDir = new Vector3d(renderInfo.getLookVector());
			
			boolean cullingDisabled = LodConfig.CLIENT.graphics.disableDirectionalCulling.get();
			
			// used to determine what type of fog to render
			int halfWidth = vbos.length / 2;
			int quarterWidth = vbos.length / 4;

			for (int i = 0; i < vbos.length; i++)
			{
				for (int j = 0; j < vbos.length; j++)
				{
					RegionPos vboPos = new RegionPos(i + lodDim.getCenterRegionPosX() - lodDim.getWidth() / 2, j + lodDim.getCenterRegionPosZ() - lodDim.getWidth() / 2);
					if (cullingDisabled || RenderUtil.isRegionInViewFrustum(renderInfo.getBlockPosition(), cameraDir, vboPos.blockPos()))
					{
						if ((i > halfWidth - quarterWidth && i < halfWidth + quarterWidth) && (j > halfWidth - quarterWidth && j < halfWidth + quarterWidth))
							setupFog(fogSettings.near.distance, fogSettings.near.quality);
						else
							setupFog(fogSettings.far.distance, fogSettings.far.quality);


						sendLodsToGpuAndDraw(vbos[i][j], modelViewMatrix);
					}
				}
			}
		}
		

		//=========//
		// cleanup //
		//=========//

		profiler.popPush("LOD cleanup");

		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(LOD_GL_LIGHT_NUMBER);
		// re-enable the lights Minecraft uses
		GL11.glEnable(GL11.GL_LIGHT0);
		GL11.glEnable(GL11.GL_LIGHT1);
		RenderSystem.disableLighting();

		// reset the fog settings so the normal chunks
		// will be drawn correctly
		cleanupFog(fogSettings, defaultFogStartDist, defaultFogEndDist, defaultFogMode, defaultFogDistance);

		// reset the projection matrix so anything drawn after
		// the LODs will use the correct projection matrix
		gameRender.resetProjectionMatrix(mcProjectionMatrix);
		
		// clear the depth buffer so anything drawn is drawn
		// over the LODs
		GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
		

		// end of internal LOD profiling
		profiler.pop();
	}


	/**
	 * This is where the actual drawing happens.
	 */
	private void sendLodsToGpuAndDraw(VertexBuffer vbo, Matrix4f modelViewMatrix)
	{
		if (vbo == null)
			return;
		
		GL15C.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo.id);
		// 0L is the starting pointer
		LOD_VERTEX_FORMAT.setupBufferState(0L);
		
		vbo.draw(modelViewMatrix, GL11.GL_QUADS);
		
		GL15C.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		LOD_VERTEX_FORMAT.clearBufferState();
	}


	//=================//
	// Setup Functions //
	//=================//

	@SuppressWarnings("deprecation")
	private void setupFog(FogDistance fogDistance, FogQuality fogQuality)
	{
		if (fogQuality == FogQuality.OFF)
		{
			FogRenderer.setupNoFog();
			RenderSystem.disableFog();
			return;
		}

		if (fogDistance == FogDistance.NEAR_AND_FAR)
		{
			throw new IllegalArgumentException("setupFog doesn't accept the NEAR_AND_FAR fog distance.");
		}

		// determine the fog distance mode to use
		int glFogDistanceMode;
		if (fogQuality == FogQuality.FANCY)
		{
			// fancy fog (fragment distance based fog)
			glFogDistanceMode = NVFogDistance.GL_EYE_RADIAL_NV;
		}
		else
		{
			// fast fog (frustum distance based fog)
			glFogDistanceMode = NVFogDistance.GL_EYE_PLANE_ABSOLUTE_NV;
		}

		// the multipliers are percentages
		// of the regular view distance.
		if (fogDistance == FogDistance.FAR)
		{
			// the reason that I wrote fogEnd then fogStart backwards
			// is because we are using fog backwards to how
			// it is normally used, with it hiding near objects
			// instead of far objects.

			if (fogQuality == FogQuality.FANCY)
			{
				// for more realistic fog when using FAR
				if(LodConfig.CLIENT.graphics.fogDistance.get() == FogDistance.NEAR_AND_FAR)
				{
					RenderSystem.fogStart(farPlaneBlockDistance * 0.9f);
					RenderSystem.fogEnd(farPlaneBlockDistance * 1.0f);
				}else{
					RenderSystem.fogStart(farPlaneBlockDistance * 0.1f);
					RenderSystem.fogEnd(farPlaneBlockDistance * 1.0f);
				}
			}
			else if (fogQuality == FogQuality.FAST)
			{
				// for the far fog of the normal chunks
				// to start right where the LODs' end use:
				// end = 0.8f, start = 1.5f
				RenderSystem.fogStart(farPlaneBlockDistance * 1.5f);
				RenderSystem.fogEnd(farPlaneBlockDistance * 2.0f);
			}
		}
		else if (fogDistance == FogDistance.NEAR)
		{
			if (fogQuality == FogQuality.FANCY)
			{
				RenderSystem.fogEnd(mc.getRenderDistance() * 16 * 1.41f);
				RenderSystem.fogStart(mc.getRenderDistance() * 16 * 1.6f);
			}
			else if (fogQuality == FogQuality.FAST)
			{
				RenderSystem.fogEnd(mc.getRenderDistance() * 16 * 1.0f);
				RenderSystem.fogStart(mc.getRenderDistance() * 16 * 1.5f);
			}
		}

		GL11.glEnable(GL11.GL_FOG);
		RenderSystem.enableFog();
		RenderSystem.setupNvFogDistance();
		RenderSystem.fogMode(GlStateManager.FogMode.LINEAR);
		GL11.glFogi(NVFogDistance.GL_FOG_DISTANCE_MODE_NV, glFogDistanceMode);
	}

	/**
	 * Revert any changes that were made to the fog.
	 */
	private void cleanupFog(NearFarFogSettings fogSettings,
	                        float defaultFogStartDist, float defaultFogEndDist,
	                        int defaultFogMode, int defaultFogDistance)
	{
		RenderSystem.fogStart(defaultFogStartDist);
		RenderSystem.fogEnd(defaultFogEndDist);
		RenderSystem.fogMode(defaultFogMode);
		GL11.glFogi(NVFogDistance.GL_FOG_DISTANCE_MODE_NV, defaultFogDistance);

		// disable fog if Minecraft wasn't rendering fog
		// but we were
		if (!fogSettings.vanillaIsRenderingFog &&
				    (fogSettings.near.quality != FogQuality.OFF ||
						     fogSettings.far.quality != FogQuality.OFF))
		{
			GL11.glDisable(GL11.GL_FOG);
		}
	}


	/**
	 * Translate the camera relative to the LodDimension's center,
	 * this is done since all LOD buffers are created in world space
	 * instead of object space.
	 * (since AxisAlignedBoundingBoxes (LODs) use doubles and thus have a higher
	 * accuracy vs the model view matrix, which only uses floats)
	 */
	private Matrix4f offsetTheModelViewMatrix(MatrixStack mcMatrixStack, float partialTicks)
	{
		// duplicate the last matrix
		mcMatrixStack.pushPose();
		
		
		// get all relevant camera info
		ActiveRenderInfo renderInfo = mc.getGameRenderer().getMainCamera();
		Vector3d projectedView = renderInfo.getPosition();
		
		// translate the camera relative to the regions' center
		// (AxisAlignedBoundingBoxes (LODs) use doubles and thus have a higher
		// accuracy vs the model view matrix, which only uses floats)
		BlockPos bufferPos = vbosCenter.getWorldPosition();
		Vector3d eyePos = mc.getPlayer().getEyePosition(partialTicks);
		double xDiff = eyePos.x - bufferPos.getX();
		double zDiff = eyePos.z - bufferPos.getZ();
		mcMatrixStack.translate(-xDiff, -projectedView.y, -zDiff);
		
		
		// get the modified model view matrix
		Matrix4f lodModelViewMatrix = mcMatrixStack.last().pose(); 
		// remove the lod ModelViewMatrix
		mcMatrixStack.popPose();
		
		return lodModelViewMatrix;
	}


	/**
	 * create a new projection matrix and send it over to the GPU
	 * 
	 * @param currentProjectionMatrix this is Minecraft's current projection matrix
	 * @param partialTicks how many ticks into the frame we are
	 */
	private void setupProjectionMatrix(Matrix4f currentProjectionMatrix, float partialTicks)
	{
		// create the new projection matrix
		Matrix4f lodPoj =
			Matrix4f.perspective(
					getFov(partialTicks, true),
					(float) this.mc.getWindow().getScreenWidth() / (float) this.mc.getWindow().getScreenHeight(),
					mc.getRenderDistance()/2,
					farPlaneBlockDistance * LodUtil.CHUNK_WIDTH * 2 / 4);
		
		// get Minecraft's un-edited projection matrix
		// (this is before it is zoomed, distorted, etc.)
		Matrix4f defaultMcProj = mc.getGameRenderer().getProjectionMatrix(mc.getGameRenderer().getMainCamera(), partialTicks, true);
		// true here means use "use fov setting" (probably)
		
		
		// this logic strips away the defaultMcProj matrix so we 
		// can get the distortionMatrix, which represents all
		// transformations, zooming, distortions, etc. done
		// to Minecraft's Projection matrix
		Matrix4f defaultMcProjInv = defaultMcProj.copy();
		defaultMcProjInv.invert();
		
		Matrix4f distortionMatrix = defaultMcProjInv.copy();
		distortionMatrix.multiply(currentProjectionMatrix);
		
		
		// edit the lod projection to match Minecraft's
		// (so the LODs line up with the real world)
		lodPoj.multiply(distortionMatrix);
		
		// send the projection over to the GPU
		gameRender.resetProjectionMatrix(lodPoj);
	}


	/**
	 * setup the lighting to be used for the LODs
	 */
	/*private void setupLighting(LodDimension lodDimension, float partialTicks)
	{
		// Determine if the player has night vision
		boolean playerHasNightVision = false;
		if (this.mc.getPlayer() != null)
		{
			Iterator<EffectInstance> iterator = this.mc.getPlayer().getActiveEffects().iterator();
			while (iterator.hasNext())
			{
				EffectInstance instance = iterator.next();
				if (instance.getEffect() == Effects.NIGHT_VISION)
				{
					playerHasNightVision = true;
					break;
				}
			}
		}

		float sunBrightness = lodDimension.dimension.hasSkyLight() ? mc.getSkyDarken(partialTicks) : 0.2f;
		sunBrightness = playerHasNightVision ? 1.0f : sunBrightness;
		float gamma = (float) mc.getOptions().gamma - 0.0f;
		float dayEffect = (sunBrightness - 0.2f) * 1.25f;
		float lightStrength = (gamma * 0.34f - 0.01f) * (1.0f - dayEffect) + dayEffect - 0.20f; //gamma * 0.2980392157f + 0.1647058824f
		float blueLightStrength = (gamma * 0.44f + 0.12f) * (1.0f - dayEffect) + dayEffect - 0.20f; //gamma * 0.4235294118f + 0.2784313725f

		float[] lightAmbient = {lightStrength, lightStrength, blueLightStrength, 1.0f};


		// can be used for debugging
		//		if (partialTicks < 0.005)
		//			ClientProxy.LOGGER.debug(lightStrength);

		ByteBuffer temp = ByteBuffer.allocateDirect(16);
		temp.order(ByteOrder.nativeOrder());
		GL11.glLightfv(LOD_GL_LIGHT_NUMBER, GL11.GL_AMBIENT, (FloatBuffer) temp.asFloatBuffer().put(lightAmbient).flip());
		GL11.glEnable(LOD_GL_LIGHT_NUMBER); // Enable the above lighting

		RenderSystem.enableLighting();
	}*/

	/**
	 * Create all buffers that will be used.
	 */
	public void setupBuffers(LodDimension lodDim)
	{
		lodBufferBuilder.setupBuffers(lodDim);
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
	 *
	 * For some reason this has to be called after the frame has been rendered,
	 * otherwise visual stuttering/rubber banding may happen. I'm not sure why...
	 */
	private void swapBuffers()
	{
		// replace the drawable buffers with
		// the newly created buffers from the lodBufferBuilder
		VertexBuffersAndOffset result = lodBufferBuilder.getVertexBuffers();
		vbos = result.vbos;
		vbosCenter = result.drawableCenterChunkPos;
	}

	/**
	 * Calls the BufferBuilder's destroyBuffers method.
	 */
	public void destroyBuffers()
	{
		lodBufferBuilder.destroyBuffers();
	}


	private double getFov(float partialTicks, boolean useFovSetting)
	{
		return mc.getGameRenderer().getFov(mc.getGameRenderer().getMainCamera(), partialTicks, useFovSetting);
	}


	/**
	 * Return what fog settings should be used when rendering.
	 */
	private NearFarFogSettings determineFogSettings()
	{
		NearFarFogSettings fogSettings = new NearFarFogSettings();


		FogQuality quality = ReflectionHandler.INSTANCE.getFogQuality();
		FogDrawOverride override = LodConfig.CLIENT.graphics.fogDrawOverride.get();


		fogSettings.vanillaIsRenderingFog = quality != FogQuality.OFF;


		// use any fog overrides the user may have set
		switch (override)
		{
			case ALWAYS_DRAW_FOG_FANCY:
				quality = FogQuality.FANCY;
				break;

			case NEVER_DRAW_FOG:
				quality = FogQuality.OFF;
				break;

			case ALWAYS_DRAW_FOG_FAST:
				quality = FogQuality.FAST;
				break;

			case USE_OPTIFINE_FOG_SETTING:
				// don't override anything
				break;
		}


		// only use fancy fog if the user's GPU can deliver
		if (!GlProxy.getInstance().fancyFogAvailable && quality == FogQuality.FANCY)
		{
			quality = FogQuality.FAST;
		}


		// how different distances are drawn depends on the quality set
		switch (quality)
		{
			case FANCY:
				fogSettings.near.quality = FogQuality.FANCY;
				fogSettings.far.quality = FogQuality.FANCY;

				switch (LodConfig.CLIENT.graphics.fogDistance.get())
				{
					case NEAR_AND_FAR:
						fogSettings.near.distance = FogDistance.NEAR;
						fogSettings.far.distance = FogDistance.FAR;
						break;

					case NEAR:
						fogSettings.near.distance = FogDistance.NEAR;
						fogSettings.far.distance = FogDistance.NEAR;
						break;

					case FAR:
						fogSettings.near.distance = FogDistance.FAR;
						fogSettings.far.distance = FogDistance.FAR;
						break;
				}
				break;

			case FAST:
				fogSettings.near.quality = FogQuality.FAST;
				fogSettings.far.quality = FogQuality.FAST;

				// fast fog setting should only have one type of
				// fog, since the LODs are separated into a near
				// and far portion; and fast fog is rendered from the
				// frustrum's perspective instead of the camera
				switch (LodConfig.CLIENT.graphics.fogDistance.get())
				{
					case NEAR_AND_FAR:
						fogSettings.near.distance = FogDistance.NEAR;
						fogSettings.far.distance = FogDistance.NEAR;
						break;

					case NEAR:
						fogSettings.near.distance = FogDistance.NEAR;
						fogSettings.far.distance = FogDistance.NEAR;
						break;

					case FAR:
						fogSettings.near.distance = FogDistance.FAR;
						fogSettings.far.distance = FogDistance.FAR;
						break;
				}
				break;

			case OFF:

				fogSettings.near.quality = FogQuality.OFF;
				fogSettings.far.quality = FogQuality.OFF;
				break;

		}


		return fogSettings;
	}


	/**
	 * Determines if the LODs should have a fullRegen or partialRegen
	 */
	private void determineIfLodsShouldRegenerate(LodDimension lodDim)
	{
		short chunkRenderDistance = (short) mc.getRenderDistance();
		
		int vanillaRenderedChunksWidth = chunkRenderDistance*2+2;
		vanillaRenderedChunks = new boolean[vanillaRenderedChunksWidth][vanillaRenderedChunksWidth];
		
		
		
		//=============//
		// full regens //
		//=============//
		
		// check if the view distance changed
		if (ClientProxy.previousLodRenderDistance != LodConfig.CLIENT.graphics.lodChunkRenderDistance.get()
				    ||  mc.getRenderDistance()  != prevRenderDistance
				    || prevFogDistance != LodConfig.CLIENT.graphics.fogDistance.get())
		{
			DetailDistanceUtil.updateSettings();
			fullRegen = true;
			previousPos = LevelPosUtil.createLevelPos((byte) 4, mc.getPlayer().xChunk, mc.getPlayer().zChunk);
			prevFogDistance = LodConfig.CLIENT.graphics.fogDistance.get();
			prevRenderDistance = mc.getRenderDistance();
		}

		// did the user change the debug setting?
		if (LodConfig.CLIENT.debugging.debugMode.get() != previousDebugMode)
		{
			previousDebugMode = LodConfig.CLIENT.debugging.debugMode.get();
			fullRegen = true;
		}


		long newTime = System.currentTimeMillis();

		if(LodConfig.CLIENT.graphics.detailDropOff.get() == DetailDropOff.FANCY)
		{
			// check if the player has moved
			if (newTime - prevPlayerPosTime > LodConfig.CLIENT.buffers.bufferRebuildPlayerMoveTimeout.get())
			{
				if (LevelPosUtil.getDetailLevel(previousPos) == 0
						    || mc.getPlayer().xChunk != LevelPosUtil.getPosX(previousPos)
						    || mc.getPlayer().zChunk != LevelPosUtil.getPosZ(previousPos))
				{
					fullRegen = true;
					previousPos = LevelPosUtil.createLevelPos((byte) 4, mc.getPlayer().xChunk, mc.getPlayer().zChunk);
				}
				prevPlayerPosTime = newTime;
			}
		}



		//================//
		// partial regens //
		//================//


		// check if the vanilla rendered chunks changed
		if (newTime - prevVanillaChunkTime > LodConfig.CLIENT.buffers.bufferRebuildChunkChangeTimeout.get())
		{
			if (vanillaRenderedChunksChanged)
			{
				partialRegen = true;
				vanillaRenderedChunksChanged = false;

			}
			prevVanillaChunkTime = newTime;
		}


		// check if there is any newly generated terrain to show
		if (newTime - prevChunkTime > LodConfig.CLIENT.buffers.bufferRebuildLodChangeTimeout.get())
		{
			if (lodDim.regenDimensionBuffers)
			{
				partialRegen = true;
				lodDim.regenDimensionBuffers = false;
			}
			prevChunkTime = newTime;
		}

		// check if the lighting has changed
		if (mc.getClientWorld().getDayTime() - prevDayTime > 1000 || mc.getOptions().gamma != prevBrightness || lightMap == null)
		{
			fullRegen = true;
			lightMap = mc.getCurrentLightMap();
			prevBrightness = mc.getOptions().gamma;
			prevDayTime = mc.getClientWorld().getDayTime();
		}




		//==============//
		// LOD skipping //
		//==============//

		// determine which LODs should not be rendered close to the player
		HashSet<ChunkPos> chunkPosToSkip = LodUtil.getNearbyLodChunkPosToSkip(lodDim, mc.getPlayer().blockPosition());
		int xIndex;
		int zIndex;
		for (ChunkPos pos : chunkPosToSkip)
		{
			xIndex = (pos.x - mc.getPlayer().xChunk) + (chunkRenderDistance + 1);
			zIndex = (pos.z - mc.getPlayer().zChunk) + (chunkRenderDistance + 1);
			
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
		if(chunkPosToSkip.isEmpty() && mc.getPlayer().position().y > 256)
		{
			vanillaRenderedChunks = new boolean[vanillaRenderedChunksWidth][vanillaRenderedChunksWidth];
			vanillaRenderedChunksChanged = true;
		}
	}

}
