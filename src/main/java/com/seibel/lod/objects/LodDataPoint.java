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
package com.seibel.lod.objects;

import java.awt.*;
import java.io.Serializable;
import java.util.Objects;

import com.seibel.lod.handlers.LodQuadTreeDimensionFileHandler;
import com.seibel.lod.util.LodUtil;

/**
 * This stores the height and color
 * for a specific area in a LodChunk.
 * 
 * @author James Seibel
 * @version 8-8-2021
 */
public class LodDataPoint implements Serializable
{
	/** This is what separates each piece of data in the toData method */
	private static final char DATA_DELIMITER = LodQuadTreeDimensionFileHandler.DATA_DELIMITER;
	
	/** this is how many pieces of data are exported when toData is called */
	public static final int NUMBER_OF_DELIMITERS = 5;
	
	/** a empty data point that can be used for comparisons */
	public static final LodDataPoint EMPTY_DATA_POINT = new LodDataPoint();
	
	
	/** highest point */
	public short height;
	
	/** lowest point */
	public short depth;
	
	/** The average color for the 6 cardinal directions */
	public Color color;
	
	
	/**
	 * Creates an empty LodDataPoint
	 */
	public LodDataPoint()
	{
		height = -1;
		depth = -1;
		color = LodUtil.COLOR_INVISIBLE;
	}
	
	
	public LodDataPoint(short newHeight, short newDepth, Color newColor)
	{
		height = newHeight;
		depth = newDepth;
		color = newColor;
	}
	
	public LodDataPoint(int newHeight, int newDepth, Color newColor)
	{
		height = (short) newHeight;
		depth = (short) newDepth;
		color = newColor;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(this.height, this.depth, this.color);
	}

	public boolean equals(LodDataPoint other)
	{
		return (this.height == other.height
				&& this.depth == other.depth
				&& this.color == other.color);
	}
	
	public boolean isEmpty()
	{
		return this.equals(EMPTY_DATA_POINT);
	}

	/**
	 * Outputs all data in a csv format
	 * with the given delimiter.
	 * <br>
	 * Exports data in the form:
	 * <br>
	 * height, depth, rgb color data
	 * 
	 * <br>
	 * example output:
	 * <br>
	 * 4, 0, 255,255,255, 
	 */
	public String toData()
	{
		String s = Short.toString(height) + DATA_DELIMITER;
		
		s += Short.toString(depth) + DATA_DELIMITER;
		
		s += Integer.toString(color.getRed()) + DATA_DELIMITER + Integer.toString(color.getGreen()) + DATA_DELIMITER + Integer.toString(color.getBlue()) + DATA_DELIMITER;
		
		return s;
	}
	
	@Override
	public String toString()
	{
		String s = Short.toString(height) + DATA_DELIMITER;
		
		s += Short.toString(depth) + DATA_DELIMITER;
		
		s += Integer.toString(color.getRed()) + DATA_DELIMITER + Integer.toString(color.getGreen()) + DATA_DELIMITER + Integer.toString(color.getBlue()) + DATA_DELIMITER;
		
		return s;
	}
}
