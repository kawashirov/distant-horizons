/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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

import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import com.seibel.lod.fabric.ClientProxy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.seibel.lod.core.util.DummyRunExecutorService;

import net.minecraft.Util;

@Mixin(Util.class)
public class MixinUtilBackgroudThread
{

	#if !MC_VERSION_1_16_5
	@Inject(method = "wrapThreadWithTaskName(Ljava/lang/String;Ljava/lang/Runnable;)Ljava/lang/Runnable;",
			at = @At("HEAD"), cancellable = true)
	private static void overrideUtil$wrapThreadWithTaskName(String string, Runnable r, CallbackInfoReturnable<Runnable> ci)
	{
		if (ClientProxy.isGenerationThreadChecker != null && ClientProxy.isGenerationThreadChecker.get())
		{
			//ApiShared.LOGGER.info("util wrapThreadWithTaskName(Runnable) triggered");
			ci.setReturnValue(r);
		}
	}
	#endif
	#if MC_VERSION_1_18_1 || MC_VERSION_1_18_2
	@Inject(method = "wrapThreadWithTaskName(Ljava/lang/String;Ljava/util/function/Supplier;)Ljava/util/function/Supplier;",
			at = @At("HEAD"), cancellable = true)
	private static void overrideUtil$wrapThreadWithTaskNameForSupplier(String string, Supplier<?> r, CallbackInfoReturnable<Supplier<?>> ci)
	{
		if (ClientProxy.isGenerationThreadChecker != null && ClientProxy.isGenerationThreadChecker.get())
		{
			//ApiShared.LOGGER.info("util wrapThreadWithTaskName(Supplier) triggered");
			ci.setReturnValue(r);
		}
	}
	#endif
	
	@Inject(method = "backgroundExecutor", at = @At("HEAD"), cancellable = true)
	private static void overrideUtil$backgroundExecutor(CallbackInfoReturnable<ExecutorService> ci)
	{
		if (ClientProxy.isGenerationThreadChecker != null && ClientProxy.isGenerationThreadChecker.get())
		{
			//ApiShared.LOGGER.info("util backgroundExecutor triggered");
			ci.setReturnValue(new DummyRunExecutorService());
		}
	}
}
