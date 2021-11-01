package com.seibel.lod.wrappers.Chunk;

import com.seibel.lod.util.LodUtil;
import com.seibel.lod.wrappers.Block.BlockColorWrapper;
import com.seibel.lod.wrappers.Block.BlockPosWrapper;
import com.seibel.lod.wrappers.Block.BlockShapeWrapper;
import com.seibel.lod.wrappers.World.BiomeWrapper;
import net.minecraft.block.BlockState;
import net.minecraft.block.ILiquidContainer;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.world.chunk.IChunk;

public class ChunkWrapper
{
	
	private final IChunk chunk;
	private final ChunkPosWrapper chunkPos;
	
	public int getHeight(){
		return chunk.getMaxBuildHeight();
	}
	
	public boolean isPositionInWater(BlockPosWrapper blockPos)
	{
		BlockState blockState = chunk.getBlockState(blockPos.getBlockPos());
		
		//This type of block is always in water
		return ((blockState.getBlock() instanceof ILiquidContainer) && !(blockState.getBlock() instanceof IWaterLoggable))
				|| (blockState.hasProperty(BlockStateProperties.WATERLOGGED) && blockState.getValue(BlockStateProperties.WATERLOGGED).booleanValue());
	}
	
	public int getHeightMapValue(int xRel, int zRel){
		return chunk.getOrCreateHeightmapUnprimed(LodUtil.DEFAULT_HEIGHTMAP).getFirstAvailable(xRel, zRel);
	}
	
	public BiomeWrapper getBiome(int xRel, int yAbs, int zRel)
	{
		return BiomeWrapper.getBiomeWrapper(chunk.getBiomes().getNoiseBiome(xRel >> 2, yAbs >> 2, zRel >> 2));
	}
	
	public BlockColorWrapper getBlockColorWrapper(BlockPosWrapper blockPos)
	{
		return BlockColorWrapper.getBlockColorWrapper(chunk.getBlockState(blockPos.getBlockPos()).getBlock());
	}
	
	public BlockShapeWrapper getBlockShapeWrapper(BlockPosWrapper blockPos)
	{
		return BlockShapeWrapper.getBlockShapeWrapper(chunk.getBlockState(blockPos.getBlockPos()).getBlock(), this, blockPos);
	}
	
	public ChunkWrapper(IChunk chunk)
	{
		this.chunk = chunk;
		this.chunkPos = new ChunkPosWrapper(chunk.getPos());
	}
	
	public IChunk getChunk(){
		return chunk;
	}
	public ChunkPosWrapper getPos(){
		return chunkPos;
	}
	
	public boolean isLightCorrect(){
		return chunk.isLightCorrect();
	}
	
	public boolean
	isWaterLogged(BlockPosWrapper blockPos)
	{
		BlockState blockState = chunk.getBlockState(blockPos.getBlockPos());
		
		//This type of block is always in water
		return ((blockState.getBlock() instanceof ILiquidContainer) && !(blockState.getBlock() instanceof IWaterLoggable))
					   || (blockState.hasProperty(BlockStateProperties.WATERLOGGED) && blockState.getValue(BlockStateProperties.WATERLOGGED).booleanValue());
	}
	
	public int getEmittedBrightness(BlockPosWrapper blockPos)
	{
		return chunk.getLightEmission(blockPos.getBlockPos());
	}
}
