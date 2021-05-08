package com.backsun.lod.enums;

/**
 * low, medium, high
 * 
 * @author James Seibel
 * @version 05-08-2021
 */
public enum LodGeometryQuality
{
	/** render 1 LOD for each chunk */
	SINGLE,
	
	/** render 4 LODs for each near chunk and 1 LOD for each far chunk */
	SINGLE_CLOSE_QUAD_FAR,
	
	/** render 4 LODs for each chunk */
	QUAD;
}
