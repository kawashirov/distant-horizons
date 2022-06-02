package com.seibel.lod.fabric.mixins.server;

import com.seibel.lod.common.wrappers.chunk.ChunkWrapper;
import com.seibel.lod.common.wrappers.world.WorldWrapper;
import com.seibel.lod.core.api.internal.a7.ServerApi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkMap.class)
public class MixinChunkMap {

    @Unique
    private static final String CHUNK_SERIALIZER_WRITE
            = "Lnet/minecraft/world/level/chunk/storage/ChunkSerializer;write(" +
            "Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;)" +
            "Lnet/minecraft/nbt/CompoundTag;";

    @Shadow
    @Final
    ServerLevel level;

    @Inject(method = "save", at = @At(value = "INVOKE", target = CHUNK_SERIALIZER_WRITE))
    private void onChunkSave(ChunkAccess chunk, CallbackInfoReturnable<Boolean> ci) {
        ServerApi.INSTANCE.serverChunkSaveEvent(
                new ChunkWrapper(chunk, level),
                WorldWrapper.getWorldWrapper(level)
        );
    }

}
