package com.seibel.lod.objects;

import com.seibel.lod.util.DataPointUtil;
import com.seibel.lod.util.LevelPosUtil;
import com.seibel.lod.util.LodUtil;
import com.seibel.lod.util.ThreadMapUtil;

import java.security.InvalidParameterException;

public class VerticalLevelContainer implements LevelContainer
{

	public final byte detailLevel;

	public final long[][][] dataContainer;

	public VerticalLevelContainer(byte detailLevel)
	{
		this.detailLevel = detailLevel;
		int size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
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
		data = DataPointUtil.mergeVerticalData(dataToMerge);
		addData(data,posX,posZ);
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
