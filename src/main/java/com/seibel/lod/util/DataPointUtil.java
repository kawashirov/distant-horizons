package com.seibel.lod.util;

import com.seibel.lod.enums.DistanceGenerationMode;

import net.minecraft.client.renderer.texture.NativeImage;

import java.lang.reflect.Array;
import java.util.Arrays;

public class DataPointUtil
{
	/*
	|a  |a  |a  |a  |r  |r  |r  |r   |

	|r  |r  |r  |r  |g  |g  |g  |g  |

	|g  |g  |g  |g  |b  |b  |b  |b  |

	|b  |b  |b  |b  |h  |h  |h  |h  |

	|h  |h  |h  |h  |h  |h  |d  |d  |

	|d  |d  |d  |d  |d  |d  |d  |d  |

	|bl |bl |bl |bl |sl |sl |sl |sl |

	|l  |l  |f  |g  |g  |g  |v  |e  |


	 */
	//To be used in the future for negative value
	//public final static int MIN_DEPTH = -64;
	//public final static int MIN_HEIGHT = -64;
	public final static int EMPTY_DATA = 0;
	public final static int WORLD_HEIGHT = 256;

	public final static int ALPHA_DOWNSIZE_SHIFT = 4;

	public final static int BLUE_COLOR_SHIFT = 0;
	public final static int GREEN_COLOR_SHIFT = 8;
	public final static int RED_COLOR_SHIFT = 16;
	public final static int ALPHA_COLOR_SHIFT = 24;

	public final static int BLUE_SHIFT = 36;
	public final static int GREEN_SHIFT = BLUE_SHIFT + 8;
	public final static int RED_SHIFT = BLUE_SHIFT + 16 ;
	public final static int ALPHA_SHIFT = BLUE_SHIFT + 24;

	public final static int COLOR_SHIFT = 36;

	public final static int HEIGHT_SHIFT = 26;
	public final static int DEPTH_SHIFT = 16;
	public final static int BLOCK_LIGHT_SHIFT = 12;
	public final static int SKY_LIGHT_SHIFT = 8;
	public final static int LIGHTS_SHIFT = SKY_LIGHT_SHIFT;
	public final static int VERTICAL_INDEX_SHIFT = 6;
	public final static int FLAG_SHIFT = 5;
	public final static int GEN_TYPE_SHIFT = 2;
	public final static int VOID_SHIFT = 1;
	public final static int EXISTENCE_SHIFT = 0;

	public final static long ALPHA_MASK = 0b1111;
	public final static long RED_MASK = 0b1111_1111;
	public final static long GREEN_MASK = 0b1111_1111;
	public final static long BLUE_MASK = 0b1111_1111;
	public final static long COLOR_MASK = 0b11111111_11111111_11111111;
	public final static long HEIGHT_MASK = 0b11_1111_1111;
	public final static long DEPTH_MASK = 0b11_1111_1111;
	public final static long LIGHTS_MASK = 0b1111_1111;
	public final static long BLOCK_LIGHT_MASK = 0b1111;
	public final static long SKY_LIGHT_MASK = 0b1111;
	public final static long VERTICAL_INDEX_MASK = 0b11;
	public final static long FLAG_MASK = 0b1;
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

	public static long createDataPoint(int height, int depth, int color, int lightSky, int lightBlock, int generationMode)
	{
		return createDataPoint(
				ColorUtil.getAlpha(color),
				ColorUtil.getRed(color),
				ColorUtil.getGreen(color),
				ColorUtil.getBlue(color),
				height, depth, lightSky, lightBlock, generationMode);
	}

