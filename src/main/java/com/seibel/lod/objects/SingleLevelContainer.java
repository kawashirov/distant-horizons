package com.seibel.lod.objects;

import java.util.Arrays;

import com.seibel.lod.util.DataPointUtil;
import com.seibel.lod.util.LevelPosUtil;
import com.seibel.lod.util.LodUtil;
import com.seibel.lod.util.ThreadMapUtil;

/**
 * This object holds the LOD data for a single dataPoint.
 * 
 * @author Leonardo Amato
 * @version 9-28-2021
 */
public class SingleLevelContainer implements LevelContainer
{
	/** The detailLevel of this LevelContainer */
	public final byte detailLevel;
	/** How many dataPoints wide is this LevelContainer? */
	public final int dataWidthCount;
	
	/** This holds all the dataPoints for this LevelContainer */
	public final long[][] dataContainer;
	
	
	
	/** Constructor */
	public SingleLevelContainer(byte newDetailLevel)
	{
		this.detailLevel = newDetailLevel;
		
		// equivalent to 2^(...)
		dataWidthCount = 1 << (LodUtil.REGION_DETAIL_LEVEL - newDetailLevel);
		dataContainer = new long[dataWidthCount][dataWidthCount];
	}
	
	/**  */
	public SingleLevelContainer(byte[] inputData)
	{
		int tempIndex;
		int index = 0;
		long newData;
		detailLevel = inputData[index];
		index++;
		dataWidthCount = (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		this.dataContainer = new long[dataWidthCount][dataWidthCount];
		
		for (int x = 0; x < dataWidthCount; x++)
		{
			for (int z = 0; z < dataWidthCount; z++)
			{
				newData = 0;
				if (inputData[index] == 0)
				{
					index++;
				}
				else if (inputData[index] == 3)
				{
					newData = 3;
					index++;
				}
				else if (index + 7 >= inputData.length)
				{
					break;
				}
				else
				{
					for (tempIndex = 0; tempIndex < 8; tempIndex++)
						newData += (((long) inputData[index + tempIndex]) & 0xff) << (8 * tempIndex);
					index = index + 8;
				}
				
				dataContainer[x][z] = newData;
			}
		}
	}
	
	
	
	
	
	@Override
	public void clear(int posX, int posZ)
	{
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		dataContainer[posX][posZ] = DataPointUtil.EMPTY_DATA;
	}
	
	@Override
	public boolean addData(long data, int posX, int posZ, int index)
	{
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		dataContainer[posX][posZ] = data;
		return true;
	}
	
	@Override
	public boolean addSingleData(long newData, int posX, int posZ)
	{
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		dataContainer[posX][posZ] = newData;
		return true;
	}
	
	@Override
	public long getData(int posX, int posZ, int index)
	{
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		// TODO Improve this using a thread map to long[]
		return dataContainer[posX][posZ];
	}
	
	@Override
	public long getSingleData(int posX, int posZ)
	{
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		// TODO Improve this using a thread map to long[]
		return dataContainer[posX][posZ];
	}
	
	@Override
	public byte getDetailLevel()
	{
		return detailLevel;
	}
	
	@Override
	public LevelContainer expand()
	{
		return new SingleLevelContainer((byte) (getDetailLevel() - 1));
	}
	
	
	
	/** TODO could this be renamed mergeData? */
	@Override
	public void updateData(LevelContainer lowerLevelContainer, int posX, int posZ)
	{
		//We reset the array
		long[] dataToMerge = ThreadMapUtil.getSingleUpdateArray();
		
		int childPosX;
		int childPosZ;
		long data;
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		for (int x = 0; x <= 1; x++)
		{
			for (int z = 0; z <= 1; z++)
			{
				childPosX = 2 * posX + x;
				childPosZ = 2 * posZ + z;
				dataToMerge[2 * x + z] = lowerLevelContainer.getSingleData(childPosX, childPosZ);
			}
		}
		data = DataPointUtil.mergeSingleData(dataToMerge);
		addSingleData(data, posX, posZ);
	}
	
	@Override
	public int getMaxVerticalData()
	{
		return 1;
	}
	
	@Override
	public boolean doesItExist(int posX, int posZ)
	{
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		//Improve this using a thread map to long[]
		return DataPointUtil.doesItExist(getSingleData(posX, posZ));
	}
	
	@Override
	public byte[] toDataString()
	{
		int index = 0;
		int tempIndex;
		byte[] tempData = ThreadMapUtil.getSaveContainer(1 + (dataWidthCount * dataWidthCount * 8));
		
		tempData[index] = detailLevel;
		index++;
		for (int x = 0; x < dataWidthCount; x++)
		{
			for (int z = 0; z < dataWidthCount; z++)
			{
				if (dataContainer[x][z] == 0)
				{
					tempData[index] = 0;
					index++;
				} else if (dataContainer[x][z] == 3)
				{
					tempData[index] = 3;
					index++;
				} else
				{
					for (tempIndex = 0; tempIndex < 8; tempIndex++)
						tempData[index + tempIndex] = (byte) (dataContainer[x][z] >>> (8 * tempIndex));
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
	
	
	@Override
	public int getMaxNumberOfLods()
	{
		return dataWidthCount * dataWidthCount * getMaxVerticalData();
	}
	
	@Override
	public int getMaxMemoryUse()
	{
		return getMaxNumberOfLods() * 2; //2 byte
	}
}
