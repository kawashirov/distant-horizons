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
import com.seibel.lod.forge.wrappers.block.BlockShapeWrapper;
import com.seibel.lod.forge.wrappers.world.BiomeWrapper;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ILiquidContainer;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;

/**
 * @author ??
 * @version 11-17-2021
 */
public class ChunkWrapper implements IChunkWrapper
{
	private final IChunk chunk;
	
	private final int CHUNK_SECTION_SHIFT = 4;
	private final int CHUNK_SECTION_MASK = 0b1111;
	private final int CHUNK_SIZE_SHIFT = 4;
	private final int CHUNK_SIZE_MASK = 0b1111;
	
	@Override
	public int getHeight()
	{
		return chunk.getMaxBuildHeight();
	}
	
	@Override
	public boolean isPositionInWater(int x, int y, int z)
	{
		BlockState blockState = chunk.getSections()[y >> CHUNK_SECTION_SHIFT].getBlockState(x & CHUNK_SIZE_MASK, y & CHUNK_SECTION_MASK, z & CHUNK_SIZE_MASK);
		
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
	public BiomeWrapper getBiome(int x, int y, int z)
	{
		return BiomeWrapper.getBiomeWrapper(chunk.getBiomes().getNoiseBiome((x & CHUNK_SIZE_MASK) >> 2, y >> 2, (z & CHUNK_SIZE_MASK) >> 2));
	}
	
	@Override
	public IBlockColorWrapper getBlockColorWrapper(int x, int y, int z)
	{
		Block block = chunk.getSections()[y >> CHUNK_SECTION_SHIFT].getBlockState(x & CHUNK_SIZE_MASK, y & CHUNK_SECTION_MASK, z & CHUNK_SIZE_MASK).getBlock();
		return BlockColorWrapper.getBlockColorWrapper(block);
	}
	
	@Override
	public IBlockShapeWrapper getBlockShapeWrapper(int x, int y, int z)
	{
		Block block = chunk.getSections()[y >> CHUNK_SECTION_SHIFT].getBlockState(x & CHUNK_SIZE_MASK, y & CHUNK_SECTION_MASK, z & CHUNK_SIZE_MASK).getBlock();
		return BlockShapeWrapper.getBlockShapeWrapper(block, this, x, y, z);
	}
	
	public ChunkWrapper(IChunk chunk)
	{
		this.chunk = chunk;
	}
	
	public IChunk getChunk()
	{
		return chunk;
	}
	
	@Override
	public int getChunkPosX(){
		return chunk.getPos().x;
	}
	
	@Override
	public int getChunkPosZ(){
		return chunk.getPos().z;
	}
	
	@Override
	public int getRegionPosX(){
		return chunk.getPos().getRegionX();
	}
	
	@Override
	public int getRegionPosZ(){
		return chunk.getPos().getRegionZ();
	}
	
	@Override
	public int getMaxY(int x, int z){
		return chunk.getHeight(Heightmap.Type.MOTION_BLOCKING, x, z);
	}
	@Override
	public int getMaxX(){
		return chunk.getPos().getMaxBlockX();
	}
	@Override
	public int getMaxZ(){
		return chunk.getPos().getMaxBlockZ();
	}
	@Override
	public int getMinX(){
		return chunk.getPos().getMinBlockX();
	}
	@Override
	public int getMinZ(){
		return chunk.getPos().getMinBlockZ();
	}
	
	@Override
	public boolean isLightCorrect()
	{
		return chunk.isLightCorrect();
	}
	
	@Override
	public boolean isWaterLogged(int x, int y, int z)
	{
		BlockState blockState = chunk.getSections()[y >> CHUNK_SECTION_SHIFT].getBlockState(x & CHUNK_SIZE_MASK, y & CHUNK_SECTION_MASK, z & CHUNK_SIZE_MASK);
		
		//This type of block is always in water
		return (!(blockState.getBlock() instanceof ILiquidContainer) && (blockState.getBlock() instanceof IWaterLoggable))
					   && (blockState.hasProperty(BlockStateProperties.WATERLOGGED) && blockState.getValue(BlockStateProperties.WATERLOGGED));
	}
	
	
	@Override
	public int getEmittedBrightness(int x, int y, int z)
	{
		BlockPos blockPos = new BlockPos(x,y,z);
		return chunk.getLightEmission(blockPos);
	}
}
