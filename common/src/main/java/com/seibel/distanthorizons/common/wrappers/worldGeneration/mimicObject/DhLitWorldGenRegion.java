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

package com.seibel.distanthorizons.common.wrappers.worldGeneration.mimicObject;

import java.lang.invoke.MethodHandles;
import java.util.List;

import com.seibel.distanthorizons.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SpawnerBlock;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ColorResolver;
#if POST_MC_1_17_1
import net.minecraft.world.level.LevelHeightAccessor;
#endif
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;

public class DhLitWorldGenRegion extends WorldGenRegion
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
	
	public final DummyLightEngine lightEngine;
	public final BatchGenerationEnvironment.EmptyChunkGenerator generator;
	public final int writeRadius;
	public final int size;
	private final ChunkPos firstPos;
	private final List<ChunkAccess> cache;
	Long2ObjectOpenHashMap<ChunkAccess> chunkMap = new Long2ObjectOpenHashMap<ChunkAccess>();
	
	#if PRE_MC_1_18_2
	private ChunkPos overrideCenterPos = null;
	
	public void setOverrideCenter(ChunkPos pos) { overrideCenterPos = pos; }
	#if PRE_MC_1_17_1
	@Override
	public int getCenterX() 
	{
		return overrideCenterPos==null ? super.getCenterX() : overrideCenterPos.x;
	}
	@Override
	public int getCenterZ() 
	{
		return overrideCenterPos==null ? super.getCenterX() : overrideCenterPos.z;
	}
	#else
	@Override
	public ChunkPos getCenter()
	{
		return overrideCenterPos == null ? super.getCenter() : overrideCenterPos;
	}
	#endif
	#endif
	
	public DhLitWorldGenRegion(
			ServerLevel serverLevel, DummyLightEngine lightEngine,
			List<ChunkAccess> chunkList, ChunkStatus chunkStatus, int writeRadius,
			BatchGenerationEnvironment.EmptyChunkGenerator generator)
	{
		super(serverLevel, chunkList #if POST_MC_1_17_1 , chunkStatus, writeRadius #endif );
		this.firstPos = chunkList.get(0).getPos();
		this.generator = generator;
		this.lightEngine = lightEngine;
		this.writeRadius = writeRadius;
		this.cache = chunkList;
		this.size = Mth.floor(Math.sqrt(chunkList.size()));
	}
	
	#if POST_MC_1_17_1
	// Bypass BCLib mixin overrides.
	@Override
	public boolean ensureCanWrite(BlockPos blockPos)
	{
		int i = SectionPos.blockToSectionCoord(blockPos.getX());
		int j = SectionPos.blockToSectionCoord(blockPos.getZ());
		ChunkPos chunkPos = this.getCenter();
		ChunkAccess center = this.getChunk(chunkPos.x, chunkPos.z);
		int k = Math.abs(chunkPos.x - i);
		int l = Math.abs(chunkPos.z - j);
		if (k > this.writeRadius || l > this.writeRadius)
		{
			return false;
		}
		#if POST_MC_1_18_2
		if (center.isUpgrading())
		{
			LevelHeightAccessor levelHeightAccessor = center.getHeightAccessorForGeneration();
			if (blockPos.getY() < levelHeightAccessor.getMinBuildHeight() || blockPos.getY() >= levelHeightAccessor.getMaxBuildHeight())
			{
				return false;
			}
		}
		#endif
		return true;
	}
	#endif
	
	// TODO Check this
//	@Override
//	public List<? extends StructureStart<?>> startsForFeature(SectionPos sectionPos,
//			StructureFeature<?> structureFeature) {
//		return structFeat.startsForFeature(sectionPos, structureFeature);
//	}
	
	// Skip updating the related tile entities
	@Override
	public boolean setBlock(BlockPos blockPos, BlockState blockState, int i, int j)
	{
		ChunkAccess chunkAccess = this.getChunk(blockPos);
		if (chunkAccess instanceof LevelChunk)
			return true;
		chunkAccess.setBlockState(blockPos, blockState, false);
		// This is for post ticking for water on gen and stuff like that. Not enabled
		// for now.
		// if (blockState.hasPostProcess(this, blockPos))
		// this.getChunk(blockPos).markPosForPostprocessing(blockPos);
		return true;
	}
	
	// Skip Dropping the item on destroy
	@Override
	public boolean destroyBlock(BlockPos blockPos, boolean bl, @Nullable Entity entity, int i)
	{
		BlockState blockState = this.getBlockState(blockPos);
		if (blockState.isAir())
		{
			return false;
		}
		return this.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 3, i);
	}
	
	// Skip BlockEntity stuff. It aren't really needed
	@Override
	public BlockEntity getBlockEntity(BlockPos blockPos)
	{
		BlockState blockState = this.getBlockState(blockPos);
		
		// This is a bypass for the spawner block since MC complains about not having it
		#if POST_MC_1_17_1
		if (blockState.getBlock() instanceof SpawnerBlock)
		{
			return ((EntityBlock) blockState.getBlock()).newBlockEntity(blockPos, blockState);
		}
		else return null;
		#else
		if (blockState.getBlock() instanceof SpawnerBlock) {
			return ((EntityBlock) blockState.getBlock()).newBlockEntity(this);
		} else return null;
		#endif
	}
	
	// Skip BlockEntity stuff. It aren't really needed
	@Override
	public boolean addFreshEntity(Entity entity)
	{
		return true;
	}
	
	// Allays have empty chunks even if it's outside the worldGenRegion
	// @Override
	// public boolean hasChunk(int i, int j) {
	// return true;
	// }
	
	// Override to ensure no other mod mixins cause skipping the overrided
	// getChunk(...)
	@Override
	public ChunkAccess getChunk(int i, int j)
	{
		return this.getChunk(i, j, ChunkStatus.EMPTY);
	}
	
	// Override to ensure no other mod mixins cause skipping the overrided
	// getChunk(...)
	@Override
	public ChunkAccess getChunk(int i, int j, ChunkStatus chunkStatus)
	{
		return this.getChunk(i, j, chunkStatus, true);
	}
	
	// Use this instead of super.getChunk() to bypass C2ME concurrency checks
	private ChunkAccess superGetChunk(int x, int z, ChunkStatus cs)
	{
		int k = x - firstPos.x;
		int l = z - firstPos.z;
		return cache.get(k + l * size);
	}
	
	// Use this instead of super.hasChunk() to bypass C2ME concurrency checks
	public boolean superHasChunk(int x, int z)
	{
		int k = x - firstPos.x;
		int l = z - firstPos.z;
		return l >= 0 && l < size && k >= 0 && k < size;
	}
	
	// Allow creating empty chunks even if it's outside the worldGenRegion
	@Override
	@Nullable
	public ChunkAccess getChunk(int i, int j, ChunkStatus chunkStatus, boolean bl)
	{
		ChunkAccess chunk = getChunkAccess(i, j, chunkStatus, bl);
		if (chunk instanceof LevelChunk)
		{
			chunk = new ImposterProtoChunk((LevelChunk) chunk #if POST_MC_1_18_2 , true #endif );
		}
		return chunk;
	}
	
	private static ChunkStatus debugTriggeredForStatus = null;
	
	private ChunkAccess getChunkAccess(int i, int j, ChunkStatus chunkStatus, boolean bl)
	{
		ChunkAccess chunk = superHasChunk(i, j) ? superGetChunk(i, j, ChunkStatus.EMPTY) : null;
		if (chunk != null && chunk.getStatus().isOrAfter(chunkStatus))
		{
			return chunk;
		}
		if (!bl)
			return null;
		if (chunk == null)
		{
			chunk = chunkMap.get(ChunkPos.asLong(i, j));
			if (chunk == null)
			{
				chunk = generator.generate(i, j);
				if (chunk == null)
					throw new NullPointerException("The provided generator should not return null!");
				chunkMap.put(ChunkPos.asLong(i, j), chunk);
			}
		}
		if (chunkStatus != ChunkStatus.EMPTY && chunkStatus != debugTriggeredForStatus)
		{
			LOGGER.info("WorldGen requiring " + chunkStatus
					+ " outside expected range detected. Force passing EMPTY chunk and seeing if it works.");
			debugTriggeredForStatus = chunkStatus;
		}
		return chunk;
	}
	
	/** Overriding allows us to use our own lighting engine */
	@Override
	public LevelLightEngine getLightEngine() { return this.lightEngine; }
	
	/** Overriding allows us to use our own lighting engine */
	@Override
	public int getBrightness(LightLayer lightLayer, BlockPos blockPos) { return 0; }
	
	/** Overriding allows us to use our own lighting engine */
	@Override
	public int getRawBrightness(BlockPos blockPos, int i) { return 0; }
	
	/** Overriding allows us to use our own lighting engine */
	@Override
	public boolean canSeeSky(BlockPos blockPos)
	{
		return (getBrightness(LightLayer.SKY, blockPos) >= getMaxLightLevel());
	}
	
	public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver)
	{
		return calculateBlockTint(blockPos, colorResolver);
	}
	
	private Biome _getBiome(BlockPos pos)
	{
		#if POST_MC_1_18_2
		return getBiome(pos).value();
		#else
		return getBiome(pos);
		#endif
	}
	
	public int calculateBlockTint(BlockPos blockPos, ColorResolver colorResolver)
	{
		#if PRE_MC_1_19_2
		int i = (Minecraft.getInstance()).options.biomeBlendRadius;
		#else
		int i = (Minecraft.getInstance()).options.biomeBlendRadius().get();
		#endif
		if (i == 0)
			return colorResolver.getColor((Biome) _getBiome(blockPos), blockPos.getX(), blockPos.getZ());
		int j = (i * 2 + 1) * (i * 2 + 1);
		int k = 0;
		int l = 0;
		int m = 0;
		Cursor3D cursor3D = new Cursor3D(blockPos.getX() - i, blockPos.getY(), blockPos.getZ() - i, blockPos.getX() + i, blockPos.getY(), blockPos.getZ() + i);
		BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
		while (cursor3D.advance())
		{
			mutableBlockPos.set(cursor3D.nextX(), cursor3D.nextY(), cursor3D.nextZ());
			int n = colorResolver.getColor((Biome) _getBiome((BlockPos) mutableBlockPos), mutableBlockPos.getX(), mutableBlockPos.getZ());
			k += (n & 0xFF0000) >> 16;
			l += (n & 0xFF00) >> 8;
			m += n & 0xFF;
		}
		return (k / j & 0xFF) << 16 | (l / j & 0xFF) << 8 | m / j & 0xFF;
	}
	
}