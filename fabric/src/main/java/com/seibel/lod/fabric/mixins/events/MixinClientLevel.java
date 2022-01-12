package com.seibel.lod.fabric.mixins.events;

import com.seibel.lod.fabric.Main;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
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
    @Inject(method = "<init>", at = @At("TAIL"))
    private void loadWorldEvent(ClientPacketListener clientPacketListener, ClientLevel.ClientLevelData clientLevelData, ResourceKey resourceKey, DimensionType dimensionType, int i, int j, Supplier supplier, LevelRenderer levelRenderer, boolean bl, long l, CallbackInfo ci) {
        Main.client_proxy.worldLoadEvent((ClientLevel) (Object) this);
    }
    @Inject(method = "setLightReady", at = @At("HEAD"))
    private void onChunkLightReady(int x, int z, CallbackInfo ci) {
    	ClientLevel l = (ClientLevel) (Object) this;
    	LevelChunk chunk = l.getChunkSource().getChunk(x, z, false);
    	if (chunk!=null && !chunk.isClientLightReady())
    		Main.client_proxy.chunkLoadEvent(l, chunk);
    }
}
