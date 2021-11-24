package com.seibel.lod.fabric.wrappers.chunk;

import java.util.Objects;

import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.fabric.wrappers.block.BlockPosWrapper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;


/**
 * This class wraps minecraft's ChunkPos class
 * 
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
