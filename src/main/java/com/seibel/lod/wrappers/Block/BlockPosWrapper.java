package com.seibel.lod.wrappers.Block;

import java.util.Objects;

import com.seibel.lod.wrappers.Chunk.ChunkPosWrapper;

import net.minecraft.core.BlockPos;


//This class wraps the minecraft BlockPos.Mutable (and BlockPos) class
public class BlockPosWrapper
{
	private BlockPos.MutableBlockPos blockPos;
	
	
	public BlockPosWrapper()
	{
		this.blockPos = new BlockPos.MutableBlockPos(0,0,0);
	}
	
	public BlockPosWrapper(int x, int y, int z)
	{
		this.blockPos = new BlockPos.MutableBlockPos(x, y, z);
	}
	
	
	
	public void set(int x, int y, int z)
	{
		blockPos.set(x, y, z);
	}
	
	public int getX()
	{
		return blockPos.getX();
	}
	
	public int getY()
	{
		return blockPos.getY();
	}
	
	public int getZ()
	{
		return blockPos.getZ();
	}
	
	public BlockPos.MutableBlockPos getBlockPos()
	{
		return blockPos;
	}
	
	@Override public boolean equals(Object o)
	{
		return blockPos.equals(o);
	}
	
	@Override public int hashCode()
	{
		return Objects.hash(blockPos);
	}

	public ChunkPosWrapper offset(int x, int y, int z)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
}
