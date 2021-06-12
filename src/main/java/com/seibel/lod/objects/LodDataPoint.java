package com.seibel.lod.objects;

import java.awt.Color;

import com.seibel.lod.enums.ColorDirection;
import com.seibel.lod.handlers.LodDimensionFileHandler;

/**
 * This stores the height and color
 * for a specific area in a LodChunk.
 * 
 * @author James Seibel
 * @version 6-12-2021
 */
public class LodDataPoint
{
	/** This is what separates each piece of data in the toData method */
	private static final char DATA_DELIMITER = LodDimensionFileHandler.DATA_DELIMITER;
	
	private static final Color INVISIBLE = new Color(0,0,0,0);
	
	/** highest point */
	public short height;
	
	/** lowest point */
	public short depth;
	
	/** The average color for the 6 cardinal directions */
	public Color colors[];
	
	
	/**
	 * default constructor
	 */
	public LodDataPoint()
	{
		height = 0;
		depth = 0;
		colors = new Color[ColorDirection.values().length];
		
		// by default have the colors invisible
		for(ColorDirection dir : ColorDirection.values())
			colors[dir.value] = INVISIBLE;
	}
	
	
	public LodDataPoint(short newHeight, short newDepth, Color[] newColors)
	{
		height = newHeight;
		depth = newDepth;
		colors = newColors;
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
	 * 4, 0, 255,255,255, 255,255,255, 255,255,255, 255,255,255, 255,255,255, 255,255,255,
	 */
	public String toData()
	{
		
		String s = Short.toString(height) + DATA_DELIMITER;
		
		s += Short.toString(depth) + DATA_DELIMITER;
		
		for(int i = 0; i < colors.length; i++)
		{
			s += Integer.toString(colors[i].getRed()) + DATA_DELIMITER + Integer.toString(colors[i].getGreen()) + DATA_DELIMITER + Integer.toString(colors[i].getBlue()) + DATA_DELIMITER;
		}
		
		return s;
	}
}
