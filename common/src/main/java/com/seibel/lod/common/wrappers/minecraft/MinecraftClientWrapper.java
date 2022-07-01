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

package com.seibel.lod.common.wrappers.minecraft;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Objects;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.seibel.lod.common.wrappers.world.LevelWrapper;
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.enums.ELodDirection;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftSharedWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IDimensionTypeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.lod.common.wrappers.McObjectConverter;
import com.seibel.lod.core.objects.DHBlockPos;
import com.seibel.lod.core.objects.DHChunkPos;
import com.seibel.lod.common.wrappers.world.DimensionTypeWrapper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
#if PRE_MC_1_19
import net.minecraft.network.chat.TextComponent;
#endif
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.dimension.DimensionType;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * A singleton that wraps the Minecraft object.
 *
 * @author James Seibel
 * @version 3-5-2022
 */
@Environment(EnvType.CLIENT)
public class MinecraftClientWrapper implements IMinecraftClientWrapper, IMinecraftSharedWrapper
{
    private static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
    
    public static final MinecraftClientWrapper INSTANCE = new MinecraftClientWrapper();

    public final Minecraft mc = Minecraft.getInstance();

    /**
     * The lightmap for the current:
     * Time, dimension, brightness setting, etc.
     */
    private NativeImage lightMap = null;

    private ProfilerWrapper profilerWrapper;


    private MinecraftClientWrapper()
    {

    }



    //================//
    // helper methods //
    //================//

    /**
     * This should be called at the beginning of every frame to
     * clear any Minecraft data that becomes out of date after a frame. <br> <br>
     * <p>
     * LightMaps and other time sensitive objects fall in this category. <br> <br>
     * <p>
     * This doesn't affect OpenGL objects in any way.
     */
    @Override
    public void clearFrameObjectCache()
    {
        lightMap = null;
    }



    //=================//
    // method wrappers //
    //=================//

    @Override
    public float getShade(ELodDirection lodDirection) {
        if (mc.level != null)
        {
            Direction mcDir = McObjectConverter.Convert(lodDirection);
            return mc.level.getShade(mcDir, true);
        }
        else return 0.0f;
    }

    @Override
    public boolean hasSinglePlayerServer()
    {
        return mc.hasSingleplayerServer();
    }

    @Override
    public String getCurrentServerName()
    {
        return mc.getCurrentServer().name;
    }

    @Override
    public String getCurrentServerIp()
    {
        return mc.getCurrentServer().ip;
    }

    @Override
    public String getCurrentServerVersion()
    {
        return mc.getCurrentServer().version.getString();
    }

    /** Returns the dimension the player is currently in */
    @Override
    public IDimensionTypeWrapper getCurrentDimension()
    {
        if (mc.player != null)
            return DimensionTypeWrapper.getDimensionTypeWrapper(mc.player.level.dimensionType());
        else return null;
    }

    @Override
    public String getCurrentDimensionId()
    {
        return LodUtil.getDimensionIDFromWorld(LevelWrapper.getWorldWrapper(mc.level));
    }

    //=============//
    // Simple gets //
    //=============//

    public LocalPlayer getPlayer()
    {
        return mc.player;
    }

    @Override
    public boolean playerExists()
    {
        return mc.player != null;
    }

    @Override
    public DHBlockPos getPlayerBlockPos()
    {
        BlockPos playerPos = getPlayer().blockPosition();
        return new DHBlockPos(playerPos.getX(), playerPos.getY(), playerPos.getZ());
    }

    @Override
    public DHChunkPos getPlayerChunkPos()
    {
        #if PRE_MC_1_17_1
        ChunkPos playerPos = new ChunkPos(getPlayer().blockPosition());
        #else
        ChunkPos playerPos = getPlayer().chunkPosition();
        #endif
        return new DHChunkPos(playerPos.x, playerPos.z);
    }

    public ModelManager getModelManager()
    {
        return mc.getModelManager();
    }

    public ClientLevel getClientLevel()
    {
        return mc.level;
    }

    @Override
    public ILevelWrapper getWrappedServerWorld()
    {
        if (mc.level == null)
            return null;

        DimensionType dimension = mc.level.dimensionType();
        IntegratedServer server = mc.getSingleplayerServer();

        if (server == null)
            return null;

        ServerLevel serverWorld = null;
        Iterable<ServerLevel> worlds = server.getAllLevels();
        for (ServerLevel world : worlds)
        {
            if (world.dimensionType() == dimension)
            {
                serverWorld = world;
                break;
            }
        }
        return LevelWrapper.getWorldWrapper(serverWorld);
    }

