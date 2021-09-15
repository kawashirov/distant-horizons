package com.seibel.lod.objects;

import com.seibel.lod.util.LevelPosUtil;
import com.seibel.lod.util.LodUtil;

public class PosToRenderContainer
{
	public byte minDetail;
	private int regionPosX;
	private int regionPosZ;
	private int numberOfPosToRender;
	private int[][] posToRender;
	/*TODO this population matrix could be converted to boolean to improve memory use*/
	private byte[][] population;

	public PosToRenderContainer(byte minDetail, int regionPosX, int regionPosZ)
	{
		this.minDetail = minDetail;
		this.numberOfPosToRender = 0;
		this.regionPosX = regionPosX;
		this.regionPosZ = regionPosZ;
		int size = 1 << (LodUtil.REGION_DETAIL_LEVEL - minDetail);
		posToRender = new int[size*size][3];
		population = new byte[size][size];
	}

	public void addPosToRender(byte detailLevel, int posX, int posZ)
	{
		//if(numberOfPosToRender >= posToRender.length)
		//	posToRender = Arrays.copyOf(posToRender, posToRender.length*2);
		posToRender[numberOfPosToRender][0] = detailLevel;
		posToRender[numberOfPosToRender][1] = posX;
		posToRender[numberOfPosToRender][2] = posZ;
		numberOfPosToRender++;
		population[LevelPosUtil.getRegionModule(minDetail, LevelPosUtil.convert(detailLevel,posX,minDetail))]
				[LevelPosUtil.getRegionModule(minDetail, LevelPosUtil.convert(detailLevel,posZ,minDetail))] = (byte) (detailLevel + 1);
	}

	public boolean contains(byte detailLevel, int posX, int posZ)
	{
		if(LevelPosUtil.getRegion(detailLevel, posX) == regionPosX && LevelPosUtil.getRegion(detailLevel, posZ) == regionPosZ)
		{
			return (population[LevelPosUtil.getRegionModule(minDetail, LevelPosUtil.convert(detailLevel,posX,minDetail))]
					        [LevelPosUtil.getRegionModule(minDetail, LevelPosUtil.convert(detailLevel,posZ,minDetail))] == (detailLevel + 1));
		}else
		{
			return false;
		}
	}
	public void clear(byte minDetail, int regionPosX, int regionPosZ){
		this.numberOfPosToRender = 0;
		this.regionPosX = regionPosX;
		this.regionPosZ = regionPosZ;
		if(this.minDetail == minDetail)
		{
			int size = 1 << (LodUtil.REGION_DETAIL_LEVEL - minDetail);
			for (int x = 0; x < size; x++)
			{
				for (int z = 0; z < size; z++)
				{
					posToRender[0][0] = 0;
					posToRender[0][1] = 0;
					posToRender[0][2] = 0;
					population[x][z] = 0;
				}
			}
		}else{
			this.minDetail = minDetail;
			int size = 1 << (LodUtil.REGION_DETAIL_LEVEL - minDetail);
			posToRender = new int[size*size][3];
			population = new byte[size][size];
		}
	}

	public int getNumberOfPos()
	{
		return numberOfPosToRender;
	}

	public byte getNthDetailLevel(int n)
	{
		return (byte) posToRender[n][0];
	}
	public int getNthPosX(int n)
	{
		return posToRender[n][1];
	}
	public int getNthPosZ(int n)
	{
		return posToRender[n][2];
	}

	public int[] getNthPos(int n)
	{
		return posToRender[n];
	}

	@Override
	public String toString()
	{

		StringBuilder builder = new StringBuilder();
		builder.append("To render ");
		builder.append(numberOfPosToRender);
		builder.append('\n');
		for(int i = 0; i < numberOfPosToRender; i++)
		{
			builder.append(posToRender[i][0]);
			builder.append(" ");
			builder.append(posToRender[i][1]);
			builder.append(" ");
			builder.append(posToRender[i][2]);
			builder.append('\n');
		}
		builder.append('\n');
		return builder.toString();
	}
}
