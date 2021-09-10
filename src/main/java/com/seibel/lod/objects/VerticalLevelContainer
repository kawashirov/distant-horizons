package com.seibel.lod.objects;

import java.io.Serializable;

import com.seibel.lod.util.DataPointUtil;
import com.seibel.lod.util.DetailDistanceUtil;
import com.seibel.lod.util.LevelPosUtil;
import com.seibel.lod.util.LodUtil;

public class VerticalLevelContainer implements LevelContainer
{
	public final byte detailLevel;

	public final long[][][] data;

	public VerticalLevelContainer(byte detailLevel)
	{
		this.detailLevel = detailLevel;
		int size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		data = new long[size][size][];
	}

	public VerticalLevelContainer(byte detailLevel, long[][][] data)
	{
		this.detailLevel = detailLevel;
		this.data = data;
	}

	public boolean addData(long[] data, int posX, int posZ){
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		data[posX][posZ] = data
		return true;
	}

	public long[] getData(int posX, int posZ){
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		return data[posX][posZ];
	}

	public VerticalLevelContainer(String inputString)
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


	public void updateData(LevelContainer lowerLevelContainer, byte detailLevel, int posX, int posZ)
	{
		long[][] dataArray;
		long[] newDataPoint;
		int[] indexes;
		long[] dataToCombine;
		//int maxSize = Math.max(Math.max(Math.max(dataArray[0].length, dataArray[1].length), dataArray[2].length), dataArray[3].length);
		//DetailDistanceUtil.getMaxVerticalData(detailLevel);
		//we are re-using these arrays so we must reset them to 0
		int dataIndex = 0;
		int i;
		for (i = 0; i < newDataPoint.length; i++)
			newDataPoint[i] = 0;
		for (i = 0; i < 4; i++)
			indexes[i] = 0;
		//We continue until all the data has been read
		int minDepth;
		int maxHeight;
		int selectedDepth;
		int selectedHeight;
		int startingArray;
		while (indexes[0] < dataArray[0].length
				       && indexes[1] < dataArray[1].length
				       && indexes[2] < dataArray[2].length
				       && indexes[3] < dataArray[3].length)
		{
			//We select the data that at the lowest point
			minDepth = Integer.MAX_VALUE;
			maxHeight = Integer.MIN_VALUE;
			startingArray = 0;
			for (int arrayIndex = 0; arrayIndex < 4; arrayIndex++)
			{
				if (indexes[arrayIndex] < dataArray[arrayIndex].length)
				{
					if (minDepth < getDepth(dataArray[arrayIndex][indexes[arrayIndex]]))
					{
						minDepth = getDepth(dataArray[arrayIndex][indexes[arrayIndex]]);
						startingArray = arrayIndex;
					}
				}
			}
			selectedDepth = minDepth;
			//now we have selected the dataPoint that has yet to be analyzed with min depth
			dataToCombine[startingArray] = dataArray[startingArray][indexes[startingArray]];
			indexes[startingArray]++;
			newDataPoint[dataIndex] = minDepth;

			//now we must check if the other data can be combined with this lod
			maxHeight = DataPointUtil.getHeight(dataArray[startingArray][indexes[startingArray]]);

			for (int arrayIndex = 0; arrayIndex < 4; arrayIndex++)
			{
				while (maxHeight >= getDepth(dataArray[arrayIndex][indexes[arrayIndex]]))
				{
					maxHeight = getHeight(dataArray[arrayIndex][indexes[arrayIndex]]);
					dataToCombine[arrayIndex] = dataArray[arrayIndex][indexes[arrayIndex]];
					indexes[arrayIndex]++;
				}
			}
			dataIndex++;

		}
		return null;
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
