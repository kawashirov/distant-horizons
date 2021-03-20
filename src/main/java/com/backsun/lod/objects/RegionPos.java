package com.backsun.lod.objects;

/**
 * This object is similar to ChunkPos or BlockPos.
 * 
 * @author James Seibel
 * @version 03-19-2021
 */
public class RegionPos
{
	public int x;
	public int z;
	
	
	/**
	 * Default Constructor <br>
	 * 
	 * Sets x and z to 0
	 */
	public RegionPos()
	{
		x = 0;
		z = 0;
	}
	
	public RegionPos(int newX, int newZ)
	{
		x = newX;
		z = newZ;
	}
}
