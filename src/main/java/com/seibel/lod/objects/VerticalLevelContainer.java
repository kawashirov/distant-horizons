package com.seibel.lod.objects;

import com.seibel.lod.util.*;

import java.security.InvalidParameterException;
import java.util.Arrays;

public class VerticalLevelContainer implements LevelContainer
{

	public final byte detailLevel;
	public final int size;
	public final int maxVerticalData;

	public final long[] dataContainer;

	public VerticalLevelContainer(byte detailLevel)
	{
		this.detailLevel = detailLevel;
		size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		maxVerticalData = DetailDistanceUtil.getMaxVerticalData(detailLevel);
		dataContainer = new long[size * size * DetailDistanceUtil.getMaxVerticalData(detailLevel)];
	}

	@Override
	public byte getDetailLevel()
	{
		return detailLevel;
	}

	public void clear(int posX, int posZ){

		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		int index = 0;
		for(int i = 0; i < maxVerticalData; i++){
			index = posX*size*maxVerticalData + posZ*maxVerticalData + i;
			dataContainer[index] = DataPointUtil.EMPTY_DATA;
		}
	}

	public boolean addData(long data, int posX, int posZ, int verticalIndex){

		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		dataContainer[posX*size*maxVerticalData + posZ*maxVerticalData + verticalIndex] = data;
		return true;
	}

	public boolean addSingleData(long data, int posX, int posZ){
		return addData(data, posX, posZ, 0);
	}

	public long getData(int posX, int posZ, int verticalIndex){
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		return dataContainer[posX*size*maxVerticalData + posZ*maxVerticalData + verticalIndex];
	}

	public long getSingleData(int posX, int posZ){
		return getData(posX,posZ,0);
	}

	public int getMaxVerticalData(){
		return maxVerticalData;
	}

	public int getSize(){
		return size;
	}

	public boolean doesItExist(int posX, int posZ){
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		return DataPointUtil.doesItExist(getSingleData(posX,posZ));
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
		//long[] dataToMerge = ThreadMapUtil.getVerticalUpdateArray(maxVerticalData);
		long[] dataToMerge = new long[4*lowerLevelContainer.getMaxVerticalData()];

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
				for(int verticalIndex = 0; verticalIndex < maxVerticalData; verticalIndex++)
					dataToMerge[(z*2+x)*maxVerticalData + verticalIndex] = lowerLevelContainer.getData(childPosX, childPosZ, verticalIndex);
			}
		}
		data = DataPointUtil.mergeMultiData(dataToMerge, lowerLevelContainer.getDetailLevel());

		for(int verticalIndex = 0; (verticalIndex < data.length) && (verticalIndex < maxVerticalData); verticalIndex++)
		{
			addData(data[verticalIndex],
					posX,
					posZ,
					verticalIndex);
		}
	}

	public byte[] toDataString()
	{
		int index = 0;
		int tempIndex;
		byte[] tempData = new byte[2 + (size * size * maxVerticalData * 8)];
		tempData[index] = detailLevel;
		index++;
		tempData[index] = (byte) maxVerticalData;
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
