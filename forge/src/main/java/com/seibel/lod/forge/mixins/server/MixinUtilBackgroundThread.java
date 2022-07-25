/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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
 
package com.seibel.lod.forge.mixins.server;

import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.seibel.lod.common.wrappers.DependencySetupDoneCheck;
import com.seibel.lod.core.util.DummyRunExecutorService;

import net.minecraft.Util;

@Mixin(Util.class)
public class MixinUtilBackgroundThread
{
	
	private static boolean shouldApplyOverride() {
		return DependencySetupDoneCheck.getIsCurrentThreadDistantGeneratorThread.get();
	}

	@Inject(method = "backgroundExecutor", at = @At("HEAD"), cancellable = true)
	private static void overrideUtil$backgroundExecutor(CallbackInfoReturnable<ExecutorService> ci)
	{
		if (DependencySetupDoneCheck.isDone && shouldApplyOverride())
		{
			//ApiShared.LOGGER.info("util backgroundExecutor triggered");
			ci.setReturnValue(new DummyRunExecutorService());
		}
	}

	#if POST_MC_1_17_1
	@Inject(method = "wrapThreadWithTaskName(Ljava/lang/String;Ljava/lang/Runnable;)Ljava/lang/Runnable;",
			at = @At("HEAD"), cancellable = true)
	private static void overrideUtil$wrapThreadWithTaskName(String string, Runnable r, CallbackInfoReturnable<Runnable> ci)
	{
		if (DependencySetupDoneCheck.isDone && shouldApplyOverride())
		{
			//ApiShared.LOGGER.info("util wrapThreadWithTaskName(Runnable) triggered");
			ci.setReturnValue(r);
		}
	}
	#endif
	#if POST_MC_1_18_1
	@Inject(method = "wrapThreadWithTaskName(Ljava/lang/String;Ljava/util/function/Supplier;)Ljava/util/function/Supplier;",
			at = @At("HEAD"), cancellable = true)
	private static void overrideUtil$wrapThreadWithTaskNameForSupplier(String string, Supplier<?> r, CallbackInfoReturnable<Supplier<?>> ci)
	{
		if (DependencySetupDoneCheck.isDone && shouldApplyOverride())
		{
			//ApiShared.LOGGER.info("util wrapThreadWithTaskName(Supplier) triggered");
			ci.setReturnValue(r);
		}
	}
	#endif

}
