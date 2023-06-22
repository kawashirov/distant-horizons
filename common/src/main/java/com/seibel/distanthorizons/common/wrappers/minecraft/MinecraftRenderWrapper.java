/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package com.seibel.distanthorizons.common.wrappers.minecraft;

import java.awt.Color;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.seibel.distanthorizons.common.wrappers.McObjectConverter;
import com.seibel.distanthorizons.common.wrappers.WrapperFactory;
import com.seibel.distanthorizons.common.wrappers.misc.LightMapWrapper;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector;

import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.ILightMapWrapper;

import com.mojang.math.Vector3f;
import com.seibel.distanthorizons.coreapi.util.math.Mat4f;
import com.seibel.distanthorizons.coreapi.util.math.Vec3d;
import com.seibel.distanthorizons.coreapi.util.math.Vec3f;
import com.seibel.distanthorizons.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IOptifineAccessor;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.ISodiumAccessor;
import com.seibel.distanthorizons.core.pos.DhBlockPos;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
#if PRE_MC_1_17_1
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
import org.lwjgl.opengl.GL15;
#else
import net.minecraft.world.level.material.FogType;
#endif
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.Logger;


/**
 * A singleton that contains everything
 * related to rendering in Minecraft.
 *
 * @author James Seibel
 * @version 12-12-2021
 */
//@Environment(EnvType.CLIENT)
public class MinecraftRenderWrapper implements IMinecraftRenderWrapper
{
	public static final MinecraftRenderWrapper INSTANCE = new MinecraftRenderWrapper();
	
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
	private static final Minecraft MC = Minecraft.getInstance();
	private static final IWrapperFactory FACTORY = WrapperFactory.INSTANCE;

	private static final IOptifineAccessor OPTIFINE_ACCESSOR = ModAccessorInjector.INSTANCE.get(IOptifineAccessor.class);
	
	public LightMapWrapper lightmap = null;

	
	
	@Override
	public Vec3f getLookAtVector()
	{
		Camera camera = MC.gameRenderer.getMainCamera();
		Vector3f cameraDir = camera.getLookVector();
		return new Vec3f(cameraDir.x(), cameraDir.y(), cameraDir.z());
	}
	
