package com.seibel.lod.wrappers.Chunk;

import com.seibel.lod.util.LodUtil;
import com.seibel.lod.wrappers.Block.BlockColorWrapper;
import com.seibel.lod.wrappers.Block.BlockPosWrapper;
import com.seibel.lod.wrappers.Block.BlockShapeWrapper;
import com.seibel.lod.wrappers.World.BiomeWrapper;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;

public class ChunkWrapper
{
	
	private ChunkAccess chunk;
	private ChunkPosWrapper chunkPos;
	
	public int getHeight(){
		return chunk.getMaxBuildHeight();
	}
	
	public boolean isPositionInWater(BlockPosWrapper blockPos)
	{
		BlockState blockState = chunk.getBlockState(blockPos.getBlockPos());
		
		//This type of block is always in water
		if((blockState.getBlock() instanceof LiquidBlock) && !(blockState.getBlock() instanceof IWaterLoggable))
			return true;
		
		//This type of block could be in water
		if(blockState.getOptionalValue(BlockStateProperties.WATERLOGGED).isPresent() && blockState.getOptionalValue(BlockStateProperties.WATERLOGGED).get())
			return true;
		
		return false;
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
		return BlockColorWrapper.getBlockColorWrapper(chunk.getBlockState(blockPos.getBlockPos()),blockPos);
	}
	
	public BlockShapeWrapper getBlockShapeWrapper(BlockPosWrapper blockPos)
	{
		return BlockShapeWrapper.getBlockShapeWrapper(chunk.getBlockState(blockPos.getBlockPos()).getBlock(), this, blockPos);
	}
	
	public ChunkWrapper(ChunkAccess chunk)
	{
		this.chunk = chunk;
		this.chunkPos = new ChunkPosWrapper(chunk.getPos());
	}
	
	public ChunkAccess getChunk(){
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
		if((blockState.getBlock() instanceof ILiquidContainer) && !(blockState.getBlock() instanceof IWaterLoggable))
			return true;
		
		//This type of block could be in water
		if(blockState.getOptionalValue(BlockStateProperties.WATERLOGGED).isPresent() && blockState.getOptionalValue(BlockStateProperties.WATERLOGGED).get())
			return true;
		
		return false;
	}
	
	public int getEmittedBrightness(BlockPosWrapper blockPos)
	{
		return chunk.getLightEmission(blockPos.getBlockPos());
	}
}
