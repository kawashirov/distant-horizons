package com.seibel.lod.util;

import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.enums.LodCorner;
import com.seibel.lod.enums.LodDetail;
import com.seibel.lod.handlers.LodConfig;
import net.minecraft.client.Minecraft;

public class DetailDistanceUtil
{
	private static double genMultiplier = 1.25;
	private static double treeGenMultiplier = 1.5;
	private static double treeCutMultiplier = 1.25;
	private static int minDetail = LodConfig.CLIENT.maxGenerationDetail.get().detailLevel;
	private static int maxDetail = LodUtil.REGION_DETAIL_LEVEL + 1;
	private static int minDistance = 0;
	private static int maxDistance = LodConfig.CLIENT.lodChunkRenderDistance.get() * 16 * 2;



	private static LodDetail[] lodDetails = {
			LodDetail.FULL,
			LodDetail.HALF,
			LodDetail.QUAD,
			LodDetail.DOUBLE,
			LodDetail.SINGLE,
			LodDetail.SINGLE,
			LodDetail.SINGLE,
			LodDetail.SINGLE,
			LodDetail.SINGLE,
			LodDetail.SINGLE,
			LodDetail.SINGLE};

	public static int getDistanceRendering(int detail)
	{
		int initial;
		int distance = 0;
		if (detail <= minDetail)
			return minDistance;
		if (detail == maxDetail)
			return maxDistance;
		if (detail == maxDetail + 1)
			return maxDistance;
		switch (LodConfig.CLIENT.lodDistanceCalculatorType.get())
		{
			case LINEAR:
				initial = LodConfig.CLIENT.lodQuality.get() * 128;
				return (detail * initial);
			case QUADRATIC:
				initial = LodConfig.CLIENT.lodQuality.get() * 128;
				return (int) (Math.pow(2, detail) * initial);
			case RENDER_DEPENDANT:
				int realRenderDistance = Minecraft.getInstance().options.renderDistance * 16;
				int border = 64;
				byte detailAtBorder = (byte) 4;
				if (detail > detailAtBorder)
				{
					return (detail * (border - realRenderDistance) / detailAtBorder + realRenderDistance);
				} else
				{
					return ((maxDetail - detail) * (maxDistance - border) / (maxDetail - detailAtBorder) + border);
				}
		}
		return distance;
	}

	public static int getDistanceGeneration(int detail)
	{
		if (detail == maxDetail)
			return maxDistance;
		return (int) (getDistanceRendering(detail) * genMultiplier);
	}

	public static int getDistanceTreeCut(int detail)
	{
		if (detail == maxDetail)
			return maxDistance;
		return (int) (getDistanceRendering(detail) * treeCutMultiplier);
	}

	public static int getDistanceTreeGen(int detail)
	{
		if (detail == maxDetail)
			return maxDistance;
		return (int) (getDistanceRendering(detail) * treeGenMultiplier);
	}

	public static DistanceGenerationMode getDistanceGenerationMode(int detail)
	{
		return LodConfig.CLIENT.distanceGenerationMode.get();
	}

	public static LodDetail getLodDetail(int detail)
	{
		if (detail < minDetail)
		{
			return lodDetails[minDetail];
		} else
		{
			return lodDetails[detail];
		}
	}


	public static byte getCutLodDetail(int detail)
	{
		if (detail < minDetail)
		{
			return lodDetails[minDetail].detailLevel;
		} else if (detail == maxDetail)
		{
			return LodUtil.REGION_DETAIL_LEVEL;
		} else
		{
			return lodDetails[detail].detailLevel;
		}
	}
}
