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

import com.seibel.lod.common.wrappers.minecraft.MinecraftClientWrapper;
import com.seibel.lod.core.a7.world.WorldEnvironment;
import com.seibel.lod.core.api.internal.a7.SharedApi;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.objects.DHChunkPos;
import com.seibel.lod.core.enums.ELevelType;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftSharedWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.lod.common.wrappers.chunk.ChunkWrapper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
public class LevelWrapper implements ILevelWrapper
{
    private static final ConcurrentMap<LevelAccessor, LevelWrapper> levelWrapperMap = new ConcurrentHashMap<>();
    private final LevelAccessor level;
    public final ELevelType levelType;
    private static final IMinecraftSharedWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftSharedWrapper.class);
    
    
    public LevelWrapper(LevelAccessor newWorld)
    {
        level = newWorld;
        
        if (level.getClass() == ServerLevel.class)
            levelType = ELevelType.SERVER_LEVEL;
        else if (level.getClass() == ClientLevel.class)
            levelType = ELevelType.CLIENT_LEVEL;
        else
            levelType = ELevelType.UNKNOWN;
    }

    //@Environment(EnvType.CLIENT)
    private static LevelAccessor getSinglePlayerServerLevel() {
        MinecraftClientWrapper client = MinecraftClientWrapper.INSTANCE;
        return client.mc.getSingleplayerServer().getPlayerList()
                .getPlayer(client.mc.player.getUUID()).getLevel();
    }
    
    
    @Nullable
    public static LevelWrapper getWorldWrapper(LevelAccessor level)
    {
        if (level == null) return null;

        if (level.isClientSide() && SharedApi.getEnvironment()
                == WorldEnvironment.Client_Server) {
            level = getSinglePlayerServerLevel();
        }

        //first we check if the level has already been wrapped
        if(levelWrapperMap.containsKey(level) && levelWrapperMap.get(level) != null)
            return levelWrapperMap.get(level);
        
        
        //if it hasn't been created yet, we create it and save it in the map
        LevelWrapper levelWrapper = new LevelWrapper(level);
        levelWrapperMap.put(level, levelWrapper);
        
        //we return the newly created wrapper
        return levelWrapper;
    }
    
    public static void clearMap()
    {
        levelWrapperMap.clear();
    }
    
    @Override
    public ELevelType getLevelType()
    {
        return levelType;
    }
    
    @Override
    public DimensionTypeWrapper getDimensionType()
    {
        return DimensionTypeWrapper.getDimensionTypeWrapper(level.dimensionType());
    }
    
    @Override
    public int getBlockLight(int x, int y, int z)
    {
        return level.getBrightness(LightLayer.BLOCK, new BlockPos(x,y,z));
    }
    
    @Override
    public int getSkyLight(int x, int y, int z)
    {
        return level.getBrightness(LightLayer.SKY, new BlockPos(x,y,z));
    }
    
    public LevelAccessor getLevel()
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
    public short getMinHeight()
    {
        #if PRE_MC_1_17_1
        return (short) 0;
        #else
        return (short) level.getMinBuildHeight();
        #endif
    }
    
    /** @throws UnsupportedOperationException if the WorldWrapper isn't for a ServerWorld */
    @Override
    public File getSaveFolder() throws UnsupportedOperationException
    {
        if (levelType != ELevelType.SERVER_LEVEL)
            throw new UnsupportedOperationException("getSaveFolder can only be called for ServerWorlds.");
        
        ServerChunkCache chunkSource = ((ServerLevel) level).getChunkSource();
        return chunkSource.getDataStorage().dataFolder;
    }
    
    
    /** @throws UnsupportedOperationException if the WorldWrapper isn't for a ServerWorld */
    public ServerLevel getServerWorld() throws UnsupportedOperationException
    {
        if (levelType != ELevelType.SERVER_LEVEL)
            throw new UnsupportedOperationException("getSaveFolder can only be called for ServerWorlds.");
        
        return (ServerLevel) level;
    }
    
    @Override
    public int getSeaLevel()
    {
        // TODO this is depreciated, what should we use instead?
        return level.getSeaLevel();
    }
    
    @Override
    public IChunkWrapper tryGetChunk(DHChunkPos pos) {
        ChunkAccess chunk = level.getChunk(pos.getX(), pos.getZ(), ChunkStatus.EMPTY, false);
        if (chunk == null) return null;
        return new ChunkWrapper(chunk, level);
    }
    
    @Override
    public boolean hasChunkLoaded(int chunkX, int chunkZ) {
        // world.hasChunk(chunkX, chunkZ); THIS DOES NOT WORK FOR CLIENT LEVEL CAUSE MOJANG ALWAYS RETURN TRUE FOR THAT!
        ChunkSource source = level.getChunkSource();
        return source.hasChunk(chunkX, chunkZ);
    }

    @Override
    public String toString() {
        return "Wrapped{" + level.toString() + "@" + getDimensionType().getDimensionName() + "}";
    }
}
