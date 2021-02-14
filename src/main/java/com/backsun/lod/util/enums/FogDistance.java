package com.backsun.lod.util.enums;

/**
 * Near, far, or both.
 * 
 * @author James Seibel
 * @version 01-27-2021
 */
public enum FogDistance
{
	/** valid for both fast and fancy qualities. */
	NEAR,
	/** valid for both fast and fancy qualities. */
	FAR,
	/** only valid if the quality is set to Fancy. */
	BOTH;
}