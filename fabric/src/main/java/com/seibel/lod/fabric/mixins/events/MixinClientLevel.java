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
 
package com.seibel.lod.fabric.mixins.events;

import com.seibel.lod.fabric.Main;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
#if MC_VERSION_1_18_2
import net.minecraft.core.Holder;
#endif
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/**
 * This class is used for world loading events
 * @author Ran
 */

@Mixin(ClientLevel.class)
public class MixinClientLevel {
    #if MC_VERSION_1_18_2
    @Inject(method = "<init>", at = @At("TAIL"))
    private void loadWorldEvent(ClientPacketListener clientPacketListener, ClientLevel.ClientLevelData clientLevelData, ResourceKey resourceKey, Holder holder, int i, int j, Supplier supplier, LevelRenderer levelRenderer, boolean bl, long l, CallbackInfo ci) {
        Main.client_proxy.worldLoadEvent((ClientLevel) (Object) this);
    }
    #elif MC_VERSION_1_18_1
    @Inject(method = "<init>", at = @At("TAIL"))
    private void loadWorldEvent(ClientPacketListener clientPacketListener, ClientLevel.ClientLevelData clientLevelData, ResourceKey resourceKey, DimensionType dimensionType, int i, int j, Supplier supplier, LevelRenderer levelRenderer, boolean bl, long l, CallbackInfo ci) {
        Main.client_proxy.worldLoadEvent((ClientLevel) (Object) this);
    }
	#elif MC_VERSION_1_17_1
	@Inject(method = "<init>", at = @At("TAIL"))
	private void loadWorldEvent(ClientPacketListener clientPacketListener, ClientLevel.ClientLevelData clientLevelData, ResourceKey<Level> resourceKey, DimensionType dimensionType, int i, Supplier<ProfilerFiller> supplier, LevelRenderer levelRenderer, boolean bl, long l, CallbackInfo ci) {
		Main.client_proxy.worldLoadEvent((ClientLevel) (Object) this);
	}
    #endif

	#if MC_VERSION_1_18_1 || MC_VERSION_1_18_2
    @Inject(method = "setLightReady", at = @At("HEAD"))
    private void onChunkLightReady(int x, int z, CallbackInfo ci) {
    	ClientLevel l = (ClientLevel) (Object) this;
    	LevelChunk chunk = l.getChunkSource().getChunk(x, z, false);
    	if (chunk!=null&& !chunk.isClientLightReady())
    		Main.client_proxy.chunkLoadEvent(l, chunk);
    }
	#endif
}
