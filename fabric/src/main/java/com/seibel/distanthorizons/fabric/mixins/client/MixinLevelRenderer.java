/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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

package com.seibel.distanthorizons.fabric.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.core.config.Config;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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
public class MixinLevelRenderer
{
    @Shadow
    private ClientLevel level;
    @Unique
    private static float previousPartialTicks = 0;

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
//		// only render before solid blocks
//		if (renderType.equals(RenderType.solid()))
//		{
//			// get MC's current projection matrix
//			float[] mcProjMatrixRaw = new float[16];
//			GL15.glGetFloatv(GL15.GL_PROJECTION_MATRIX, mcProjMatrixRaw);
//			Mat4f mcProjectionMatrix = new Mat4f(mcProjMatrixRaw);
//			mcProjectionMatrix.transpose();
//			Mat4f mcModelViewMatrix = McObjectConverter.Convert(matrixStackIn.last().pose());
//
//			ClientApi.INSTANCE.renderLods(LevelWrapper.getWorldWrapper(level), mcModelViewMatrix, mcProjectionMatrix, previousPartialTicks);
//		}
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
//        // only render before solid blocks
//        if (renderType.equals(RenderType.solid()))
//        {
//            Mat4f mcModelViewMatrix = McObjectConverter.Convert(modelViewMatrixStack.last().pose());
//            Mat4f mcProjectionMatrix = McObjectConverter.Convert(projectionMatrix);
//
//            ClientApi.INSTANCE.renderLods(ClientLevelWrapper.getWrapper(level), mcModelViewMatrix, mcProjectionMatrix, previousPartialTicks);
//        }
        if (Config.Client.Advanced.Debugging.lodOnlyMode.get())
		{
            callback.cancel();
        }
    }
	#endif

    @Redirect(method =
                "Lnet/minecraft/client/renderer/LevelRenderer;" +
                "renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;" +
                "FJZLnet/minecraft/client/Camera;" +
                "Lnet/minecraft/client/renderer/GameRenderer;" +
                "Lnet/minecraft/client/renderer/LightTexture;" +
                "Lcom/mojang/math/Matrix4f;)V"
            ,
            at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;runUpdates(IZZ)I"
            ))
    private int callAfterRunUpdates(LevelLightEngine light, int pos, boolean isQueueEmpty, boolean updateBlockLight)
    {
        int r = light.runUpdates(pos, isQueueEmpty, updateBlockLight);
        ChunkWrapper.syncedUpdateClientLightStatus();
        return r;
    }

}