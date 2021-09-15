package com.seibel.lod.objects;

import com.seibel.lod.builders.LodBuilder;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.util.*;

public class SingleLevelContainer implements LevelContainer
{
	public final byte detailLevel;
	public final int size;

	public final long[][] data;

	public SingleLevelContainer(byte detailLevel)
	{
		this.detailLevel = detailLevel;
		size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		data = new long[size][size];
	}

	public boolean addData(long[] newData, int posX, int posZ){

		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		data[posX][posZ] = newData[0];
		return true;
	}

	public boolean addSingleData(long newData, int posX, int posZ){
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		data[posX][posZ] = newData;
		return true;
	}

	public long[] getData(int posX, int posZ){
		long[] dataArray = ThreadMapUtil.getSingleGetDataArray();
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		//Improve this using a thread map to long[]
		dataArray[0] = data[posX][posZ];
		return dataArray;
	}

	public long getSingleData(int posX, int posZ){
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		//Improve this using a thread map to long[]
		return data[posX][posZ];
	}

	public byte getDetailLevel(){
		return detailLevel;
	}

	public LevelContainer expand(){
		return new SingleLevelContainer((byte) (getDetailLevel() - 1));
	}

	public SingleLevelContainer(String inputString)
	{
		int tempIndex;
		int shift = 0;
		int index = 0;
		int digit;
		char currentChar;
		long newData;
		currentChar = inputString.charAt(index);
		digit = Character.digit(currentChar,16);
		detailLevel = (byte) digit;
		size = (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		this.data = new long[size][size];
		for (int x = 0; x < size; x++)
		{
			for (int z = 0; z < size; z++)
			{
				newData = 0;
				for(tempIndex = 0; tempIndex < 16; tempIndex++)
				{
					if(index+tempIndex >= inputString.length())
						break;
					currentChar = inputString.charAt(index+tempIndex);
					if(currentChar == DATA_DELIMITER){
						break;
					}
					shift = (15-tempIndex)*4;
					digit = Character.digit(currentChar,16);
					newData += ((((long) digit & 0xf)) << shift);
				}
				newData = newData >>> (shift);
				data[x][z] = newData;
				index = index + tempIndex;
			}
		}
	}

	public void updateData(LevelContainer lowerLevelContainer, int posX, int posZ)
	{
		//We reset the array
		long[] dataToMerge = ThreadMapUtil.getSingleUpdateArray();

		int childPosX;
		int childPosZ;
		long data = 0;
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		for (int x = 0; x <= 1; x++)
		{
			for (int z = 0; z <= 1; z++)
			{
				childPosX = 2 * posX + x;
				childPosZ = 2 * posZ + z;
				dataToMerge[2*x + z] = ((SingleLevelContainer) lowerLevelContainer).getSingleData(childPosX, childPosZ);
			}
		}
		data = DataPointUtil.mergeSingleData(dataToMerge);
		addSingleData(data,posX,posZ);
	}


	public boolean doesItExist(int posX, int posZ){
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		//Improve this using a thread map to long[]
		return DataPointUtil.doesItExist(getSingleData(posX, posZ));
	}

	public String toDataString()
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
				stringBuilder.append(Long.toHexString(data[x][z]));
				stringBuilder.append(DATA_DELIMITER);
			}
		}
		return stringBuilder.toString();
	}

	@Override
	public String toString()
	{
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(detailLevel);
		return stringBuilder.toString();
	}
}
