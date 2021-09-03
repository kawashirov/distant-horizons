package com.seibel.lod.objects;

public class PosToRenderContainer
{
	public int playerPosX;
	public int playerPosZ;
	public int numberOfPosToRender;
	public int[][] posToRender;
	public boolean[][] posToRenderAdjacency;

	public PosToRenderContainer(byte minDetail)
	{
		this.playerPosX = playerPosX;
		this.playerPosZ = playerPosZ;
		this.numberOfPosToRender = 0;
		posToRender = new int[1][4];
		posToRenderAdjacency = new boolean[minDetail][minDetail];
	}

	public void addPosToRender(int[] levelPos)
	{
		addPosToRender(LevelPosUtil.getDetailLevel(levelPos), LevelPosUtil.getPosX(levelPos), LevelPosUtil.getPosZ(levelPos));
	}

	public void addPosToRender(byte detailLevel, int posX, int posZ)
	{
		/*
		if(numberOfPosToRender >= posToRender.length)
			;*/
	}
}
