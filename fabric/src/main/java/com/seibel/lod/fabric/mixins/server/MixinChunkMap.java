package com.seibel.lod.fabric.mixins.server;

import com.seibel.lod.common.wrappers.chunk.ChunkWrapper;
import com.seibel.lod.common.wrappers.world.WorldWrapper;
import com.seibel.lod.core.api.internal.a7.ServerApi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(ChunkMap.class)
public class MixinChunkMap {
    static final String CHUNK_SERIALIZER_WRITE
            = "Lnet/minecraft/world/level/chunk/storage/ChunkSerializer;write(" +
            "Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;)" +
            "Lnet/minecraft/nbt/CompoundTag;";

    @Inject(method = "save", at = @At(value = "INVOKE", target = CHUNK_SERIALIZER_WRITE))
    private void onChunkSave(ServerLevel serverLevel, ChunkAccess chunkAccess, CompoundTag compoundTag) {
        ServerApi.INSTANCE.serverChunkSaveEvent(
                new ChunkWrapper(chunkAccess, serverLevel),
                WorldWrapper.getWorldWrapper(serverLevel)
        );
    }

}
