package com.seibel.lod.mixins.client;

import com.seibel.lod.core.api.internal.ClientApi;
import net.minecraft.client.renderer.GameRenderer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer
{
	private static final Logger LOGGER = LogManager.getLogger(MixinGameRenderer.class.getSimpleName());
	
	
	#if POST_MC_1_17_1
    @Inject(method = "shutdownShaders", at = @At("HEAD"))
    public void onShutdownShaders(CallbackInfo ci) {
        LOGGER.info("Shutting down renderer");
        ClientApi.INSTANCE.rendererShutdownEvent();
    }

    // FIXME: This I think will dup multiple renderStartupEvent calls...
    @Inject(method = {"reloadShaders", "preloadUiShader", "preloadShader"}, at = @At("TAIL"))
    public void onStartupShaders(CallbackInfo ci) {
        LOGGER.info("Starting up renderer");
        ClientApi.INSTANCE.rendererStartupEvent();
    }
    #else
    // FIXME: on 1.16 we dont have stuff for reloading/shutting down shaders

    @Inject(method = "shutdownShaders", at = @At("HEAD"))
    public void onShutdownShaders(CallbackInfo ci) {
        LOGGER.info("Shutting down renderer");
        ClientApi.INSTANCE.rendererShutdownEvent();
    }

    // FIXME: This I think will dup multiple renderStartupEvent calls...
    @Inject(method = {"reloadShaders", "preloadUiShader", "preloadShader"}, at = @At("TAIL"))
    public void onStartupShaders(CallbackInfo ci) {
        LOGGER.info("Starting up renderer");
        ClientApi.INSTANCE.rendererStartupEvent();
    }
    #endif
}
