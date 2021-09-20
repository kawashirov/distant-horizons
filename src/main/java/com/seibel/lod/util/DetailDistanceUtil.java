package com.seibel.lod.util;

import com.seibel.lod.config.LodConfig;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.enums.LodResolution;

public class DetailDistanceUtil
{
	private static double genMultiplier = 1.0;
	private static double treeGenMultiplier = 1.0;
	private static double treeCutMultiplier = 1.0;
	private static int minGenDetail = LodConfig.CLIENT.worldGenerator.maxGenerationDetail.get().detailLevel;
	private static int minDrawDetail = Math.max(LodConfig.CLIENT.graphics.maxDrawDetail.get().detailLevel,LodConfig.CLIENT.worldGenerator.maxGenerationDetail.get().detailLevel);
	private static int maxDetail = LodUtil.REGION_DETAIL_LEVEL + 1;
	private static int minDistance = 0;
	private static int maxDistance = LodConfig.CLIENT.graphics.lodChunkRenderDistance.get() * 16 * 2;
	private static int base = 2;
	private static double logBase = Math.log(2);

	private static int[] maxVerticalData = {
			8,
			4,
			4,
			2,
			2,
			1,
			1,
			1,
			1,
			1,
			1,};


	private static LodResolution[] lodGenDetails = {
			LodResolution.BLOCK,
			LodResolution.TWO_BLOCKS,
			LodResolution.FOUR_BLOCKS,
			LodResolution.HALF_CHUNK,
			LodResolution.CHUNK,
			LodResolution.CHUNK,
			LodResolution.CHUNK,
			LodResolution.CHUNK,
			LodResolution.CHUNK,
			LodResolution.CHUNK,
			LodResolution.CHUNK};



	public static void updateSettings(){
		minGenDetail = LodConfig.CLIENT.worldGenerator.maxGenerationDetail.get().detailLevel;
		minDrawDetail = Math.max(LodConfig.CLIENT.graphics.maxDrawDetail.get().detailLevel,LodConfig.CLIENT.worldGenerator.maxGenerationDetail.get().detailLevel);
		maxDistance = LodConfig.CLIENT.graphics.lodChunkRenderDistance.get() * 16 * 2;
	}

	public static int baseDistanceFunction(int detail)
	{
		int initial;
		int distance = 0;
		if (detail <= minGenDetail)
			return minDistance;
		if (detail == maxDetail)
			return maxDistance;
		if (detail == maxDetail + 1)
			return maxDistance;
		switch (LodConfig.CLIENT.worldGenerator.lodDistanceCalculatorType.get())
		{
			case LINEAR:
				initial = LodConfig.CLIENT.graphics.lodQuality.get() * 128;
				return (detail * initial);
			case QUADRATIC:
				initial = LodConfig.CLIENT.graphics.lodQuality.get() * 128;
				return (int) (Math.pow(base, detail) * initial);
		}
		return distance;
	}

	public static byte baseInverseFunction(int distance, int minDetail)
	{
		int initial;
		byte detail = 0;
		if (distance == 0)
			detail = (byte) minDetail;
		if (distance > maxDistance)
			detail = (byte) (maxDetail-1);
		switch (LodConfig.CLIENT.worldGenerator.lodDistanceCalculatorType.get())
		{
			case LINEAR:
				initial = LodConfig.CLIENT.graphics.lodQuality.get() * 128;
				detail = (byte) Math.floorDiv(distance, initial);
				break;
			case QUADRATIC:
				initial = LodConfig.CLIENT.graphics.lodQuality.get() * 128;
				detail = (byte) (Math.log(Math.floorDiv(distance, initial))/logBase);
				break;
		}
		return (byte) Math.min(detail, LodUtil.REGION_DETAIL_LEVEL);
	}

	public static byte getDrawDetailFromDistance(int distance)
	{
		return baseInverseFunction(distance, minDrawDetail);
	}

	public static byte getGenerationDetailFromDistance(int distance)
	{
		return baseInverseFunction((int) (distance * genMultiplier), minGenDetail);
	}

	public static byte getTreeCutDetailFromDistance(int distance)
	{
		return baseInverseFunction((int) (distance * treeCutMultiplier), minGenDetail);
	}


	public static byte getTreeGenDetailFromDistance(int distance)
	{
		return baseInverseFunction((int) (distance * treeGenMultiplier), minGenDetail);
	}

	public static DistanceGenerationMode getDistanceGenerationMode(int detail)
	{
		return LodConfig.CLIENT.worldGenerator.distanceGenerationMode.get();
	}

	public static byte getLodDrawDetail(int detail)
	{
		if (detail < minDrawDetail)
		{
			return lodGenDetails[minDrawDetail].detailLevel;
		} else
		{
			return lodGenDetails[detail].detailLevel;
		}
	}

	public static LodResolution getLodGenDetail(int detail)
	{
		if (detail < minGenDetail)
		{
			return lodGenDetails[minGenDetail];
		} else
		{
			return lodGenDetails[detail];
		}
	}


	public static byte getCutLodDetail(int detail)
	{
		if (detail < minGenDetail)
		{
			return lodGenDetails[minGenDetail].detailLevel;
		} else if (detail == maxDetail)
		{
			return LodUtil.REGION_DETAIL_LEVEL;
		} else
		{
			return lodGenDetails[detail].detailLevel;
		}
	}

	public static int getMaxVerticalData(int detail)
	{
		return maxVerticalData[LodUtil.clamp(minGenDetail, detail, LodUtil.REGION_DETAIL_LEVEL)];
	}

}
