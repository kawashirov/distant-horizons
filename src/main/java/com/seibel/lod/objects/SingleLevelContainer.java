package com.seibel.lod.objects;

import com.seibel.lod.util.*;
import java.util.Arrays;

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

	public SingleLevelContainer(byte inputData[])
	{
		int tempIndex;
		int index = 0;
		long newData;
		detailLevel = inputData[index];
		index++;
		size = (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		this.data = new long[size][size];
		for (int x = 0; x < size; x++)
		{
			for (int z = 0; z < size; z++)
			{
				newData = 0;
				if(inputData[index] == 0)
					index++;
				else if(index + 7 >= inputData.length)
				break;
			else {
				for (tempIndex = 0; tempIndex < 8; tempIndex++)
					newData += (((long) inputData[index + tempIndex]) & 0xff) << (8 * tempIndex);
				index = index + 8;
			}
				data[x][z] = newData;

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

	public byte[] toDataString()
	{
		int index = 0;
		int tempIndex;
		byte[] tempData = new byte[1 + (size * size * 8)];
		tempData[index] = detailLevel;
		index++;
		for (int x = 0; x < size; x++)
		{
			for (int z = 0; z < size; z++)
			{
				if(data[x][z] == 0){
					tempData[index] = 0;
					index++;
				} else {
				for (tempIndex = 0; tempIndex < 8; tempIndex++)
					tempData[index + tempIndex] = (byte) (data[x][z] >>> (8 * tempIndex));
				index += 8;
			}
			}
		}
		return Arrays.copyOfRange(tempData, 0, index);
	}

	@Override
	public String toString()
	{
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(detailLevel);
		return stringBuilder.toString();
	}
}
