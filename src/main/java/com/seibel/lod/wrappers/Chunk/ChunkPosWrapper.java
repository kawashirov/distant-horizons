package com.seibel.lod.wrappers.Chunk;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.Objects;


//This class wraps the minecraft ChunkPos class
public class ChunkPosWrapper
{
	private final ChunkPos chunkPos;
	
	public ChunkPosWrapper(ChunkPos chunkPos)
	{
		this.chunkPos = chunkPos;
	}
	
	public int getX()
	{
		return chunkPos.x;
	}
	
	public int getZ()
	{
		return chunkPos.z;
	}
	
	public int getMinBlockX()
	{
		return chunkPos.getMinBlockX();
	}
	
	public int getMinBlockZ()
	{
		return chunkPos.getMinBlockZ();
	}
	
	public int getRegionX()
	{
		return chunkPos.getRegionX();
	}
	
	public int getRegionZ()
	{
		return chunkPos.getRegionZ();
	}
	
	public ChunkPos getChunkPos()
	{
		return chunkPos;
	}
	
	@Override public boolean equals(Object o)
	{
		return chunkPos.equals(o);
	}
	
	@Override public int hashCode()
	{
		return Objects.hash(chunkPos);
	}
	
}
