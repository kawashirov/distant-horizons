/*
 *    This file is part of the LOD Mod, licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.seibel.lod.enums;

import com.seibel.lod.objects.LodDataPoint;

/**
 * single, double, quad, half, full
 * 
 * @author James Seibel
 * @version 07-26-2021
 */
public enum LodDetail
{
	/** render 1 LOD for each chunk */
	SINGLE(1, 4, 4),
	
	/** render 4 LODs for each chunk */
	DOUBLE(2, 2, 3),
	
	/** render 16 LODs for each chunk */
	QUAD(4, 1, 2),
	
	/** render 64 LODs for each chunk */
	HALF(8, 0, 1),
	
	/** render 256 LODs for each chunk */
	FULL(16, 0 , 0);
	
	
	/** How many DataPoints should 
	 * be drawn per side per LodChunk */
	public final int dataPointLengthCount;
	/** How wide each LOD DataPoint is */
	public final int dataPointWidth;
	public final int detailLevel;
	
	/* Start/End X/Z give the block positions
	 * for each individual dataPoint in a LodChunk */
	public final int[] startX;
	public final int[] startZ;
	
	public final int[] endX;
	public final int[] endZ;
	
	/** This is how many pieces of data should be expected
	 * when creating a LodChunk with this detail level */
	public final int lodChunkStringDelimiterCount;
	
	/** in LodBufferBuilder some detail options don't render
	 * in the correct spot, this number fixes that.
	 * TODO find out why this is needed and see if it
	 * needed / could be removed */
	public final int offset;
	
	
	private LodDetail(int newLengthCount, int newOffset, int newDetail)
	{
		detailLevel = newDetail;
		dataPointLengthCount = newLengthCount;
		dataPointWidth = 16 / dataPointLengthCount;
		
		offset = newOffset;
		
		startX = new int[dataPointLengthCount * dataPointLengthCount];
		endX = new int[dataPointLengthCount * dataPointLengthCount];
		
		startZ = new int[dataPointLengthCount * dataPointLengthCount];
		endZ = new int[dataPointLengthCount * dataPointLengthCount];
		
		
		int index = 0;
		for(int x = 0; x < newLengthCount; x++)
		{
			for(int z = 0; z < newLengthCount; z++)
			{
				startX[index] = x * dataPointWidth;
				startZ[index] = z * dataPointWidth;
				
				// special case for FULL
				if(dataPointWidth != 1)
				{
					endX[index] = (x*dataPointWidth) + dataPointWidth - 1; 
					endZ[index] = (z*dataPointWidth) + dataPointWidth - 1; 
				}
				else
				{
					endX[index] = (x*dataPointWidth) + dataPointWidth;
					endZ[index] = (z*dataPointWidth) + dataPointWidth;
				}
				
				index++;
			}
		}
		
		
		lodChunkStringDelimiterCount = 2 + (dataPointLengthCount * dataPointLengthCount * LodDataPoint.NUMBER_OF_DELIMITERS);
		
	}// constructor
}
