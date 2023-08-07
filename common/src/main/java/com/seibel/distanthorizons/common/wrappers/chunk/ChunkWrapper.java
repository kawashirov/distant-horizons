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
 
package com.seibel.distanthorizons.common.wrappers.chunk;

import com.seibel.distanthorizons.api.enums.config.ELightGenerationMode;
import com.seibel.distanthorizons.common.wrappers.block.BiomeWrapper;
import com.seibel.distanthorizons.common.wrappers.block.BlockStateWrapper;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.mimicObject.DhLitWorldGenRegion;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.pos.DhBlockPos;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IBiomeWrapper;

import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

#if POST_MC_1_17_1
import net.minecraft.core.QuartPos;
#endif

#if POST_MC_1_20_1
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.core.SectionPos;
#endif

public class ChunkWrapper implements IChunkWrapper
{
	private final ChunkAccess chunk;
	private final DhChunkPos chunkPos;
	private final LevelReader lightSource;
	private final ILevelWrapper wrappedLevel;
	
	private boolean isDhLightCorrect = false;
	private final HashMap<DhBlockPos, Integer> blockLightAtRelBlockPos = new HashMap<>(LodUtil.CHUNK_WIDTH * LodUtil.CHUNK_WIDTH * 256);
	private final HashMap<DhBlockPos, Integer> skyLightAtRelBlockPos = new HashMap<>(LodUtil.CHUNK_WIDTH * LodUtil.CHUNK_WIDTH * 256);
	
	private LinkedList<DhBlockPos> blockLightPosList = null;
	
	private boolean useDhLighting;
	
