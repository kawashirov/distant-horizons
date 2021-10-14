package com.seibel.lod.enums;

/**
 * NONE, GAME_SHADING
 * @author James Seibel
 * @version 7-25-2020
 */
public enum ShadingMode
{
	/**
	 * LODs will have the same lighting on every side.
	 * can make large similarly colored areas hard to differentiate
	 */
	NONE,
	
	/**
	 * LODs will have darker sides and bottoms to simulate
	 * Minecraft's fast, top down lighting.
	 */
	GAME_SHADING
}