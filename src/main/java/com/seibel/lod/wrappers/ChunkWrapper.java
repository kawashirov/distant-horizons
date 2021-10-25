package com.seibel.lod.wrappers;

import jdk.nashorn.internal.ir.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.IChunk;

public class ChunkWrapper
{
	
	private IChunk chunk;
	
	
	public ChunkWrapper(IChunk chunk)
	{
		this.chunk = chunk;
	}
	
	public BlockWrapper getBlock(BlockPosWrapper blockPos)
	{
		return BlockWrapper.getBlockWrapper(chunk.getBlockState(blockPos.getBlockPos()).getBlock());
	}
	
	public int getEmittedBrightness(BlockPosWrapper blockPos)
	{
		return chunk.getLightEmission(blockPos.getBlockPos());
	}
}
