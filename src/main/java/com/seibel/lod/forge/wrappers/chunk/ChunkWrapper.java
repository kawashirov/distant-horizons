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

import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockColorWrapper;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockShapeWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.forge.wrappers.WrapperUtil;
import com.seibel.lod.forge.wrappers.block.BlockColorWrapper;
import com.seibel.lod.forge.wrappers.block.BlockPosWrapper;
import com.seibel.lod.forge.wrappers.block.BlockShapeWrapper;
import com.seibel.lod.forge.wrappers.world.BiomeWrapper;

import net.minecraft.block.BlockState;
import net.minecraft.block.ILiquidContainer;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.world.chunk.IChunk;

/**
 * @author ??
 * @version 11-17-2021
 */
public class ChunkWrapper implements IChunkWrapper
{
	private final IChunk chunk;
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
		return ((blockState.getBlock() instanceof ILiquidContainer) && !(blockState.getBlock() instanceof IWaterLoggable))
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
	
	public ChunkWrapper(IChunk chunk)
	{
		this.chunk = chunk;
		this.chunkPos = new ChunkPosWrapper(chunk.getPos());
	}
	
	public IChunk getChunk()
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
		return ((blockState.getBlock() instanceof ILiquidContainer) && !(blockState.getBlock() instanceof IWaterLoggable))
				|| (blockState.hasProperty(BlockStateProperties.WATERLOGGED) && blockState.getValue(BlockStateProperties.WATERLOGGED));
	}
	
	@Override
	public int getEmittedBrightness(AbstractBlockPosWrapper blockPos)
	{
		return chunk.getLightEmission(((BlockPosWrapper)blockPos).getBlockPos());
	}
}
