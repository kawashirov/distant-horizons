package com.seibel.lod.fabriclike.mixins.client;


import com.mojang.blaze3d.platform.NativeImage;
import com.seibel.lod.common.wrappers.minecraft.MinecraftRenderWrapper;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightTexture.class)
public class MixinLightmap {
    @Shadow
    @Final
    public NativeImage lightPixels;

    @Inject(method="updateLightTexture", at=@At(
            value="INVOKE",
            target="Lnet/minecraft/client/renderer/texture/DynamicTexture;upload()V"))
    public void updateLightTexture(float f, CallbackInfo ci) {
        //ApiShared.LOGGER.info("Lightmap update");
        MinecraftRenderWrapper.INSTANCE.updateLightmap(lightPixels);
    }
}
