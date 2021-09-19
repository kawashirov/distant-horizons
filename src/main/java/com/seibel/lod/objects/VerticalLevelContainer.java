package com.seibel.lod.objects;

import com.seibel.lod.util.*;

import java.security.InvalidParameterException;

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
		return DataPointUtil.doesItExist(getSingleData(posX,posZ));
	}

	public VerticalLevelContainer(String inputString)
	{

		throw new InvalidParameterException("loading not yet implemented");

/*
		int index = 0;
		int lastIndex = 0;


		index = inputString.indexOf(DATA_DELIMITER, 0);
		this.detailLevel = (byte) Integer.parseInt(inputString.substring(0, index));
		int size = (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel);

		this.dataContainer = new long[size][size][1];
		for (int x = 0; x < size; x++)
		{
			for (int z = 0; z < size; z++)
			{
				lastIndex = index;
				index = inputString.indexOf(DATA_DELIMITER, lastIndex + 1);
				dataContainer[x][z][0] = Long.parseLong(inputString.substring(lastIndex + 1, index), 16);
			}
		}*/
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
		byte[] temp = {};
		return temp;
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
