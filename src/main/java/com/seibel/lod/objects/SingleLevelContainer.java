package com.seibel.lod.objects;

import com.seibel.lod.builders.LodBuilder;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.util.DataPointUtil;
import com.seibel.lod.util.DetailDistanceUtil;
import com.seibel.lod.util.LevelPosUtil;
import com.seibel.lod.util.LodUtil;

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

	private boolean addSingleData(long newData, int posX, int posZ){
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		data[posX][posZ] = newData;
		return true;
	}
	public long[] getData(int posX, int posZ){

		if(!LevelContainer.threadGetDataMap.containsKey(Thread.currentThread().getName()) || (LevelContainer.threadGetDataMap.get(Thread.currentThread().getName()) == null))
		{
			LevelContainer.threadGetDataMap.put(Thread.currentThread().getName(), new long[1]);
		}
		long[] dataArray = LevelContainer.threadGetDataMap.get(Thread.currentThread().getName());

		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		//Improve this using a thread map to long[]
		dataArray[0] = data[posX][posZ];
		return dataArray;
	}
	private long getSingleData(int posX, int posZ){
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

		int index = 0;
		int lastIndex = 0;


		index = inputString.indexOf(DATA_DELIMITER, 0);
		detailLevel = (byte) Integer.parseInt(inputString.substring(0, index));
		size = (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel);

		this.data = new long[size][size];
		for (int x = 0; x < size; x++)
		{
			for (int z = 0; z < size; z++)
			{
				lastIndex = index;
				index = inputString.indexOf(DATA_DELIMITER, lastIndex + 1);
				data[x][z] = Long.parseLong(inputString.substring(lastIndex + 1, index), 16);
			}
		}
	}

	public void updateData(LevelContainer lowerLevelContainer, int posX, int posZ)
	{
		//We reset the array
		if(!LevelContainer.threadGetDataMap.containsKey(Thread.currentThread().getName()) || (LevelContainer.threadGetDataMap.get(Thread.currentThread().getName()) == null))
		{
			LevelContainer.threadGetDataMap.put(Thread.currentThread().getName(), new long[4]);
		}
		long[] dataToMerge = LevelContainer.threadGetDataMap.get(Thread.currentThread().getName());

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
				dataToMerge[2*z + x] = lowerLevelContainer.getData(childPosX, childPosZ)[0];
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
