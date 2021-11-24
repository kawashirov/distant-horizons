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

package com.seibel.lod.core.objects.lod;

import java.util.Hashtable;
import java.util.Map;

import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.wrapperInterfaces.world.IDimensionTypeWrapper;

/**
 * This stores all LODs for a given world.
 * 
 * @author James Seibel
 * @author Leonardo Amato
 * @version 9-27-2021
 */
public class LodWorld
{
	/** name of this world */
	private String worldName;
	
	/** dimensions in this world */
	private Map<IDimensionTypeWrapper, LodDimension> lodDimensions;
	
	/** If true then the LOD world is setup and ready to use */
	private boolean isWorldLoaded = false;
	
	/** the name given to the world if it isn't loaded */
	public static final String NO_WORLD_LOADED = "No world loaded";
	
	
	
	public LodWorld()
	{
		worldName = NO_WORLD_LOADED;
	}
	
	
	
	/**
	 * Set up the LodWorld with the given newWorldName. <br>
	 * This should be done whenever loading a new world. <br><br>
	 * <p>
	 * Note a System.gc() call may be in order after calling this <Br>
	 * since a lot of LOD data is now homeless. <br>
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
		lodDimensions = new Hashtable<>();
		isWorldLoaded = true;
	}
	
	/**
	 * Set the worldName to "No world loaded"
	 * and clear the lodDimensions Map. <br>
	 * This should be done whenever unloaded a world. <br><br>
	 * <p>
	 * Note a System.gc() call may be in order after calling this <Br>
	 * since a lot of LOD data is now homeless. <br>
	 */
	public void deselectWorld()
	{
		worldName = NO_WORLD_LOADED;
		lodDimensions = null;
		isWorldLoaded = false;
	}
	
	
	/**
	 * Adds newDimension to this world, if a LodDimension
	 * already exists for the given dimension it is replaced.
	 */
	public void addLodDimension(LodDimension newDimension)
	{
		if (lodDimensions == null)
			return;
		
		lodDimensions.put(newDimension.dimension, newDimension);
	}
	
	/**
	 * Returns null if no LodDimension exists for the given dimension
	 */
	public LodDimension getLodDimension(IDimensionTypeWrapper dimType)
	{
		if (lodDimensions == null)
			return null;
		
		return lodDimensions.get(dimType);
	}
	
	/**
	 * Resizes the max width in regions that each LodDimension
	 * should use.
	 */
	public void resizeDimensionRegionWidth(int newRegionWidth)
	{
		if (lodDimensions == null)
			return;
		
		saveAllDimensions();
		
		for (IDimensionTypeWrapper key : lodDimensions.keySet())
			lodDimensions.get(key).setRegionWidth(newRegionWidth);
	}
	
	/**
	 * Requests all dimensions save any dirty regions they may have.
	 */
	public void saveAllDimensions()
	{
		if (lodDimensions == null)
			return;
		
		// TODO we should only print this if lods were actually saved to file
		// but that requires a LodDimension.hasDirtyRegions() method or something similar
		ClientApi.LOGGER.info("Saving LODs");
		
		for (IDimensionTypeWrapper key : lodDimensions.keySet())
			lodDimensions.get(key).saveDirtyRegionsToFileAsync();
	}
	
	
	public boolean getIsWorldNotLoaded()
	{
		return !isWorldLoaded;
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

