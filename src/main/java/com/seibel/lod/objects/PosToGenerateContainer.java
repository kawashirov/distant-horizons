package com.seibel.lod.objects;

public class PosToGenerateContainer
{
	public int playerPosX;
	public int playerPosZ;
	public byte farMinDetail;
	public int maxSize;
	public int maxNearSize;
	public int maxFarSize;
	public int nearSize;
	public int farSize;
	public int[][] posToGenerate;

	public PosToGenerateContainer(byte farMinDetail, int maxDataToGenerate, int maxFarDataToGenerate, int playerPosX, int playerPosZ)
	{
		this.playerPosX = playerPosX;
		this.playerPosZ = playerPosZ;
		this.farMinDetail = farMinDetail;
		maxNearSize = maxDataToGenerate;
		maxFarSize = maxFarDataToGenerate;
		maxSize = maxDataToGenerate;
		nearSize = 0;
		farSize = 0;
		posToGenerate = new int[maxDataToGenerate][4];
	}

	public void addPosToGenerate(int[] levelPos)
	{
		addPosToGenerate(LevelPosUtil.getDetailLevel(levelPos), LevelPosUtil.getPosX(levelPos), LevelPosUtil.getPosZ(levelPos));
	}

	public void addPosToGenerate(byte detailLevel, int posX, int posZ)
	{
		int distance = LevelPosUtil.minDistance(detailLevel, posX, posZ, playerPosX, playerPosZ);
		int index;
		int[] tempPos = new int[]{
				detailLevel,
				posX,
				posZ,
				distance};
		if (detailLevel >= farMinDetail)
		{//We are introducing a position in the far array
			if (farSize < maxFarSize)
			{
				farSize++;
				if (nearSize == maxNearSize)
				{
					nearSize--;
				}
				maxNearSize--;
				index = posToGenerate.length - farSize - 1;

				if(farSize == 1)
					posToGenerate[index] = tempPos;
			} else //farSize == maxFarSize, the far section is full
			{
				index = posToGenerate.length - farSize;
				//The max far pos is smaller than the one we want to insert
				if (LevelPosUtil.compareLevelAndDistance(tempPos, posToGenerate[index]) >= 0)
				{
					index = posToGenerate.length;
				}
			}

			while(index < posToGenerate.length - 1 && LevelPosUtil.compareLevelAndDistance(tempPos, posToGenerate[index + 1]) <= 0)
			{
				posToGenerate[index] = posToGenerate[index + 1];
				index++;
			}
			if(index <= posToGenerate.length - 1)
				posToGenerate[index] = tempPos;

		} else
		{//We are introducing a position in the near array
			if (nearSize < maxNearSize)
			{
				nearSize++;
				index = nearSize - 1;
			} else //nearSize == maxNearSize, the far section is full
			{
				//The max near pos is smaller than the one we want to insert
				//We remove the max
				index = nearSize - 1;
				if (LevelPosUtil.compareDistance(tempPos, posToGenerate[index]) >= 0)
				{
					index = -1;
				}
			}

			while(index > 0 && LevelPosUtil.compareDistance(tempPos, posToGenerate[index - 1]) <= 0)
			{
				posToGenerate[index] = posToGenerate[index - 1];
				index--;
			}
			if(index >= 0)
				posToGenerate[index] = tempPos;
		}
	}

}
