package com.seibel.lod.wrappers.Chunk;

import com.seibel.lod.wrappers.Block.BlockWrapper;
import com.seibel.lod.wrappers.Block.BlockPosWrapper;
import net.minecraft.world.chunk.IChunk;

public class ChunkWrapper
{
	
	private IChunk chunk;
	private ChunkPosWrapper chunkPos;
	
	public ChunkWrapper(IChunk chunk)
	{
		this.chunk = chunk;
		this.chunkPos = new ChunkPosWrapper(chunk.getPos());
	}
	
	public BlockWrapper getBlock(BlockPosWrapper blockPos)
	{
		return BlockWrapper.getBlockWrapper(chunk.getBlockState(blockPos.getBlockPos()).getBlock());
	}
	
	public ChunkPosWrapper getChunkPos(){
		return chunkPos;
	}
	public int getEmittedBrightness(BlockPosWrapper blockPos)
	{
		return chunk.getLightEmission(blockPos.getBlockPos());
	}
}
