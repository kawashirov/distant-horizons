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


import com.mojang.blaze3d.platform.NativeImage;

import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftRenderWrapper;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.common.util.ILightTextureMarker;

import net.minecraft.client.renderer.texture.DynamicTexture;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DynamicTexture.class)
public class MixinDynamicTexture implements ILightTextureMarker
{
	/** Used to prevent accidentally using other dynamic textures as a lightmap */
	@Unique
	private boolean isLightTexture = false;
	
	@Shadow
	@Final
	private NativeImage pixels;
	
	@Inject(method = "upload()V", at = @At("HEAD"))
	public void updateLightTexture(CallbackInfo ci)
	{
		// since the light map is always updated on the client render thread we should be able to access the client level at the same time
		IMinecraftClientWrapper mc = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
		if (!this.isLightTexture
			|| mc == null
			|| mc.getWrappedClientLevel() == null
			)
		{
			return;
		}
		
		//ApiShared.LOGGER.info("Lightmap update");
		IClientLevelWrapper clientLevel = mc.getWrappedClientLevel();
		MinecraftRenderWrapper.INSTANCE.updateLightmap(this.pixels, clientLevel);
	}
	
	public void markLightTexture() { this.isLightTexture = true; }
	
}
