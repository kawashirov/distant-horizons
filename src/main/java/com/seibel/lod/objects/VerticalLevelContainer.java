package com.seibel.lod.objects;

import java.util.Arrays;

import com.seibel.lod.util.DataPointUtil;
import com.seibel.lod.util.DetailDistanceUtil;
import com.seibel.lod.util.LevelPosUtil;
import com.seibel.lod.util.LodUtil;
import com.seibel.lod.util.ThreadMapUtil;

/**
 * a VerticalLevelContainer is a quadTree level that can contain multiple voxel column per position.
 */
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

	@Override
	public void clear(int posX, int posZ){

		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		for(int verticalIndex = 0; verticalIndex < maxVerticalData; verticalIndex++){
			dataContainer[posX*size*maxVerticalData + posZ*maxVerticalData + verticalIndex] = DataPointUtil.EMPTY_DATA;
		}
	}

	@Override
	public boolean addData(long data, int posX, int posZ, int verticalIndex){

		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		dataContainer[posX*size*maxVerticalData + posZ*maxVerticalData + verticalIndex] = data;
		return true;
	}

	@Override
	public boolean addSingleData(long data, int posX, int posZ){
		return addData(data, posX, posZ, 0);
	}

	@Override
	public long getData(int posX, int posZ, int verticalIndex){
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		return dataContainer[posX*size*maxVerticalData + posZ*maxVerticalData + verticalIndex];
	}

	@Override
	public long getSingleData(int posX, int posZ){
		return getData(posX,posZ,0);
	}

	@Override
	public int getMaxVerticalData(){
		return maxVerticalData;
	}

	public int getSize(){
		return size;
	}

	@Override
	public boolean doesItExist(int posX, int posZ){
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		return DataPointUtil.doesItExist(getSingleData(posX,posZ));
	}

	public VerticalLevelContainer(byte[] inputData)
	{
		int tempIndex;
		int index = 0;
		int counter = -1;
		long newData;
		byte last = 0;
		detailLevel = inputData[index];
		index++;
		maxVerticalData = inputData[index];
		index++;
		size = (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		int x = size * size * maxVerticalData;
		this.dataContainer = new long[x];
		for ( int i = 0; i < x; i++)
		{
			newData = 0;
			if (counter > -1)
			{
				dataContainer[i] = last;
				if (last == 3)
				{ //skip rest of void chunk
					for (tempIndex = 1; tempIndex < maxVerticalData; tempIndex++) {
						dataContainer[i + tempIndex] = 0;
					}
					i += maxVerticalData - 1;
				}
				counter--;
			} else if ((inputData[index] & 0x3) == 0 || (inputData[index] & 0x3) == 3)
			{
				last = (byte)(inputData[index] & 0x3);
				//recover counter
				counter = (inputData[index] & 0x7c) >>> 2;
				tempIndex = 0;
				while ((inputData[index] & 0x80) == 0x80)
				{ //overflow bit is on
					index++;
					counter += (inputData[index] & 0x7f) << (5 + 7 * tempIndex);
					tempIndex++;
				}
				index++;
				//since loop expects from us to put some data in, we just make it rerun it with new counter;
				i--;
			} else if (index + 7 >= inputData.length)
				break;
			else {
				for (tempIndex = 0; tempIndex < 8; tempIndex++)
					newData += (((long) inputData[index + tempIndex]) & 0xff) << (8 * tempIndex);
				index = index + 8;
				dataContainer[i] = newData;
			}
		}
	}

	@Override
	public LevelContainer expand(){
		return new VerticalLevelContainer((byte) (getDetailLevel() - 1));
	}

	@Override
	public void updateData(LevelContainer lowerLevelContainer, int posX, int posZ)
	{
		//We reset the array
		long[] dataToMerge = ThreadMapUtil.getVerticalUpdateArray(detailLevel);

		int lowerMaxVertical = dataToMerge.length/4;
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
				for(int verticalIndex = 0; verticalIndex < lowerMaxVertical; verticalIndex++)
					dataToMerge[(z*2+x)*lowerMaxVertical + verticalIndex] = lowerLevelContainer.getData(childPosX, childPosZ, verticalIndex);
			}
		}
		data = DataPointUtil.mergeMultiData(dataToMerge, lowerMaxVertical, getMaxVerticalData());
		
		for(int verticalIndex = 0; (verticalIndex < data.length) && (verticalIndex < maxVerticalData); verticalIndex++)
		{
			addData(data[verticalIndex],
					posX,
					posZ,
					verticalIndex);
		}
	}

	@Override
	public byte[] toDataString()
	{
		int index = 0;
		int counter = -1;
		byte last = -1;
		int x = size * size * maxVerticalData;
		int tempIndex;
		long current;
		
		byte[] tempData = ThreadMapUtil.getSaveContainer(2 + (x * 8));

		tempData[index] = detailLevel;
		index++;
		tempData[index] = (byte) maxVerticalData;
		index++;

		for (int i = 0; i < x; i++)
		{
			current = dataContainer[i];
			if ((current & 0b11) == 0 || (current & 0b11) == 3)
			{
				current &= 0b11; //clean any garbage data after those two bits
				last = (byte) current;
				if (current == 3) //skip rest of void chunk
					i += maxVerticalData - 1;
				counter++;
			} else {
				for (tempIndex = 0; tempIndex < 8; tempIndex++)
					tempData[index + tempIndex] = (byte) (current >>> (8 * tempIndex));
				index += 8;
			}
			if (last != -1 && ( i == x - 1 || last != ((dataContainer[i + 1]) & 0b11)))
			{ //save compressed data if next is different or if we reached onf of the data
				tempData[index] = (byte)(0x7f & ((counter << 2) + last)); //save 5 bits of counter and compressed block

				tempIndex = 0;
				while ((counter >>> (5 + 7 * tempIndex)) != 0) //there is more of that counter
				{
					tempData[index] = (byte)(tempData[index] | 0x80); //set overflow bit to true
					index++; // after setting overflow bit w can actually index++
					tempData[index] = (byte)(0x7f & (counter >>> (5 + 7 * tempIndex))); // save 7 bits of counter
					tempIndex++;
				}
				index++;
				last = -1;
				counter = -1;
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

	@Override
	public int getMaxNumberOfLods(){
		return size*size*getMaxVerticalData();
	}

}
