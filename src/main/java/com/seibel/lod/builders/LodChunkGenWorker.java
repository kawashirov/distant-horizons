package com.seibel.lod.builders;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.seibel.lod.enums.LodDetail;
import com.seibel.lod.handlers.LodConfigHandler;
import com.seibel.lod.objects.LodChunk;
import com.seibel.lod.objects.LodDataPoint;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.objects.LodRegion;
import com.seibel.lod.proxy.ClientProxy;
import com.seibel.lod.render.LodRender;
import com.seibel.lod.util.LodUtils;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.WorldWorkerManager.IWorker;

/**
 * This is used to generate a LodChunk at a given ChunkPos.
 * 
 * @author James Seibel
 * @version 6-19-2021
 */
public class LodChunkGenWorker implements IWorker
{
	// this seems to be faster as a singled threaded process
    private static final ExecutorService genThread = Executors.newSingleThreadExecutor();
    
    private boolean threadStarted = false;
    private LodChunkGenThread thread;
    
    
    public LodChunkGenWorker(ChunkPos newPos, LodRender newLodRenderer, 
    		LodBuilder newLodBuilder, LodBufferBuilder newLodBufferBuilder, 
    		LodDimension newLodDimension, ServerWorld newServerWorld,
    		BiomeContainer newBiomeContainer)
    {
        if (newServerWorld == null)
        	throw new IllegalArgumentException("LodChunkGenWorker must have a non-null ServerWorld"); 
        	
        thread = new LodChunkGenThread(newPos, newLodRenderer, 
        		newLodBuilder, newLodBufferBuilder, 
        		newLodDimension, newServerWorld,
        		newBiomeContainer);
    }
    
    @Override
    public boolean doWork()
    {
        if (!threadStarted)
        {
        	// make sure we don't generate this chunk again
        	thread.lodDim.addLod(new LodChunk(thread.pos));
        	
        	thread.lodBufferBuilder.numberOfChunksWaitingToGenerate--;
        	
        	if (LodConfigHandler.CLIENT.distanceBiomeOnlyGeneration.get())
			{
        		// if we are using biome only generation
        		// that can be done asynchronously
        		genThread.execute(thread);
			}
        	else
        	{
        		// if we are using normal generation that has to be done
        		// synchronously to prevent crashing and harmful
        		// interactions with the normal world generator
        		thread.run();
        	}
        	
        	threadStarted = true;
        	
    		// useful for debugging
//        	ClientProxy.LOGGER.info(lodDim.getNumberOfLods());
        }
        
        return false;
    }
    
    @Override
    public boolean hasWork()
    {
        return !threadStarted;
    }
    
    
    
    
    private class LodChunkGenThread implements Runnable
    {
    	public final ServerWorld serverWorld;
        public final LodDimension lodDim;
        public final LodBuilder lodBuilder;
        public final LodRender lodRender;
        public final BiomeContainer biomeContainer;
        private LodBufferBuilder lodBufferBuilder;
    	
    	private ChunkPos pos;
    	
    	public LodChunkGenThread(ChunkPos newPos, LodRender newLodRenderer, 
        		LodBuilder newLodBuilder, LodBufferBuilder newLodBufferBuilder, 
        		LodDimension newLodDimension, ServerWorld newServerWorld,
        		BiomeContainer newBiomeContainer)
    	{
    		pos = newPos;
    		lodRender = newLodRenderer;
    		lodBuilder = newLodBuilder;
    		lodBufferBuilder = newLodBufferBuilder;
    		lodDim = newLodDimension;
    		serverWorld = newServerWorld;
    		biomeContainer = newBiomeContainer;
    	}
    	
    	
    	
		@Override
		public void run()
		{
			// only generate LodChunks if they can
            // be added to the current LodDimension
			if (lodDim.regionIsInRange(pos.x / LodRegion.SIZE, pos.z / LodRegion.SIZE))
			{
				long startTime = System.currentTimeMillis();
				
				if (LodConfigHandler.CLIENT.distanceBiomeOnlyGeneration.get())
				{
					Chunk chunk = new Chunk(serverWorld, pos, biomeContainer);
					List<IChunk> chunkList = new LinkedList<>();
					chunkList.add(chunk);
					
					WorldGenRegion worldGenRegion = new WorldGenRegion(serverWorld, chunkList);
					Biome biome = worldGenRegion.getBiome(pos.asBlockPos());
					
					//biome.buildSurface(serverWorld.rand, chunk, pos.x, pos.z, 0, 0, Blocks.STONE.getDefaultState(), Blocks.WATER.getDefaultState(), serverWorld.getSeaLevel(), serverWorld.getSeed());
					
					// biome.buildSurface(Random random, IChunk chunkIn, int x, int z, int startHeight, double noise, BlockState defaultBlock, BlockState defaultFluid, int seaLevel, long seed)
					//chunkGen.generateSurface(worldGenRegion, chunk);
					
					// generate features
					//chunkGen.func_230351_a_(worldGenRegion, 
					//serverWorld.field_241106_P_); // StructureManager
					
					LodDataPoint[][] details = new LodDataPoint[1][1];
					Color color;
					if (biome.getCategory() == Biome.Category.OCEAN)
					{
						color = LodUtils.intToColor(biome.getWaterColor());
					}
					else if (biome.getCategory() == Biome.Category.ICY)
					{
						color = Color.WHITE;
					}
					else
					{
						color = LodUtils.intToColor(biome.getFoliageColor());
					}
					
					
					details[0][0] = new LodDataPoint(serverWorld.getSeaLevel(), 0, color);
					LodChunk lod = new LodChunk(pos, details , LodDetail.SINGLE);
					lodDim.addLod(lod);
				}
				else
				{
					lodBuilder.generateLodChunkAsync(serverWorld.getChunk(pos.x, pos.z, ChunkStatus.FEATURES), ClientProxy.getLodWorld(), serverWorld);
				}
				
				lodRender.regenerateLODsNextFrame();
				
				
//				if (lodDim.getLodFromCoordinates(pos.x, pos.z) != null)
//					ClientProxy.LOGGER.info(pos.x + " " + pos.z + " Success!");
//				else
//					ClientProxy.LOGGER.info(pos.x + " " + pos.z);
				
				long endTime = System.currentTimeMillis();
				System.out.println(endTime - startTime);
				
			}// if in range
			
		}// run
		
    }
    
    
    /*
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
     (and any other object that is needed to make biomes distinct)
     
     Otherwise if snow/trees aren't necessary SURFACE is the next fastest (although not by much)
     */
}
