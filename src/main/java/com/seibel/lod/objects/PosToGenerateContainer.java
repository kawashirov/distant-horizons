package com.seibel.lod.objects;

import com.seibel.lod.util.LevelPosUtil;

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

	public void addPosToGenerate(byte detailLevel, int posX, int posZ)
	{
		int distance = LevelPosUtil.minDistance(detailLevel, posX, posZ, playerPosX, playerPosZ);
		int index;
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
			while (index < posToGenerate.length - 1 && LevelPosUtil.compareLevelAndDistance(detailLevel, distance, (byte) (posToGenerate[index + 1][0] - 1), posToGenerate[index + 1][3]) <= 0)
			{
				posToGenerate[index][0] = posToGenerate[index + 1][0];
				posToGenerate[index][1] = posToGenerate[index + 1][1];
				posToGenerate[index][2] = posToGenerate[index + 1][2];
				posToGenerate[index][3] = posToGenerate[index + 1][3];
				index++;
			}
			if (index <= posToGenerate.length - 1)
			{
				posToGenerate[index][0] = detailLevel + 1;
				posToGenerate[index][1] = posX;
				posToGenerate[index][2] = posZ;
				posToGenerate[index][3] = distance;
			}
		} else
		{//We are introducing a position in the near array
			if (nearSize < maxNearSize)
				nearSize++;
			index = nearSize - 1;

			while (index > 0 && LevelPosUtil.compareDistance(distance, posToGenerate[index - 1][3]) <= 0)
			{
				posToGenerate[index][0] = posToGenerate[index - 1][0];
				posToGenerate[index][1] = posToGenerate[index - 1][1];
				posToGenerate[index][2] = posToGenerate[index - 1][2];
				posToGenerate[index][3] = posToGenerate[index - 1][3];
				index--;
			}
			if (index >= 0)
			{
				posToGenerate[index][0] = detailLevel + 1;
				posToGenerate[index][1] = posX;
				posToGenerate[index][2] = posZ;
				posToGenerate[index][3] = distance;
			}
		}
	}

	public int getNumberOfPos()
	{
		return nearSize+farSize;
	}

	public int getNthDetail(int n)
	{
		int index;
		if (n > farSize * 2)
			index = n - farSize;
		else if (n % 2 == 0)
			index = n / 2;
		else
			index = posToGenerate.length - n / 2 - 1;
		return posToGenerate[index][0];
	}

	public int getNthPosX(int n)
	{
		int index;
		if (n > farSize * 2)
			index = n - farSize;
		else if (n % 2 == 0)
			index = n / 2;
		else
			index = posToGenerate.length - n / 2 - 1;
		return posToGenerate[index][1];
	}
	public int getNthPosZ(int n)
	{
		int index;
		if (n > farSize * 2)
			index = n - farSize;
		else if (n % 2 == 0)
			index = n / 2;
		else
			index = posToGenerate.length - n / 2 - 1;
		return posToGenerate[index][2];
	}
	public int getNthGeneration(int n)
	{
		int index;
		if (n > farSize * 2)
			index = n - farSize;
		else if (n % 2 == 0)
			index = n / 2;
		else
			index = posToGenerate.length - n / 2 - 1;
		return posToGenerate[index][3];
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
			builder.append(posToGenerate[i][0]-1);
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
			builder.append(posToGenerate[i][0]-1);
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
