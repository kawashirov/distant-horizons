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

package com.seibel.lod.forge.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.seibel.lod.common.Config;
import com.seibel.lod.common.clouds.CloudTexture;
import com.seibel.lod.common.clouds.NoiseCloudHandler;
import com.seibel.lod.common.wrappers.McObjectConverter;
import net.minecraft.client.renderer.LevelRenderer;
import org.lwjgl.opengl.GL15;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.objects.math.Mat4f;

import net.minecraft.client.renderer.RenderType;


import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;

import java.util.Random;

/**
 * This class is used to mix in my rendering code
 * before Minecraft starts rendering blocks.
 * If this wasn't done, and we used Forge's
 * render last event, the LODs would render on top
 * of the normal terrain.
 *
 * This is also the mixin for rendering the clouds
 *
 * @author coolGi2007
 * @author James Seibel
 * @version 12-31-2021
 */
@Mixin(LevelRenderer.class)
public class MixinWorldRenderer
{
	// TODO: Fix clouds
	private static float previousPartialTicks = 0;


	@Inject(at = @At("RETURN"), method = "renderSky(Lcom/mojang/blaze3d/vertex/PoseStack;F)V")
	private void renderSky(PoseStack matrixStackIn, float partialTicks, CallbackInfo callback)
	{
		// get the partial ticks since renderBlockLayer doesn't
		// have access to them
		previousPartialTicks = partialTicks;
	}

	// HEAD or RETURN
	@Inject(at = @At("HEAD"), method = "renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDD)V")
	private void renderChunkLayer(RenderType renderType, PoseStack matrixStackIn, double xIn, double yIn, double zIn, CallbackInfo callback)
	{
		// only render before solid blocks
		if (renderType.equals(RenderType.solid()))
		{
			// get MC's current projection matrix
			float[] mcProjMatrixRaw = new float[16];
			GL15.glGetFloatv(GL15.GL_PROJECTION_MATRIX, mcProjMatrixRaw);
			Mat4f mcProjectionMatrix = new Mat4f(mcProjMatrixRaw);
			// OpenGl outputs their matrices in col,row form instead of row,col
			// (or maybe vice versa I have no idea :P)
			mcProjectionMatrix.transpose();


			Mat4f mcModelViewMatrix = McObjectConverter.Convert(matrixStackIn.last().pose());

			ClientApi.INSTANCE.renderLods(mcModelViewMatrix, mcProjectionMatrix, previousPartialTicks);
		}
	}
}
