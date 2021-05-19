package com.backsun.lod.enums;

/**
 * TOP, NORTH, SOUTH, EAST, WEST, BOTTOM
 * 
 * @author James Seibel
 * @version 10-17-2020
 */
public enum ColorDirection
{
	// used for colors
	/** +Y */
	TOP(0),
	
	/** -Z */
	NORTH(1),
	/** +Z */
	SOUTH(2),
	
	/** +X */
	EAST(3),
	/** -X */
	WEST(4),
	
	/** -Y */
	BOTTOM(5);
	
	public final int value;
	
	private ColorDirection(int newValue)
	{
		value = newValue;
	}
}
