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

package com.seibel.distanthorizons.common.wrappers.minecraft;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.UUID;

import com.mojang.blaze3d.platform.NativeImage;
import com.seibel.distanthorizons.api.enums.config.ELodShading;
import com.seibel.distanthorizons.common.wrappers.McObjectConverter;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.common.wrappers.world.ServerLevelWrapper;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.enums.EDhDirection;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.coreapi.ModInfo;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftSharedWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.distanthorizons.core.pos.DhBlockPos;
import com.seibel.distanthorizons.core.pos.DhChunkPos;

import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
#if PRE_MC_1_19_2
import net.minecraft.network.chat.TextComponent;
#endif
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * A singleton that wraps the Minecraft object.
 *
 * @author James Seibel
 * @version 3-5-2022
 */
//@Environment(EnvType.CLIENT)
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
	public float getShade(EDhDirection lodDirection)
	{
		ELodShading lodShading = Config.Client.Advanced.Graphics.AdvancedGraphics.lodShading.get();
		switch (lodShading)
		{
			default:
			case MINECRAFT:
				if (this.mc.level != null)
				{
					Direction mcDir = McObjectConverter.Convert(lodDirection);
					return this.mc.level.getShade(mcDir, true);
				}
				else
				{
					return 0.0f;
				}
			
			case OLD_LIGHTING:
				switch (lodDirection)
				{
					case DOWN:
						return 0.5F;
					default:
					case UP:
						return 1.0F;
					case NORTH:
					case SOUTH:
						return 0.8F;
					case WEST:
					case EAST:
						return 0.6F;
				}
			
			case NONE:
				return 1.0F;
		}
	}
	
	@Override
	public boolean hasSinglePlayerServer() { return mc.hasSingleplayerServer(); }
	@Override
	public boolean clientConnectedToDedicatedServer() { return mc.getCurrentServer() != null && !this.hasSinglePlayerServer(); }
	
	@Override
	public String getCurrentServerName() { return mc.getCurrentServer().name; }
	
	@Override
	public String getCurrentServerIp() { return mc.getCurrentServer().ip; }
	
	@Override
	public String getCurrentServerVersion()
	{
		return mc.getCurrentServer().version.getString();
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
	public UUID getPlayerUUID()
	{
		return getPlayer().getUUID();
	}
	
	@Override
	public DhBlockPos getPlayerBlockPos()
	{
		BlockPos playerPos = getPlayer().blockPosition();
		return new DhBlockPos(playerPos.getX(), playerPos.getY(), playerPos.getZ());
	}
	
	@Override
	public DhChunkPos getPlayerChunkPos()
	{
        #if PRE_MC_1_17_1
        ChunkPos playerPos = new ChunkPos(getPlayer().blockPosition());
        #else
		ChunkPos playerPos = getPlayer().chunkPosition();
        #endif
		return new DhChunkPos(playerPos.x, playerPos.z);
	}
	
	public ModelManager getModelManager()
	{
		return mc.getModelManager();
	}
	
	@Nullable
	@Override
	public IClientLevelWrapper getWrappedClientLevel()
	{
		if (this.mc.level == null)
		{
			return null;
		}
		
		return ClientLevelWrapper.getWrapperIgnoringOverride(this.mc.level);
	}
	
	/** Please move over to getInstallationDirectory() */
	@Deprecated
	@Override
	public File getGameDirectory()
	{
		return getInstallationDirectory();
	}
	
	@Override
	public IProfilerWrapper getProfiler()
	{
		if (profilerWrapper == null)
			profilerWrapper = new ProfilerWrapper(mc.getProfiler());
		else if (mc.getProfiler() != profilerWrapper.profiler)
			profilerWrapper.profiler = mc.getProfiler();
		return profilerWrapper;
	}
	
	/** Returns all worlds available to the server */
	@Override
	public ArrayList<ILevelWrapper> getAllServerWorlds()
	{
		ArrayList<ILevelWrapper> worlds = new ArrayList<ILevelWrapper>();
		
		Iterable<ServerLevel> serverWorlds = mc.getSingleplayerServer().getAllLevels();
		for (ServerLevel world : serverWorlds)
		{
			worlds.add(ServerLevelWrapper.getWrapper(world));
		}
		
		return worlds;
	}
	
	
	
	@Override
	public void sendChatMessage(String string)
	{
		LocalPlayer p = getPlayer();
		if (p == null) return;
        #if PRE_MC_1_19_2
		p.sendMessage(new TextComponent(string), getPlayer().getUUID());
        #else
		p.sendSystemMessage(net.minecraft.network.chat.Component.translatable(string));
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
		LOGGER.error(ModInfo.READABLE_NAME + " had the following error: [" + errorMessage + "]. Crashing Minecraft...", exception);
		CrashReport report = new CrashReport(errorMessage, exception);
		Minecraft.crash(report);
	}
	
	@Override
	public Object getOptionsObject()
	{
		return mc.options;
	}
	
	@Override
	public boolean isDedicatedServer()
	{
		return false;
	}
	
	@Override
	public File getInstallationDirectory()
	{
		return mc.gameDirectory;
	}
	
	@Override
	public void executeOnRenderThread(Runnable runnable) { this.mc.execute(runnable); }
	
}
