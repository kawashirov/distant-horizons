package com.seibel.lod.objects;

import org.lwjgl.system.CallbackI;

public class PosToGenerateContainer
{
	private int playerPosX;
	private int playerPosZ;
	private byte farMinDetail;
	private int maxSize;
	private int maxNearSize;
	private int maxFarSize;
	private int nearSize;
	private int farSize;
	private int[][] posToGenerate;

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
			}
			index = posToGenerate.length - farSize;
			while (index < posToGenerate.length - 1 && LevelPosUtil.compareLevelAndDistance(tempPos, posToGenerate[index + 1]) <= 0)
			{
				posToGenerate[index] = posToGenerate[index + 1];
				index++;
			}
			if (index <= posToGenerate.length - 1)
				posToGenerate[index] = tempPos;
		} else
		{//We are introducing a position in the near array
			if (nearSize < maxNearSize)
				nearSize++;
			index = nearSize - 1;

			while (index > 0 && LevelPosUtil.compareDistance(tempPos, posToGenerate[index - 1]) <= 0)
			{
				posToGenerate[index] = posToGenerate[index - 1];
				index--;
			}
			if (index >= 0)
				posToGenerate[index] = tempPos;
		}
	}


	public int getNumberOfPos()
	{
		return farSize + nearSize;
	}

	public int[] getNthPos(int n)
	{
		int index;
		if (n > farSize * 2)
			index = n - farSize;
		else if (n % 2 == 0)
			index = n / 2;
		else
			index = posToGenerate.length - n / 2 - 1;
		return posToGenerate[index];
	}

	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("Number of pos to generate ");
		builder.append(farSize + nearSize);
		builder.append('\n');
		builder.append("Number of near pos to generate ");
		builder.append(nearSize);
		builder.append('\n');
		builder.append("Number of far pos to generate ");
		builder.append(farSize);
		builder.append('\n');
		builder.append('\n');
		builder.append("near pos to generate");
		builder.append('\n');
		for (int i = 0; i < nearSize; i++)
		{
			builder.append(posToGenerate[i][0]);
			builder.append(" ");
			builder.append(posToGenerate[i][1]);
			builder.append(" ");
			builder.append(posToGenerate[i][2]);
			builder.append(" ");
			builder.append(posToGenerate[i][3]);
			builder.append('\n');
		}
		builder.append('\n');
		builder.append("far pos to generate");
		builder.append('\n');
		for (int i = maxSize - 1; i >= maxSize - farSize; i--)
		{
			builder.append(posToGenerate[i][0]);
			builder.append(" ");
			builder.append(posToGenerate[i][1]);
			builder.append(" ");
			builder.append(posToGenerate[i][2]);
			builder.append(" ");
			builder.append(posToGenerate[i][3]);
			builder.append('\n');
		}
		builder.append('\n');
		return builder.toString();
	}
}
