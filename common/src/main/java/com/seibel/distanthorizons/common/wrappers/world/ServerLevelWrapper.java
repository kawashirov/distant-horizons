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

package com.seibel.distanthorizons.common.wrappers.world;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiLevelType;
import com.seibel.distanthorizons.common.wrappers.McObjectConverter;
import com.seibel.distanthorizons.common.wrappers.block.BiomeWrapper;
import com.seibel.distanthorizons.common.wrappers.block.BlockStateWrapper;
import com.seibel.distanthorizons.common.wrappers.block.cache.ServerBlockDetailMap;
import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftClientWrapper;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhBlockPos;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;

import com.seibel.distanthorizons.core.wrapperInterfaces.world.IServerLevelWrapper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;

import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * @version 2022-9-16
 */
public class ServerLevelWrapper implements IServerLevelWrapper
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	private static final ConcurrentHashMap<ServerLevel, ServerLevelWrapper> LEVEL_WRAPPER_BY_SERVER_LEVEL = new ConcurrentHashMap<>();
	
	final ServerLevel level;
	ServerBlockDetailMap blockMap = new ServerBlockDetailMap(this);
	
	
	
	//==============//
	// constructors //
	//==============//
	
	public static ServerLevelWrapper getWrapper(ServerLevel level) { return LEVEL_WRAPPER_BY_SERVER_LEVEL.computeIfAbsent(level, ServerLevelWrapper::new); }
	
	public ServerLevelWrapper(ServerLevel level)
	{
		this.level = level;
	}
	
	
	
	//=========//
	// methods //
	//=========//
	
	@Nullable
	@Override
	public IClientLevelWrapper tryGetClientLevelWrapper()
	{
		MinecraftClientWrapper client = MinecraftClientWrapper.INSTANCE;
		if (client.mc.level == null)
		{
			return null;
		}
		
		return ClientLevelWrapper.getWrapper(client.mc.level);
	}
	
	@Override
	public File getSaveFolder()
	{
		return level.getChunkSource().getDataStorage().dataFolder;
	}
	
	@Override
	public DimensionTypeWrapper getDimensionType()
	{
		return DimensionTypeWrapper.getDimensionTypeWrapper(level.dimensionType());
	}
	
	@Override
	public EDhApiLevelType getLevelType() { return EDhApiLevelType.SERVER_LEVEL; }
	
	public ServerLevel getLevel()
	{
		return level;
	}
	
	@Override
	public boolean hasCeiling()
	{
		return level.dimensionType().hasCeiling();
	}
	
	@Override
	public boolean hasSkyLight()
	{
		return level.dimensionType().hasSkyLight();
	}
	
	@Override
	public int getHeight()
	{
		return level.getHeight();
	}
	
	@Override
	public int getMinHeight()
	{
        #if PRE_MC_1_17_1
        return 0;
        #else
		return level.getMinBuildHeight();
        #endif
	}
	@Override
	public IChunkWrapper tryGetChunk(DhChunkPos pos)
	{
		if (!level.hasChunk(pos.x, pos.z)) return null;
		ChunkAccess chunk = level.getChunk(pos.x, pos.z, ChunkStatus.FULL, false);
		if (chunk == null) return null;
		return new ChunkWrapper(chunk, level, this);
	}
	
	@Override
	public boolean hasChunkLoaded(int chunkX, int chunkZ)
	{
		// world.hasChunk(chunkX, chunkZ); THIS DOES NOT WORK FOR CLIENT LEVEL CAUSE MOJANG ALWAYS RETURN TRUE FOR THAT!
		ChunkSource source = level.getChunkSource();
		return source.hasChunk(chunkX, chunkZ);
	}
	
	@Override
	public IBlockStateWrapper getBlockState(DhBlockPos pos)
	{
		return BlockStateWrapper.fromBlockState(level.getBlockState(McObjectConverter.Convert(pos)), this);
	}
	
	@Override
	public IBiomeWrapper getBiome(DhBlockPos pos)
	{
		return BiomeWrapper.getBiomeWrapper(level.getBiome(McObjectConverter.Convert(pos)), this);
	}
	
	@Override
	public ServerLevel getWrappedMcObject()
	{
		return level;
	}
	
	@Override
	public void onUnload() { LEVEL_WRAPPER_BY_SERVER_LEVEL.remove(this.level); }
	
	@Override
	public String toString()
	{
		return "Wrapped{" + level.toString() + "@" + getDimensionType().getDimensionName() + "}";
	}
	
}
