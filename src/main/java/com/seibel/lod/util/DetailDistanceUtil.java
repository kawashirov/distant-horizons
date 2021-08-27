package com.seibel.lod.util;

import com.ibm.icu.util.IslamicCalendar;
import com.seibel.lod.enums.DistanceCalculatorType;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.enums.LodCorner;
import com.seibel.lod.enums.LodDetail;
import com.seibel.lod.handlers.LodConfig;
import com.seibel.lod.objects.RegionPos;
import net.minecraft.client.Minecraft;

public class DetailDistanceUtil
{
	private static double genMultiplier = 1.0;
	private static double treeGenMultiplier = 1.0;
	private static double treeCutMultiplier = 1.0;
	//private static int minDetail = LodConfig.CLIENT.maxGenerationDetail.get().detailLevel;
	private static int minDetail = LodDetail.FULL.detailLevel;
	private static int maxDetail = LodUtil.REGION_DETAIL_LEVEL + 1;
	private static int minDistance = 0;
	//private static int maxDistance = LodConfig.CLIENT.lodChunkRenderDistance.get() * 16 * 2;
	private static int maxDistance = 128 * 16 * 2;
	private static int base = 2;
	private static double logBase = Math.log(2);

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
				return (int) (Math.pow(base, detail) * initial);
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

	public static byte getDistanceRenderingInverse(int distance)
	{
		int initial;
		byte detail = 0;
		if (distance == 0)
			detail = (byte) minDetail;
		if (distance > maxDistance)
			detail = (byte) (maxDetail-1);
		switch (LodConfig.CLIENT.lodDistanceCalculatorType.get())
		{
			case LINEAR:
				initial = LodConfig.CLIENT.lodQuality.get() * 128;
				detail = (byte) Math.floorDiv(distance, initial);
				break;
			case QUADRATIC:
				initial = LodConfig.CLIENT.lodQuality.get() * 128;
				detail = (byte) (Math.log(Math.floorDiv(distance, initial))/logBase);
				break;
			case RENDER_DEPENDANT:
				detail = (byte) 9;
				break;
		}
		return (byte) Math.min(detail, LodUtil.REGION_DETAIL_LEVEL);
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


	public static boolean regionInView(int playerPosX, int playerPosY, int playerPosZ, int alpha, int beta, int fov, RegionPos regionPos)
	{

		//System.out.println(Math.floorMod((int) mc.player.xRot,360) + " " + Math.floorMod((int) mc.player.yRot,360) + " " + mc.options.fov);
		//System.out.println(mc.player.xRotO + " " + mc.player.yRotO);
		//mc.options.fov;
		return false;
	}

}
