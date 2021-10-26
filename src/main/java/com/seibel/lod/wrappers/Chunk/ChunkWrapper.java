package com.seibel.lod.wrappers.Chunk;

import com.seibel.lod.util.LodUtil;
import com.seibel.lod.wrappers.Block.BlockWrapper;
import com.seibel.lod.wrappers.Block.BlockPosWrapper;
import com.seibel.lod.wrappers.World.BiomeWrapper;
import com.sun.javafx.scene.control.behavior.OptionalBoolean;
import net.minecraft.block.BlockState;
import net.minecraft.block.ILiquidContainer;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.loot.conditions.BlockStateProperty;
import net.minecraft.state.Property;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.world.chunk.IChunk;

import java.util.Optional;

public class ChunkWrapper
{
	
	private IChunk chunk;
	private ChunkPosWrapper chunkPos;
	
	public int getHeight(){
		return chunk.getMaxBuildHeight();
	}
	
	public boolean isPositionInWater(BlockPosWrapper blockPos)
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
	
	public int getHeightMapValue(int xRel, int zRel){
		return chunk.getOrCreateHeightmapUnprimed(LodUtil.DEFAULT_HEIGHTMAP).getFirstAvailable(xRel, zRel);
	}
	
	public BiomeWrapper getBiome(int xRel, int yAbs, int zRel)
	{
		return BiomeWrapper.getBiomeWrapper(chunk.getBiomes().getNoiseBiome(xRel >> 2, yAbs >> 2, zRel >> 2));
	}
	
	public BlockWrapper getBlock(BlockPosWrapper blockPos)
	{
		return BlockWrapper.getBlockWrapper(chunk.getBlockState(blockPos.getBlockPos()).getBlock(), this, blockPos);
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
