package com.seibel.lod.fabric.mixins;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.level.chunk.ChunkGenerator;

#if MC_VERSION_1_16_5 || MC_VERSION_1_17_1
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.WorldgenRandom;

@Mixin(ChunkGenerator.class)
public class MixinChunkGenerator {
    @Redirect(method = "applyBiomeDecoration", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/biome/Biome;generate(Lnet/minecraft/world/level/StructureFeatureManager;"
                    + "Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/server/level/WorldGenRegion;J"
                    + "Lnet/minecraft/world/level/levelgen/WorldgenRandom;Lnet/minecraft/core/BlockPos;)V"

    ))
    private void wrapBiomeGenerateCall(Biome biome, StructureFeatureManager structFeatManager, ChunkGenerator generator,
                                       WorldGenRegion genRegion, long l, WorldgenRandom random, BlockPos pos) {
        synchronized(ChunkGenerator.class) {
            biome.generate(structFeatManager, (ChunkGenerator)(Object)this, genRegion, l, random, pos);
        }
    }
}

#else
@Mixin(ChunkGenerator.class)
public class MixinChunkGenerator {}
#endif