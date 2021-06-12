package com.seibel.lod.builders;

import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.objects.LodRegion;
import com.seibel.lod.proxy.ClientProxy;
import com.seibel.lod.render.LodRender;
import com.seibel.lod.util.LodUtils;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.WorldWorkerManager.IWorker;

/**
 * This is used to generate a LodChunk at a given ChunkPos.
 * 
 * @author James Seibel
 * @version 6-12-2021
 */
public class LodChunkGenWorker implements IWorker
{
    private ServerWorld serverWorld;
    private ChunkPos pos;
    private LodDimension lodDim;
    private LodBuilder lodBuilder;
    private LodBufferBuilder lodBufferBuilder;
    private LodRender lodRender;
    
    public LodChunkGenWorker(ChunkPos newPos, LodRender newLodRenderer, 
    		LodBuilder newLodBuilder, LodBufferBuilder newLodBufferBuilder, 
    		LodDimension newLodDimension)
    {
        serverWorld  = LodUtils.getServerWorldFromDimension(newLodDimension.dimension);
        if (serverWorld == null)
        	throw new IllegalArgumentException("LodChunkGenWorker must have a non-null ServerWorld"); 
        	
        pos = newPos;
        lodDim = newLodDimension;
        lodBuilder = newLodBuilder;
        lodBufferBuilder = newLodBufferBuilder;
        lodRender = newLodRenderer;
    }
    
    @Override
    public boolean doWork()
    {
        if (pos != null)
        {
            int x = pos.x;
            int z = pos.z;
            
            // only generate LodChunks if they can
            // be added to the current LodDimension
            if (lodDim.regionIsInRange(pos.x / LodRegion.SIZE, pos.z / LodRegion.SIZE))
            {
                IChunk chunk;
                
                //long startTime = System.currentTimeMillis();
                chunk = serverWorld.getChunk(x, z, ChunkStatus.FEATURES);
                //long endTime = System.currentTimeMillis();
                //System.out.println(endTime - startTime + "\t" + lodBuilder.hasBlockData(chunk));
                
                
                lodBuilder.generateLodChunkAsync(chunk, ClientProxy.getLodWorld(), serverWorld);
                // this is called so that the new LOD chunk is drawn
                // after it is generated
                lodRender.regenerateLODsNextFrame();
                
                
                // useful for debugging
//                ClientProxy.LOGGER.info(lodDim.getNumberOfLods());
                
//                if (lodDim.getLodFromCoordinates(x, z) != null)
//                	ClientProxy.LOGGER.info(x + " " + z + " Success!");
//                else
//                	ClientProxy.LOGGER.info(x + " " + z);
            }
            // can be used for debugging
            //else
            //{
            //	System.out.println("Out of range " + x + " " + z);
            //}
            
            lodBufferBuilder.numberOfChunksWaitingToGenerate--;
            
            pos = null;
        }
        
        return false;
    }
    
    @Override
    public boolean hasWork()
    {
        return pos != null;
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
