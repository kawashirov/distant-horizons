package com.seibel.lod.objects;
/*
import com.seibel.lod.util.DataPointUtil;
import com.seibel.lod.util.LevelPosUtil;
import com.seibel.lod.util.LodUtil;

public class VerticalLevelContainer implements LevelContainer
{

	public final byte detailLevel;

	public final long[][][] dataContainer;

	public VerticalLevelContainer(byte detailLevel)
	{
		this.detailLevel = detailLevel;
		int size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		dataContainer = new long[size][size][];
	}

	public VerticalLevelContainer(byte detailLevel, long[][][] data)
	{
		this.detailLevel = detailLevel;
		this.dataContainer = data;
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

	public long[] getData(int posX, int posZ){
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		return dataContainer[posX][posZ];
	}

	public boolean doesItExist(int posX, int posZ){
		long[] data = getData(posX,posZ);
		return (data != null && DataPointUtil.doesItExist(data[0]));
	}

	public VerticalLevelContainer(String inputString)
	{

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
		}
	}

	public LevelContainer expand(){
		return new SingleLevelContainer((byte) (getDetailLevel() - 1));
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
		addData(data,posX,posZ);
	}

	public void updateData(LevelContainer lowerLevelContainer, int posX, int posZ)
	{
		long[][][] updateTemps;
		int[] indexes;
		if(!LevelContainer.threadVerticalUpdateMap.containsKey(Thread.currentThread().getName()) || (LevelContainer.threadVerticalUpdateMap.get(Thread.currentThread().getName()) == null))
		{
			//To avoid the creation of multiple
			updateTemps = new long[4][][];
			updateTemps[0] = new long[4][16];
			updateTemps[1] = new long[1][32];
			updateTemps[2] = new long[1][4];
			updateTemps[3] = new long[1][4];
			LevelContainer.threadVerticalUpdateMap.put(Thread.currentThread().getName(), updateTemps);
		}
		if(!LevelContainer.threadVerticalIndexesMap.containsKey(Thread.currentThread().getName()) || (LevelContainer.threadVerticalIndexesMap.get(Thread.currentThread().getName()) == null))
		{
			//To avoid the creation of multiple
			indexes = new int[4];
			LevelContainer.threadVerticalIndexesMap.put(Thread.currentThread().getName(), updateTemps);
		}

		updateTemps = LevelContainer.threadVerticalIndexesMap.get(Thread.currentThread().getName());

		long[][] dataArray = updateTemps[0];
		long[] newDataPoint = updateTemps[1][1];
		long[] indexes = updateTemps[2][1];
		long[] dataToCombine = updateTemps[3][1];
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
					if (minDepth < DataPointUtil.getDepth(dataArray[arrayIndex][indexes[arrayIndex]]))
					{
						minDepth = DataPointUtil.getDepth(dataArray[arrayIndex][indexes[arrayIndex]]);
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
				stringBuilder.append(Long.toHexString(dataContainer[x][z][0]));
				stringBuilder.append(DATA_DELIMITER);
			}
		}
		return stringBuilder.toString();
	}
}*/
