/*
 * Minecraft Forge
 * Copyright (c) 2016-2020.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.backsun.lod.proxy;

import com.backsun.lod.builders.LodBuilder;
import com.backsun.lod.objects.LodDimension;
import com.backsun.lod.objects.LodRegion;
import com.backsun.lod.renderer.LodRenderer;
import com.backsun.lod.util.LodUtils;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.WorldWorkerManager.IWorker;

/**
 * This is used to generate a LodChunk at a given ChunkPos.
 * 
 * @author James Seibel
 * @version 03-19-2021
 */
public class SingleLodChunkGenWorker implements IWorker
{
    private ServerWorld serverWorld;
    private ChunkPos pos;
    private LodDimension lodDim;
    private LodBuilder lodBuilder;
    private LodRenderer lodRenderer;
    
    public SingleLodChunkGenWorker(ChunkPos newPos, LodRenderer newLodRenderer, LodBuilder newLodBuilder, LodDimension newLodDimension)
    {
        serverWorld  = LodUtils.getServerWorldFromDimension(newLodDimension.dimension);
        pos = newPos;
        lodDim = newLodDimension;
        lodBuilder = newLodBuilder;
        lodRenderer = newLodRenderer;
    }

    @Override
    public boolean hasWork()
    {
        return pos != null;
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
                IChunk chunk = serverWorld.getChunk(x, z, ChunkStatus.EMPTY, true);
                
                chunk = serverWorld.getChunk(x, z, ChunkStatus.FULL);
                
                lodBuilder.generateLodChunkAsync((Chunk) chunk);
                // this is called so that the new LOD chunk is drawn
                // after it is generated
                lodRenderer.regenerateLODsNextFrame();
                
                // useful for debugging
                //if (lodDim.getLodFromCoordinates(x, z) != null)
                //	System.out.println(x + " " + z + " Success!");
                //else
                //	System.out.println(x + " " + z);
            }
            // can be used for debugging
            //else
            //{
            //	System.out.println("Out of range " + x + " " + z);
            //}
            
            pos = null;
        }

        if (pos == null)
        {
            return false;
        }
        return true;
    }
}
