package com.backsun.lod.objects;

import java.util.Hashtable;
import java.util.Map;

import net.minecraft.world.DimensionType;

/**
 * This stores all LODs for a given world.
 * 
 * @author James Seibel
 * @version 02-22-2021
 */
public class LodWorld
{
	public String worldName;
	
	/**
	 * Key = Dimension id (as an int)
	 */
	private Map<DimensionType, LodDimension> lodDimensions;
	
	
	public LodWorld(String newWorldName)
	{
		worldName = newWorldName;
		lodDimensions = new Hashtable<DimensionType, LodDimension>();
	}
	
	
	
	public void addLodDimension(LodDimension newStorage)
	{
		lodDimensions.put(newStorage.dimension, newStorage);
	}
	
	public LodDimension getLodDimension(DimensionType dimension)
	{
		return lodDimensions.get(dimension);
	}
	
	/**
	 * Resizes the max width in regions that each LodDimension
	 * should use. 
	 */
	public void resizeDimensionRegionWidth(int newWidth)
	{
		for(DimensionType key : lodDimensions.keySet())
			lodDimensions.get(key).setRegionWidth(newWidth);
	}
	
	
	
	@Override
	public String toString()
	{
		String s = "";
		
		s += worldName + "\t - dimensions: ";
		for(DimensionType key : lodDimensions.keySet())
			s += lodDimensions.get(key).dimension.toString() + ", ";
		
		return s;
	}
}
