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

import com.seibel.lod.api.enums.config.ELightGenerationMode;
import com.seibel.lod.common.wrappers.block.BlockStateWrapper;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.pos.DhBlockPos;
import com.seibel.lod.core.pos.DhChunkPos;
import com.seibel.lod.core.pos.Pos2D;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;

import com.seibel.lod.common.wrappers.block.BiomeWrapper;
import com.seibel.lod.common.wrappers.worldGeneration.mimicObject.LightedWorldGenRegion;

import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import net.minecraft.core.BlockPos;
#if POST_MC_1_17_1
import net.minecraft.core.QuartPos;
#endif
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

// Which nullable should be used???
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class ChunkWrapper implements IChunkWrapper
{
	private final ChunkAccess chunk;
	private final DhChunkPos chunkPos;
	private final LevelReader lightSource;
	private final ILevelWrapper wrappedLevel;
	
	private final boolean useMcLightingEngine;
	private final boolean isDhGeneratedChunk;
	
	private final HashMap<BlockPos, BlockState> blockStateByBlockPosCache = new HashMap<>();
	
	
	
	public ChunkWrapper(ChunkAccess chunk, LevelReader lightSource, @Nullable ILevelWrapper wrappedLevel)
	{
		this.chunk = chunk;
		this.lightSource = lightSource;
		this.wrappedLevel = wrappedLevel;
		this.chunkPos = new DhChunkPos(chunk.getPos().x, chunk.getPos().z);
		
		this.useMcLightingEngine = (Config.Client.Advanced.WorldGenerator.lightingEngine.get() == ELightGenerationMode.MINECRAFT);
		// TODO is this the best way to differentiate between when we are generating chunks and when MC gave us a chunk?
		this.isDhGeneratedChunk = (this.lightSource.getClass() == LightedWorldGenRegion.class);
	}
	
	
	
	//=========//
	// methods //
	//=========//
	
	@Override
	public int getHeight()
	{
		#if PRE_MC_1_17_1
		return 255;
		#else
		return this.chunk.getHeight();
		#endif
	}
	
	@Override
	public int getMinBuildHeight()
	{
		#if PRE_MC_1_17_1
		return 0;
		#else
		return this.chunk.getMinBuildHeight();
		#endif
	}
	@Override
	public int getMaxBuildHeight() { return this.chunk.getMaxBuildHeight(); }
	
	
	@Override
	public int getSolidHeightMapValue(int xRel, int zRel) { return this.chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR).getFirstAvailable(xRel, zRel); }
	
	@Override
	public int getLightBlockingHeightMapValue(int xRel, int zRel) { return this.chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING).getFirstAvailable(xRel, zRel); }
	
	
	
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
	public DhChunkPos getChunkPos() { return this.chunkPos; }

	public ChunkAccess getChunk() { return this.chunk; }
	
	@Override
	public int getMaxX() { return this.chunk.getPos().getMaxBlockX(); }
	@Override
	public int getMaxZ() { return this.chunk.getPos().getMaxBlockZ(); }
	@Override
	public int getMinX() { return this.chunk.getPos().getMinBlockX(); }
	@Override
	public int getMinZ() { return this.chunk.getPos().getMinBlockZ(); }
	
	@Override
	public long getLongChunkPos() { return this.chunk.getPos().toLong(); }
	
	@Override
	public boolean isLightCorrect()
	{
		#if PRE_MC_1_18_1
		return true;
		#else
		if (this.chunk instanceof LevelChunk)
		{
			// called when connected to a server (and sometimes when in a singleplayer world)
			return ((LevelChunk) this.chunk).isClientLightReady() || this.chunk.isLightCorrect();
		}
		else
		{
			// called when in a single player world
			return this.chunk.isLightCorrect();	
		}
		#endif
	}
	
	@Override
	public int getBlockLight(int x, int y, int z)
	{
		// use the full lighting engine when the chunks are within render distance or the config requests it
		if (this.useMcLightingEngine || !this.isDhGeneratedChunk)
		{
			// FIXME this returns 0 if the chunks unload
			
			// MC lighting method
			return this.lightSource.getBrightness(LightLayer.BLOCK, new BlockPos(x+this.getMinX(), y, z+this.getMinZ()));
		}
		else
		{
			// DH lighting method
			return this.getMaxBlockLightAtBlockPos(new DhBlockPos(x, y, z));
		}
	}
	/** 
	 * Note: this doesn't take into account blocks outside this chunk's borders <br>
	 * AKA: there will be sharp shadow cut-offs on chunk boundaries
	 * 
	 * @param inputPos a relative position for this chunk (IE between [0,0] and [15,15]) 
	 * @apiNote TODO DH lighting should be moved into its own class in Core 
	 */
	private int getMaxBlockLightAtBlockPos(DhBlockPos inputPos)
	{
		// how many blocks (in a square) to take into account when generating lighting
		// higher numbers will make the lighting more accurate but will take significantly longer
		int width = 4;
		
		int maxBlockLight = this.getCachedBlockState(new BlockPos(inputPos.x, inputPos.y, inputPos.z)).getLightEmission();
		int inputMaxYPos = this.getSolidHeightMapValue(inputPos.x, inputPos.z);
		
		// min and max calls to clamp the position to this chunk
		for (int relX = Math.max(0, inputPos.x-width); relX < Math.min(inputPos.x+width, LodUtil.CHUNK_WIDTH); relX++)
		{
			for (int relZ = Math.max(0, inputPos.z-width); relZ < Math.min(inputPos.z+width, LodUtil.CHUNK_WIDTH); relZ++)
			{
				int lightYPos = this.getSolidHeightMapValue(relX, relZ);
				
				if (inputPos.y < lightYPos && inputPos.y < inputMaxYPos)
				{
					// input is below the light
					// and another block that isn't the light, ignore
					continue;	
				}
				
				
				// the max(height, height-1) is to fix an edge case involving torches
				int lightValue = Math.max(
									this.getCachedBlockState(new BlockPos(relX, lightYPos, relZ)).getLightEmission(),
									this.getCachedBlockState(new BlockPos(relX, lightYPos-1, relZ)).getLightEmission());
				
				
				int centerDistanceFromLight = new Pos2D(inputPos.x, inputPos.z).manhattanDist(new Pos2D(relX, relZ));
				// multiply falloff by 2 to make light fade faster to reduce the number of hard edges caused by going outside of chunk boundaries
				// (This could be removed if we add the ability to access blocks outside this chunk)
				centerDistanceFromLight *= 2;
				
				lightValue = lightValue - centerDistanceFromLight;
				maxBlockLight = Math.max(maxBlockLight, lightValue);
			}
		}
		
		return maxBlockLight;
	}
	
	@Override
	public int getSkyLight(int x, int y, int z)
	{
		// use the full lighting engine when the chunks are within render distance or the config requests it
		if (this.useMcLightingEngine || !this.isDhGeneratedChunk)
		{
			// FIXME this returns 0 if the chunks unload
			
			// MC lighting method
			return this.lightSource.getBrightness(LightLayer.SKY, new BlockPos(x+this.getMinX(), y, z+this.getMinZ()));
		}
		else
		{
			// DH lighting method
			return this.getMaxSkyLightAtBlockPos(new DhBlockPos(x,y,z));
		}
	}
	/**
	 * Note: this doesn't take into account blocks outside this chunk's borders <br>
	 * AKA: there will be sharp shadow cut-offs on chunk boundaries
	 * 	 
	 * @param inputPos a relative position for this chunk (IE between [0,0] and [15,15])
	 * @apiNote TODO DH lighting should be moved into its own class in Core
	 */
	private int getMaxSkyLightAtBlockPos(DhBlockPos inputPos)
	{
		// Note: this doesn't take into account blocks outside this chunk's borders
		// AKA: there will be sharp shadow cut-offs on chunk boundaries
		
		// how many blocks (in a square) to take into account when generating lighting
		// higher numbers will make the lighting more accurate but will take longer
		int width = 4;
		
		
		int maxSkyLight = 0;
		Pos2D centerPos = new Pos2D(inputPos.x,inputPos.z);
		
		// min and max calls to clamp the position to this chunk
		for (int relX = Math.max(0, inputPos.x-width); relX < Math.min(inputPos.x+width, LodUtil.CHUNK_WIDTH); relX++)
		{
			for (int relZ = Math.max(0, inputPos.z-width); relZ < Math.min(inputPos.z+width, LodUtil.CHUNK_WIDTH); relZ++)
			{
				int heightAtPos = this.getLightBlockingHeightMapValue(relX, relZ);
				int lightAtRelPos = (inputPos.y >= heightAtPos) ? 15 : 0; // 15 if it can see the sky, 0 otherwise
				
				int centerDistanceFromLight = centerPos.manhattanDist(new Pos2D(relX, relZ));
				int centerLight = Math.max(0, lightAtRelPos - centerDistanceFromLight);
				
				maxSkyLight = Math.max(maxSkyLight, centerLight);
			}
		}
		
		return maxSkyLight;
	}
	
	@Override
	public boolean doesNearbyChunksExist()
	{
		if (this.lightSource instanceof LightedWorldGenRegion)
		{
			return true;
		}
		
		for (int dx = -1; dx <= 1; dx++)
		{
			for (int dz = -1; dz <= 1; dz++)
			{
				if (dx == 0 && dz == 0)
				{
					continue;
				}
				else if (this.lightSource.getChunk(dx + this.chunk.getPos().x, dz + this.chunk.getPos().z, ChunkStatus.BIOMES, false) == null)
				{
					return false;
				}
			}
		}
		
		return true;
	}
	
	public LevelReader getColorResolver() { return this.lightSource; }

	@Override
	public String toString() { return this.chunk.getClass().getSimpleName() + this.chunk.getPos(); }

	@Override
	public IBlockStateWrapper getBlockState(int x, int y, int z)
	{
		//if (wrappedLevel != null) return wrappedLevel.getBlockState(new DhBlockPos(x + getMinX(), y, z + getMinZ()));
		return BlockStateWrapper.fromBlockState(this.chunk.getBlockState(new BlockPos(x,y,z)));
	}

	@Override
	public boolean isStillValid() { return this.wrappedLevel == null || this.wrappedLevel.tryGetChunk(this.chunkPos) == this; }
	
	
	
	//================//
	// helper methods //
	//================//
	
	/** can be used to speed up operations that need to interact with blockstates often */
	private BlockState getCachedBlockState(BlockPos pos)
	{
		if (!this.blockStateByBlockPosCache.containsKey(pos))
		{
			BlockState blockState = this.chunk.getBlockState(pos);
			this.blockStateByBlockPosCache.put(pos, blockState);
		}
		
		return this.blockStateByBlockPosCache.get(pos);
	}
	
}
