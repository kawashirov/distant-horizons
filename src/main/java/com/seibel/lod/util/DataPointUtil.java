package com.seibel.lod.util;

public class DataPointUtil
{

	//To be used in the future for negative value
	//public final static int MIN_DEPTH = -64;
	//public final static int MIN_HEIGHT = -64;

	public final static int ALPHA_SHIFT = 56;
	public final static int RED_SHIFT = 48;
	public final static int GREEN_SHIFT = 40;
	public final static int BLUE_SHIFT = 32;
	public final static int COLOR_SHIFT = 32;
	public final static int HEIGHT_SHIFT = 22;
	public final static int DEPTH_SHIFT = 12;
	public final static int LIGHT_SHIFT = 8;
	public final static int GEN_TYPE_SHIFT = 2;
	public final static int VOID_SHIFT = 1;
	public final static int EXISTENCE_SHIFT = 0;

	public final static long ALPHA_MASK = 0b1111_1111;
	public final static long RED_MASK = 0b1111_1111;
	public final static long GREEN_MASK = 0b1111_1111;
	public final static long BLUE_MASK = 0b1111_1111;
	public final static long COLOR_MASK = 0b11111111_11111111_11111111_11111111;
	public final static long HEIGHT_MASK = 0b11_1111_1111;
	public final static long DEPTH_MASK = 0b11_1111_1111;
	public final static long LIGHT_MASK = 0b1111;
	public final static long GEN_TYPE_MASK = 0b111;
	public final static long VOID_MASK = 1;
	public final static long EXISTENCE_MASK = 1;


	public static long createDataPoint(int generationMode)
	{
		long dataPoint = 0;
		dataPoint += (generationMode & GEN_TYPE_MASK) << GEN_TYPE_SHIFT;
		dataPoint += VOID_MASK << VOID_SHIFT;
		dataPoint += EXISTENCE_MASK << EXISTENCE_SHIFT;
		return dataPoint;
	}

	public static long createDataPoint(int height, int depth, int color)
	{
		int alpha = (color >> 24) & 0xFF000000;
		int red = (color >> 16) & 0x00FF0000;
		int green = (color >> 8) & 0x0000FF00;
		int blue = color & 0x000000FF;
		return createDataPoint(height, depth, alpha, red, green, blue);
	}

	public static long createDataPoint(int height, int depth, int alpha, int red, int green, int blue, int lightValue, int generationMode)
	{
		long dataPoint = 0;
		dataPoint += (alpha & ALPHA_MASK) << ALPHA_SHIFT;
		dataPoint += (red & RED_MASK) << RED_SHIFT;
		dataPoint += (green & GREEN_MASK) << GREEN_SHIFT;
		dataPoint += (blue & BLUE_MASK) << BLUE_SHIFT;
		dataPoint += (height & HEIGHT_MASK) << HEIGHT_SHIFT;
		dataPoint += (depth & DEPTH_MASK) << DEPTH_SHIFT;
		dataPoint += (depth & LIGHT_MASK) << LIGHT_SHIFT;
		dataPoint += (depth & GEN_TYPE_MASK) << GEN_TYPE_SHIFT;
		dataPoint += EXISTENCE_MASK << EXISTENCE_SHIFT;
		return dataPoint;
	}

	public static short getHeight(long dataPoint)
	{
		return (short) ((dataPoint >> HEIGHT_SHIFT) & HEIGHT_MASK);
	}

	public static short getDepth(long dataPoint)
	{

		return (short) ((dataPoint >> DEPTH_SHIFT) & DEPTH_MASK);
	}

	public static short getAlpha(long dataPoint)
	{
		return (short) ((dataPoint >> ALPHA_SHIFT) & ALPHA_MASK);
	}

	public static short getRed(long dataPoint)
	{
		return (short) ((dataPoint >> RED_SHIFT) & RED_MASK);
	}

	public static short getGreen(long dataPoint)
	{
		return (short) ((dataPoint >> GREEN_SHIFT) & GREEN_MASK);
	}

	public static short getBlue(long dataPoint)
	{
		return (short) ((dataPoint >> BLUE_SHIFT) & BLUE_MASK);
	}

	public static byte getLightValue(long dataPoint)
	{
		return (byte) ((dataPoint >> LIGHT_SHIFT) & LIGHT_MASK);
	}

	public static byte getGenerationMode(long dataPoint)
	{
		return (byte) ((dataPoint >> GEN_TYPE_SHIFT) & GEN_TYPE_MASK);
	}


	public static boolean isItVoid(long dataPoint)
	{
		return (((dataPoint >> VOID_SHIFT) & VOID_MASK) == 1);
	}

	public static boolean doesItExist(long dataPoint)
	{
		return (((dataPoint >> EXISTENCE_SHIFT) & EXISTENCE_MASK) == 1);
	}

	public static int getColor(long dataPoint)
	{
		int R = (getRed(dataPoint) << 16) & 0x00FF0000;
		int G = (getGreen(dataPoint) << 8) & 0x0000FF00;
		int B = getBlue(dataPoint) & 0x000000FF;
		return 0xFF000000 | R | G | B;
	}

	public static String toString(long dataPoint)
	{
		StringBuilder s = new StringBuilder();
		s.append(getHeight(dataPoint));
		s.append(" ");
		s.append(getDepth(dataPoint));
		s.append(" ");
		s.append(getRed(dataPoint));
		s.append(" ");
		s.append(getBlue(dataPoint));
		s.append(" ");
		s.append(getGreen(dataPoint));
		s.append('\n');
		return s.toString();
	}


	public static long[] compress(long[] data, byte detailLevel)
	{
		return null;
	}
}
VerticalLevelContainer