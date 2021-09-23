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
	private int[][] nearPosToGenerate;
	private int[][] farPosToGenerate;


	public PosToGenerateContainer(byte farMinDetail, int maxDataToGenerate, int maxFarDataToGenerate, int playerPosX, int playerPosZ)
	{
		this.playerPosX = playerPosX;
		this.playerPosZ = playerPosZ;
		this.farMinDetail = farMinDetail;
		maxNearSize = maxDataToGenerate-maxFarDataToGenerate;
		maxFarSize = maxFarDataToGenerate;
		maxSize = maxDataToGenerate;
		nearSize = 0;
		farSize = 0;
		nearPosToGenerate = new int[maxDataToGenerate][4];
		farPosToGenerate = new int[maxDataToGenerate][4];
	}

	public void addPosToGenerate(byte detailLevel, int posX, int posZ)
	{
		int distance = LevelPosUtil.minDistance(detailLevel, posX, posZ, playerPosX, playerPosZ);
		int index;
		if (detailLevel >= farMinDetail)
		{//We are introducing a position in the far array

			if(farSize < farPosToGenerate.length)
				farSize++;
			index = farSize;
			//while (index > 0 && LevelPosUtil.compareDistance(distance, farPosToGenerate[index - 1][3]) <= 0)
			while (index > 0 && LevelPosUtil.compareDistance(distance, farPosToGenerate[index - 1][3]) <= 0)
			{
				farPosToGenerate[index][0] = farPosToGenerate[index - 1][0];
				farPosToGenerate[index][1] = farPosToGenerate[index - 1][1];
				farPosToGenerate[index][2] = farPosToGenerate[index - 1][2];
				farPosToGenerate[index][3] = farPosToGenerate[index - 1][3];
				index--;
			}
			if (index != farSize-1 || farSize != farPosToGenerate.length)
			{
				farPosToGenerate[index][0] = detailLevel + 1;
				farPosToGenerate[index][1] = posX;
				farPosToGenerate[index][2] = posZ;
				farPosToGenerate[index][3] = distance;
			}
		} else
		{//We are introducing a position in the near array
			if(nearSize < nearPosToGenerate.length)
				nearSize++;
			index = nearSize-1;
			while (index > 0 && LevelPosUtil.compareDistance(distance, nearPosToGenerate[index - 1][3]) <= 0)
			{
				nearPosToGenerate[index][0] = nearPosToGenerate[index - 1][0];
				nearPosToGenerate[index][1] = nearPosToGenerate[index - 1][1];
				nearPosToGenerate[index][2] = nearPosToGenerate[index - 1][2];
				nearPosToGenerate[index][3] = nearPosToGenerate[index - 1][3];
				index--;
			}
			if (index != nearSize-1 || nearSize != nearPosToGenerate.length)
			{
				nearPosToGenerate[index][0] = detailLevel + 1;
				nearPosToGenerate[index][1] = posX;
				nearPosToGenerate[index][2] = posZ;
				nearPosToGenerate[index][3] = distance;
			}
		}
	}

	public int getNumberOfPos()
	{
		return nearSize+farSize;
	}

	public int getNumberOfNearPos()
	{
		return nearSize;
	}

	public int getNumberOfFarPos()
	{
		return farSize;
	}

	public int getNthDetail(int n, boolean near)
	{
		if (near)
			return nearPosToGenerate[n][0];
		else
			return farPosToGenerate[n][0];
	}
	public int getNthPosX(int n, boolean near)
	{
		if (near)
			return nearPosToGenerate[n][1];
		else
			return farPosToGenerate[n][1];
	}
	public int getNthPosZ(int n, boolean near)
	{
		if (near)
			return nearPosToGenerate[n][2];
		else
			return farPosToGenerate[n][2];
	}
	public int getNthGeneration(int n, boolean near)
	{
		if (near)
			return nearPosToGenerate[n][3];
		else
			return farPosToGenerate[n][3];
	}

	public String toString()
	{/*
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
		return builder.toString();*/
		return " ";
	}
}
