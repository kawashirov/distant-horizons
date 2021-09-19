package com.seibel.lod.objects;

import com.seibel.lod.util.DataPointUtil;
import com.seibel.lod.util.LevelPosUtil;
import com.seibel.lod.util.LodUtil;
import com.seibel.lod.util.ThreadMapUtil;

import java.security.InvalidParameterException;
import java.util.Arrays;

public class VerticalLevelContainer implements LevelContainer
{

	public final byte detailLevel;
	public final int size;
	public final int maxVerticalData;

	public final long[][][] dataContainer;

	public VerticalLevelContainer(byte detailLevel)
	{
		this.detailLevel = detailLevel;
		size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		dataContainer = new long[size][size][1];
	}

	@Override
	public byte getDetailLevel()
	{
		return detailLevel;
	}

	public boolean addData(long[] newData, int posX, int posZ){
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		dataContainer[posX][posZ] = newData;
		return true;
	}

	public boolean addSingleData(long newData, int posX, int posZ){
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		dataContainer[posX][posZ][0] = newData;
		return true;
	}

	public long[] getData(int posX, int posZ){
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		return dataContainer[posX][posZ];
	}

	public long getSingleData(int posX, int posZ){
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		//Improve this using a thread map to long[]
		return dataContainer[posX][posZ][0];
	}

	public boolean doesItExist(int posX, int posZ){
		long[] data = getData(posX,posZ);
		if(data == null)
			return false;
		return DataPointUtil.doesItExist(data[0]);
	}

	public VerticalLevelContainer(byte inputData[])
	{
		int tempIndex;
		int index = 0;
		long newData;
		detailLevel = inputData[index];
		index++;
		maxVerticalData = inputData[index];
		index++;
		size = (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		this.dataContainer = new long[size * size * maxVerticalData];
		int x, y, z = 0;
		for (x = 0; x < size; x++)
		{
			for (z = 0; z < size; z++)
			{
				for (y = 0; y < maxVerticalData; y++) {
					newData = 0;
					if (inputData[index] == 0)
						index++;
					else if (index + 7 >= inputData.length)
						break;
					else {
						for (tempIndex = 0; tempIndex < 8; tempIndex++)
							newData += (((long) inputData[index + tempIndex]) & 0xff) << (8 * tempIndex);
						index = index + 8;
					}
					dataContainer[(x * size + z) * maxVerticalData + y] = newData;
				}
			}
		}
	}

	public LevelContainer expand(){
		return new VerticalLevelContainer((byte) (getDetailLevel() - 1));
	}

	public void updateData(LevelContainer lowerLevelContainer, int posX, int posZ)
	{
		//We reset the array
		long[][] dataToMerge = ThreadMapUtil.getVerticalUpdateArray();

		int childPosX;
		int childPosZ;
		long[] data;
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		for (int x = 0; x <= 1; x++)
		{
			for (int z = 0; z <= 1; z++)
			{
				childPosX = 2 * posX + x;
				childPosZ = 2 * posZ + z;
				dataToMerge[2*z + x] = lowerLevelContainer.getData(childPosX, childPosZ);
			}
		}
		data = DataPointUtil.mergeMultiData(dataToMerge);
		addData(data,posX,posZ);
	}

	public byte[] toDataString()
	{
		int index = 0;
		int tempIndex;
		byte[] tempData = new byte[2 + (size * size * maxVerticalData * 8)];
		tempData[index] = detailLevel;
		index++;
		tempData[index] = maxVerticalData;
		index++;
		int x, y, z = 0;
		for (x = 0; x < size; x++)
		{
			for (z = 0; z < size; z++)
			{
				for (y = 0; y < maxVerticalData; y++)
				{
					if (dataContainer[(x * size + z) * maxVerticalData + y] == 0)
					{
						tempData[index] = 0;
						index++;
					} else if (dataContainer[(x * size + z) * maxVerticalData + y] == 3)
					{
						tempData[index] = 3;
						index++;
					} else {
						for (tempIndex = 0; tempIndex < 8; tempIndex++)
							tempData[index + tempIndex] = (byte) (dataContainer[(x * size + z) * maxVerticalData + y] >>> (8 * tempIndex));
						index += 8;
					}
				}
			}
		}
		return Arrays.copyOfRange(tempData, 0, index);
	}

	@Override
	public String toString()
	{
		/*
		StringBuilder stringBuilder = new StringBuilder();
		int size = (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		stringBuilder.append(detailLevel);
		stringBuilder.append(DATA_DELIMITER);
		for (int x = 0; x < size; x++)
		{
			for (int z = 0; z < size; z++)
			{
				//Converting the dataToHex
				stringBuilder.append(Long.toHexString(dataContainer[x][z][0]));
				stringBuilder.append(DATA_DELIMITER);
			}
		}
		return stringBuilder.toString();
		 */
		return " ";
	}
}
