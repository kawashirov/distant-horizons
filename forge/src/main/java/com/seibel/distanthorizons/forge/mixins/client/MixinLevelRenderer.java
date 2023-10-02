/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2023 James Seibel
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

package com.seibel.distanthorizons.forge.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
#if PRE_MC_1_19_4
import com.mojang.math.Matrix4f;
#else
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
#endif
import com.seibel.distanthorizons.common.rendering.SeamlessOverdraw;
import com.seibel.distanthorizons.common.wrappers.McObjectConverter;
import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.coreapi.util.math.Mat4f;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.FloatBuffer;

#if PRE_MC_1_17_1
import org.lwjgl.opengl.GL15;
#endif


/**
 * This class is used to mix in my rendering code
 * before Minecraft starts rendering blocks.
 * If this wasn't done, and we used Forge's
 * render last event, the LODs would render on top
 * of the normal terrain. <br><br>
 *
 * This is also the mixin for rendering the clouds
 *
 * @author coolGi
 * @author James Seibel
 * @version 12-31-2021
 */
@Mixin(LevelRenderer.class)
public class MixinLevelRenderer
{
	@Shadow
	private ClientLevel level;
	@Unique
	private static float previousPartialTicks = 0;
	
	// TODO: Is there any reason why this is here? Can it be deleted?
	public MixinLevelRenderer()
	{
		throw new NullPointerException("Null cannot be cast to non-null type.");
	}
	
	#if PRE_MC_1_17_1
	@Inject(at = @At("RETURN"), method = "renderSky(Lcom/mojang/blaze3d/vertex/PoseStack;F)V")
	private void renderSky(PoseStack matrixStackIn, float partialTicks, CallbackInfo callback)
	#else
	@Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
	public void renderClouds(PoseStack poseStack, Matrix4f projectionMatrix, float partialTicks, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) 
	#endif
	{
		// get the partial ticks since renderBlockLayer doesn't
		// have access to them
		previousPartialTicks = partialTicks;
	}
	
	
	// TODO: Can we move this to forge's client proxy similarly to how fabric does it
	#if PRE_MC_1_17_1
    @Inject(at = @At("HEAD"),
			method = "renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDD)V",
			cancellable = true)
	private void renderChunkLayer(RenderType renderType, PoseStack matrixStackIn, double xIn, double yIn, double zIn, CallbackInfo callback)
	#elif PRE_MC_1_19_4
	@Inject(at = @At("HEAD"),
			method = "renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLcom/mojang/math/Matrix4f;)V",
			cancellable = true)
	private void renderChunkLayer(RenderType renderType, PoseStack modelViewMatrixStack, double cameraXBlockPos, double cameraYBlockPos, double cameraZBlockPos, Matrix4f projectionMatrix, CallbackInfo callback)
	#elif PRE_MC_1_20_2
	@Inject(at = @At("HEAD"),
			method = "renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V",
			cancellable = true)
	private void renderChunkLayer(RenderType renderType, PoseStack modelViewMatrixStack, double cameraXBlockPos, double cameraYBlockPos, double cameraZBlockPos, Matrix4f projectionMatrix, CallbackInfo callback)
    #else
    @Inject(at = @At("HEAD"),
            method = "Lnet/minecraft/client/renderer/LevelRenderer;renderSectionLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V",
            cancellable = true)
    private void renderChunkLayer(RenderType renderType, PoseStack modelViewMatrixStack, double camX, double camY, double camZ, Matrix4f projectionMatrix, CallbackInfo callback)
    #endif
	{
		// get MC's model view and projection matrices
		#if MC_1_16_5
		// get the matrices from the OpenGL fixed pipeline
		float[] mcProjMatrixRaw = new float[16];
		GL15.glGetFloatv(GL15.GL_PROJECTION_MATRIX, mcProjMatrixRaw);
		Mat4f mcProjectionMatrix = new Mat4f(mcProjMatrixRaw);
		mcProjectionMatrix.transpose();
		
		Mat4f mcModelViewMatrix = McObjectConverter.Convert(matrixStackIn.last().pose());
		
		#else
		// get the matrices directly from MC
		Mat4f mcModelViewMatrix = McObjectConverter.Convert(modelViewMatrixStack.last().pose());
		Mat4f mcProjectionMatrix = McObjectConverter.Convert(projectionMatrix);
		#endif
		
		
		
		// only render before solid blocks
		if (renderType.equals(RenderType.solid()))
		{
			ClientApi.INSTANCE.renderLods(ClientLevelWrapper.getWrapper(level), mcModelViewMatrix, mcProjectionMatrix, previousPartialTicks);
			
			// experimental proof-of-concept option
			if (Config.Client.Advanced.Graphics.AdvancedGraphics.seamlessOverdraw.get())
			{
				float[] matrixFloatArray = SeamlessOverdraw.overwriteMinecraftNearFarClipPlanes(mcProjectionMatrix, previousPartialTicks);
				
				#if MC_1_16_5
				SeamlessOverdraw.applyLegacyProjectionMatrix(matrixFloatArray);
				#elif PRE_MC_1_19_4
				projectionMatrix.load(FloatBuffer.wrap(matrixFloatArray));
				#else
				projectionMatrix.set(matrixFloatArray);
				#endif
			}
		}
		
		if (Config.Client.Advanced.Debugging.lodOnlyMode.get())
		{
			callback.cancel();
		}
	}
	
	#if PRE_MC_1_19_4
	@Inject(at = @At(value = "TAIL", target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;runUpdates(IZZ)I"), method = "renderLevel")
	public void callAfterRunUpdates(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci)
	#elif PRE_MC_1_20_1
	@Inject(at = @At(value = "TAIL", target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;runUpdates(IZZ)I"), method = "renderLevel")
	public void callAfterRunUpdates(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci)
	#else
	@Inject(at = @At(value = "TAIL", target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;runLightUpdates()I"), method = "renderLevel")
	private void callAfterRunUpdates(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) 
	#endif
	{
		ChunkWrapper.syncedUpdateClientLightStatus();
	}
	
}
