/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.core.wrapperInterfaces.minecraft;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;

import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.misc.ILightMapWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IDimensionTypeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;

/**
 * Contains everything related to the Minecraft object.
 * 
 * @author James Seibel
 * @version 9-16-2021
 */
public interface IMinecraftWrapper
{
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
	public void clearFrameObjectCache();
	
	
	
	//=================//
	// method wrappers //
	//=================//
	
	public float getShade(LodDirection lodDirection);
	
	public boolean hasSinglePlayerServer();
	
	public String getCurrentServerName();
	public String getCurrentServerIp();
	public String getCurrentServerVersion();
	
	/** Returns the dimension the player is currently in */
	public IDimensionTypeWrapper getCurrentDimension();
	
	public String getCurrentDimensionId();
	
	/** This texture changes every frame */
	ILightMapWrapper getCurrentLightMap();
	
	/**
	 * Returns the color int at the given pixel coordinates
	 * from the current lightmap.
	 * @param u x location in texture space
	 * @param v z location in texture space
	 */
	public int getColorIntFromLightMap(int u, int v);
	
	/**
	 * Returns the Color at the given pixel coordinates
	 * from the current lightmap.
	 * @param u x location in texture space
	 * @param v z location in texture space
	 */
	public Color getColorFromLightMap(int u, int v);
	
	
	
	
	//=============//
	// Simple gets //
	//=============//
	
	public boolean playerExists();
	
	public AbstractBlockPosWrapper getPlayerBlockPos();
	
	public AbstractChunkPosWrapper getPlayerChunkPos();
	
	/** 
	 * Attempts to get the ServerWorld for the dimension
	 * the user is currently in.
	 * @returns null if no ServerWorld is available
	 */
	public IWorldWrapper getWrappedServerWorld();
	
	public IWorldWrapper getWrappedClientWorld();
	
	public File getGameDirectory();
	
	public IProfilerWrapper getProfiler();
	
	public float getSkyDarken(float partialTicks);
	
	boolean connectedToServer();
	
	/** Returns all worlds available to the server */
	public ArrayList<IWorldWrapper> getAllServerWorlds();
	
	
	
	public void sendChatMessage(String string);
	
	/**
	 * Crashes Minecraft, displaying the given errorMessage <br> <br>
	 * In the following format: <br>
	 * 
	 * The game crashed whilst <strong>errorMessage</strong>  <br>
	 * Error: <strong>ExceptionClass: exceptionErrorMessage</strong>  <br>
	 * Exit Code: -1  <br>
	 */
	public void crashMinecraft(String errorMessage, Throwable exception);

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
