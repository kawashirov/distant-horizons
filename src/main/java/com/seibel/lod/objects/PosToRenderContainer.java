package com.seibel.lod.objects;

import java.util.Arrays;

public class PosToRenderContainer
{
	private byte minDetail;
	private int numberOfPosToRender;
	private int[][] posToRender;
	private byte[][] population;

	public PosToRenderContainer(byte minDetail)
	{
		this.minDetail = minDetail;
		this.numberOfPosToRender = 0;
		posToRender = new int[1][4];
		population = new byte[minDetail][minDetail];
	}

	public void addPosToRender(int[] levelPos)
	{
		if(numberOfPosToRender >= posToRender.length)
			posToRender = Arrays.copyOf(posToRender, posToRender.length*2);
		posToRender[numberOfPosToRender] = levelPos;
		numberOfPosToRender++;
		int[] newLevelPos = LevelPosUtil.convert(levelPos, minDetail);
		population[LevelPosUtil.getPosZ(newLevelPos)][LevelPosUtil.getPosZ(newLevelPos)] = (byte) (LevelPosUtil.getDetailLevel(levelPos) + 1);
	}

	public boolean isToRender(int[] levelPos)
	{
		return (population[LevelPosUtil.getPosZ(levelPos)][LevelPosUtil.getPosZ(levelPos)] == (LevelPosUtil.getDetailLevel(levelPos) + 1));
	}

	public int getNumberOfPos()
	{
		return numberOfPosToRender;
	}

	public int[] getNthPos(int n)
	{
		return posToRender[n];
	}
}
