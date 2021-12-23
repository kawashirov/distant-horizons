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

package com.seibel.lod.fabric.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.seibel.lod.common.wrappers.McObjectConverter;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.objects.math.Mat4f;

import net.minecraft.client.renderer.RenderType;

/**
 * This class is used to mix in my rendering code
 * before Minecraft starts rendering blocks.
 * If this wasn't done, and we used Forge's
 * render last event, the LODs would render on top
 * of the normal terrain.
 *
 * @author coolGi2007
 * @author James Seibel
 * @version 11-21-2021
 */
@Mixin(LevelRenderer.class)
public class MixinWorldRenderer
{
	private static float previousPartialTicks = 0;

	@Inject(at = @At("RETURN"), method = "renderClouds(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/math/Matrix4f;FDDD)V")
	private void renderClouds(PoseStack modelViewMatrixStack, Matrix4f projectionMatrix, float partialTicks, double cameraXBlockPos, double cameraYBlockPos, double cameraZBlockPos, CallbackInfo callback)
	{
		// get the partial ticks since renderChunkLayer doesn't
		// have access to them
		previousPartialTicks = partialTicks;
	}

	// HEAD or RETURN
	@Inject(at = @At("HEAD"), method = "renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLcom/mojang/math/Matrix4f;)V")
	private void renderChunkLayer(RenderType renderType, PoseStack modelViewMatrixStack, double cameraXBlockPos, double cameraYBlockPos, double cameraZBlockPos, Matrix4f projectionMatrix, CallbackInfo callback)
	{
		// only render before solid blocks
		if (renderType.equals(RenderType.solid()))
		{
			Mat4f mcModelViewMatrix = McObjectConverter.Convert(modelViewMatrixStack.last().pose());
			Mat4f mcProjectionMatrix = McObjectConverter.Convert(projectionMatrix);

			ClientApi.INSTANCE.renderLods(mcModelViewMatrix, mcProjectionMatrix, previousPartialTicks);
		}
	}
}
