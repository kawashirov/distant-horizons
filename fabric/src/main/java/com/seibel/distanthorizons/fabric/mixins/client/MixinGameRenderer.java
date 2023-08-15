package com.seibel.distanthorizons.fabric.mixins.client;

import com.seibel.distanthorizons.common.wrappers.DependencySetupDoneCheck;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
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
	// FIXME: This I think will dup multiple renderStartupEvent calls...
	@Inject(method = {"reloadShaders", "preloadUiShader"}, at = @At("TAIL"))
	public void onStartupShaders(CallbackInfo ci)
	{
		LOGGER.info("Starting up renderer (fabric)");
		if (!DependencySetupDoneCheck.isDone)
		{
			LOGGER.warn("Dependency setup is not done yet, skipping renderer this startup event!");
			return;
		}
		ClientApi.INSTANCE.rendererStartupEvent();
	}
	
	@Inject(method = "shutdownShaders", at = @At("HEAD"))
	public void onShutdownShaders(CallbackInfo ci)
	{
		LOGGER.info("Shutting down renderer (fabric)");
		if (!DependencySetupDoneCheck.isDone)
		{
			LOGGER.warn("Dependency setup is not done yet, skipping renderer this shutdown event!");
			return;
		}
		ClientApi.INSTANCE.rendererShutdownEvent();
	}
    #else
    // FIXME: on 1.16 we dont have stuff for reloading/shutting down shaders
	
	// FIXME: This I think will dup multiple renderStartupEvent calls...
	@Inject(method = {"loadEffect"}, at = @At("TAIL"))
	public void onStartupShaders(CallbackInfo ci) {
		ClientApi.INSTANCE.rendererStartupEvent();
	}
	
	@Inject(method = "shutdownEffect", at = @At("HEAD"))
	public void onShutdownShaders(CallbackInfo ci) {
		ClientApi.INSTANCE.rendererShutdownEvent();
	}
    #endif
}
