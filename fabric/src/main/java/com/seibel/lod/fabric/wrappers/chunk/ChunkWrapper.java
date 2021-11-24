package com.seibel.lod.fabric.wrappers.chunk;

import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockColorWrapper;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockShapeWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.fabric.wrappers.WrapperUtil;
import com.seibel.lod.fabric.wrappers.block.BlockColorWrapper;
import com.seibel.lod.fabric.wrappers.block.BlockPosWrapper;
import com.seibel.lod.fabric.wrappers.block.BlockShapeWrapper;
import com.seibel.lod.fabric.wrappers.world.BiomeWrapper;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * 
 * @author ??
 * @version 11-17-2021
 */
public class ChunkWrapper implements IChunkWrapper
{
	private final ChunkAccess chunk;
	private final ChunkPosWrapper chunkPos;
	
	@Override
	public int getHeight()
	{
		return chunk.getMaxBuildHeight();
	}
	
	@Override
	public boolean isPositionInWater(AbstractBlockPosWrapper blockPos)
	{
		BlockState blockState = chunk.getBlockState(((BlockPosWrapper) blockPos).getBlockPos());
		
		//This type of block is always in water
		return ((blockState.getBlock() instanceof LiquidBlockContainer) && !(blockState.getBlock() instanceof SimpleWaterloggedBlock))
				|| (blockState.hasProperty(BlockStateProperties.WATERLOGGED) && blockState.getValue(BlockStateProperties.WATERLOGGED));
	}
	
	@Override
	public int getHeightMapValue(int xRel, int zRel)
	{
		return chunk.getOrCreateHeightmapUnprimed(WrapperUtil.DEFAULT_HEIGHTMAP).getFirstAvailable(xRel, zRel);
	}
	
	@Override
	public BiomeWrapper getBiome(int xRel, int yAbs, int zRel)
	{
		return BiomeWrapper.getBiomeWrapper(chunk.getBiomes().getNoiseBiome(xRel >> 2, yAbs >> 2, zRel >> 2));
	}
	
	@Override
	public IBlockColorWrapper getBlockColorWrapper(AbstractBlockPosWrapper blockPos)
	{
		return BlockColorWrapper.getBlockColorWrapper(chunk.getBlockState(((BlockPosWrapper) blockPos).getBlockPos()).getBlock());
	}
	
	@Override
	public IBlockShapeWrapper getBlockShapeWrapper(AbstractBlockPosWrapper blockPos)
	{
		return BlockShapeWrapper.getBlockShapeWrapper(chunk.getBlockState(((BlockPosWrapper) blockPos).getBlockPos()).getBlock(), this, blockPos);
	}
	
	public ChunkWrapper(ChunkAccess chunk)
	{
		this.chunk = chunk;
		this.chunkPos = new ChunkPosWrapper(chunk.getPos());
	}
	
	public ChunkAccess getChunk()
	{
		return chunk;
	}
	
	@Override
	public ChunkPosWrapper getPos()
	{
		return chunkPos;
	}
	
	@Override
	public boolean isLightCorrect()
	{
		return chunk.isLightCorrect();
	}
	
	@Override
	public boolean isWaterLogged(AbstractBlockPosWrapper blockPos)
	{
		BlockState blockState = chunk.getBlockState(((BlockPosWrapper)blockPos).getBlockPos());
		
		//This type of block is always in water
		return ((blockState.getBlock() instanceof LiquidBlockContainer) && !(blockState.getBlock() instanceof SimpleWaterloggedBlock))
				|| (blockState.hasProperty(BlockStateProperties.WATERLOGGED) && blockState.getValue(BlockStateProperties.WATERLOGGED));
	}
	
	@Override
	public int getEmittedBrightness(AbstractBlockPosWrapper blockPos)
	{
		return chunk.getLightEmission(((BlockPosWrapper)blockPos).getBlockPos());
	}
}
