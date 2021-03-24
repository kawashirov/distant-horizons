package com.backsun.lod.enums;

/**
 * TOP, N, S, E, W, BOTTOM
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
	N(1),
	/** +Z */
	S(2),
	
	/** +X */
	E(3),
	/** -X */
	W(4),
	
	/** -Y */
	BOTTOM(5);
	
	public final int value;
	
	private ColorDirection(int newValue)
	{
		value = newValue;
	}
}