	public static long createDataPoint(int alpha, int red, int green, int blue, int height, int depth, int lightSky, int lightBlock, int generationMode)
	{
		long dataPoint = 0;
		dataPoint += ((alpha & ALPHA_MASK) >>> ALPHA_DOWNSIZE_SHIFT) << ALPHA_SHIFT;
		dataPoint += (red & RED_MASK) << RED_SHIFT;
		dataPoint += (green & GREEN_MASK) << GREEN_SHIFT;
		dataPoint += (blue & BLUE_MASK) << BLUE_SHIFT;
		dataPoint += (height & HEIGHT_MASK) << HEIGHT_SHIFT;
		dataPoint += (depth & DEPTH_MASK) << DEPTH_SHIFT;
		dataPoint += (lightBlock & BLOCK_LIGHT_MASK) << BLOCK_LIGHT_SHIFT;
		dataPoint += (lightSky & SKY_LIGHT_MASK) << SKY_LIGHT_SHIFT;
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
		return (short) (((dataPoint >>> ALPHA_SHIFT) & ALPHA_MASK) << ALPHA_DOWNSIZE_SHIFT);
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

	public static int getLightSky(long dataPoint)
	{
		return (int) ((dataPoint >>> SKY_LIGHT_SHIFT) & SKY_LIGHT_MASK);
	}

	public static int getLightBlock(long dataPoint)
	{
		return (int) ((dataPoint >>> BLOCK_LIGHT_SHIFT) & BLOCK_LIGHT_MASK);
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
		//int color = getBlue(dataPoint) << BLUE_COLOR_SHIFT;
		//color += getRed(dataPoint) << BLUE_COLOR_SHIFT;
		return (int) (dataPoint >>> COLOR_SHIFT);
	}

	public static int getLightColor(long dataPoint, NativeImage lightMap)
	{
		int lightBlock = getLightBlock(dataPoint);
		int lightSky = getLightSky(dataPoint);
		/**TODO ALL of this should be dimension dependent and lightMap dependent*/
		int color = lightMap.getPixelRGBA(lightBlock, lightSky);
		int red = ColorUtil.getBlue(color);
		int green = ColorUtil.getGreen(color);
		int blue = ColorUtil.getRed(color);

		return ColorUtil.multiplyRGBcolors(getColor(dataPoint), ColorUtil.rgbToInt(red, green, blue));
	}

	public static String toString(long dataPoint)
	{
		StringBuilder s = new StringBuilder();
		s.append(getHeight(dataPoint));
		s.append(" ");
		s.append(getDepth(dataPoint));
		s.append(" ");
		s.append(getAlpha(dataPoint));
		s.append(" ");
		s.append(getRed(dataPoint));
		s.append(" ");
		s.append(getBlue(dataPoint));
		s.append(" ");
		s.append(getGreen(dataPoint));
		s.append(" ");
		s.append(getLightBlock(dataPoint));
		s.append(" ");
		s.append(getLightSky(dataPoint));
		s.append(" ");
		s.append(getGenerationMode(dataPoint));
		s.append(" ");
		s.append(isItVoid(dataPoint));
		s.append(" ");
		s.append(doesItExist(dataPoint));
		s.append('\n');
		return s.toString();
	}

	public static long mergeSingleData(long[] dataToMerge)
	{
		int numberOfChildren = 0;

		int tempAlpha = 0;
		int tempRed = 0;
		int tempGreen = 0;
		int tempBlue = 0;
		int tempHeight = 0;
		int tempDepth = 0;
		int tempLightBlock = 0;
		int tempLightSky = 0;
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
					tempLightBlock += DataPointUtil.getLightBlock(data);
					tempLightSky += DataPointUtil.getLightSky(data);
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
			tempLightBlock = tempLightBlock / numberOfChildren;
			tempLightSky = tempLightSky / numberOfChildren;
			return DataPointUtil.createDataPoint(tempAlpha, tempRed, tempGreen, tempBlue, tempHeight, tempDepth, tempLightSky, tempLightBlock, tempGenMode);
		}
	}