    public LevelWrapper getWrappedClientLevel()
    {
        return LevelWrapper.getWorldWrapper(mc.level);
    }

    public LevelWrapper getWrappedServerLevel()
    {

        if (mc.level == null)
            return null;
        DimensionType dimension = mc.level.dimensionType();
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null)
            return null;

        Iterable<ServerLevel> worlds = server.getAllLevels();
        ServerLevel returnWorld = null;

        for (ServerLevel world : worlds)
        {
            if (world.dimensionType() == dimension)
            {
                returnWorld = world;
                break;
            }
        }

        return LevelWrapper.getWorldWrapper(returnWorld);
    }

    @Nullable
    @Override
    public ILevelWrapper getWrappedClientWorld()
    {
        return LevelWrapper.getWorldWrapper(mc.level);
    }

    @Override
    public File getGameDirectory()
    {
        return mc.gameDirectory;
    }

    @Override
    public IProfilerWrapper getProfiler()
    {
        if (profilerWrapper == null)
            profilerWrapper = new ProfilerWrapper(mc.getProfiler());
        else if (mc.getProfiler() != profilerWrapper.profiler)
            profilerWrapper.profiler = mc.getProfiler();

        return profilerWrapper;	}

    public ClientPacketListener getConnection()
    {
        return mc.getConnection();
    }

    public GameRenderer getGameRenderer()
    {
        return mc.gameRenderer;
    }

    public Entity getCameraEntity()
    {
        return mc.cameraEntity;
    }

    public Window getWindow()
    {
        return mc.getWindow();
    }

    @Override
    public float getSkyDarken(float partialTicks)
    {
        return mc.level.getSkyDarken(partialTicks);
    }

    public IntegratedServer getSinglePlayerServer()
    {
        return mc.getSingleplayerServer();
    }

    @Override
    public boolean connectedToServer()
    {
        return mc.getCurrentServer() != null;
    }

    @Override
    public int getPlayerSkylight() {
        if (mc.level == null) return -1;
        if (mc.player == null) return -1;
        if (mc.player.blockPosition() == null) return -1;
        return mc.level.getBrightness(LightLayer.SKY, mc.player.blockPosition());
    }

    public ServerData getCurrentServer()
    {
        return mc.getCurrentServer();
    }

    public LevelRenderer getLevelRenderer()
    {
        return mc.levelRenderer;
    }

    /** Returns all worlds available to the server */
    @Override
    public ArrayList<ILevelWrapper> getAllServerWorlds()
    {
        ArrayList<ILevelWrapper> worlds = new ArrayList<ILevelWrapper>();

        Iterable<ServerLevel> serverWorlds = mc.getSingleplayerServer().getAllLevels();
        for (ServerLevel world : serverWorlds)
        {
            worlds.add(LevelWrapper.getWorldWrapper(world));
        }

        return worlds;
    }



    @Override
    public void sendChatMessage(String string)
    {
        #if PRE_MC_1_19
        getPlayer().sendMessage(new TextComponent(string), getPlayer().getUUID());
        #else
        getPlayer().sendSystemMessage(Component.translatable(string));
        #endif
    }

    /**
     * Crashes Minecraft, displaying the given errorMessage <br> <br>
     * In the following format: <br>
     *
     * The game crashed whilst <strong>errorMessage</strong>  <br>
     * Error: <strong>ExceptionClass: exceptionErrorMessage</strong>  <br>
     * Exit Code: -1  <br>
     */
    @Override
    public void crashMinecraft(String errorMessage, Throwable exception)
    {
        LOGGER.error(ModInfo.READABLE_NAME + " had the following error: [" + errorMessage + "]. Crashing Minecraft...");
        CrashReport report = new CrashReport(errorMessage, exception);
        Minecraft.crash(report);
    }

    @Override
    public Object getOptionsObject()
    {
        return mc.options;
    }

    @Override
    public File getSinglePlayerServerFolder() {
        return Objects.requireNonNull(mc.getSingleplayerServer()).getServerDirectory();
    }

    @Override
    public boolean isDedicatedServer() {
        return false;
    }

    @Override
    public File getInstallationDirectory() {
        return mc.gameDirectory;
    }
}
