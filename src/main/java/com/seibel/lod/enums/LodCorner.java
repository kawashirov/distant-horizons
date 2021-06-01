package com.seibel.lod.enums;

/**
 * NE, SE, SW, NW
 * 
 * @author James Seibel
 * @version 1-20-2020
 */
public enum LodCorner
{
	/** -Z, +X */
	NE(0),
	/** +Z, +X */
	SE(1),
	/** +Z, -X */
	SW(2),
	/** -Z, -X */
	NW(3);
	
	public final int value;
	
	private LodCorner(int newValue)
	{
		value = newValue;
	}
}
