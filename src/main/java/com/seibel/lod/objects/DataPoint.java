package com.seibel.lod.objects;

public class DataPoint
{
	public final static int HEIGHT_SHIFT = 54;
	public final static int DEPTH_SHIFT = 44;
	public final static int RED_SHIFT = 36;
	public final static int GREEN_SHIFT = 28;
	public final static int BLUE_SHIFT = 20;
	public final static int GEN_TYPE_SHIFT = 17;
	public final static int LIGHT_SHIFT = 13;
	public final static int EXISTENCE_SHIFT = 0;

	public final static long HEIGHT_MASK = Long.parseUnsignedLong("1111111111", 2);
	public final static long DEPTH_MASK = Long.parseUnsignedLong("1111111111", 2);
	public final static long RED_MASK = Long.parseUnsignedLong("11111111", 2);
	public final static long GREEN_MASK = Long.parseUnsignedLong("11111111", 2);
	public final static long BLUE_MASK = Long.parseUnsignedLong("11111111", 2);
	public final static long GEN_TYPE_MASK = Long.parseUnsignedLong("111", 2);
	public final static long LIGHT_MASK = Long.parseUnsignedLong("1111", 2);
	public final static long EXISTENCE_MASK = 1;

	public static long createDataPoint(int height, int depth, int color)
	{
		int red = (getRed(color) << 16) & 0x00FF0000;
		int green = (getGreen(color) << 8) & 0x0000FF00;
		int blue = getBlue(color)& 0x000000FF;
		return createDataPoint(height, depth, red, green, blue);
	}
	public static long createDataPoint(int height, int depth, int red, int green, int blue)
	{
		long dataPoint = 0;
		dataPoint += (height & HEIGHT_MASK) << HEIGHT_SHIFT;
		dataPoint += (depth & DEPTH_MASK) << DEPTH_SHIFT;
		dataPoint += (red & RED_MASK) << RED_SHIFT;
		dataPoint += (green & GREEN_MASK) << GREEN_SHIFT;
		dataPoint += (blue & BLUE_MASK) << BLUE_SHIFT;
		dataPoint += 1;
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

	public static boolean doesItExist(long dataPoint)
	{
		return ((dataPoint & EXISTENCE_MASK) == 1);
	}

	public static int getColor(long dataPoint)
	{
		int R = (getRed(dataPoint) << 16) & 0x00FF0000;
		int G = (getGreen(dataPoint) << 8) & 0x0000FF00;
		int B = getBlue(dataPoint)& 0x000000FF;
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
}
