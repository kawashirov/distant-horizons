package com.seibel.lod.forge.mixins;

import java.util.concurrent.Executor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.seibel.lod.common.wrappers.DependencySetupDoneCheck;
import com.seibel.lod.common.wrappers.worldGeneration.BatchGenerationEnvironment;

import net.minecraft.Util;

@Mixin(Util.class)
public class MixinUtilBackgroudThread
{
	
	@Inject(method = "wrapThreadWithTaskName(Ljava/lang/String;Ljava/lang/Runnable;)Ljava/lang/Runnable;",
			at = @At("HEAD"), cancellable = true)
	private static void overrideUtil$wrapThreadWithTaskName(String string, Runnable r, CallbackInfoReturnable<Runnable> ci)
	{
		if (DependencySetupDoneCheck.isDone && BatchGenerationEnvironment.isCurrentThreadDistantGeneratorThread())
		{
			// ClientApi.LOGGER.info("util wrapThreadWithTaskName(Runnable) triggered");
			ci.setReturnValue(r);
		}
	}

	@Inject(method = "backgroundExecutor", at = @At("HEAD"), cancellable = true)
	private static void overrideUtil$backgroundExecutor(CallbackInfoReturnable<Executor> ci)
	{
		if (DependencySetupDoneCheck.isDone && BatchGenerationEnvironment.isCurrentThreadDistantGeneratorThread())
		{
			// ClientApi.LOGGER.info("util backgroundExecutor triggered");
			ci.setReturnValue(Runnable::run);
		}
	}
}
