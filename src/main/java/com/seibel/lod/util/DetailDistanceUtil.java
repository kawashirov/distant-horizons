package com.seibel.lod.util;

import com.seibel.lod.config.LodConfig;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.enums.LodDetail;
import com.seibel.lod.objects.RegionPos;
import com.seibel.lod.wrapper.MinecraftWrapper;

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


	public static void updateSettings(){
		minGenDetail = LodConfig.CLIENT.worldGenerator.maxGenerationDetail.get().detailLevel;
		minDrawDetail = Math.max(LodConfig.CLIENT.graphics.maxDrawDetail.get().detailLevel,LodConfig.CLIENT.worldGenerator.maxGenerationDetail.get().detailLevel);
		maxDistance = LodConfig.CLIENT.graphics.lodChunkRenderDistance.get() * 16 * 2;
	}

	public static int getDistanceRendering(int detail)
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
			case RENDER_DEPENDANT:
				int realRenderDistance = MinecraftWrapper.INSTANCE.getRenderDistance() * 16;
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

	public static byte baseInverse(int distance, int minDetail)
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
			case RENDER_DEPENDANT:
				detail = (byte) 9;
				break;
		}
		return (byte) Math.min(detail, LodUtil.REGION_DETAIL_LEVEL);
	}

	public static byte getDistanceRenderingInverse(int distance)
	{
		return baseInverse(distance, minDrawDetail);
	}

	public static byte getDistanceGenerationInverse(int distance)
	{
		return baseInverse((int) (distance * genMultiplier), minGenDetail);
	}

	public static byte getDistanceTreeCutInverse(int distance)
	{
		return baseInverse((int) (distance * treeCutMultiplier), minGenDetail);
	}


	public static byte getDistanceTreeGenInverse(int distance)
	{
		return baseInverse((int) (distance * treeGenMultiplier), minGenDetail);
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
		return LodConfig.CLIENT.worldGenerator.distanceGenerationMode.get();
	}

	public static byte getLodDrawDetail(int detail)
	{
		return (byte) Math.max(detail, minDrawDetail);
	}

	public static LodDetail getLodGenDetail(int detail)
	{
		if (detail < minGenDetail)
		{
			return lodDetails[minGenDetail];
		} else
		{
			return lodDetails[detail];
		}
	}


	public static byte getCutLodDetail(int detail)
	{
		if (detail < minGenDetail)
		{
			return lodDetails[minGenDetail].detailLevel;
		} else if (detail == maxDetail)
		{
			return LodUtil.REGION_DETAIL_LEVEL;
		} else
		{
			return lodDetails[detail].detailLevel;
		}
	}


	public static boolean regionInView(int playerPosX, int playerPosY, int playerPosZ, int xRot, int yRot, int fov, RegionPos regionPos)
	{

		//System.out.println(Math.floorMod((int) mc.player.xRot,360) + " " + Math.floorMod((int) mc.player.yRot,360) + " " + mc.options.fov);
		//System.out.println(mc.player.xRotO + " " + mc.player.yRotO);
		//mc.options.fov;
		return false;
	}

}
