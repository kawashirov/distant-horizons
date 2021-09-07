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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashSet;
import java.util.Iterator;

import com.seibel.lod.objects.LevelPosUtil;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.NVFogDistance;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.seibel.lod.builders.LodBufferBuilder;
import com.seibel.lod.builders.LodBufferBuilder.VertexBuffersAndOffset;
import com.seibel.lod.config.LodConfig;
import com.seibel.lod.enums.DebugMode;
import com.seibel.lod.enums.FogDistance;
import com.seibel.lod.enums.FogDrawOverride;
import com.seibel.lod.enums.FogQuality;
import com.seibel.lod.handlers.ReflectionHandler;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.objects.NearFarFogSettings;
import com.seibel.lod.objects.RegionPos;
import com.seibel.lod.proxy.ClientProxy;
import com.seibel.lod.util.DetailDistanceUtil;
import com.seibel.lod.util.LodUtil;
import com.seibel.lod.wrapper.MinecraftWrapper;

import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.Entity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;


/**
 * This is where all the magic happens. <br>
 * This is where LODs are draw to the world.
 *
 * @author James Seibel
 * @version 8-31-2021
 */
public class LodRenderer
{
	/**
	 * this is the light used when rendering the LODs,
	 * it should be something different than what is used by Minecraft
	 */
	private static final int LOD_GL_LIGHT_NUMBER = GL11.GL_LIGHT2;

	/**
	 * 64 MB by default is the maximum amount of memory that
	 * can be directly allocated. <br><br>
	 * <p>
	 * I know there are commands to change that amount
	 * (specifically "-XX:MaxDirectMemorySize"), but
	 * I have no idea how to access that amount. <br>
	 * So I guess this will be the hard limit for now. <br><br>
	 * <p>
	 * https://stackoverflow.com/questions/50499238/bytebuffer-allocatedirect-and-xmx
	 */
	public static final int MAX_ALOCATEABLE_DIRECT_MEMORY = 64 * 1024 * 1024;

	/**
	 * Does this computer's GPU support fancy fog?
	 */
	private static Boolean fancyFogAvailable = null;


	/**
	 * If true the LODs colors will be replaced with
	 * a checkerboard, this can be used for debugging.
	 */
	public DebugMode previousDebugMode = DebugMode.OFF;

	private MinecraftWrapper mc;
	private GameRenderer gameRender;
	private IProfiler profiler;
	private int farPlaneBlockDistance;
	private ReflectionHandler reflectionHandler;


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

