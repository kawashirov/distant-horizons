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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
#if MC_VERSION_1_16_5
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
#else
import net.minecraft.world.level.material.FogType;
#endif

@Mixin(FogRenderer.class)
public class MixinFogRenderer {
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	
	// Using this instead of Float.MAX_VALUE because Sodium don't like it.
	private static final float A_REALLY_REALLY_BIG_VALUE = 420694206942069.F;
	private static final float A_EVEN_LARGER_VALUE = 42069420694206942069.F;

	#if MC_VERSION_1_16_5

	@Inject(at = @At("RETURN"), method = "setupFog(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/FogRenderer$FogMode;FZ)V")
	private static void disableSetupFog(Camera camera, FogMode fogMode, float f, boolean bl, CallbackInfo callback) {
		FluidState fluidState = camera.getFluidInCamera();
		Entity entity = camera.getEntity();
		boolean isUnderWater = (entity instanceof LivingEntity) && ((LivingEntity)entity).hasEffect(MobEffects.BLINDNESS);
		isUnderWater |= fluidState.is(FluidTags.WATER);
		isUnderWater |= fluidState.is(FluidTags.LAVA);

		if (!isUnderWater) {
			if (fogMode == FogMode.FOG_TERRAIN && CONFIG.client().graphics().fogQuality().getDisableVanillaFog()) {
				RenderSystem.fogStart(A_REALLY_REALLY_BIG_VALUE);
				RenderSystem.fogEnd(A_EVEN_LARGER_VALUE);
			}
		}
	}

	#else
	@Inject(at = @At("RETURN"), method = "setupFog(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/FogRenderer$FogMode;FZ)V")
	private static void disableSetupFog(Camera camera, FogMode fogMode, float f, boolean bl, CallbackInfo callback) {
	    FogType fogTypes = camera.getFluidInCamera();
	    Entity entity = camera.getEntity();
	    boolean isUnderWater = (entity instanceof LivingEntity) && ((LivingEntity)entity).hasEffect(MobEffects.BLINDNESS);
	    if (!isUnderWater) {
			if (fogMode == FogMode.FOG_TERRAIN && fogTypes == FogType.NONE && CONFIG.client().graphics().fogQuality().getDisableVanillaFog()) {
			    RenderSystem.setShaderFogStart(A_REALLY_REALLY_BIG_VALUE);
			    RenderSystem.setShaderFogEnd(A_EVEN_LARGER_VALUE);
			}
	    }
	}
	#endif

}
