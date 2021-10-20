package com.seibel.lod.enums;

/**
 * NONE, GAME_SHADING
 * 
 * @author James Seibel
 * @version 7-25-2020
 */
public enum ShadingMode
{
	/**
	 * LODs will have darker sides and bottoms to simulate
	 * Minecraft's fast lighting.
	 */
	GAME_SHADING,
	
	/**
	 * LODs will use ambient occlusion to mimic Minecarft's
	 * Fancy lighting.
	 */
	AMBIENT_OCCLUSION
}