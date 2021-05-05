package com.backsun.lod.enums;

/**
 * 
 * 
 * @author James Seibel
 * @version 05-05-2021
 */
public enum LodDrawMode
{
	// used for position

	/** Chunks are rendered as
	 * rectangular prisms. */
	CUBIC,
	
	/** Chunks smoothly transition between
	 * each other. */
	TRIANGULAR,
	
	/** Chunks smoothly transition between
	 * each other, unless a neighboring chunk
	 * is at a significantly different height. */
	DYNAMIC;
	
}
