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
package com.seibel.lod.mixin;

import com.seibel.lod.proxy.ClientProxy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.seibel.lod.LodMain;
import com.seibel.lod.config.LodConfig;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldRenderer;

/**
 * This class is used to mix in my rendering code
 * before Minecraft starts rendering blocks.
 * If this wasn't done and we used Forge's 
 * render last event, the LODs would render on top
 * of the normal terrain.
 * 
 * @author James Seibel
 * @version 9-19-2021
 */
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
	
	@Inject(at = @At("HEAD"), method = "renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/matrix/MatrixStack;DDD)V", cancellable = false)
	private void renderChunkLayer(RenderType renderType, MatrixStack matrixStackIn, double xIn, double yIn, double zIn, CallbackInfo callback)
	{
		// only render if LODs are enabled and
		// only render before solid blocks
		if (ClientProxy.drawLods && renderType.equals(RenderType.solid()))
			LodMain.client_proxy.renderLods(matrixStackIn, previousPartialTicks);
	}
}
