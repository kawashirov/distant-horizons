package com.backsun.lod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.backsun.lod.LodMain;
import com.backsun.lod.util.LodConfig;
import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldRenderer;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer
{
	private static float previousPartialTicks = 0;
	
	@Inject(at = @At("RETURN"), method = "renderSky(Lcom/mojang/blaze3d/matrix/MatrixStack;F)V", cancellable = false)
	private void renderSky(MatrixStack matrixStackIn, float partialTicks, CallbackInfo callback)
	{
		// get the partial ticks since renderBlockLayer doesn't
		// have access to them
		previousPartialTicks = partialTicks;
	}
	
	@Inject(at = @At("HEAD"), method = "renderBlockLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/matrix/MatrixStack;DDD)V", cancellable = false)
	private void renderBlockLayer(RenderType renderType, MatrixStack matrixStackIn, double xIn, double yIn, double zIn, CallbackInfo callback)
	{
		// only render if LODs are enabled and
		// only render before solid blocks
		if (LodConfig.CLIENT.drawLODs.get() && renderType == RenderType.getSolid())
			LodMain.client_proxy.renderLods(previousPartialTicks);
	}
}
