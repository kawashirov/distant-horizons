package com.seibel.lod.fabric.mixins.client;

import com.seibel.lod.core.api.internal.a7.ClientApi;
import com.seibel.lod.core.api.internal.a7.SharedApi;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Inject(method = "shutdownShaders", at = @At("HEAD"))
    public void onShutdownShaders(CallbackInfo ci) {
        SharedApi.LOGGER.info("Shutting down renderer");
        ClientApi.INSTANCE.rendererShutdownEvent();
    }

    //FIXME: This I think will dup multiple renderStartupEvent calls...
    @Inject(method = {"reloadShaders", "preloadUiShader", "preloadShader"}, at = @At("TAIL"))
    public void onStartupShaders(CallbackInfo ci) {
        SharedApi.LOGGER.info("Starting up renderer");
        ClientApi.INSTANCE.rendererStartupEvent();
    }
}
