package com.seibel.lod.forge.mixins;

import org.spongepowered.asm.mixin.Mixin;

#if MC_VERSION_1_16_5

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.terraforged.mod.chunk.generator.FeatureGenerator;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

@Mixin(FeatureGenerator.class)
public class MixinTFChunkGenerator {

	@Redirect(method = "decorate("
			+ "Lnet/minecraft/world/level/StructureFeatureManager;"
			+ "Lnet/minecraft/world/level/WorldGenLevel;"
			+ "Lnet/minecraft/world/level/chunk/ChunkAccess;"
			+ "Lnet/minecraft/world/level/biome/Biome;"
			+ "Lnet/minecraft/core/BlockPos;"
			+ "Lcom/terraforged/mod/profiler/watchdog/WatchdogContext;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/feature/ConfiguredFeature;place("
					+ "Lnet/minecraft/world/level/WorldGenLevel;"
					+ "Lnet/minecraft/world/level/chunk/ChunkGenerator;"
					+ "Ljava/util/Random;Lnet/minecraft/core/BlockPos;)Z"
		))
	private boolean wrapDecorate$FeaturePlace(ConfiguredFeature<?, ?> feature, WorldGenLevel arg,
			ChunkGenerator arg2, Random random, BlockPos arg3) {
		synchronized(FeatureGenerator.class) {
			//ClientApi.LOGGER.info("wrapDecorate FeaturePlace triggered");
			return feature.place(arg, arg2, random, arg3);
		}
	}

	//METHOD: com.terraforged.mod.chunk.generator.FeatureGenerator.decorate(StructureFeatureManager manager,
	// WorldGenLevel region, ChunkAccess chunk, Biome biome, BlockPos pos, WatchdogContext context)

	//TARGET: boolean net.minecraft.world.level.levelgen.feature.ConfiguredFeature.place
	// (WorldGenLevel arg, ChunkGenerator arg2, Random random, BlockPos arg3)
}

#else
class MixinTFChunkGenerator {
}
#endif