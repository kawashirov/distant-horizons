package com.seibel.lod.forge.mixins;

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

    @Inject(at = @At("RETURN"), method = "setupFog(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/FogRenderer$FogMode;FZ)V")
    private static final void disableSetupFog(Camera camera, FogMode fogMode, float f, boolean bl, CallbackInfo callback) {
        FogType fogTypes = camera.getFluidInCamera();
        Entity entity = camera.getEntity();
        boolean isUnderWater = (entity instanceof LivingEntity) && ((LivingEntity)entity).hasEffect(MobEffects.BLINDNESS);
        if (!isUnderWater) {
            if (fogMode == FogMode.FOG_TERRAIN && fogTypes == FogType.NONE && CONFIG.client().graphics().fogQuality().getDisableVanillaFog()) {
                RenderSystem.setShaderFogStart(Float.MAX_VALUE);
                RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
            }
        }
    }
}
