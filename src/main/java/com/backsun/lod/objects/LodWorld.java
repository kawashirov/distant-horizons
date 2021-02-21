package com.backsun.lod.objects;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import net.minecraft.world.DimensionType;

/**
 * This stores all LODs for a given world.
 * 
 * @author James Seibel
 * @version 01-31-2021
 */
public class LodWorld
{
	public String worldName;
	
	/**
	 * Key = Dimension id (as an int)
	 */
	private Dictionary<DimensionType, LodDimension> lodDimensions;
	
	
	public LodWorld(String newWorldName)
	{
		worldName = newWorldName;
		lodDimensions = new Hashtable<>();
	}
	
	
	
	public void addLodDimension(LodDimension newStorage)
	{
		lodDimensions.put(newStorage.dimension, newStorage);
	}
	
	public LodDimension getLodDimension(DimensionType dimension)
	{
		return lodDimensions.get(dimension);
	}
	
	
	public void resizeDimensionRegionWidth(int newWidth)
	{
		Enumeration<DimensionType> keys = lodDimensions.keys();
		
		while(keys.hasMoreElements())
			lodDimensions.get(keys.nextElement()).setRegionWidth(newWidth);
	}
}
