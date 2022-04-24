/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package com.seibel.lod.common.wrappers.chunk;

import java.util.Objects;

import com.seibel.lod.core.util.LevelPosUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.common.wrappers.block.BlockPosWrapper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;


/**
 * @author James Seibel
 * @version 11-21-2021
 */
public class ChunkPosWrapper extends AbstractChunkPosWrapper
{
    private final net.minecraft.world.level.ChunkPos chunkPos;

    public ChunkPosWrapper()
    {
        this.chunkPos = new ChunkPos(0, 0);
    }

    public ChunkPosWrapper(BlockPos blockPos)
    {
        this.chunkPos = new ChunkPos(blockPos);
    }

    public ChunkPosWrapper(AbstractChunkPosWrapper newChunkPos)
    {
        this.chunkPos = ((ChunkPosWrapper) newChunkPos).chunkPos;
    }

    public ChunkPosWrapper(AbstractBlockPosWrapper blockPos)
    {
        this.chunkPos = new ChunkPos(((BlockPosWrapper) blockPos).getBlockPos());
    }

    public ChunkPosWrapper(int chunkX, int chunkZ)
    {
        this.chunkPos = new ChunkPos(chunkX, chunkZ);
    }
    
    public ChunkPosWrapper(long l) {
    	this.chunkPos = new ChunkPos(l);
    }


    public ChunkPosWrapper(ChunkPos pos)
    {
        this.chunkPos = pos;
    }




    @Override
    public int getX()
    {
        return chunkPos.x;
    }

    @Override
    public int getZ()
    {
        return chunkPos.z;
    }

    @Override
    public int getMinBlockX()
    {
        return chunkPos.getMinBlockX();
    }

    @Override
    public int getMinBlockZ()
    {
        return chunkPos.getMinBlockZ();
    }

    @Override
    public int getRegionX()
    {
    	return LevelPosUtil.convert(LodUtil.CHUNK_DETAIL_LEVEL, chunkPos.x, LodUtil.REGION_DETAIL_LEVEL);
    }

    @Override
    public int getRegionZ()
    {
    	return LevelPosUtil.convert(LodUtil.CHUNK_DETAIL_LEVEL, chunkPos.z, LodUtil.REGION_DETAIL_LEVEL);
    }
    
    @Override
    public long getLong() {
    	return chunkPos.toLong();
    }

    public ChunkPos getChunkPos()
    {
        return chunkPos;
    }

    @Override
    public boolean equals(Object o)
    {
		// If the object is compared with itself then return true 
        if (o == this) {
            return true;
        }
        // Check if o is an instance of RegionPos or not
        if (!(o instanceof ChunkPosWrapper)) {
            return false;
        }
        ChunkPosWrapper c = (ChunkPosWrapper) o;
        return c.chunkPos.equals(chunkPos);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(chunkPos);
    }

    @Override
    public AbstractBlockPosWrapper getWorldPosition()
    {
        // the parameter here is the y position
        #if PRE_MC_1_17_1
        BlockPos blockPos = chunkPos.getWorldPosition();
        #else
        BlockPos blockPos = chunkPos.getMiddleBlockPosition(0);
        #endif
        return new BlockPosWrapper(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

}
