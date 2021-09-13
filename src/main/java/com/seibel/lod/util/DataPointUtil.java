package com.seibel.lod.util;

import com.seibel.lod.enums.DistanceGenerationMode;
import org.lwjgl.system.CallbackI;

import java.lang.reflect.Array;
import java.util.Arrays;

public class DataPointUtil
{

	//To be used in the future for negative value
	//public final static int MIN_DEPTH = -64;
	//public final static int MIN_HEIGHT = -64;
	public final static int EMPTY_DATA = 0;

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


	public static long createVoidDataPoint(int generationMode)
	{
		long dataPoint = 0;
		dataPoint += (generationMode & GEN_TYPE_MASK) << GEN_TYPE_SHIFT;
		dataPoint += VOID_MASK << VOID_SHIFT;
		dataPoint += EXISTENCE_MASK << EXISTENCE_SHIFT;
		return dataPoint;
	}

	public static long createDataPoint(int height, int depth, int color, int lightValue, int generationMode)
	{
		return createDataPoint(
				ColorUtil.getAlpha(color),
				ColorUtil.getRed(color),
				ColorUtil.getGreen(color),
				ColorUtil.getBlue(color),
				height, depth, lightValue, generationMode);
	}

	public static long createDataPoint(int alpha, int red, int green, int blue, int height, int depth, int lightValue, int generationMode)
	{
		long dataPoint = 0;
		dataPoint += (alpha & ALPHA_MASK) << ALPHA_SHIFT;
		dataPoint += (red & RED_MASK) << RED_SHIFT;
		dataPoint += (green & GREEN_MASK) << GREEN_SHIFT;
		dataPoint += (blue & BLUE_MASK) << BLUE_SHIFT;
		dataPoint += (height & HEIGHT_MASK) << HEIGHT_SHIFT;
		dataPoint += (depth & DEPTH_MASK) << DEPTH_SHIFT;
		dataPoint += (lightValue & LIGHT_MASK) << LIGHT_SHIFT;
		dataPoint += (generationMode & GEN_TYPE_MASK) << GEN_TYPE_SHIFT;
		dataPoint += EXISTENCE_MASK << EXISTENCE_SHIFT;
		return dataPoint;
	}

	public static short getHeight(long dataPoint)
	{
		return (short) ((dataPoint >>> HEIGHT_SHIFT) & HEIGHT_MASK);
	}

	public static short getDepth(long dataPoint)
	{

		return (short) ((dataPoint >>> DEPTH_SHIFT) & DEPTH_MASK);
	}

	public static short getAlpha(long dataPoint)
	{
		return (short) ((dataPoint >>> ALPHA_SHIFT) & ALPHA_MASK);
	}

	public static short getRed(long dataPoint)
	{
		return (short) ((dataPoint >>> RED_SHIFT) & RED_MASK);
	}

	public static short getGreen(long dataPoint)
	{
		return (short) ((dataPoint >>> GREEN_SHIFT) & GREEN_MASK);
	}

	public static short getBlue(long dataPoint)
	{
		return (short) ((dataPoint >>> BLUE_SHIFT) & BLUE_MASK);
	}

	public static byte getLightValue(long dataPoint)
	{
		return (byte) ((dataPoint >>> LIGHT_SHIFT) & LIGHT_MASK);
	}

	public static byte getGenerationMode(long dataPoint)
	{
		return (byte) ((dataPoint >>> GEN_TYPE_SHIFT) & GEN_TYPE_MASK);
	}


	public static boolean isItVoid(long dataPoint)
	{
		return (((dataPoint >>> VOID_SHIFT) & VOID_MASK) == 1);
	}

	public static boolean doesItExist(long dataPoint)
	{
		return (((dataPoint >>> EXISTENCE_SHIFT) & EXISTENCE_MASK) == 1);
	}

	public static int getColor(long dataPoint)
	{
		return (int) ((dataPoint >>> COLOR_SHIFT) & COLOR_MASK);
	}

