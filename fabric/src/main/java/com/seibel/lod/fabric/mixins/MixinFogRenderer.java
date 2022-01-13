package com.seibel.lod.fabric.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.seibel.lod.common.wrappers.minecraft.MinecraftRenderWrapper;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;

@Mixin(FogRenderer.class)
public class MixinFogRenderer {
    private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);

	// Using this instead of Float.MAX_VALUE because Sodium don't like it.
	private static final float A_REALLY_REALLY_BIG_VALUE = 420694206942069.F;
	private static final float A_EVEN_LARGER_VALUE = 42069420694206942069.F;
	
	@Inject(at = @At("RETURN"), method = "setupFog(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/FogRenderer$FogMode;FZ)V")
	private static final void disableSetupFog(Camera camera, FogMode fogMode, float f, boolean bl, CallbackInfo callback) {
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
}
