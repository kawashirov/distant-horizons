/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
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

package com.seibel.lod.objects;

import java.util.Arrays;

import com.seibel.lod.proxy.ClientProxy;
import com.seibel.lod.util.LevelPosUtil;
import com.seibel.lod.util.LodUtil;

/**
 * @author Leonardo Amato
 * @version 9-18-2021
 */
public class PosToRenderContainer
{
	public byte minDetail;
	private final int size;
	private int regionPosX;
	private int regionPosZ;
	private int numberOfPosToRender;
	private int[] posToRender;
	/*TODO this population matrix could be converted to boolean to improve memory use
	 *  no since bools are stored as bytes anyway - cola*/
	private byte[][] population;
	
	public PosToRenderContainer(byte minDetail, int regionPosX, int regionPosZ)
	{
		this.minDetail = minDetail;
		this.numberOfPosToRender = 0;
		this.regionPosX = regionPosX;
		this.regionPosZ = regionPosZ;
		this.size = 1 << (LodUtil.REGION_DETAIL_LEVEL - minDetail);
		posToRender = new int[size * size * 3];
		population = new byte[size][size];
	}
	
	public void addPosToRender(byte detailLevel, int posX, int posZ)
	{
		// When rapidly changing dimensions the bufferBuilder can cause this,
		// James isn't sure why, but this will prevent an exception at
		// the very least (while stilling logging the problem). 
		if (numberOfPosToRender >= posToRender.length)
		{
			// This is might be due to dimensions having a different width
			// when first loading in
			ClientProxy.LOGGER.error("Unable to addPosToRender. numberOfPosToRender [" + numberOfPosToRender + "] detailLevel [" + detailLevel + "] Pos [" + posX + "," + posZ + "]");
			numberOfPosToRender++; // incrementing so we can see how many pos over the limit we would go
			return;
		}
		
		//if(numberOfPosToRender >= posToRender.length)
		//	posToRender = Arrays.copyOf(posToRender, posToRender.length*2);
		posToRender[numberOfPosToRender * 3] = detailLevel;
		posToRender[numberOfPosToRender * 3 + 1] = posX;
		posToRender[numberOfPosToRender * 3 + 2] = posZ;
		numberOfPosToRender++;
		population[LevelPosUtil.getRegionModule(minDetail, LevelPosUtil.convert(detailLevel, posX, minDetail))]
				[LevelPosUtil.getRegionModule(minDetail, LevelPosUtil.convert(detailLevel, posZ, minDetail))] = (byte) (detailLevel + 1);
	}
	
	public boolean contains(byte detailLevel, int posX, int posZ)
	{
		if (LevelPosUtil.getRegion(detailLevel, posX) == regionPosX && LevelPosUtil.getRegion(detailLevel, posZ) == regionPosZ)
			return (population[LevelPosUtil.getRegionModule(minDetail, LevelPosUtil.convert(detailLevel, posX, minDetail))]
					[LevelPosUtil.getRegionModule(minDetail, LevelPosUtil.convert(detailLevel, posZ, minDetail))] == (detailLevel + 1));
		else
			return false;
	}
	
	public void clear(byte minDetail, int regionPosX, int regionPosZ)
	{
		this.numberOfPosToRender = 0;
		this.regionPosX = regionPosX;
		this.regionPosZ = regionPosZ;
		if (this.minDetail == minDetail)
		{
			Arrays.fill(posToRender, 0);
			for (byte[] bytes : population)
				Arrays.fill(bytes, (byte) 0);
		}
		else
		{
			this.minDetail = minDetail;
			int size = 1 << (LodUtil.REGION_DETAIL_LEVEL - minDetail);
			posToRender = new int[size * size * 3];
			population = new byte[size][size];
		}
	}
	
	public int getNumberOfPos()
	{
		return numberOfPosToRender;
	}
	
	public byte getNthDetailLevel(int n)
	{
		return (byte) posToRender[n * 3];
	}
	
	public int getNthPosX(int n)
	{
		return posToRender[n * 3 + 1];
	}
	
	public int getNthPosZ(int n)
	{
		return posToRender[n * 3 + 2];
	}
	
	@Override
	public String toString()
	{
		
		StringBuilder builder = new StringBuilder();
		builder.append("To render ");
		builder.append(numberOfPosToRender);
		builder.append('\n');
		for (int i = 0; i < numberOfPosToRender; i++)
		{
			builder.append(posToRender[i * 3]);
			builder.append(" ");
			builder.append(posToRender[i * 3 + 1]);
			builder.append(" ");
			builder.append(posToRender[i * 3 + 2]);
			builder.append('\n');
		}
		builder.append('\n');
		return builder.toString();
	}
}
