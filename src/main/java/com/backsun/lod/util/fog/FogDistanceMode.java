package com.backsun.lod.util.fog;

/**
 * Near, far, or both.
 * 
 * @author James Seibel
 * @version 01-27-2021
 */
public enum FogDistanceMode
{
	/** valid for both fast and fancy qualities. */
	NEAR,
	/** valid for both fast and fancy qualities. */
	FAR,
	/** only valid if the quality is set to Fancy. */
	BOTH;
}