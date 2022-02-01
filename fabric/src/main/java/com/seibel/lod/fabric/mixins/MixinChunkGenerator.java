package com.seibel.lod.fabric.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;

@Mixin(ChunkGenerator.class)
public class MixinChunkGenerator {
	@Shadow
	@Final
	protected BiomeSource biomeSource;
	
	@Overwrite
    public void applyBiomeDecoration(WorldGenRegion worldGenRegion, StructureFeatureManager structureFeatureManager) {
        int i = worldGenRegion.getCenterX();
        int j = worldGenRegion.getCenterZ();
        int k = i * 16;
        int l = j * 16;
        BlockPos blockPos = new BlockPos(k, 0, l);
        Biome biome = this.biomeSource.getNoiseBiome((i << 2) + 2, 2, (j << 2) + 2);
        WorldgenRandom worldgenRandom = new WorldgenRandom();
        long m = worldgenRandom.setDecorationSeed(worldGenRegion.getSeed(), k, l);
        try {
			synchronized(this) {
				biome.generate(structureFeatureManager, (ChunkGenerator)(Object)this, worldGenRegion, m, worldgenRandom, blockPos);
			}
        }
        catch (Exception exception) {
            CrashReport crashReport = CrashReport.forThrowable(exception, "Biome decoration");
            crashReport.addCategory("Generation").setDetail("CenterX", i).setDetail("CenterZ", j).setDetail("Seed", m).setDetail("Biome", biome);
            throw new ReportedException(crashReport);
        }
    }

}
