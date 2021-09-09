package com.seibel.lod.objects;

import com.seibel.lod.builders.LodBuilder;
import com.seibel.lod.util.DataPointUtil;
import com.seibel.lod.util.LevelPosUtil;
import com.seibel.lod.util.LodUtil;

public class SingleLevelContainer implements LevelContainer
{
	public final byte detailLevel;

	public final long[][] data;

	public SingleLevelContainer(byte detailLevel, long[][] data)
	{
		this.detailLevel = detailLevel;
		this.data = data;
	}

	public boolean putData(long[] data, int posX, int posZ){
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		data[posX][posZ] = data[0];
		return true;
	}

	public long[] getData(int posX, int posZ){
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		return data[posX][posZ];
	}

	public SingleLevelContainer(String inputString)
	{

		int index = 0;
		int lastIndex = 0;


		index = inputString.indexOf(DATA_DELIMITER, 0);
		this.detailLevel = (byte) Integer.parseInt(inputString.substring(0, index));
		int size = (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel);

		this.data = new long[size][size][1];
		for (int x = 0; x < size; x++)
		{
			for (int z = 0; z < size; z++)
			{
				lastIndex = index;
				index = inputString.indexOf(DATA_DELIMITER, lastIndex + 1);
				data[x][z][0] = Long.parseLong(inputString.substring(lastIndex + 1, index), 16);
			}
		}
	}

	public long[] mergeData(long[][] dataArray, long[] newDataPoint, int[] indexes, long[] dataToCombine)
	{
		int numberOfChildren = 0;
		int numberOfVoidChildren = 0;

		int tempRed = 0;
		int tempGreen = 0;
		int tempBlue = 0;
		int tempHeight = 0;
		int tempDepth = 0;
		int childPosX;
		int childPosZ;
		byte childDetailLevel;
		for (int index = 0; x <= 4; x++)
		{
				childDetailLevel = (byte) (detailLevel - 1);
				if (doesDataExist(childDetailLevel, childPosX, childPosZ))
				{
					if (!(DataPointUtil.getHeight(data[childDetailLevel][childPosX][childPosZ]) == LodBuilder.DEFAULT_HEIGHT
							      && DataPointUtil.getDepth(data[childDetailLevel][childPosX][childPosZ]) == LodBuilder.DEFAULT_DEPTH))
					{
						numberOfChildren++;

						tempRed += DataPointUtil.getRed(data[childDetailLevel][childPosX][childPosZ]);
						tempGreen += DataPointUtil.getGreen(data[childDetailLevel][childPosX][childPosZ]);
						tempBlue += DataPointUtil.getBlue(data[childDetailLevel][childPosX][childPosZ]);
						tempHeight += DataPointUtil.getHeight(data[childDetailLevel][childPosX][childPosZ]);
						tempDepth += DataPointUtil.getDepth(data[childDetailLevel][childPosX][childPosZ]);
					} else
					{
						// void children have the default height (most likely -1)
						// and represent a LOD with no blocks in it
						numberOfVoidChildren++;
					}
				}
			}
		}
		if (numberOfChildren > 0)
		{
			tempRed = tempRed / numberOfChildren;
			tempGreen = tempGreen / numberOfChildren;
			tempBlue = tempBlue / numberOfChildren;
			tempHeight = tempHeight / numberOfChildren;
			tempDepth = tempDepth / numberOfChildren;
		} else if (numberOfVoidChildren > 0)
		{
			tempRed = (byte) 0;
			tempGreen = (byte) 0;
			tempBlue = (byte) 0;
			tempHeight = LodBuilder.DEFAULT_HEIGHT;
			tempDepth = LodBuilder.DEFAULT_DEPTH;
		}
		data[detailLevel][posX][posZ] = DataPointUtil.createDataPoint(tempHeight, tempDepth, tempRed, tempGreen, tempBlue);
	}

	public String toDataString()
	{
		return toString();
	}

	@Override
	public String toString()
	{
		StringBuilder stringBuilder = new StringBuilder();
		int size = (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		stringBuilder.append(detailLevel);
		stringBuilder.append(DATA_DELIMITER);
		for (int x = 0; x < size; x++)
		{
			for (int z = 0; z < size; z++)
			{
				//Converting the dataToHex
				stringBuilder.append(Long.toHexString(data[x][z][0]));
				stringBuilder.append(DATA_DELIMITER);
			}
		}
		return stringBuilder.toString();
	}
}
