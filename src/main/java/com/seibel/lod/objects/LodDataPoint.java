package com.seibel.lod.objects;

import java.awt.Color;

import com.seibel.lod.handlers.LodDimensionFileHandler;

/**
 * This stores the height and color
 * for a specific area in a LodChunk.
 * 
 * @author James Seibel
 * @version 6-13-2021
 */
public class LodDataPoint
{
	/** This is what separates each piece of data in the toData method */
	private static final char DATA_DELIMITER = LodDimensionFileHandler.DATA_DELIMITER;
	
	/** this is how many pieces of data are exported when toData is called */
	public static final int NUMBER_OF_DELIMITERS = 5;
	
	private static final Color INVISIBLE = new Color(0,0,0,0);
	
	/** highest point */
	public short height;
	
	/** lowest point */
	public short depth;
	
	/** The average color for the 6 cardinal directions */
	public Color color;
	
	
	/**
	 * Creates and empty LodDataPoint
	 */
	public LodDataPoint()
	{
		height = -1;
		depth = -1;
		color = INVISIBLE;
	}
	
	
	public LodDataPoint(short newHeight, short newDepth, Color newColor)
	{
		height = newHeight;
		depth = newDepth;
		color = newColor;
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
}
