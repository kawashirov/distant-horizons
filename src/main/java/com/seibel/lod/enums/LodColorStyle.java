package com.seibel.lod.enums;

/**
 * top, individual_sides
 * 
 * @author James Seibel
 * @version 05-08-2021
 */
public enum LodColorStyle
{
	/** Use the color from the top of the LOD chunk 
	 * for all sides */
	TOP,
	
	/** For each side of the LOD use the color corresponding
	 * to that side */
	INDIVIDUAL_SIDES;
}
