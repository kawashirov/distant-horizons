package com.seibel.lod.util;

import com.seibel.lod.config.LodConfig;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.enums.HorizontalResolution;

public class DetailDistanceUtil
{
	private static double genMultiplier = 1.0;
	private static double treeGenMultiplier = 1.0;
	private static double treeCutMultiplier = 1.0;
	private static int minGenDetail = LodConfig.CLIENT.worldGenerator.generationResolution.get().detailLevel;
	private static int minDrawDetail = Math.max(LodConfig.CLIENT.graphics.drawResolution.get().detailLevel,LodConfig.CLIENT.worldGenerator.generationResolution.get().detailLevel);
	private static int maxDetail = LodUtil.REGION_DETAIL_LEVEL + 1;
	private static int minDistance = 0;
	private static int maxDistance = LodConfig.CLIENT.graphics.lodChunkRenderDistance.get() * 16 * 2;

	
	private static HorizontalResolution[] lodGenDetails = {
			HorizontalResolution.BLOCK,
			HorizontalResolution.TWO_BLOCKS,
			HorizontalResolution.FOUR_BLOCKS,
			HorizontalResolution.HALF_CHUNK,
			HorizontalResolution.CHUNK,
			HorizontalResolution.CHUNK,
			HorizontalResolution.CHUNK,
			HorizontalResolution.CHUNK,
			HorizontalResolution.CHUNK,
			HorizontalResolution.CHUNK,
			HorizontalResolution.CHUNK};



	public static void updateSettings(){
		minGenDetail = LodConfig.CLIENT.worldGenerator.generationResolution.get().detailLevel;
		minDrawDetail = Math.max(LodConfig.CLIENT.graphics.drawResolution.get().detailLevel,LodConfig.CLIENT.worldGenerator.generationResolution.get().detailLevel);
		maxDistance = LodConfig.CLIENT.graphics.lodChunkRenderDistance.get() * 16 * 8;
	}

	public static int baseDistanceFunction(int detail)
	{
		if (detail <= minGenDetail)
			return minDistance;
		if (detail >= maxDetail)
			return maxDistance;

		int distanceUnit = LodConfig.CLIENT.worldGenerator.horizontalScale.get().distanceUnit;
		switch (LodConfig.CLIENT.worldGenerator.horizontalQuality.get()){

			case LINEAR:;
				return (detail * distanceUnit);
			default:
				double base = LodConfig.CLIENT.worldGenerator.horizontalQuality.get().quadraticBase;
				return (int) (Math.pow(base, detail) * distanceUnit);
		}
	}

	public static int getDrawDistanceFromDetail(int detail)
	{
		return baseDistanceFunction(detail);
	}

	public static byte baseInverseFunction(int distance, int minDetail)
	{

		int detail = 0;
		if (distance == 0)
			return (byte) minDetail;
		int distanceUnit = LodConfig.CLIENT.worldGenerator.horizontalScale.get().distanceUnit;
		switch (LodConfig.CLIENT.worldGenerator.horizontalQuality.get()){
			case LINEAR:
				detail = (byte) Math.floorDiv(distance, distanceUnit);
				break;
			default:
				double base = LodConfig.CLIENT.worldGenerator.horizontalQuality.get().quadraticBase;
				double logBase = Math.log(base);
				detail = (byte) (Math.log(Math.floorDiv(distance, distanceUnit))/logBase);
				break;
		}
		return (byte) LodUtil.clamp(minDetail, detail, maxDetail-1);
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
			if(LodConfig.CLIENT.graphics.alwaysDrawAtMaxQuality.get())
				return getLodGenDetail(minDrawDetail).detailLevel;
			else
				return (byte) minDrawDetail;
		} else
		{
			if(LodConfig.CLIENT.graphics.alwaysDrawAtMaxQuality.get())
				return getLodGenDetail(detail).detailLevel;
			else
				return (byte) detail;
		}
	}

	public static HorizontalResolution getLodGenDetail(int detail)
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
		return LodConfig.CLIENT.worldGenerator.verticalQuality.get().maxVerticalData[LodUtil.clamp(minGenDetail, detail, LodUtil.REGION_DETAIL_LEVEL)];
	}

}
