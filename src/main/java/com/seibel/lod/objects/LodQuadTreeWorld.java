/*
 *    This file is part of the LOD Mod, licensed under the GNU GPL v3 License.
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
package com.seibel.lod.objects;

import java.util.Hashtable;
import java.util.Map;

import com.seibel.lod.proxy.ClientProxy;

import net.minecraft.world.DimensionType;

/**
 * This stores all LODs for a given world.
 * 
 * @author James Seibel
 * @author Leonardo Amato
 * @version 8-17-2021
 */
public class LodQuadTreeWorld
{
	private String worldName;
	
	private Map<DimensionType, LodQuadTreeDimension> lodDimensions;
	
	/** If true then the LOD world is setup and ready to use */
	private boolean isWorldLoaded = false;
	
	public static final String NO_WORLD_LOADED = "No world loaded";
	
	
	
	public LodQuadTreeWorld()
	{
		worldName = NO_WORLD_LOADED;
	}
	
	/**
	 * Set up the LodQuadTreeWorld with the given newWorldName. <br>
	 * This should be done whenever loading a new world.
	 * 
	 * @param newWorldName name of the world
	 */
	public void selectWorld(String newWorldName)
	{
		if (newWorldName.isEmpty())
		{
			deselectWorld();
			return;
		}
		
		if (worldName.equals(newWorldName))
			// don't recreate everything if we
			// didn't actually change worlds
			return;
		
		worldName = newWorldName;
		lodDimensions = new Hashtable<DimensionType, LodQuadTreeDimension>();
		isWorldLoaded = true;
	}
	
	/**
	 * Set the worldName to "No world loaded"
	 * and clear the lodDimensions Map. <br>
	 * This should be done whenever unloaded a world.
	 */
	public void deselectWorld()
	{
		worldName = NO_WORLD_LOADED;
		lodDimensions = null;
		isWorldLoaded = false;
	}
	
	
	/**
	 * Adds newStorage to this world, if a LodQuadTreeDimension
	 * already exists for the given dimension it is replaced.
	 */
	public void addLodDimension(LodQuadTreeDimension newStorage)
	{
		if (lodDimensions == null)
			return;
		
		lodDimensions.put(newStorage.dimension, newStorage);
	}
	
	/**
	 * Returns null if no LodQuadTreeDimension exists for the given dimension
	 */
	public LodQuadTreeDimension getLodDimension(DimensionType dimension)
	{
		if (lodDimensions == null)
			return null;
		
		return lodDimensions.get(dimension);
	}
	
	/**
	 * Resizes the max width in regions that each LodDimension
	 * should use.
	 */
	public void resizeDimensionRegionWidth(int newWidth)
	{
		if (lodDimensions == null)
			return;
		
		saveAllDimensions();
		
		for (DimensionType key : lodDimensions.keySet())
			lodDimensions.get(key).setRegionWidth(newWidth);
	}
	
	/**
	 * Requests all dimensions save any dirty regions they may have.
	 */
	public void saveAllDimensions()
	{
		if (lodDimensions == null)
			return;
		
		ClientProxy.LOGGER.info("Saving LODs");
		
		for (DimensionType key : lodDimensions.keySet())
			lodDimensions.get(key).saveDirtyRegionsToFileAsync();
	}
	
	
	public boolean getIsWorldLoaded()
	{
		return isWorldLoaded;
	}
	
	public String getWorldName()
	{
		return worldName;
	}
	
	@Override
	public String toString()
	{
		return "World name: " + worldName;
	}
}

