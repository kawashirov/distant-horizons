package com.seibel.lod.enums;

/**
 * NONE, DARKEN_SIDES
 * 
 * @author James Seibel
 * @version 7-25-2020
 */
public enum ShadingMode
{
	/** LODs will have the same lighting on every side.
	Fastest, but can make large similarly colored areas hard to differentiate */
	NONE,
	
	/** LODs will have darker sides and bottoms to simulate top down lighting.
	Fastest */
	GAME_SHADING;
}