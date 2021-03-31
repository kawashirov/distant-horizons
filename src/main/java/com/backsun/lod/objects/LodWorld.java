package com.backsun.lod.objects;

import java.util.Hashtable;
import java.util.Map;

import net.minecraft.world.DimensionType;

/**
 * This stores all LODs for a given world.
 * 
 * @author James Seibel
 * @version 03-31-2021
 */
public class LodWorld
{
	private String worldName;
	
	private Map<DimensionType, LodDimension> lodDimensions;
	
	
	public LodWorld()
	{
		worldName = "No world loaded";
	}
	
	/**
	 * Set up the LodWorld with the given newWorldName. <br>
	 * This should be done whenever loading a new world.
	 * @param newWorldName
	 */
	public void selectWorld(String newWorldName)
	{
		worldName = newWorldName;
		lodDimensions = new Hashtable<DimensionType, LodDimension>();
	}
	
	/**
	 * Set the worldName to "No world loaded"
	 * and clear the lodDimensions Map. <br>
	 * This should be done whenever unloaded a world. 
	 */
	public void deselectWorld()
	{
		worldName = "No world loaded";
		lodDimensions = null;
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
