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
 
package com.seibel.lod.fabric.mixins.events;

import com.seibel.lod.fabric.Main;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProgressListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This class is used for world saving events
 * @author Ran
 */
@Mixin(ServerLevel.class)
@Deprecated // TODO: Not sure if this is needed anymore
public class MixinServerLevel {
//    #if PRE_MC_1_17_1
//    @Inject(method = "save", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache;save(Z)V", shift = At.Shift.AFTER))
//    private void saveWorldEvent(ProgressListener progressListener, boolean bl, boolean bl2, CallbackInfo ci) {
//        Main.client_proxy.worldSaveEvent();
//    }
//    #else
//    @Inject(method = "save", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;saveAll()V", shift = At.Shift.AFTER))
//    private void saveWorldEvent_sA(ProgressListener progressListener, boolean bl, boolean bl2, CallbackInfo ci) {
//        Main.client_proxy.worldSaveEvent();
//    }
//    @Inject(method = "save", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;autoSave()V", shift = At.Shift.AFTER))
//    private void saveWorldEvent_aS(ProgressListener progressListener, boolean bl, boolean bl2, CallbackInfo ci) {
//        Main.client_proxy.worldSaveEvent();
//    }
//    #endif
}
