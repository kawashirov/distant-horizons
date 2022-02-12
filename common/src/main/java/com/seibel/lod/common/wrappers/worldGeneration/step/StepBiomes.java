package com.seibel.lod.common.wrappers.worldGeneration.step;

import java.util.ArrayList;
import java.util.List;

import com.seibel.lod.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.lod.common.wrappers.worldGeneration.ThreadedParameters;

import net.minecraft.core.Registry;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.blending.Blender;

public final class StepBiomes {
	/**
	 * 
	 */
	private final BatchGenerationEnvironment environment;

	/**
	 * @param batchGenerationEnvironment
	 */
	public StepBiomes(BatchGenerationEnvironment batchGenerationEnvironment)
	{
		environment = batchGenerationEnvironment;
	}

	public final ChunkStatus STATUS = ChunkStatus.BIOMES;

	//FIXME: Bug with TerraBlender Mod!
	
    private ChunkAccess createBiomes(ChunkGenerator generator, Registry<Biome> registry, Blender blender, StructureFeatureManager structureFeatureManager, ChunkAccess chunkAccess) {
        if (generator instanceof NoiseBasedChunkGenerator) {
        	((NoiseBasedChunkGenerator) generator).doCreateBiomes(registry, blender, structureFeatureManager, chunkAccess);
        	return chunkAccess;
        } else if (generator instanceof FlatLevelSource || generator instanceof DebugLevelSource) {
        	chunkAccess.fillBiomesFromNoise(generator.getBiomeSource()::getNoiseBiome, generator.climateSampler());
        	return chunkAccess;
        } else {
        	return environment.joinSync(generator.fillFromNoise(Runnable::run, blender, structureFeatureManager, chunkAccess));
        }
    }
    
	public void generateGroup(ThreadedParameters tParams, WorldGenRegion worldGenRegion,
			List<ChunkAccess> chunks) {

		ArrayList<ChunkAccess> chunksToDo = new ArrayList<ChunkAccess>();
		
		for (ChunkAccess chunk : chunks) {
			if (chunk.getStatus().isOrAfter(STATUS)) continue;
			((ProtoChunk) chunk).setStatus(STATUS);
			chunksToDo.add(chunk);
		}
		
		for (ChunkAccess chunk : chunksToDo) {
			// System.out.println("StepBiomes: "+chunk.getPos());
			chunk = createBiomes(environment.params.generator, environment.params.biomes, Blender.of(worldGenRegion),
					tParams.structFeat.forWorldGenRegion(worldGenRegion), chunk);
		}
	}
}