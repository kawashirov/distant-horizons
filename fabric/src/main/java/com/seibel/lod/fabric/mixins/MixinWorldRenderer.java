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

package com.seibel.lod.fabric.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.seibel.lod.common.wrappers.McObjectConverter;
import com.seibel.lod.core.Config;
import com.seibel.lod.core.api.internal.ClientApi;
import com.seibel.lod.core.objects.math.Mat4f;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import org.lwjgl.opengl.GL15;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This class is used to mix in my rendering code
 * before Minecraft starts rendering blocks.
 * If this wasn't done, and we used Forge's
 * render last event, the LODs would render on top
 * of the normal terrain.
 *
 * This is also the mixin for rendering the clouds
 *
 * @author coolGi
 * @author James Seibel
 * @version 12-31-2021
 */
@Mixin(LevelRenderer.class)
public class MixinWorldRenderer
{
	private static float previousPartialTicks = 0;

	public MixinWorldRenderer() {
		throw new NullPointerException("Null cannot be cast to non-null type.");
	}


	// Inject rendering at first call to renderChunkLayer
	// HEAD or RETURN
	#if PRE_MC_1_17_1
	@Inject(at = @At("RETURN"), method = "renderSky(Lcom/mojang/blaze3d/vertex/PoseStack;F)V")
	private void renderSky(PoseStack matrixStackIn, float partialTicks, CallbackInfo callback)
	{
		// get the partial ticks since renderBlockLayer doesn't
		// have access to them
		previousPartialTicks = partialTicks;
	}

	@Inject(at = @At("HEAD"),
			method = "renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDD)V",
			cancellable = true)
	private void renderChunkLayer(RenderType renderType, PoseStack matrixStackIn, double xIn, double yIn, double zIn, CallbackInfo callback)
	{
		// only render before solid blocks
		if (renderType.equals(RenderType.solid()))
		{
			// get MC's current projection matrix
			float[] mcProjMatrixRaw = new float[16];
			GL15.glGetFloatv(GL15.GL_PROJECTION_MATRIX, mcProjMatrixRaw);
			Mat4f mcProjectionMatrix = new Mat4f(mcProjMatrixRaw);
			mcProjectionMatrix.transpose();
			Mat4f mcModelViewMatrix = McObjectConverter.Convert(matrixStackIn.last().pose());

			ClientApi.INSTANCE.renderLods(mcModelViewMatrix, mcProjectionMatrix, previousPartialTicks);
		}
		if (Config.Client.Advanced.lodOnlyMode.get()) {
			callback.cancel();
		}
	}
	#else
	@Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
	public void renderClouds(PoseStack poseStack, Matrix4f projectionMatrix, float tickDelta, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
		// get the partial ticks since renderChunkLayer doesn't
		// have access to them
		previousPartialTicks = tickDelta;
	}

	@Inject(at = @At("HEAD"),
			method = "renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLcom/mojang/math/Matrix4f;)V",
			cancellable = true)
	private void renderChunkLayer(RenderType renderType, PoseStack modelViewMatrixStack, double cameraXBlockPos, double cameraYBlockPos, double cameraZBlockPos, Matrix4f projectionMatrix, CallbackInfo callback)
	{
		// only render before solid blocks
		if (renderType.equals(RenderType.solid()))
		{
			Mat4f mcModelViewMatrix = McObjectConverter.Convert(modelViewMatrixStack.last().pose());
			Mat4f mcProjectionMatrix = McObjectConverter.Convert(projectionMatrix);

			ClientApi.INSTANCE.renderLods(mcModelViewMatrix, mcProjectionMatrix, previousPartialTicks);
		}
		if (Config.Client.Advanced.lodOnlyMode.get()) {
			callback.cancel();
		}
	}
	#endif
}
