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

package com.seibel.lod.common.wrappers.world;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.seibel.lod.core.objects.DHChunkPos;
import com.seibel.lod.core.enums.EWorldType;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;
import com.seibel.lod.common.wrappers.chunk.ChunkWrapper;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;

import org.jetbrains.annotations.Nullable;

/**
 * @author James Seibel
 * @author ??
 * @version 11-21-2021
 */
public class WorldWrapper implements IWorldWrapper
{
    private static final ConcurrentMap<LevelAccessor, WorldWrapper> worldWrapperMap = new ConcurrentHashMap<>();
    private final LevelAccessor world;
    public final EWorldType worldType;
    
    
    public WorldWrapper(LevelAccessor newWorld)
    {
        world = newWorld;
        
        if (world.getClass() == ServerLevel.class)
            worldType = EWorldType.ServerWorld;
        else if (world.getClass() == ClientLevel.class)
            worldType = EWorldType.ClientWorld;
        else
            worldType = EWorldType.Unknown;
    }
    
    
    @Nullable
    public static WorldWrapper getWorldWrapper(LevelAccessor world)
    {
        if (world == null) return null;
        //first we check if the biome has already been wrapped
        if(worldWrapperMap.containsKey(world) && worldWrapperMap.get(world) != null)
            return worldWrapperMap.get(world);
        
        
        //if it hasn't been created yet, we create it and save it in the map
        WorldWrapper worldWrapper = new WorldWrapper(world);
        worldWrapperMap.put(world, worldWrapper);
        
        //we return the newly created wrapper
        return worldWrapper;
    }
    
    public static void clearMap()
    {
        worldWrapperMap.clear();
    }
    
    @Override
    public EWorldType getWorldType()
    {
        return worldType;
    }
    
    @Override
    public DimensionTypeWrapper getDimensionType()
    {
        return DimensionTypeWrapper.getDimensionTypeWrapper(world.dimensionType());
    }
    
    @Override
    public int getBlockLight(int x, int y, int z)
    {
        return world.getBrightness(LightLayer.BLOCK, new BlockPos(x,y,z));
    }
    
    @Override
    public int getSkyLight(int x, int y, int z)
    {
        return world.getBrightness(LightLayer.SKY, new BlockPos(x,y,z));
    }
    
    public LevelAccessor getWorld()
    {
        return world;
    }
    
    @Override
    public boolean hasCeiling()
    {
        return world.dimensionType().hasCeiling();
    }
    
    @Override
    public boolean hasSkyLight()
    {
        return world.dimensionType().hasSkyLight();
    }
    
    @Override
    public int getHeight()
    {
        return world.getHeight();
    }
    
    @Override
    public short getMinHeight()
    {
        #if PRE_MC_1_17_1
        return (short) 0;
        #else
        return (short) world.getMinBuildHeight();
        #endif
    }
    
    /** @throws UnsupportedOperationException if the WorldWrapper isn't for a ServerWorld */
    @Override
    public File getSaveFolder() throws UnsupportedOperationException
    {
        if (worldType != EWorldType.ServerWorld)
            throw new UnsupportedOperationException("getSaveFolder can only be called for ServerWorlds.");
        
        ServerChunkCache chunkSource = ((ServerLevel) world).getChunkSource();
        return chunkSource.getDataStorage().dataFolder;
    }
    
    
    /** @throws UnsupportedOperationException if the WorldWrapper isn't for a ServerWorld */
    public ServerLevel getServerWorld() throws UnsupportedOperationException
    {
        if (worldType != EWorldType.ServerWorld)
            throw new UnsupportedOperationException("getSaveFolder can only be called for ServerWorlds.");
        
        return (ServerLevel) world;
    }
    
    @Override
    public int getSeaLevel()
    {
        // TODO this is depreciated, what should we use instead?
        return world.getSeaLevel();
    }
    
    @Override
    public IChunkWrapper tryGetChunk(DHChunkPos pos) {
        ChunkAccess chunk = world.getChunk(pos.getX(), pos.getZ(), ChunkStatus.EMPTY, false);
        if (chunk == null) return null;
        return new ChunkWrapper(chunk, world);
    }
    
    @Override
    public boolean hasChunkLoaded(int chunkX, int chunkZ) {
        // world.hasChunk(chunkX, chunkZ); THIS DOES NOT WORK FOR CLIENT LEVEL CAUSE MOJANG ALWAYS RETURN TRUE FOR THAT!
        ChunkSource source = world.getChunkSource();
        return source.hasChunk(chunkX, chunkZ);
    }
}
