package com.backsun.lod.util.enums;

/**
 * Near, far, or NEAR_AND_FAR.
 * 
 * @author James Seibel
 * @version 02-14-2021
 */
public enum FogDistance
{
	/** valid for both fast and fancy fog qualities. */
	NEAR,
	/** valid for both fast and fancy fog qualities. */
	FAR,
	/** only looks good if the fog quality is set to Fancy. */
	NEAR_AND_FAR;
}