	public static int getLightColor(long dataPoint)
	{
		int lightBlock = getLightValue(dataPoint);
		int red = Math.min(getRed(dataPoint) + lightBlock * 8, 255);
		int green = Math.min(getGreen(dataPoint) + lightBlock * 8, 255);
		int blue = Math.min(getBlue(dataPoint) + lightBlock * 4, 255);
		return ColorUtil.rgbToInt(red, green, blue);
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

	public static long mergeSingleData(long[] dataToMerge)
	{
		int numberOfChildren = 0;
		int numberOfVoidChildren = 0;

		int tempAlpha = 0;
		int tempRed = 0;
		int tempGreen = 0;
		int tempBlue = 0;
		int tempHeight = 0;
		int tempDepth = 0;
		int tempLight = 0;
		byte tempGenMode = DistanceGenerationMode.SERVER.complexity;
		boolean allEmpty = true;
		boolean allVoid = true;
		for (long data : dataToMerge)
		{
			if (DataPointUtil.doesItExist(data))
			{
				allEmpty = false;
				if (!(DataPointUtil.isItVoid(data)))
				{
					numberOfChildren++;
					allVoid = false;
					tempAlpha += DataPointUtil.getAlpha(data);
					tempRed += DataPointUtil.getRed(data);
					tempGreen += DataPointUtil.getGreen(data);
					tempBlue += DataPointUtil.getBlue(data);
					tempHeight += DataPointUtil.getHeight(data);
					tempDepth += DataPointUtil.getDepth(data);
					tempLight += DataPointUtil.getLightValue(data);
				}
				tempGenMode = (byte) Math.min(tempGenMode, DataPointUtil.getGenerationMode(data));
			} else
			{
				tempGenMode = (byte) Math.min(tempGenMode, DistanceGenerationMode.NONE.complexity);
			}
		}

		if (allEmpty)
		{
			//no child has been initialized
			return DataPointUtil.EMPTY_DATA;
		} else if (allVoid)
		{
			//all the children are void
			return DataPointUtil.createVoidDataPoint(tempGenMode);
		} else
		{
			//we have at least 1 child
			tempAlpha = tempAlpha / numberOfChildren;
			tempRed = tempRed / numberOfChildren;
			tempGreen = tempGreen / numberOfChildren;
			tempBlue = tempBlue / numberOfChildren;
			tempHeight = tempHeight / numberOfChildren;
			tempDepth = tempDepth / numberOfChildren;
			tempLight = tempLight / numberOfChildren;
			return DataPointUtil.createDataPoint(tempAlpha, tempRed, tempGreen, tempBlue, tempHeight, tempDepth, tempLight, tempGenMode);
		}
	}

	public static long[] mergeVerticalData(long[][] dataToMerge)
	{
		int[][] dataCollector = new int[256][2];
		long[] singleDataToCollect = new long[256];
		int size = 0;

		int tempGenMode = DistanceGenerationMode.SERVER.complexity;
		boolean allEmpty = true;
		boolean allVoid = true;
		long singleData;

		//We collect the indexes of the data, ordered by the depth
		for (int index = 0; index < dataToMerge.length; index++)
		{
			for (int dataIndex = 0; dataIndex < dataToMerge.length; dataIndex++)
			{
				singleData = dataToMerge[index][dataIndex];
				if (doesItExist(singleData))
				{
					tempGenMode = Math.min(tempGenMode, getGenerationMode(singleData));
					allEmpty = false;
					if (!isItVoid(singleData))
					{
						allVoid = false;
						int j = size;
						while (j >= 0 && (getDepth(dataToMerge[dataCollector[j][0]][dataCollector[j][1]]) > getDepth(singleData)))
						{
							dataCollector[j] = dataCollector[j - 1];
							j = j - 1;
						}
						dataCollector[j][0] = dataIndex;
						dataCollector[j][1] = index;
						size++;
					}
				}
			}
		}


		//We check if there is any data that's not empty or void
		if (allEmpty)
		{
			return new long[]{EMPTY_DATA};
		}
		if (allVoid)
		{
			return new long[]{createVoidDataPoint(tempGenMode)};
		}

		//We merge together all the data
		int minDepth;
		int maxHeight = Integer.MIN_VALUE;
		int tempDepth;
		int tempHeight;
		int index = 0;
		int dataCount = 0;
		long[] singleDataToMerge = new long[dataToMerge.length];
		long[] newData = new long[dataToMerge.length];
		while (index < size)
		{
			dataCount++;
			singleData = dataToMerge[dataCollector[index][0]][dataCollector[index][1]];
			minDepth = getDepth(singleData);
			maxHeight = getHeight(singleData);
			index++;
			while(index < size)
			{
				singleData = dataToMerge[dataCollector[index][0]][dataCollector[index][1]];
				tempDepth = getDepth(singleData);
				tempHeight = getHeight(singleData);
				if(maxHeight >= tempDepth)
				{
					singleDataToMerge[dataCollector[index][0]] = singleData;
					maxHeight = tempHeight;
					index++;
				}else{
					break;
				}
			}
			singleData = mergeSingleData(singleDataToMerge);
			newData[dataCount] = createDataPoint(maxHeight, minDepth, getColor(singleData), getLightValue(singleData), getGenerationMode(singleData));
		}
		return Arrays.copyOf(newData, dataCount);
	}

	public static long[] compress(long[] data, byte detailLevel)
	{
		return null;
	}
}