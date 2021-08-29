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

import com.seibel.lod.objects.LevelPos.LevelPos;
import net.minecraft.world.chunk.Chunk;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.NVFogDistance;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.seibel.lod.builders.LodBufferBuilder;
import com.seibel.lod.enums.DebugMode;
import com.seibel.lod.enums.FogDistance;
import com.seibel.lod.enums.FogDrawOverride;
import com.seibel.lod.enums.FogQuality;
import com.seibel.lod.handlers.LodConfig;
import com.seibel.lod.handlers.ReflectionHandler;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.objects.NearFarFogSettings;
import com.seibel.lod.objects.RegionPos;
import com.seibel.lod.proxy.ClientProxy;
import com.seibel.lod.util.LodUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;


/**
 * This is where all the magic happens. <br>
 * This is where LODs are draw to the world.
 *
 * @author James Seibel
 * @version 8-21-2021
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
	
	private Minecraft mc;
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
	
	
	/**
	 * This is used to determine if the LODs should be regenerated
	 */
	private LevelPos previousPos = new LevelPos((byte) 0,0,0);
	private static long prevPlayerPosTime = 0;
	private static long prevVanillaChunkTime = 0;
	private static long prevChunkTime = 0;


	/**
	 * This is used to determine if the LODs should be regenerated
	 */
	private FogDistance prevFogDistance = FogDistance.NEAR_AND_FAR;
	
	/**
	 * if this is true the LOD buffers should be regenerated,
	 * provided they aren't already being regenerated.
	 */
	private volatile boolean regen = false;
	
	/**
	 * This HashSet contains every chunk that Vanilla Minecraft
	 * is going to render
	 */
	public HashSet<ChunkPos> vanillaRenderedChunks = new HashSet<>();
	public HashSet<ChunkPos> previousVanillaRenderedChunks = new HashSet<>();

	
	public LodRenderer(LodBufferBuilder newLodNodeBufferBuilder)
	{
		mc = Minecraft.getInstance();
		gameRender = mc.gameRenderer;
		
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
	@SuppressWarnings("deprecation")
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
		
		
		ClientPlayerEntity player = mc.player;

		// should LODs be regenerated?
		long newTime = System.currentTimeMillis();
		if(newTime - prevPlayerPosTime > 5000)
		{
			if (previousPos.detailLevel == 0 ||
					    player.xChunk != previousPos.posX ||
					    player.zChunk != previousPos.posZ ||
					    ClientProxy.previousLodRenderDistance != LodConfig.CLIENT.lodChunkRenderDistance.get() ||
					    prevFogDistance != LodConfig.CLIENT.fogDistance.get())
			{
				// yes
				regen = true;
				previousPos.changeParameters((byte) 4, player.xChunk, player.zChunk);
				prevFogDistance = LodConfig.CLIENT.fogDistance.get();
				//should use this when it's ready
				//vanillaRenderedChunks.stream().filter(pos -> ((Math.abs(pos.x - player.xChunk) > mc.options.renderDistance) || (Math.abs(pos.z - player.zChunk) > mc.options.renderDistance)));
				vanillaRenderedChunks.clear();
			}
			prevPlayerPosTime = newTime;
		}
		if(newTime - prevVanillaChunkTime > 5000)
		{
			if (previousVanillaRenderedChunks.equals(vanillaRenderedChunks)){
				regen = true;
				vanillaRenderedChunks = previousVanillaRenderedChunks;
			}
			prevVanillaChunkTime = newTime;
		}
		if(newTime - prevChunkTime > 5000)
		{
			if(lodDim.regenDimension)
			{
				regen = true;
				lodDim.regenDimension = false;
			}
			prevChunkTime = newTime;
		}

		// determine which LODs should not be rendered close to the player
		HashSet<ChunkPos> chunkPosToSkip = LodUtil.getNearbyLodChunkPosToSkip(lodDim, player.blockPosition());

		for(ChunkPos pos : chunkPosToSkip){
			if(!vanillaRenderedChunks.contains(pos))
			{
				vanillaRenderedChunks.add(pos);
				lodDim.setToRegen(pos.getRegionX(),pos.getRegionZ());
			}
		}

		// did the user change the debug setting?
		if (LodConfig.CLIENT.debugMode.get() != previousDebugMode)
		{
			previousDebugMode = LodConfig.CLIENT.debugMode.get();
			regen = true;
		}
		
		
		// determine how far the game's render distance is currently set
		farPlaneBlockDistance = LodConfig.CLIENT.lodChunkRenderDistance.get() * LodUtil.CHUNK_WIDTH;
		
		// set how how far the LODs will go
		int numbChunksWide =LodConfig.CLIENT.lodChunkRenderDistance.get() * 2;

		// see if the chunks Minecraft is going to render are the
		// same as last time
		/*
		if (!vanillaRenderedChunks.containsAll(chunkPosToSkip) || vanillaRenderedChunks.size() != chunkPosToSkip.size())
		{
			regen = true;
			vanillaRenderedChunks = chunkPosToSkip;
		}*/

		
		//=================//
		// create the LODs //
		//=================//
		
		// only regenerate the LODs if:
		// 1. we want to regenerate LODs
		// 2. we aren't already regenerating the LODs
		// 3. we aren't waiting for the build and draw buffers to swap
		//		(this is to prevent thread conflicts)
		if (regen && !lodBufferBuilder.generatingBuffers && !lodBufferBuilder.newBuffersAvaliable())
		{
			// generate the LODs on a separate thread to prevent stuttering or freezing
			lodBufferBuilder.generateLodBuffersAsync(this, lodDim, player.blockPosition(), Math.floorMod((int) player.xRot,360), Math.floorMod((int) player.yRot,360), numbChunksWide);
			
			// the regen process has been started,
			// it will be done when lodBufferBuilder.newBuffersAvaliable
			// is true
			regen = false;
		}
		
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
		
		if (LodConfig.CLIENT.debugMode.get() == DebugMode.SHOW_DETAIL_WIREFRAME)
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
			Vector3d cameraDir = mc.cameraEntity.getLookAngle().normalize();
			cameraDir = mc.options.getCameraType().isMirrored() ? cameraDir.reverse() : cameraDir;
			
			
			// used to determine what type of fog to render
			int halfWidth = vbos.length/2;
			int quarterWidth = vbos.length/4;
			
			for (int i = 0; i < vbos.length; i++)
			{
				for (int j = 0; j < vbos.length; j++)
				{
					RegionPos vboPos = new RegionPos(i + lodDim.getCenterX() - lodDim.getWidth()/2, j + lodDim.getCenterZ() - lodDim.getWidth()/2);
					if (RenderUtil.isRegionInViewFrustum(mc.cameraEntity.blockPosition(), cameraDir, vboPos.blockPos()))
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
		
		
		// end of internal LOD profiling
		profiler.pop();
	}
	
	
	/**
	 * This is where the actual drawing happens.
	 *
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
		} else
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
				RenderSystem.fogStart(farPlaneBlockDistance * 0.8f);
				RenderSystem.fogEnd(farPlaneBlockDistance * 1.0f);
			} else if (fogQuality == FogQuality.FAST)
			{
				// for the far fog of the normal chunks
				// to start right where the LODs' end use:
				// end = 0.8f, start = 1.5f
				RenderSystem.fogStart(farPlaneBlockDistance * 1.5f);
				RenderSystem.fogEnd(farPlaneBlockDistance * 2.0f);
			}
		} else if (fogDistance == FogDistance.NEAR)
		{
			if (fogQuality == FogQuality.FANCY)
			{
				RenderSystem.fogEnd(mc.options.renderDistance * 16 * 1.41f);
				RenderSystem.fogStart(mc.options.renderDistance * 16 * 1.6f);
			} else if (fogQuality == FogQuality.FAST)
			{
				RenderSystem.fogEnd(mc.options.renderDistance * 16 * 1.0f);
				RenderSystem.fogStart(mc.options.renderDistance * 16 * 1.5f);
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
	@SuppressWarnings("deprecation")
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
		ActiveRenderInfo renderInfo = mc.gameRenderer.getMainCamera();
		Vector3d projectedView = renderInfo.getPosition();
		
		
		// generate the model view matrix
		MatrixStack matrixStack = new MatrixStack();
		matrixStack.pushPose();
		// translate and rotate to the current camera location
		matrixStack.mulPose(Vector3f.XP.rotationDegrees(renderInfo.getXRot()));
		matrixStack.mulPose(Vector3f.YP.rotationDegrees(renderInfo.getYRot() + 180));
		matrixStack.translate(-projectedView.x, -projectedView.y, -projectedView.z);
		
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
		if (this.mc.options.bobView)
		{
			gameRender.bobView(matrixStack, partialTicks);
		}
		
		// potion and nausea effects
		float f = MathHelper.lerp(partialTicks, this.mc.player.oPortalTime, this.mc.player.portalTime) * this.mc.options.screenEffectScale * this.mc.options.screenEffectScale;
		if (f > 0.0F)
		{
			int i = this.mc.player.hasEffect(Effects.CONFUSION) ? 7 : 20;
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
						mc.options.renderDistance,
						farPlaneBlockDistance * LodUtil.CHUNK_WIDTH * 2);
		
		// add the screen space distortions
		projectionMatrix.multiply(matrixStack.last().pose());
		gameRender.resetProjectionMatrix(projectionMatrix);
		return;
	}
	
	
	/**
	 * setup the lighting to be used for the LODs
	 */
	@SuppressWarnings("deprecation")
	private void setupLighting(LodDimension lodDimension, float partialTicks)
	{
		// Determine if the player has night vision
		boolean playerHasNightVision = false;
		if (this.mc.player != null)
		{
			Iterator<EffectInstance> iterator = this.mc.player.getActiveEffects().iterator();
			while(iterator.hasNext())
			{
				EffectInstance instance = iterator.next();
				if (instance.getEffect() == Effects.NIGHT_VISION)
				{
					playerHasNightVision = true;
					break;
				}
			}
		}
		
		
		float sunBrightness = lodDimension.dimension.hasSkyLight() ? mc.level.getSkyDarken(partialTicks) : 0.2f;
		sunBrightness = playerHasNightVision ? 1.0f : sunBrightness;
		float gammaMultiplyer = (float) mc.options.gamma - 0.5f;
		float lightStrength = ((sunBrightness / 2f) - 0.2f) + (gammaMultiplyer * 0.3f);
		
		float lightAmbient[] = {lightStrength, lightStrength, lightStrength, 1.0f};
		
		// can be used for debugging
		//		if (partialTicks < 0.005)
		//			ClientProxy.LOGGER.debug(lightStrength);
		
		ByteBuffer temp = ByteBuffer.allocateDirect(16);
		temp.order(ByteOrder.nativeOrder());
		GL11.glLightfv(LOD_GL_LIGHT_NUMBER, GL11.GL_AMBIENT, (FloatBuffer) temp.asFloatBuffer().put(lightAmbient).flip()); // TODO, could put return null? this crashed on James' laptop
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
		regen = true;
	}
	
	
	/**
	 * Replace the current Vertex Buffers with the newly
	 * created buffers from the lodBufferBuilder.
	 */
	private void swapBuffers()
	{
		// replace the drawable buffers with
		// the newly created buffers from the lodBufferBuilder
		vbos = lodBufferBuilder.getVertexBuffers();
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
		return mc.gameRenderer.getFov(mc.gameRenderer.getMainCamera(), partialTicks, useFovSetting);
	}
	
	
	/**
	 * Return what fog settings should be used when rendering.
	 */
	private NearFarFogSettings determineFogSettings()
	{
		NearFarFogSettings fogSettings = new NearFarFogSettings();
		
		
		FogQuality quality = reflectionHandler.getFogQuality();
		FogDrawOverride override = LodConfig.CLIENT.fogDrawOverride.get();
		
		
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
			
			switch (LodConfig.CLIENT.fogDistance.get())
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
			switch (LodConfig.CLIENT.fogDistance.get())
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
	
	
}