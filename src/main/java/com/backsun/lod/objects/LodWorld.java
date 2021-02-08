package com.backsun.lod.objects;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

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
	private Dictionary<Integer, LodDimension> lodDimensions;
	
	
	public LodWorld(String newWorldName)
	{
		worldName = newWorldName;
		lodDimensions = new Hashtable<Integer, LodDimension>();
	}
	
	
	
	public void addLodDimension(LodDimension newStorage)
	{
		lodDimensions.put(newStorage.dimension.getId(), newStorage);
	}
	
	public LodDimension getLodDimension(int dimensionId)
	{
		return lodDimensions.get(dimensionId);
	}
	
	
	public void resizeDimensionRegionWidth(int newWidth)
	{
		Enumeration<Integer> keys = lodDimensions.keys();
		
		while(keys.hasMoreElements())
			lodDimensions.get(keys.nextElement()).setRegionWidth(newWidth);
	}
}