		reflectionHandler = new ReflectionHandler();
		lodBufferBuilder = newLodNodeBufferBuilder;
	}


	/**
	 * Besides drawing the LODs this method also starts
	 * the async process of generating the Buffers that hold those LODs.
	 *
	 * @param lodDim       The dimension to draw, if null doesn't replace the current dimension.
	 * @param partialTicks how far into the current tick this method was called.
	 */
	public void drawLODs(LodDimension lodDim, float partialTicks, IProfiler newProfiler)
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


		// only check the GPU capability's once
		if (fancyFogAvailable == null)
		{
			// see if this GPU can run fancy fog
			fancyFogAvailable = GL.getCapabilities().GL_NV_fog_distance;

			if (!fancyFogAvailable)
			{
				ClientProxy.LOGGER.info("This GPU does not support GL_NV_fog_distance. This means that fancy fog options will not be available.");
			}
		}


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

		// replace the buffers used to draw and build,
		// this is only done when the createLodBufferGenerationThread
		// has finished executing on a parallel thread.
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
		float[] defaultProjMatrix = new float[16];
		GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, defaultProjMatrix);

		Matrix4f modelViewMatrix = generateModelViewMatrix(partialTicks);

		// required for setupFog and setupProjectionMatrix
		farPlaneBlockDistance = LodConfig.CLIENT.graphics.lodChunkRenderDistance.get() * LodUtil.CHUNK_WIDTH;

		setupProjectionMatrix(partialTicks);
		setupLighting(lodDim, partialTicks);

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
			Entity cameraEntity = mc.getCameraEntity();
			Vector3d cameraDir = cameraEntity.getLookAngle().normalize();
			cameraDir = mc.getOptions().getCameraType().isMirrored() ? cameraDir.reverse() : cameraDir;



			// used to determine what type of fog to render
			int halfWidth = vbos.length / 2;
			int quarterWidth = vbos.length / 4;

			for (int i = 0; i < vbos.length; i++)
			{
				for (int j = 0; j < vbos.length; j++)
				{
					RegionPos vboPos = new RegionPos(i + lodDim.getCenterX() - lodDim.getWidth() / 2, j + lodDim.getCenterZ() - lodDim.getWidth() / 2);
					if (RenderUtil.isRegionInViewFrustum(cameraEntity.blockPosition(), cameraDir, vboPos.blockPos()))
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
		Matrix4f mvm = new Matrix4f(defaultProjMatrix);
		mvm.transpose();
		gameRender.resetProjectionMatrix(mvm);

		// clear the depth buffer so anything drawn is drawn
		// over the LODs
		GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);



		// replace the buffers used to draw and build,
		// this is only done when the createLodBufferGenerationThread
		// has finished executing on a parallel thread.
		if (lodBufferBuilder.newBuffersAvaliable())
		{
			// this has to be called after the VBOs have been drawn
			// otherwise rubber banding may occur
			swapBuffers();
		}


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

		vbo.bind();
		// 0L is the starting pointer
		LOD_VERTEX_FORMAT.setupBufferState(0L);

		vbo.draw(modelViewMatrix, GL11.GL_QUADS);

		VertexBuffer.unbind();
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
	 * Create the model view matrix to move the LODs
	 * from object space into world space.
	 */
	private Matrix4f generateModelViewMatrix(float partialTicks)
	{
		// get all relevant camera info
		ActiveRenderInfo renderInfo = mc.getGameRenderer().getMainCamera();
		Vector3d projectedView = renderInfo.getPosition();


		// generate the model view matrix
		MatrixStack matrixStack = new MatrixStack();
		matrixStack.pushPose();
		// rotate to the current camera's direction
		matrixStack.mulPose(Vector3f.XP.rotationDegrees(renderInfo.getXRot()));
		matrixStack.mulPose(Vector3f.YP.rotationDegrees(renderInfo.getYRot() + 180));
		// translate the camera relative to the regions' center
		// (AxisAlignedBoundingBoxes (LODs) use doubles and thus have a higher
		// accuracy vs the model view matrix, which only uses floats)
		BlockPos bufferPos = vbosCenter.getWorldPosition();
		Vector3d eyePos = mc.getPlayer().getEyePosition(partialTicks);
		double xDiff = eyePos.x - bufferPos.getX();
		double zDiff = eyePos.z - bufferPos.getZ();
		matrixStack.translate(-xDiff, -projectedView.y, -zDiff);

		return matrixStack.last().pose();
	}


	/**
	 * create a new projection matrix and send it over to the GPU
	 * <br><br>
	 * A lot of this code is copied from renderLevel (line 567)
	 * in the GameRender class. The code copied is anything with
	 * a matrixStack and is responsible for making sure the LOD
	 * objects distort correctly relative to the rest of the world.
	 * Distortions are caused by: standing in a nether portal,
	 * nausea potion effect, walking bobbing.
	 *
	 * @param partialTicks how many ticks into the frame we are
	 */
	private void setupProjectionMatrix(float partialTicks)
	{
		// Note: if the LOD objects don't distort correctly
		// compared to regular minecraft terrain, make sure
		// all the transformations in renderWorld are here too

		MatrixStack matrixStack = new MatrixStack();
		matrixStack.pushPose();

		gameRender.bobHurt(matrixStack, partialTicks);
		if (this.mc.getOptions().bobView)
		{
			gameRender.bobView(matrixStack, partialTicks);
		}

		// potion and nausea effects
		float f = MathHelper.lerp(partialTicks, this.mc.getPlayer().oPortalTime, this.mc.getPlayer().portalTime) * this.mc.getOptions().screenEffectScale * this.mc.getOptions().screenEffectScale;
		if (f > 0.0F)
		{
			int i = this.mc.getPlayer().hasEffect(Effects.CONFUSION) ? 7 : 20;
			float f1 = 5.0F / (f * f + 5.0F) - f * 0.04F;
			f1 = f1 * f1;
			Vector3f vector3f = new Vector3f(0.0F, MathHelper.SQRT_OF_TWO / 2.0F, MathHelper.SQRT_OF_TWO / 2.0F);
			matrixStack.mulPose(vector3f.rotationDegrees((gameRender.tick + partialTicks) * i));
			matrixStack.scale(1.0F / f1, 1.0F, 1.0F);
			float f2 = -(gameRender.tick + partialTicks) * i;
			matrixStack.mulPose(vector3f.rotationDegrees(f2));
		}


		// this projection matrix allows us to see past the normal
		// world render distance
		Matrix4f projectionMatrix =
				Matrix4f.perspective(
						getFov(partialTicks, true),
						(float) this.mc.getWindow().getScreenWidth() / (float) this.mc.getWindow().getScreenHeight(),
						// it is possible to see the near clip plane, but
						// you have to be flying quickly in spectator mode through ungenerated
						// terrain, so I don't think it is much of an issue.
						mc.getRenderDistance(),
						farPlaneBlockDistance * LodUtil.CHUNK_WIDTH * 2);

		// add the screen space distortions
		projectionMatrix.multiply(matrixStack.last().pose());
		gameRender.resetProjectionMatrix(projectionMatrix);
		return;
	}


	/**
	 * setup the lighting to be used for the LODs
	 */
	private void setupLighting(LodDimension lodDimension, float partialTicks)
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
		float gammaMultiplyer = (float) mc.getOptions().gamma - 0.5f;
		float lightStrength = ((sunBrightness / 2f) - 0.2f) + (gammaMultiplyer * 0.3f);

		float lightAmbient[] = {lightStrength, lightStrength, lightStrength, 1.0f};

		// can be used for debugging
		//		if (partialTicks < 0.005)
		//			ClientProxy.LOGGER.debug(lightStrength);

		ByteBuffer temp = ByteBuffer.allocateDirect(16);
		temp.order(ByteOrder.nativeOrder());
		GL11.glLightfv(LOD_GL_LIGHT_NUMBER, GL11.GL_AMBIENT, (FloatBuffer) temp.asFloatBuffer().put(lightAmbient).flip());
		GL11.glEnable(LOD_GL_LIGHT_NUMBER); // Enable the above lighting

		RenderSystem.enableLighting();
	}

	/**
	 * Create all buffers that will be used.
	 */
	public void setupBuffers(int numbRegionsWide)
	{
		// calculate the max amount of memory needed (in bytes)
		int bufferMemory = RenderUtil.getBufferMemoryForRegion();

		// if the required memory is greater than the
		// MAX_ALOCATEABLE_DIRECT_MEMORY lower the lodChunkRadiusMultiplier
		// to fit.
		if (bufferMemory > MAX_ALOCATEABLE_DIRECT_MEMORY)
		{
			ClientProxy.LOGGER.warn("setupBuffers tried to allocate too much memory for the BufferBuilders."
					                        + " It tried to allocate \"" + bufferMemory + "\" bytes, when \"" + MAX_ALOCATEABLE_DIRECT_MEMORY + "\" is the max.");
		}

		lodBufferBuilder.setupBuffers(numbRegionsWide, bufferMemory);
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


		FogQuality quality = reflectionHandler.getFogQuality();
		FogDrawOverride override = LodConfig.CLIENT.graphics.fogDrawOverride.get();


		if (quality == FogQuality.OFF)
			fogSettings.vanillaIsRenderingFog = false;
		else
			fogSettings.vanillaIsRenderingFog = true;


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
		if (!fancyFogAvailable && quality == FogQuality.FANCY)
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
	@SuppressWarnings("unchecked")
	private void determineIfLodsShouldRegenerate(LodDimension lodDim)
	{
		fullRegen = true;
		short renderDistance = (short) mc.getRenderDistance();
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
			//should use this when it's ready
			vanillaRenderedChunks = new boolean[renderDistance*2+2][renderDistance*2+2];
		}

		// did the user change the debug setting?
		if (LodConfig.CLIENT.debugging.debugMode.get() != previousDebugMode)
		{
			previousDebugMode = LodConfig.CLIENT.debugging.debugMode.get();
			fullRegen = true;
		}
Bug

		long newTime = System.currentTimeMillis();

		// check if the player has moved
		if (newTime - prevPlayerPosTime > LodConfig.CLIENT.buffers.bufferRebuildPlayerMoveTimeout.get())
		{
			if (LevelPosUtil.getDetailLevel(previousPos) == 0
					    || mc.getPlayer().xChunk != LevelPosUtil.getPosX(previousPos)
					    || mc.getPlayer().zChunk != LevelPosUtil.getPosZ(previousPos))
			{
				fullRegen = true;
				previousPos = LevelPosUtil.createLevelPos((byte) 4, mc.getPlayer().xChunk, mc.getPlayer().zChunk);
				//should use this when it's ready
				vanillaRenderedChunks = new boolean[renderDistance*2+2][renderDistance*2+2];
			}
			prevPlayerPosTime = newTime;
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
			if (lodDim.regenDimension)
			{
				partialRegen = true;
				lodDim.regenDimension = false;
			}
			prevChunkTime = newTime;
		}




		//==============//
		// LOD skipping //
		//==============//

		// determine which LODs should not be rendered close to the player
		HashSet<ChunkPos> chunkPosToSkip = LodUtil.getNearbyLodChunkPosToSkip(lodDim, mc.getPlayer().blockPosition());
		int chunkX;
		int chunkZ;
		for (ChunkPos pos : chunkPosToSkip)
		{
			chunkX = pos.x - mc.getPlayer().xChunk + renderDistance + 1;
			chunkZ = pos.z - mc.getPlayer().zChunk + renderDistance + 1;
			try
			{
				if (!vanillaRenderedChunks[chunkX][chunkZ])
				{
					vanillaRenderedChunks[chunkX][chunkZ] = true;
					vanillaRenderedChunksChanged = true;
					lodDim.setToRegen(pos.getRegionX(), pos.getRegionZ());
				}
			}catch (Exception e){
				System.out.println(vanillaRenderedChunks.length);
				e.printStackTrace();
			}
		}


		// if the player is high enough, draw all LODs
		if(chunkPosToSkip.isEmpty() && mc.getPlayer().position().y > 256)
		{
			vanillaRenderedChunks = new boolean[renderDistance*2+2][renderDistance*2+2];
			vanillaRenderedChunksChanged = true;
		}
	}

}
