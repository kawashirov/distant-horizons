/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.forge.wrappers.chunk;

import java.util.Objects;

import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.forge.wrappers.block.BlockPosWrapper;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;


/**
 * @author James Seibel
 * @version 11-18-2021
 */
public class ChunkPosWrapper extends AbstractChunkPosWrapper
{
	private final ChunkPos chunkPos;
	
    public ChunkPosWrapper(ChunkPos newChunkPos)
    {
        this.chunkPos = newChunkPos;
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
	
    public ChunkPosWrapper()
    {
        this.chunkPos = new ChunkPos(0, 0);
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
		return chunkPos.getRegionX();
	}
	
	@Override
	public int getRegionZ()
	{
		return chunkPos.getRegionZ();
	}
	
	public ChunkPos getChunkPos()
	{
		return chunkPos;
	}
	
	
	
	@Override
	public boolean equals(Object o)
	{
		return chunkPos.equals(o);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(chunkPos);
	}
	
	@Override
	public BlockPosWrapper getWorldPosition()
	{
		BlockPos blockPos = chunkPos.getWorldPosition();
		return new BlockPosWrapper(blockPos.getX(), blockPos.getY(), blockPos.getZ());
	}
	
}
