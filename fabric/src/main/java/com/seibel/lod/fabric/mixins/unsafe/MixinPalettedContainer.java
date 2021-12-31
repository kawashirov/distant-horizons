package com.seibel.lod.fabric.mixins.unsafe;

import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Semaphore;

/**
 * NOTE: THIS IS NOT A FIX TO THE PROBLEM.
 * TODO: Do/Find an actual fix to this
 *
 * @author Ran
 */
@Mixin(PalettedContainer.class)
public class MixinPalettedContainer {

    @Inject(method = "acquire", at = @At("HEAD"), cancellable = true)
    private void acquire_skip(CallbackInfo ci) {
    	ci.cancel();
    }
    @Inject(method = "release", at = @At("HEAD"), cancellable = true)
    private void release_skip(CallbackInfo ci) {
    	ci.cancel();
    }
}
