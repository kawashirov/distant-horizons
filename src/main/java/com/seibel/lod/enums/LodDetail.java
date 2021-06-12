package com.seibel.lod.enums;

import com.seibel.lod.objects.LodChunk;

/**
 * single, double, quad, half, full
 * 
 * @author James Seibel
 * @version 06-12-2021
 */
public enum LodDetail
{
	/** render 1 LOD for each chunk */
	SINGLE(1),
	
	/** render 4 LODs for each chunk */
	DOUBLE(2),
	
	/** render 16 LODs for each chunk */
	QUAD(4),
	
	/** render 64 LODs for each chunk */
	HALF(8),
	
	/** render 256 LODs for each chunk */
	FULL(16);
	
	
	/** How many LODs wide should 
	 * be drawn per LodChunk */
	public final int lengthCount;
	/** How wide each LOD is */
	public final int width;
	
	/*  */
	public final int[] startX;
	public final int[] startZ;
	
	public final int[] endX;
	public final int[] endZ;
	
	
	private LodDetail(int newLengthCount)
	{
		lengthCount = newLengthCount;
		width = 16 / lengthCount;
		
		if(newLengthCount == LodChunk.WIDTH)
		{
			// this is to prevent overflow
			newLengthCount = LodChunk.WIDTH - 1;
		}
		
		startX = new int[lengthCount * lengthCount];
		endX = new int[lengthCount * lengthCount];
		
		startZ = new int[lengthCount * lengthCount];
		endZ = new int[lengthCount * lengthCount];
		
		
		int index = 0;
		for(int x = 0; x < newLengthCount; x++)
		{
			for(int z = 0; z < newLengthCount; z++)
			{
				startX[index] = x * width;
				startZ[index] = z * width;
				
				// special case for FULL
				if(width != 1)
				{
					endX[index] = (x*width) + width - 1; 
					endZ[index] = (z*width) + width - 1; 
				}
				else
				{
					endX[index] = (x*width) + width;
					endZ[index] = (z*width) + width;
				}
				
				index++;
			}
		}
	}
}
