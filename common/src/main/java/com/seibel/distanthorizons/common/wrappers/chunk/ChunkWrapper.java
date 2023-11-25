/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2023 James Seibel
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

package com.seibel.distanthorizons.common.wrappers.chunk;

import com.seibel.distanthorizons.common.wrappers.block.BiomeWrapper;
import com.seibel.distanthorizons.common.wrappers.block.BlockStateWrapper;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.mimicObject.DhLitWorldGenRegion;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhBlockPos;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IBiomeWrapper;

import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.distanthorizons.coreapi.ModInfo;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

#if POST_MC_1_17_1
import net.minecraft.core.QuartPos;
#endif

#if MC_1_16_5
import net.minecraft.world.level.chunk.LevelChunkSection;
#endif

#if MC_1_17_1
import net.minecraft.world.level.chunk.LevelChunkSection;
#endif

#if MC_1_18_2
import net.minecraft.world.level.chunk.LevelChunkSection;
#endif

#if MC_1_19_2 || MC_1_19_4
import net.minecraft.world.level.chunk.LevelChunkSection;
#endif

#if POST_MC_1_20_1
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.core.SectionPos;
#endif

public class ChunkWrapper implements IChunkWrapper
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	/** useful for debugging, but can slow down chunk operations quite a bit due to being called every time. */
	private static final boolean RUN_RELATIVE_POS_INDEX_VALIDATION = ModInfo.IS_DEV_BUILD;
	
	/** can be used for interactions with the underlying chunk where creating new BlockPos objects could cause issues for the garbage collector. */
	private static final ThreadLocal<BlockPos.MutableBlockPos> MUTABLE_BLOCK_POS_REF = ThreadLocal.withInitial(() -> new BlockPos.MutableBlockPos());
	
	
	private final ChunkAccess chunk;
	private final DhChunkPos chunkPos;
	private final LevelReader lightSource;
	private final ILevelWrapper wrappedLevel;
	
	private boolean isDhLightCorrect = false;
	/** only used when connected to a dedicated server */
	private boolean isMcClientLightingCorrect = false;
	
	private ChunkLightStorage blockLightStorage;
	private ChunkLightStorage skyLightStorage;
	
	private ArrayList<DhBlockPos> blockLightPosList = null;
	
	private boolean useDhLighting;
	
	/**
	 * Due to vanilla `isClientLightReady()` not being designed for use by a non-render thread, it may return 'true'
	 * before the light engine has ticked, (right after all light changes is marked by the engine to be processed).
	 * To fix this, on client-only mode, we mixin-redirect the `isClientLightReady()` so that after the call, it will
	 * trigger a synchronous update of this flag here on all chunks that are wrapped. <br><br>
	 *
	 * Note: Using a static weak hash map to store the chunks that need to be updated, as instance of chunk wrapper
	 * can be duplicated, with same chunk instance. And the data stored here are all temporary, and thus will not be
	 * visible when a chunk is re-wrapped later. <br>
	 * (Also, thread safety done via a reader writer lock)
	 */
	private static final ConcurrentLinkedQueue<ChunkWrapper> chunksNeedingClientLightUpdating = new ConcurrentLinkedQueue<>(); 
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public ChunkWrapper(ChunkAccess chunk, LevelReader lightSource, ILevelWrapper wrappedLevel)
	{
		this.chunk = chunk;
		this.lightSource = lightSource;
		this.wrappedLevel = wrappedLevel;
		this.chunkPos = new DhChunkPos(chunk.getPos().x, chunk.getPos().z);
		
		// TODO is this the best way to differentiate between when we are generating chunks and when MC gave us a chunk?
		boolean isDhGeneratedChunk = (this.lightSource.getClass() == DhLitWorldGenRegion.class);
		// MC loaded chunks should get their lighting from MC, DH generated chunks should get their lighting from DH
		this.useDhLighting = isDhGeneratedChunk;
		
		// FIXME +1 is to handle the fact that LodDataBuilder adds +1 to all block lighting calculations, also done in the relative position validator

		chunksNeedingClientLightUpdating.add(this);
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
	public int getMinFilledHeight()
	{
		LevelChunkSection[] sections = this.chunk.getSections();
		for (int index = 0; index < sections.length; index++)
		{
			if (sections[index] == null)
			{
				continue;
			}
			
			#if MC_1_16_5
			if (!sections[index].isEmpty())
			{
				// convert from an index to a block coordinate
				return this.chunk.getSections()[index].bottomBlockY() * 16;
			}
			#elif MC_1_17_1
			if (!sections[index].isEmpty())
			{
				// convert from an index to a block coordinate
				return this.chunk.getSections()[index].bottomBlockY() * 16;
			}	
			#else
			if (!sections[index].hasOnlyAir())
			{
				// convert from an index to a block coordinate
				return this.chunk.getSectionYFromSectionIndex(index) * 16;
			}
			#endif
		}
		return Integer.MAX_VALUE;
	}
	
	
	@Override
	public int getSolidHeightMapValue(int xRel, int zRel) { return this.chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE).getFirstAvailable(xRel, zRel); }
	
	@Override
	public int getLightBlockingHeightMapValue(int xRel, int zRel) { return this.chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING).getFirstAvailable(xRel, zRel); }
	
	
	
	@Override
	public IBiomeWrapper getBiome(int relX, int relY, int relZ)
	{
		#if PRE_MC_1_17_1
		return BiomeWrapper.getBiomeWrapper(this.chunk.getBiomes().getNoiseBiome(
				relX >> 2, relY >> 2, relZ >> 2),
				this.wrappedLevel);
		#elif PRE_MC_1_18_2
		return BiomeWrapper.getBiomeWrapper(this.chunk.getBiomes().getNoiseBiome(
				QuartPos.fromBlock(relX), QuartPos.fromBlock(relY), QuartPos.fromBlock(relZ)),
				this.wrappedLevel);
		#elif PRE_MC_1_18_2
		return BiomeWrapper.getBiomeWrapper(this.chunk.getNoiseBiome(
				QuartPos.fromBlock(relX), QuartPos.fromBlock(relY), QuartPos.fromBlock(relZ)),
				this.wrappedLevel);
		#else 
		//Now returns a Holder<Biome> instead of Biome
		return BiomeWrapper.getBiomeWrapper(this.chunk.getNoiseBiome(
				QuartPos.fromBlock(relX), QuartPos.fromBlock(relY), QuartPos.fromBlock(relZ)),
				this.wrappedLevel);
		#endif
	}
	
	@Override
	public DhChunkPos getChunkPos() { return this.chunkPos; }
	
	public ChunkAccess getChunk() { return this.chunk; }
	
	@Override
	public int getMaxBlockX() { return this.chunk.getPos().getMaxBlockX(); }
	@Override
	public int getMaxBlockZ() { return this.chunk.getPos().getMaxBlockZ(); }
	@Override
	public int getMinBlockX() { return this.chunk.getPos().getMinBlockX(); }
	@Override
	public int getMinBlockZ() { return this.chunk.getPos().getMinBlockZ(); }
	
	@Override
	public long getLongChunkPos() { return this.chunk.getPos().toLong(); }
	
	@Override
	public void setIsDhLightCorrect(boolean isDhLightCorrect) { this.isDhLightCorrect = isDhLightCorrect; }
	
	@Override
	public void setUseDhLighting(boolean useDhLighting) { this.useDhLighting = useDhLighting; }
	
	
	
	@Override
	public boolean isLightCorrect()
	{
		if (this.useDhLighting)
		{
			return this.isDhLightCorrect;
		}
		
		
		#if MC_1_16_5 || MC_1_17_1
		return false; // MC's lighting engine doesn't work consistently enough to trust for 1.16 or 1.17
		#else
		if (this.chunk instanceof LevelChunk)
		{
			LevelChunk levelChunk = (LevelChunk) this.chunk;
			if (levelChunk.getLevel() instanceof ClientLevel)
			{
				// connected to a server
				return this.isMcClientLightingCorrect;
			}
			else
			{
				// in a single player world
				return this.chunk.isLightCorrect() && levelChunk.loaded;	
			}
		}
		else
		{
			// called when in a single player world and the chunk is a proto chunk (in world gen, and not active)
			return this.chunk.isLightCorrect();
		}
		#endif
	}
	
	
	@Override
	public int getDhBlockLight(int relX, int y, int relZ)
	{
		this.throwIndexOutOfBoundsIfRelativePosOutsideChunkBounds(relX, y, relZ);
		return this.getBlockLightStorage().get(relX, y, relZ);
	}
	@Override
	public void setDhBlockLight(int relX, int y, int relZ, int lightValue)
	{
		this.throwIndexOutOfBoundsIfRelativePosOutsideChunkBounds(relX, y, relZ);
		this.getBlockLightStorage().set(relX, y, relZ, lightValue);
	}
	
	private ChunkLightStorage getBlockLightStorage()
	{
		if (this.blockLightStorage == null)
		{
			this.blockLightStorage = new ChunkLightStorage(this.getMinBuildHeight(), this.getMaxBuildHeight());
		}
		return this.blockLightStorage;
	}
	
	
	@Override
	public int getDhSkyLight(int relX, int y, int relZ)
	{
		this.throwIndexOutOfBoundsIfRelativePosOutsideChunkBounds(relX, y, relZ);
		return this.getSkyLightStorage().get(relX, y, relZ);
	}
	@Override
	public void setDhSkyLight(int relX, int y, int relZ, int lightValue)
	{
		this.throwIndexOutOfBoundsIfRelativePosOutsideChunkBounds(relX, y, relZ);
		this.getSkyLightStorage().set(relX, y, relZ, lightValue);
	}
	
	private ChunkLightStorage getSkyLightStorage()
	{
		if (this.skyLightStorage == null)
		{
			this.skyLightStorage = new ChunkLightStorage(this.getMinBuildHeight(), this.getMaxBuildHeight());
		}
		return this.skyLightStorage;
	}
	
	
	@Override
	public int getBlockLight(int relX, int y, int relZ)
	{
		this.throwIndexOutOfBoundsIfRelativePosOutsideChunkBounds(relX, y, relZ);
		
		// use the full lighting engine when the chunks are within render distance or the config requests it
		if (this.useDhLighting)
		{
			// DH lighting method
			return this.getBlockLightStorage().get(relX, y, relZ);
		}
		else
		{
			// note: this returns 0 if the chunk is unload
			
			// MC lighting method
			return this.lightSource.getBrightness(LightLayer.BLOCK, new BlockPos(relX + this.getMinBlockX(), y, relZ + this.getMinBlockZ()));
		}
	}
	
	@Override
	public int getSkyLight(int relX, int y, int relZ)
	{
		this.throwIndexOutOfBoundsIfRelativePosOutsideChunkBounds(relX, y, relZ);
		
		// use the full lighting engine when the chunks are within render distance or the config requests it
		if (this.useDhLighting)
		{
			// DH lighting method
			return this.getSkyLightStorage().get(relX, y, relZ);
		}
		else
		{
			// MC lighting method
			return this.lightSource.getBrightness(LightLayer.SKY, new BlockPos(relX + this.getMinBlockX(), y, relZ + this.getMinBlockZ()));
		}
	}
	
	@Override
	public ArrayList<DhBlockPos> getBlockLightPosList()
	{
		// only populate the list once
		if (this.blockLightPosList == null)
		{
			this.blockLightPosList = new ArrayList<>();
			
			
			#if PRE_MC_1_20_1
			this.chunk.getLights().forEach((blockPos) ->
			{
				this.blockLightPosList.add(new DhBlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
			});
			#elif MC_1_20_1
			this.chunk.findBlockLightSources((blockPos, blockState) ->
			{
				this.blockLightPosList.add(new DhBlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
			});
			#endif
		}
		
		return this.blockLightPosList;
	}
	
	@Override
	public boolean doNearbyChunksExist()
	{
		if (this.lightSource instanceof DhLitWorldGenRegion)
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
	public IBlockStateWrapper getBlockState(int relX, int relY, int relZ)
	{
		this.throwIndexOutOfBoundsIfRelativePosOutsideChunkBounds(relX, relY, relZ);
		
		BlockPos.MutableBlockPos blockPos = MUTABLE_BLOCK_POS_REF.get();
		
		blockPos.setX(relX);
		blockPos.setY(relY);
		blockPos.setZ(relZ);
		
		return BlockStateWrapper.fromBlockState(this.chunk.getBlockState(blockPos), this.wrappedLevel);
	}
	
	@Override
	public boolean isStillValid() { return this.wrappedLevel.tryGetChunk(this.chunkPos) == this; }
	
	
	public static void syncedUpdateClientLightStatus()
	{
		#if PRE_MC_1_18_2
		// TODO: Check what to do in 1.18.1 and older
		
		// since we don't currently handle this list,
		// clear it to prevent memory leaks
		chunksNeedingClientLightUpdating.clear();
		
		#else
		
		// update the chunks client lighting
		ChunkWrapper chunkWrapper = chunksNeedingClientLightUpdating.poll();
		while (chunkWrapper != null)
		{
			chunkWrapper.updateIsClientLightingCorrect();
			chunkWrapper = chunksNeedingClientLightUpdating.poll();
		}
		
		#endif
	}
	/** Should be called after client light updates are triggered. */
	private void updateIsClientLightingCorrect()
	{
		if (this.chunk instanceof LevelChunk && ((LevelChunk) this.chunk).getLevel() instanceof ClientLevel)
		{
			LevelChunk levelChunk = (LevelChunk) this.chunk;
			ClientChunkCache clientChunkCache = ((ClientLevel) levelChunk.getLevel()).getChunkSource();
			this.isMcClientLightingCorrect = clientChunkCache.getChunkForLighting(this.chunk.getPos().x, this.chunk.getPos().z) != null &&
					#if MC_1_16_5 || MC_1_17_1
					levelChunk.isLightCorrect();
					#elif PRE_MC_1_20_1
					levelChunk.isClientLightReady();
					#else
					checkLightSectionsOnChunk(levelChunk, levelChunk.getLevel().getLightEngine());
					#endif
		}
	}
	#if POST_MC_1_20_1
	private static boolean checkLightSectionsOnChunk(LevelChunk chunk, LevelLightEngine engine)
	{
		LevelChunkSection[] sections = chunk.getSections();
		int minY = chunk.getMinSection();
		int maxY = chunk.getMaxSection();
		for (int y = minY; y < maxY; ++y)
		{
			LevelChunkSection section = sections[chunk.getSectionIndexFromSectionY(y)];
			if (section.hasOnlyAir()) continue;
			if (!engine.lightOnInSection(SectionPos.of(chunk.getPos(), y)))
			{
				return false;
			}
		}
		return true;
	}
	#endif
	
	
	
	//================//
	// helper methods //
	//================//
	
	/** used to prevent accidentally attempting to get/set values outside this chunk's boundaries */
	private void throwIndexOutOfBoundsIfRelativePosOutsideChunkBounds(int x, int y, int z) throws IndexOutOfBoundsException
	{
		if (!RUN_RELATIVE_POS_INDEX_VALIDATION)
		{
			return;
		}
		
		
		// FIXME +1 is to handle the fact that LodDataBuilder adds +1 to all block lighting calculations, also done in the constructor
		int minHeight = this.getMinBuildHeight();
		int maxHeight = this.getMaxBuildHeight() + 1;
		
		if (x < 0 || x >= LodUtil.CHUNK_WIDTH
				|| z < 0 || z >= LodUtil.CHUNK_WIDTH
				|| y < minHeight || y > maxHeight)
		{
			String errorMessage = "Relative position [" + x + "," + y + "," + z + "] out of bounds. \n" +
					"X/Z must be between 0 and 15 (inclusive) \n" +
					"Y must be between [" + minHeight + "] and [" + maxHeight + "] (inclusive).";
			throw new IndexOutOfBoundsException(errorMessage);
		}
	}
	
	
	/**
	 * Converts a 3D position into a 1D array index. <br><br>
	 *
	 * Source: <br>
	 * <a href="https://stackoverflow.com/questions/7367770/how-to-flatten-or-index-3d-array-in-1d-array">stackoverflow</a>
	 */
	public int relativeBlockPosToIndex(int xRel, int y, int zRel)
	{
		int yRel = y - this.getMinBuildHeight();
		return (zRel * LodUtil.CHUNK_WIDTH * this.getHeight()) + (yRel * LodUtil.CHUNK_WIDTH) + xRel;
	}
	
	/**
	 * Converts a 3D position into a 1D array index. <br><br>
	 *
	 * Source: <br>
	 * <a href="https://stackoverflow.com/questions/7367770/how-to-flatten-or-index-3d-array-in-1d-array">stackoverflow</a>
	 */
	public DhBlockPos indexToRelativeBlockPos(int index)
	{
		final int zRel = index / (LodUtil.CHUNK_WIDTH * this.getHeight());
		index -= (zRel * LodUtil.CHUNK_WIDTH * this.getHeight());
		
		final int y = index / LodUtil.CHUNK_WIDTH;
		final int yRel = y + this.getMinBuildHeight();
		
		final int xRel = index % LodUtil.CHUNK_WIDTH;
		return new DhBlockPos(xRel, yRel, zRel);
	}
	
	
}
