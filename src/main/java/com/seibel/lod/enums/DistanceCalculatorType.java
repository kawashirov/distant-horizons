package com.seibel.lod.enums;

/**
 * @author Leonardo Amato
 * @version 22-08-2021
 */
public enum DistanceCalculatorType
{
	/**
	 * different Lod detail render and generate linearly to the distance
	 */
	LINEAR,

	/**
	 * different Lod detail render and generate quadratically to the distance
	 */
	QUADRATIC,

	/**
	 * we calculate the distance based on game render distance and mod render distance
	 */
	RENDER_DEPENDANT;
}