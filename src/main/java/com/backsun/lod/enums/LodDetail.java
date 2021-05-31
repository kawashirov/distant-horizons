package com.backsun.lod.enums;

/**
 * single, quad
 * 
 * @author James Seibel
 * @version 05-29-2021
 */
public enum LodDetail
{
	/** render 1 LOD for each chunk */
	SINGLE(1),
	
	/** render 4 LODs for each chunk */
	DOUBLE(2);
	
	/** How many data points wide the related
	 * LodChunk object should contain */
	public final int value;
	
	private LodDetail(int newValue)
	{
		value = newValue;
	}
}