	@Override
	public DhBlockPos getCameraBlockPosition()
	{
		Camera camera = MC.gameRenderer.getMainCamera();
		BlockPos blockPos = camera.getBlockPosition();
		return new DhBlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ());
	}
	
	@Override
	public boolean playerHasBlindnessEffect()
	{
		return MC.player.getActiveEffectsMap().get(MobEffects.BLINDNESS) != null;
	}
	
	@Override
	public Vec3d getCameraExactPosition()
	{
		Camera camera = MC.gameRenderer.getMainCamera();
		Vec3 projectedView = camera.getPosition();
		
		return new Vec3d(projectedView.x, projectedView.y, projectedView.z);
	}
	
	@Override
	public Mat4f getDefaultProjectionMatrix(float partialTicks)
	{
		#if PRE_MC_1_17_1
		return McObjectConverter.Convert(Minecraft.getInstance().gameRenderer.getProjectionMatrix(Minecraft.getInstance().gameRenderer.getMainCamera(), partialTicks, true));
		#else
		return McObjectConverter.Convert(MC.gameRenderer.getProjectionMatrix(MC.gameRenderer.getFov(MC.gameRenderer.getMainCamera(), partialTicks, true)));
		#endif
	}
	
	@Override
	public double getGamma()
	{
		#if PRE_MC_1_19
		return MC.options.gamma;
		#else
		return MC.options.gamma().get();
		#endif
	}
	
	@Override
	public Color getFogColor(float partialTicks) {
		#if PRE_MC_1_17_1
		float[] colorValues = new float[4];
		GL15.glGetFloatv(GL15.GL_FOG_COLOR, colorValues);
		#else
		FogRenderer.setupColor(MC.gameRenderer.getMainCamera(), partialTicks, MC.level, 1, MC.gameRenderer.getDarkenWorldAmount(partialTicks));
		float[] colorValues = RenderSystem.getShaderFogColor();
		#endif
		return new Color(
				Math.max(0f, Math.min(colorValues[0], 1f)),
				Math.max(0f, Math.min(colorValues[1], 1f)),
				Math.max(0f, Math.min(colorValues[2], 1f)),
				Math.max(0f, Math.min(colorValues[3], 1f))
		);
	}
	// getSpecialFogColor() is the same as getFogColor()
	
	@Override
	public Color getSkyColor() {
		if (MC.level.dimensionType().hasSkyLight()) {
			#if PRE_MC_1_17_1
			Vec3 colorValues = MC.level.getSkyColor(MC.gameRenderer.getMainCamera().getBlockPosition(), MC.getFrameTime());
			#else
			Vec3 colorValues = MC.level.getSkyColor(MC.gameRenderer.getMainCamera().getPosition(), MC.getFrameTime());
			#endif
			return new Color((float) colorValues.x, (float) colorValues.y, (float) colorValues.z);
		} else
			return new Color(0, 0, 0);
	}
	
	@Override
	public double getFov(float partialTicks)
	{
		return MC.gameRenderer.getFov(MC.gameRenderer.getMainCamera(), partialTicks, true);
	}
	
	/** Measured in chunks */
	@Override
	public int getRenderDistance()
	{
		#if PRE_MC_1_18_1
		//FIXME: How to resolve this?
		return MC.options.renderDistance;
		#else
		return MC.options.getEffectiveRenderDistance();
		#endif
	}
	
	@Override
	public int getScreenWidth()
	{
		// alternate ways of getting the window's resolution,
		// using one of these methods may fix the optifine render resolution bug
		// TODO: test these once we can run with Optifine again
//		int[] heightArray = new int[1];
//		int[] widthArray = new int[1];
//		
//		long window = GLProxy.getInstance().minecraftGlContext;
//		GLFW.glfwGetWindowSize(window, widthArray, heightArray); // option 1
//		GLFW.glfwGetFramebufferSize(window, widthArray, heightArray); // option 2
		
		
		
		int width = MC.getWindow().getWidth();
		if (OPTIFINE_ACCESSOR != null)
		{
			// TODO remove comment after testing:
			// this should fix the issue where different optifine render resolutions screw up the LOD rendering
			width *= OPTIFINE_ACCESSOR.getRenderResolutionMultiplier();
		}
		return width;
	}
	@Override
	public int getScreenHeight()
	{
		int height = MC.getWindow().getHeight();
		if (OPTIFINE_ACCESSOR != null)
		{
			height *= OPTIFINE_ACCESSOR.getRenderResolutionMultiplier();
		}
		return height;
	}
	
    private RenderTarget getRenderTarget() {
        RenderTarget r = null; //MC.levelRenderer.getCloudsTarget();
        return r!=null ? r : MC.getMainRenderTarget();
    }

    @Override
    public int getTargetFrameBuffer() {
        return getRenderTarget().frameBufferId;
    }

	@Override
	public int getDepthTextureId() {
		return getRenderTarget().getDepthTextureId();
	}

	@Override
    public int getTargetFrameBufferViewportWidth() {
        return getRenderTarget().viewWidth;
    }

    @Override
    public int getTargetFrameBufferViewportHeight() {
        return getRenderTarget().viewHeight;
    }

	/**
	 * This method returns the ChunkPos of all chunks that Minecraft
	 * is going to render this frame. <br><br>
	 * <p>
	 */
	
    public boolean usingBackupGetVanillaRenderedChunks = false;
	@Override
	public HashSet<DhChunkPos> getVanillaRenderedChunks() {
		ISodiumAccessor sodium = ModAccessorInjector.INSTANCE.get(ISodiumAccessor.class);
		if (sodium != null) {
			return sodium.getNormalRenderedChunks();
		}
		IOptifineAccessor optifine = ModAccessorInjector.INSTANCE.get(IOptifineAccessor.class);
		if (optifine != null) {
			HashSet<DhChunkPos> pos = optifine.getNormalRenderedChunks();
			if (pos == null)
				pos = getMaximumRenderedChunks();
			return pos;
		}
		if (!usingBackupGetVanillaRenderedChunks) {
			try {
				LevelRenderer levelRenderer = MC.levelRenderer;
				Collection<LevelRenderer.RenderChunkInfo> chunks =
					#if PRE_MC_1_18_1 levelRenderer.renderChunks;
					#else levelRenderer.renderChunkStorage.get().renderChunks; #endif

				return (chunks.stream().map((chunk) -> {
					AABB chunkBoundingBox =
						#if PRE_MC_1_18_2 chunk.chunk.bb;
						#else chunk.chunk.getBoundingBox(); #endif
					return new DhChunkPos(Math.floorDiv((int) chunkBoundingBox.minX, 16),
							Math.floorDiv((int) chunkBoundingBox.minZ, 16));
				}).collect(Collectors.toCollection(HashSet::new)));
			} catch (LinkageError e) {
				try {
					MinecraftClientWrapper.INSTANCE.sendChatMessage(
							"\u00A7e\u00A7l\u00A7uWARNING: Distant Horizons: getVanillaRenderedChunks method failed."
									+ " Using Backup Method.");
					MinecraftClientWrapper.INSTANCE.sendChatMessage(
							"\u00A7eOverdraw prevention will be worse than normal.");
				} catch (Exception e2) {
				}
				LOGGER.error("getVanillaRenderedChunks Error: ", e);
				usingBackupGetVanillaRenderedChunks = true;
			}
		}
		return getMaximumRenderedChunks();
	}

	@Override
	public ILightMapWrapper getLightmapWrapper() {
		return lightmap;
	}
	
	@Override
	public boolean isFogStateSpecial() {
		#if PRE_MC_1_17_1
		Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
		FluidState fluidState = camera.getFluidInCamera();
		Entity entity = camera.getEntity();
		boolean isUnderWater = (entity instanceof LivingEntity) && ((LivingEntity)entity).hasEffect(MobEffects.BLINDNESS);
			isUnderWater |= fluidState.is(FluidTags.WATER);
			isUnderWater |= fluidState.is(FluidTags.LAVA);
		return isUnderWater;
		#else
		Entity entity = MC.gameRenderer.getMainCamera().getEntity();
		boolean isBlind = (entity instanceof LivingEntity) && ((LivingEntity)entity).hasEffect(MobEffects.BLINDNESS);
		return MC.gameRenderer.getMainCamera().getFluidInCamera() != FogType.NONE || isBlind;
		#endif
	}
	
	@Override
	public boolean tryDisableVanillaFog() {
		return true; // Handled via MixinFogRenderer in both forge and fabric
	}

    public void updateLightmap(NativeImage lightPixels) {
		if (lightmap== null) {
			lightmap = new LightMapWrapper();
		}
		lightmap.uploadLightmap(lightPixels);
    }
}