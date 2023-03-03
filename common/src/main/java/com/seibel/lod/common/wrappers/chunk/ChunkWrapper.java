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

import com.seibel.lod.common.wrappers.block.BlockStateWrapper;
import com.seibel.lod.core.pos.DhChunkPos;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;

import com.seibel.lod.common.wrappers.WrapperUtil;
import com.seibel.lod.common.wrappers.block.BiomeWrapper;
import com.seibel.lod.common.wrappers.worldGeneration.mimicObject.LightedWorldGenRegion;

import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import net.minecraft.core.BlockPos;
#if POST_MC_1_17_1
import net.minecraft.core.QuartPos;
#endif
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

// Which nullable should be used???
import org.jetbrains.annotations.Nullable;
//import javax.annotation.Nullable;

/**
 *
 * @author James Seibel
 * @version 3-5-2022
 */
public class ChunkWrapper implements IChunkWrapper
{
	private final ChunkAccess chunk;
	private final DhChunkPos chunkPos;
	private final LevelReader lightSource;
	private final ILevelWrapper wrappedLevel;
	
	
	public ChunkWrapper(ChunkAccess chunk, LevelReader lightSource, @Nullable ILevelWrapper wrappedLevel)
	{
		this.chunk = chunk;
		this.lightSource = lightSource;
		this.wrappedLevel = wrappedLevel;
		chunkPos = new DhChunkPos(chunk.getPos().x, chunk.getPos().z);
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
		//if (wrappedLevel != null) return wrappedLevel.getBiome(new DhBlockPos(x + getMinX(), y, z + getMinZ()));

		#if PRE_MC_1_17_1
		return BiomeWrapper.getBiomeWrapper(chunk.getBiomes().getNoiseBiome(
				x >> 2, y >> 2, z >> 2));
		#elif PRE_MC_1_18_1
		return BiomeWrapper.getBiomeWrapper(chunk.getBiomes().getNoiseBiome(
				QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z)));
		#elif PRE_MC_1_18_2
		return BiomeWrapper.getBiomeWrapper(chunk.getNoiseBiome(
				QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z)));
		#else //Now returns a Holder<Biome> instead of Biome
		return BiomeWrapper.getBiomeWrapper(chunk.getNoiseBiome(
				QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z)));
		#endif
	}

	@Override
	public DhChunkPos getChunkPos() {
		return chunkPos;
	}

	public ChunkAccess getChunk() {
		return chunk;
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
		if (chunk instanceof LevelChunk)
		{
			// called when connected to a server
			return ((LevelChunk) chunk).isClientLightReady();
		}
		else
		{
			// called when in a single player world
			return chunk.isLightCorrect();	
		}
		#endif
	}
	
	@Override
	public int getBlockLight(int x, int y, int z) {
		if (lightSource == null) return -1;
		return lightSource.getBrightness(LightLayer.BLOCK, new BlockPos(x + getMinX(),y,z + getMinZ()));
	}
	
	@Override
	public int getSkyLight(int x, int y, int z) {
		if (lightSource == null) return -1;
		return lightSource.getBrightness(LightLayer.SKY, new BlockPos(x + getMinX(),y,z + getMinZ()));
	}
	
	@Override
	public boolean doesNearbyChunksExist() {
		if (lightSource instanceof LightedWorldGenRegion) return true;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx==0 && dz==0) continue;
				if (lightSource.getChunk(dx+chunk.getPos().x, dz+chunk.getPos().z, ChunkStatus.BIOMES, false) == null) return false;
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

	@Override
	public IBlockStateWrapper getBlockState(int x, int y, int z) {
		//if (wrappedLevel != null) return wrappedLevel.getBlockState(new DhBlockPos(x + getMinX(), y, z + getMinZ()));
		return BlockStateWrapper.fromBlockState(chunk.getBlockState(new BlockPos(x,y,z)));
	}

	@Override
	public boolean isStillValid() {
		return wrappedLevel == null || wrappedLevel.tryGetChunk(chunkPos) == this;
	}
}
