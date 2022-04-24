/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package com.seibel.lod.common.wrappers.chunk;

import com.seibel.lod.common.wrappers.block.BlockDetailWrapper;
import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.util.LevelPosUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockDetailWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;

import com.seibel.lod.common.wrappers.WrapperUtil;
import com.seibel.lod.common.wrappers.block.BlockDetailMap;
import com.seibel.lod.common.wrappers.world.BiomeWrapper;
import com.seibel.lod.common.wrappers.worldGeneration.mimicObject.LightedWorldGenRegion;

import net.minecraft.core.BlockPos;
#if POST_MC_1_17_1
import net.minecraft.core.QuartPos;
#endif
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 *
 * @author James Seibel
 * @version 3-5-2022
 */
public class ChunkWrapper implements IChunkWrapper
{
	private final ChunkAccess chunk;
	private final LevelReader lightSource;
	
	
	public ChunkWrapper(ChunkAccess chunk, LevelReader lightSource)
	{
		this.chunk = chunk;
		this.lightSource = lightSource;
	}
	
	@Override
	public int getHeight(){
		#if PRE_MC_1_17_1
		return 255;
		#else
		return chunk.getHeight();
		#endif
	}
	
	@Override
	public int getMinBuildHeight()
	{
		#if PRE_MC_1_17_1
		return 0;
		#else
		return chunk.getMinBuildHeight();
		#endif
	}
	@Override
	public int getMaxBuildHeight()
	{
		return chunk.getMaxBuildHeight();
	}
	
	@Override
	public int getHeightMapValue(int xRel, int zRel)
	{
		return chunk.getOrCreateHeightmapUnprimed(WrapperUtil.DEFAULT_HEIGHTMAP).getFirstAvailable(xRel, zRel);
	}
	
	@Override
	public IBiomeWrapper getBiome(int x, int y, int z)
	{
		#if PRE_MC_1_17_1
		return BiomeWrapper.getBiomeWrapper(chunk.getBiomes().getNoiseBiome(
				x >> 2, y >> 2, z >> 2));
		#elif PRE_MC_1_18_1
		return BiomeWrapper.getBiomeWrapper(chunk.getBiomes().getNoiseBiome(
				QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z)));
		#elif PRE_MC_1_18_2
		return BiomeWrapper.getBiomeWrapper(chunk.getNoiseBiome(
				QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z)));
		#else
		return BiomeWrapper.getBiomeWrapper(chunk.getNoiseBiome(
				QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z)).value());
		#endif
	}
	
	@Override
	public IBlockDetailWrapper getBlockDetail(int x, int y, int z) {
		BlockPos pos = new BlockPos(x,y,z);
		BlockState blockState = chunk.getBlockState(pos);
		IBlockDetailWrapper blockDetail = BlockDetailMap.getOrMakeBlockDetailCache(blockState, pos, lightSource);
		return blockDetail == BlockDetailWrapper.NULL_BLOCK_DETAIL ? null : blockDetail;
	}

    @Override
    public IBlockDetailWrapper getBlockDetailAtFace(int x, int y, int z, LodDirection dir) {
        int fy = y+dir.getNormal().y;
        if (fy < getMinBuildHeight() || fy > getMaxBuildHeight()) return null;
        BlockPos pos = new BlockPos(x+dir.getNormal().x,fy,z+dir.getNormal().z);
        BlockState blockState;
        if (blockPosInsideChunk(x,y,z))
            blockState = chunk.getBlockState(pos);
        else {
            blockState = lightSource.getBlockState(pos);
        }
        if (blockState == null || blockState.isAir()) return null;
		IBlockDetailWrapper blockDetail = BlockDetailMap.getOrMakeBlockDetailCache(blockState, pos, lightSource);
        return blockDetail == BlockDetailWrapper.NULL_BLOCK_DETAIL ? null : blockDetail;
    }
	
	public ChunkAccess getChunk() {
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
		return LevelPosUtil.convert(LodUtil.CHUNK_DETAIL_LEVEL, chunk.getPos().x, LodUtil.REGION_DETAIL_LEVEL);
	}
	
	@Override
	public int getRegionPosZ(){
		return LevelPosUtil.convert(LodUtil.CHUNK_DETAIL_LEVEL, chunk.getPos().z, LodUtil.REGION_DETAIL_LEVEL);
	}
	
	@Override
	public int getMaxY(int x, int z) {
		return chunk.getHeight(Heightmap.Types.WORLD_SURFACE, Math.floorMod(x, 16), Math.floorMod(z, 16));
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
	public int getMinZ() {
		return chunk.getPos().getMinBlockZ();
	}
	
	@Override
	public long getLongChunkPos() {
		return chunk.getPos().toLong();
	}
	
	@Override
	public boolean isLightCorrect(){
		#if PRE_MC_1_18_1
		return true;
		#else
		if (chunk instanceof LevelChunk) {
			return ((LevelChunk) chunk).isClientLightReady();
		}
		return chunk.isLightCorrect();
		#endif
	}
	
	public boolean isWaterLogged(int x, int y, int z)
	{
		BlockState blockState = chunk.getBlockState(new BlockPos(x,y,z));
		
		//This type of block is always in water
		return (!(blockState.getBlock() instanceof LiquidBlockContainer) && (blockState.getBlock() instanceof SimpleWaterloggedBlock))
				&& (blockState.hasProperty(BlockStateProperties.WATERLOGGED) && blockState.getValue(BlockStateProperties.WATERLOGGED));
	}
	
	@Override
	public int getEmittedBrightness(int x, int y, int z)
	{
		return chunk.getLightEmission(new BlockPos(x,y,z));
	}
	
	@Override
	public int getBlockLight(int x, int y, int z) {
		if (lightSource == null) return -1;
		return lightSource.getBrightness(LightLayer.BLOCK, new BlockPos(x,y,z));
	}
	
	@Override
	public int getSkyLight(int x, int y, int z) {
		if (lightSource == null) return -1;
		return lightSource.getBrightness(LightLayer.SKY, new BlockPos(x,y,z));
	}
	
	@Override
	public boolean doesNearbyChunksExist() {
		if (lightSource instanceof LightedWorldGenRegion) return true;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx==0 && dz==0) continue;
				if (lightSource.getChunk(dx+getChunkPosX(), dz+getChunkPosZ(), ChunkStatus.BIOMES, false) == null) return false;
			}
		}
		return true;
	}
	
	public LevelReader getColorResolver()
	{
		return lightSource;
	}

	@Override
	public String toString() {
		return chunk.getClass().getSimpleName() + chunk.getPos();
	}
	
}
