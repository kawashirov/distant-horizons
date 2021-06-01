package com.seibel.lod.objects;

import java.util.Hashtable;
import java.util.Map;

import net.minecraft.world.DimensionType;

/**
 * This stores all LODs for a given world.
 * 
 * @author James Seibel
 * @version 04-01-2021
 */
public class LodWorld
{
	private String worldName;
	
	private Map<DimensionType, LodDimension> lodDimensions;
	/** If true then the LOD world is setup and ready to use */
	private boolean isWorldLoaded = false;
	
	public static final String NO_WORLD_LOADED = "No world loaded";
	
	public LodWorld()
	{
		worldName = NO_WORLD_LOADED;
	}
	
	/**
	 * Set up the LodWorld with the given newWorldName. <br>
	 * This should be done whenever loading a new world.
	 * @param newWorldName
	 */
	public void selectWorld(String newWorldName)
	{
		if(newWorldName.isEmpty())
		{
			deselectWorld();
			return;
		}
		
		if (worldName.equals(newWorldName))
			// don't recreate everything if we
			// didn't actually change worlds
			return;
		
		worldName = newWorldName;
		lodDimensions = new Hashtable<DimensionType, LodDimension>();
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
	
	
	public void addLodDimension(LodDimension newStorage)
	{
		if (lodDimensions == null)
			throw new IllegalStateException("LodWorld hasn't been given a world yet.");
		
		lodDimensions.put(newStorage.dimension, newStorage);
	}
	
	public LodDimension getLodDimension(DimensionType dimension)
	{
		if (lodDimensions == null)
			throw new IllegalStateException("LodWorld hasn't been given a world yet.");
		
		return lodDimensions.get(dimension);
	}
	
	/**
	 * Resizes the max width in regions that each LodDimension
	 * should use. 
	 */
	public void resizeDimensionRegionWidth(int newWidth)
	{
		if (lodDimensions == null)
			throw new IllegalStateException("LodWorld hasn't been given a world yet.");
		
		for(DimensionType key : lodDimensions.keySet())
			lodDimensions.get(key).setRegionWidth(newWidth);
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