	public static long[] mergeMultiData(long[] dataToMerge, int inputVerticalData,int maxVerticalData)
	{
		int size = dataToMerge.length / inputVerticalData;
		short[] projection = ThreadMapUtil.getProjectionShort((WORLD_HEIGHT) / 16  + 1);
		short[][] heightAndDepth = ThreadMapUtil.getHeightAndDepth(inputVerticalData);
		long[] singleDataToMerge = ThreadMapUtil.getSingleAddDataToMerge(size);
		int genMode = DistanceGenerationMode.SERVER.complexity;
		boolean allEmpty = true;
		boolean allVoid = true;
		long singleData;


		for(int k=0; k < projection.length; k++) //probably can remove
			projection[k] = 0;
		short depth = 0;
		short height = 0;

		//We collect the indexes of the data, ordered by the depth
		for (int index = 0; index < size; index++)
		{
			for (int dataIndex = 0; dataIndex < inputVerticalData; dataIndex++)
			{
				singleData = dataToMerge[index * inputVerticalData + dataIndex];
				if (doesItExist(singleData))
				{
					genMode = Math.min(genMode, getGenerationMode(singleData));
					allEmpty = false;
					if (!isItVoid(singleData))
					{
						allVoid = false;
						depth = getDepth(singleData);
						height = getHeight(singleData);
						for (int y = depth; y <= height; y++)
							projection[y / 16] |= 1 << (y & 0xf);
					}
				}
			}
		}

		//We check if there is any data that's not empty or void
		if (allEmpty)
			return new long[]{EMPTY_DATA};
		if (allVoid)
			return new long[]{createVoidDataPoint(genMode)};

		//We extract the merged data
		int count = 0;
		int i = 0;
		int ii = 0;
		while (i < projection.length)
		{
			while (i < projection.length && projection[i] == 0)	i++;
			if (i == projection.length)
				break; //we reached end of WORLD_HEIGHT and it's nothing more here
			while (ii < 15 && ((projection[i] >>> ii) & 1) == 0) ii++;
			if (ii >= 15 && ((projection[i] >>> ii) & 1) == 0) //there is nothing more in this chunk
			{
				ii = 0;
				i++;
				continue;
			}
			depth = (short)( i * 16 + ii);

			while (ii < 15 && ((projection[i] >>> ii) & 1) == 1) ii++;
			if (ii >= 15 && ((projection[i] >>> ii) & 1) == 1) //if end is not in this chunk
			{
				ii = 0;
				i++;
				while (i < projection.length && ~(projection[i]) == 0)	i++; //check for big solid blocks
				if (i == projection.length) //solid to WORLD_HEIGHT
				{
					heightAndDepth[count][0] = depth;
					heightAndDepth[count][1] = WORLD_HEIGHT - 1;
					break;
				}
				while ((((projection[i] >>> ii) & 1) == 1)) ii++;
			}
			height = (short)(i * 16 + ii - 1);
			heightAndDepth[count][0] = depth;
			heightAndDepth[count][1] = height;
			count++;
		}

		//we limit the vertical portion to maxVerticalData
		int j = 0;
		while (count > maxVerticalData)
		{
			ii = WORLD_HEIGHT;
			for (i = 0; i < count - 1; i++)
			{
				if (heightAndDepth[i][1] - heightAndDepth[i + 1][0] < ii)
				{
					ii = heightAndDepth[i][1] - heightAndDepth[i + 1][0];
					j = i;
				}
			}
			heightAndDepth[j][1] = heightAndDepth[j + 1][1];
			for (i = j + 1; i < count - 1; i++){
				heightAndDepth[i][1] = heightAndDepth[i + 1][1];
				heightAndDepth[i][2] = heightAndDepth[i + 1][2];
			}
			//System.arraycopy(heightAndDepth,j + 1, heightAndDepth, j,count - j - 1);
			count--;
		}

		//As standard the vertical lods are ordered from top to bottom
		long[] dataPoint = new long[count];
		for (j = count - 1; j >= 0; j--)
		{
			depth = heightAndDepth[j][0];
			height = heightAndDepth[j][1];
			for(int k = 0; k < size; k++){
				singleDataToMerge[k] = 0;
			}
			for (int index = 0; index < size; index++)
			{
				for (int dataIndex = 0; dataIndex < inputVerticalData; dataIndex++)
				{
					singleData = dataToMerge[index * inputVerticalData + dataIndex];
					if (doesItExist(singleData) && !isItVoid(singleData))
					{
						if ((depth <= getDepth(singleData) && getDepth(singleData) <= height)
								    || (depth <= getHeight(singleData) && getHeight(singleData) <= height))
						{
							if(getHeight(singleData) > getHeight(singleDataToMerge[index]))
							{
								singleDataToMerge[index] = singleData;
							}
						}
					}
				}
			}
			long data = mergeSingleData(singleDataToMerge);
			dataPoint[j] = createDataPoint(height, depth, getColor(data), getLightSky(data), getLightBlock(data), getGenerationMode(data));
		}
		return dataPoint;
	}

	public static long[] compress(long[] data, byte detailLevel)
	{
		return null;
	}
}