	/**
	 * Due to vanilla `isClientLightReady()` not designed to be used by non-render thread, that value may return 'true'
	 * just before the light engine is ticked, (right after all light changes is marked to the engine to be processed).
	 * To fix this, on client-only mode, we mixin-redirect the `isClientLightReady()` so that after the call, it will
	 * trigger a synchronous update of this flag here on all chunks that are wrapped. <br><br>
	 * 
	 * Note: Using a static weak hash map to store the chunks that need to be updated, as instance of chunk wrapper
	 * can be duplicated, with same chunk instance. And the data stored here are all temporary, and thus will not be
	 * visible when a chunk is re-wrapped later. <br>
	 * (Also, thread safety done via a reader writer lock)
	 */
	private final static WeakHashMap<ChunkAccess, Boolean> chunksToUpdateClientLightReady = new WeakHashMap<>(); // TODO this is never cleared
	private final static ReentrantReadWriteLock weakMapLock = new ReentrantReadWriteLock();
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public ChunkWrapper(ChunkAccess chunk, LevelReader lightSource, @Nullable ILevelWrapper wrappedLevel)
	{
		this.chunk = chunk;
		this.lightSource = lightSource;
		this.wrappedLevel = wrappedLevel;
		this.chunkPos = new DhChunkPos(chunk.getPos().x, chunk.getPos().z);
		
		// TODO is this the best way to differentiate between when we are generating chunks and when MC gave us a chunk?
		boolean isDhGeneratedChunk = (this.lightSource.getClass() == DhLitWorldGenRegion.class);
		this.useDhLighting = isDhGeneratedChunk && (Config.Client.Advanced.WorldGenerator.worldGenLightingEngine.get() == ELightGenerationMode.DISTANT_HORIZONS);
		
		weakMapLock.writeLock().lock();
		chunksToUpdateClientLightReady.put(chunk, false);
		weakMapLock.writeLock().unlock();
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
	public IBiomeWrapper getBiome(int relX, int relY, int relZ)
	{
		//if (wrappedLevel != null) return wrappedLevel.getBiome(new DhBlockPos(x + getMinX(), y, z + getMinZ()));

		#if PRE_MC_1_17_1
		return BiomeWrapper.getBiomeWrapper(this.chunk.getBiomes().getNoiseBiome(
				relX >> 2, relY >> 2, relZ >> 2));
		#elif PRE_MC_1_18_2
		return BiomeWrapper.getBiomeWrapper(this.chunk.getBiomes().getNoiseBiome(
				QuartPos.fromBlock(relX), QuartPos.fromBlock(relY), QuartPos.fromBlock(relZ)));
		#elif PRE_MC_1_18_2
		return BiomeWrapper.getBiomeWrapper(this.chunk.getNoiseBiome(
				QuartPos.fromBlock(relX), QuartPos.fromBlock(relY), QuartPos.fromBlock(relZ)));
		#else //Now returns a Holder<Biome> instead of Biome
		return BiomeWrapper.getBiomeWrapper(this.chunk.getNoiseBiome(
				QuartPos.fromBlock(relX), QuartPos.fromBlock(relY), QuartPos.fromBlock(relZ)));
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
		
		
		#if PRE_MC_1_18_2
		return true;
		#else
		if (this.chunk instanceof LevelChunk)
		{
			LevelChunk levelChunk = (LevelChunk) this.chunk;
			if (levelChunk.getLevel() instanceof ClientLevel)
			{
				weakMapLock.readLock().lock();
				boolean fixedIsClientLightReady = chunksToUpdateClientLightReady.get(this.chunk);
				weakMapLock.readLock().unlock();
				return fixedIsClientLightReady;
			}

			// called when in single player or in dedicated server, and the chunk is a level chunk (active)
			return this.chunk.isLightCorrect() && levelChunk.loaded;
		}
		else
		{
			// called when in a single player world and the chunk is a proto chunk (in world gen, and not active)
			return this.chunk.isLightCorrect();	
		}
		#endif
	}
	
	
	@Override
	public int getDhBlockLight(int relX, int relY, int relZ) 
	{
		throwIndexOutOfBoundsIfRelativePosOutsideChunkBounds(relX, relY, relZ);
		return this.blockLightAtRelBlockPos.getOrDefault(new DhBlockPos(relX, relY, relZ), 0); 
	}
	@Override
	public void setDhBlockLight(int relX, int relY, int relZ, int lightValue) 
	{
		throwIndexOutOfBoundsIfRelativePosOutsideChunkBounds(relX, relY, relZ);
		this.blockLightAtRelBlockPos.put(new DhBlockPos(relX, relY, relZ), lightValue); 
	}
	
	@Override
	public int getDhSkyLight(int relX, int relY, int relZ) 
	{
		throwIndexOutOfBoundsIfRelativePosOutsideChunkBounds(relX, relY, relZ);
		return this.skyLightAtRelBlockPos.getOrDefault(new DhBlockPos(relX, relY, relZ), 0); 
	}
	@Override
	public void setDhSkyLight(int relX, int relY, int relZ, int lightValue) 
	{
		throwIndexOutOfBoundsIfRelativePosOutsideChunkBounds(relX, relY, relZ);
		this.skyLightAtRelBlockPos.put(new DhBlockPos(relX, relY, relZ), lightValue); 
	}
	
	
	@Override
	public int getBlockLight(int relX, int relY, int relZ)
	{
		throwIndexOutOfBoundsIfRelativePosOutsideChunkBounds(relX, relY, relZ);
		
		// use the full lighting engine when the chunks are within render distance or the config requests it
		if (this.useDhLighting)
		{
			// DH lighting method
			return this.blockLightAtRelBlockPos.getOrDefault(new DhBlockPos(relX, relY, relZ), 0);
		}
		else
		{
			// note: this returns 0 if the chunk is unload
			
			// MC lighting method
			return this.lightSource.getBrightness(LightLayer.BLOCK, new BlockPos(relX +this.getMinBlockX(), relY, relZ +this.getMinBlockZ()));
		}
	}
	
	@Override
	public int getSkyLight(int relX, int relY, int relZ)
	{
		throwIndexOutOfBoundsIfRelativePosOutsideChunkBounds(relX, relY, relZ);
		
		// use the full lighting engine when the chunks are within render distance or the config requests it
		if (this.useDhLighting)
		{
			// DH lighting method
			return this.skyLightAtRelBlockPos.getOrDefault(new DhBlockPos(relX, relY, relZ), 0);
		}
		else
		{
			// MC lighting method
			return this.lightSource.getBrightness(LightLayer.SKY, new BlockPos(relX +this.getMinBlockX(), relY, relZ +this.getMinBlockZ()));
		}
	}
	
	@Override 
	public List<DhBlockPos> getBlockLightPosList()
	{
		// only populate the list once
		if (this.blockLightPosList == null)
		{
			this.blockLightPosList = new LinkedList<>();
			
			
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
		//if (wrappedLevel != null) return wrappedLevel.getBlockState(new DhBlockPos(x + getMinX(), y, z + getMinZ()));
		return BlockStateWrapper.fromBlockState(this.chunk.getBlockState(new BlockPos(relX, relY, relZ)));
	}

	@Override
	public boolean isStillValid() { return this.wrappedLevel == null || this.wrappedLevel.tryGetChunk(this.chunkPos) == this; }

	#if POST_MC_1_20_1
	private static boolean checkLightSectionsOnChunk(LevelChunk chunk, LevelLightEngine engine) {
		LevelChunkSection[] sections = chunk.getSections();
		int minY = chunk.getMinSection();
		int maxY = chunk.getMaxSection();
		for (int y = minY; y < maxY; ++y) {
			LevelChunkSection section = sections[chunk.getSectionIndexFromSectionY(y)];
			if (section.hasOnlyAir()) continue;
			if (!engine.lightOnInSection(SectionPos.of(chunk.getPos(), y))) {
				return false;
			}
		}
		return true;
	}
	#endif

	// Should be called after client light updates are triggered.
	private static boolean updateClientLightReady(ChunkAccess chunk, boolean oldValue)
	{
		if (chunk instanceof LevelChunk && ((LevelChunk)chunk).getLevel() instanceof ClientLevel)
		{
			LevelChunk levelChunk = (LevelChunk)chunk;
			ClientChunkCache clientChunkCache = ((ClientLevel)levelChunk.getLevel()).getChunkSource();
			return clientChunkCache.getChunkForLighting(chunk.getPos().x, chunk.getPos().z) != null &&
					#if MC_1_16_5 || MC_1_17_1
					levelChunk.isLightCorrect();
					#elif PRE_MC_1_20_1 
					levelChunk.isClientLightReady();
					#else 
					checkLightSectionsOnChunk(levelChunk, levelChunk.getLevel().getLightEngine());
					#endif
		}
		else
		{
			return oldValue;
		}
	}

	public static void syncedUpdateClientLightStatus()
	{
		#if PRE_MC_1_18_2
		// TODO: Check what to do in 1.18.1, or in other versions
		#else
		weakMapLock.writeLock().lock();
		try
		{
			chunksToUpdateClientLightReady.replaceAll(ChunkWrapper::updateClientLightReady);
		}
		finally 
		{
			weakMapLock.writeLock().unlock();
		}
		#endif
	}
	
	
	
	//================//
	// helper methods //
	//================//
	
	/** used to prevent accidentally attempting to get/set values outside this chunk's boundaries */
	private static void throwIndexOutOfBoundsIfRelativePosOutsideChunkBounds(int x, int y, int z) throws IndexOutOfBoundsException
	{
		if (x < 0 || x >= LodUtil.CHUNK_WIDTH
			|| z < 0 || z >= LodUtil.CHUNK_WIDTH)
		{
			throw new IndexOutOfBoundsException("Indices are relative and must be between 0 and 15 (inclusive), X:"+x+", Y:"+y+" Z:"+z);
		}
	}
	
}
