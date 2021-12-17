package com.seibel.lod.common.wrappers.worldGeneration;

import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.builders.lodBuilding.LodBuilderConfig;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;
import com.seibel.lod.core.wrapperInterfaces.worldGeneration.AbstractWorldGeneratorWrapper;
import com.seibel.lod.common.wrappers.chunk.ChunkWrapper;
import com.seibel.lod.common.wrappers.world.WorldWrapper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.*;

/**
 * @author James Seibel
 * @version 11-13-2021
 */
public class WorldGeneratorWrapper extends AbstractWorldGeneratorWrapper
{
    public final ServerLevel serverWorld;
    public final LodDimension lodDim;
    public final LodBuilder lodBuilder;

    public WorldGeneratorWrapper(LodBuilder newLodBuilder, LodDimension newLodDimension, IWorldWrapper worldWrapper)
    {
        super(newLodBuilder, newLodDimension, worldWrapper);

        lodBuilder = newLodBuilder;
        lodDim = newLodDimension;
        serverWorld = ((WorldWrapper) worldWrapper).getServerWorld();
    }

    /** takes about 2-5 ms */
    @Override
    public void generateBiomesOnly(AbstractChunkPosWrapper pos, DistanceGenerationMode generationMode)
    {
    	generate(pos.getX(), pos.getZ(), generationMode);
    }

    /** takes about 10 - 20 ms */
    @Override
    public void generateSurface(AbstractChunkPosWrapper pos)
    {
    	generate(pos.getX(), pos.getZ(), DistanceGenerationMode.SURFACE);
    }

    /**
     * takes about 15 - 20 ms
     */
    @Override
    public void generateFeatures(AbstractChunkPosWrapper pos)
    {
    	generate(pos.getX(), pos.getZ(), DistanceGenerationMode.FEATURES);
    }

    /**
     * Generates using MC's ServerWorld.
     * <p>
     * on pre generated chunks 0 - 1 ms <br>
     * on un generated chunks 0 - 50 ms <br>
     * with the median seeming to hover around 15 - 30 ms <br>
     * and outliers in the 100 - 200 ms range <br>
     * <p>
     * Note this should not be multithreaded and does cause server/simulation lag
     * (Higher lag for generating than loading)
     */
    @Override
    public void generateFull(AbstractChunkPosWrapper pos)
    {
    	generate(pos.getX(), pos.getZ(), DistanceGenerationMode.FULL);
    }
    
    private void generate(int chunkX, int chunkZ, DistanceGenerationMode generationMode) {
    	
    	// long t = System.nanoTime();
    	
    	ChunkStatus targetStatus;
		switch (generationMode) {
		case NONE:
			return;
		case BIOME_ONLY:
			targetStatus = ChunkStatus.BIOMES;
			break;
		case BIOME_ONLY_SIMULATE_HEIGHT:
			targetStatus = ChunkStatus.NOISE;
			break;
		case SURFACE:
			targetStatus = ChunkStatus.SURFACE;
			break;
		case FEATURES:
			targetStatus = ChunkStatus.FEATURES;
			break;
		case FULL:
			targetStatus = ChunkStatus.FULL;
			break;
		default:
			return;
		}
		
		// The bool=true means that we wants to generate chunk, and that the returned ChunkAccess must not be null
		ChunkAccess ca = serverWorld.getChunkSource().getChunk(chunkX, chunkZ, targetStatus, true);
		if (ca == null) throw new RuntimeException("This should NEVER be null due to bool being true");
		lodBuilder.generateLodNodeFromChunk(lodDim, new ChunkWrapper(ca), new LodBuilderConfig(generationMode));
		
		// long duration = System.nanoTime()-t;
		
		// Debug print the duration
		// System.out.println("LodChunkGenFull["+chunkX+","+chunkZ+"]: "+(double)(duration)/1000.);
    	
    }

	/* TODO: Update this chart
	 * performance/generation tests related to
	 * serverWorld.getChunk(x, z, ChunkStatus. *** )

     true/false is whether they generated blocks or not
     the time is how long it took to generate

     ChunkStatus.EMPTY					0  - 1  ms	false	(empty, what did you expect? :P)
     ChunkStatus.STRUCTURE_REFERENCES	1  - 2  ms  false	(no height, only generates some chunks)
     ChunkStatus.BIOMES 				1  - 10 ms	false	(no height)
     ChunkStatus.NOISE					4  - 15 ms	true	(all blocks are stone)
     ChunkStatus.LIQUID_CARVERS			6  - 12 ms	true	(no snow/trees, just grass)
     ChunkStatus.SURFACE				5  - 15 ms	true	(no snow/trees, just grass)
     ChunkStatus.CARVERS				5  - 30 ms	true	(no snow/trees, just grass)
     ChunkStatus.FEATURES				7  - 25 ms	true
     ChunkStatus.HEIGHTMAPS 			20 - 40 ms	true
     ChunkStatus.LIGHT					20 - 40 ms	true
     ChunkStatus.FULL 					30 - 50 ms	true
     ChunkStatus.SPAWN			   		50 - 80 ms	true

     At this point I would suggest using FEATURES, as it generates snow and trees
     (and any other object that are needed to make biomes distinct)

     Otherwise, if snow/trees aren't necessary SURFACE is the next fastest (although not by much)
	 */
}
