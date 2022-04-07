/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
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
 
package com.seibel.lod.forge.mixins.unsafe;

import net.minecraft.util.ThreadingDetector;
import org.spongepowered.asm.mixin.Mixin;

#if MC_VERSION_1_18_1 || MC_VERSION_1_18_2

import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Semaphore;

/**
 * Why does this exist? But okay! (Will be probably removed when the experimental generator is done)
 */
@Mixin(ThreadingDetector.class)
public class MixinThreadingDectector {
    @Mutable
    @Shadow
    private Semaphore lock;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void setSemaphore(CallbackInfo ci) {
        this.lock = new Semaphore(2);
    }
}
#else
@Mixin(ThreadingDetector.class)
public class MixinThreadingDectector {} //FIXME: Is there some way to make this file just not be added?
#endif