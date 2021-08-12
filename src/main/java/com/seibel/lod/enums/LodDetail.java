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

import java.util.ArrayList;
import java.util.Collections;

import com.seibel.lod.objects.LodDataPoint;
import com.seibel.lod.util.LodUtil;

/**
 * single, double, quad, half, full
 * 
 * @author James Seibel
 * @version 8-11-2021
 */
public enum LodDetail
{
	/** render 1 LOD for each chunk */
	SINGLE(1, 4),
	
	/** render 4 LODs for each chunk */
	DOUBLE(2, 3),
	
	/** render 16 LODs for each chunk */
	QUAD(4, 2),
	
	/** render 64 LODs for each chunk */
	HALF(8, 1),
	
	/** render 256 LODs for each chunk */
	FULL(16, 0);
	
	
	/** How many DataPoints should 
	 * be drawn per side per LodChunk */
	public final int dataPointLengthCount;
	/** How wide each LOD DataPoint is */
	public final int dataPointWidth;
	/** This is the same as detailLevel in LodQuadTreeNode, 
	 * lowest is 0 highest is 9 */
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
	
	/** 
	 * 1st dimension: LodDetail.detailLevel <br>
	 * 2nd dimension: An array of all LodDetails that are less than or <br>
	 *                equal to that detailLevel
	 */
	private static LodDetail[][] lowerDetailArrays;
	
	
	
	
	private LodDetail(int newLengthCount, int newDetailLevel)
	{
		detailLevel = newDetailLevel;
		dataPointLengthCount = newLengthCount;
		dataPointWidth = 16 / dataPointLengthCount;
		
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
				
				endX[index] = (x*dataPointWidth) + dataPointWidth;
				endZ[index] = (z*dataPointWidth) + dataPointWidth;
				
				index++;
			}
		}
		
		
		lodChunkStringDelimiterCount = 2 + (dataPointLengthCount * dataPointLengthCount * LodDataPoint.NUMBER_OF_DELIMITERS);
		
	}// constructor
	
	
	
	
	
	
	/** 
	 * Returns an array of all LodDetails that have a detail level
	 * that is less than or equal to the given LodDetail
	 */
	public static LodDetail[] getSelfAndLowerDetails(LodDetail detail)
	{
		if (lowerDetailArrays == null)
		{
			// run first time setup
			lowerDetailArrays = new LodDetail[LodDetail.values().length][];
			
			// go through each LodDetail
			for(LodDetail currentDetail : LodDetail.values())
			{
				ArrayList<LodDetail> lowerDetails = new ArrayList<>();
				
				// find the details lower than currentDetail
				for(LodDetail compareDetail : LodDetail.values())
				{
					if (currentDetail.detailLevel <= compareDetail.detailLevel)
					{
						lowerDetails.add(compareDetail);
					}
				}
				
				// have the highest detail item first in the list
				Collections.sort(lowerDetails);
				Collections.reverse(lowerDetails);
				
				lowerDetailArrays[currentDetail.detailLevel] = lowerDetails.toArray(new LodDetail[lowerDetails.size()]); 
			}
		}
		
		return lowerDetailArrays[detail.detailLevel];
	}
	
	/** Returns what detail level should be used at a given distance and maxDistance. */
	public static LodDetail getDetailForDistance(LodDetail maxDetailLevel, int distance, int maxDistance)
	{
		LodDetail[] lowerDetails = getSelfAndLowerDetails(maxDetailLevel);
		int distaneBetweenDetails = maxDistance / lowerDetails.length;
		int index = LodUtil.clamp(0, distance / distaneBetweenDetails, lowerDetails.length - 1);
		
		return lowerDetails[index];
		
	}
	
}
