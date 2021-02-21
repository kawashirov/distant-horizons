package com.backsun.lod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.backsun.lod.renderer.RenderGlobalHook;
import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldRenderer;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer
{
	@Inject(at = @At("HEAD"), method = "renderBlockLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/matrix/MatrixStack;DDD)V", cancellable = false)
	private void renderBlockLayer(RenderType blockLayerIn, MatrixStack matrixStackIn, double xIn, double yIn, double zIn, CallbackInfo callback)
	{
		RenderGlobalHook.startRenderingStencil(blockLayerIn);
	}
}
