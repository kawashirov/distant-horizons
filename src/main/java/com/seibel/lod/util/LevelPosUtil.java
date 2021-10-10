package com.seibel.lod.util;

public class LevelPosUtil
{
	public static int[] convert(int[] levelPos, byte newDetailLevel)
	{
		return convert(getDetailLevel(levelPos), getPosX(levelPos), getPosZ(levelPos), newDetailLevel);
	}
	
	public static int[] convert(byte detailLevel, int posX, int posZ, byte newDetailLevel)
	{
		int width;
		if (newDetailLevel >= detailLevel)
		{
			width = 1 << (newDetailLevel - detailLevel);
			return createLevelPos(
					newDetailLevel,
					Math.floorDiv(posX, width),
					Math.floorDiv(posZ, width));
		}
		else
		{
			width = 1 << (detailLevel - newDetailLevel);
			return createLevelPos(
					newDetailLevel,
					posX * width,
					posZ * width);
		}
	}
	
	public static int[] createLevelPos(byte detailLevel, int posX, int posZ)
	{
		return new int[] { detailLevel, posX, posZ };
	}
	
	public static int convert(byte detailLevel, int pos, byte newDetailLevel)
	{
		int width;
		if (newDetailLevel >= detailLevel)
		{
			width = 1 << (newDetailLevel - detailLevel);
			return Math.floorDiv(pos, width);
		}
		else
		{
			width = 1 << (detailLevel - newDetailLevel);
			return pos * width;
		}
	}
	
	public static int getRegion(byte detailLevel, int pos)
	{
		return Math.floorDiv(pos, 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel));
	}
	
	public static int getRegionModule(byte detailLevel, int pos)
	{
		return Math.floorMod(pos, 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel));
	}
	
	public static byte getDetailLevel(int[] levelPos)
	{
		return (byte) levelPos[0];
	}
	
	public static int getPosX(int[] levelPos)
	{
		return levelPos[1];
	}
	
	public static int getPosZ(int[] levelPos)
	{
		return levelPos[2];
	}
	
	public static int getDistance(int[] levelPos)
	{
		return levelPos[3];
	}
	
	public static int[] getRegionModule(int[] levelPos)
	{
		return getRegionModule(getDetailLevel(levelPos), getPosX(levelPos), getPosZ(levelPos));
	}
	
	public static int[] getRegionModule(byte detailLevel, int posX, int posZ)
	{
		int width = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		return createLevelPos(
				detailLevel,
				Math.floorMod(posX, width),
				Math.floorMod(posZ, width));
	}
	
	public static int[] applyOffset(int[] levelPos, int xOffset, int zOffset)
	{
		return createLevelPos(
				getDetailLevel(levelPos),
				getPosX(levelPos) + xOffset,
				getPosZ(levelPos) + zOffset);
	}
	
	public static int[] applyLevelOffset(int[] levelPos, byte detailOffset, int xOffset, int zOffset)
	{
		return createLevelPos(
				getDetailLevel(levelPos),
				getPosX(levelPos) + xOffset * (1 << detailOffset),
				getPosZ(levelPos) + zOffset * (1 << detailOffset));
	}
	
	public static int getRegionPosX(int[] levelPos)
	{
		int width = 1 << (LodUtil.REGION_DETAIL_LEVEL - getDetailLevel(levelPos));
		return Math.floorDiv(getPosX(levelPos), width);
	}
	
	public static int getRegionPosZ(int[] levelPos)
	{
		int width = 1 << (LodUtil.REGION_DETAIL_LEVEL - getDetailLevel(levelPos));
		return Math.floorDiv(getPosZ(levelPos), width);
	}
	
	public static int getChunkPos(byte detailLevel, int pos)
	{
		return convert(detailLevel, pos, LodUtil.CHUNK_DETAIL_LEVEL);
	}
	
	
	public static int maxDistance(byte detailLevel, int posX, int posZ, int playerPosX, int playerPosZ)
	{
		int width = 1 << detailLevel;
		
		int startPosX = posX * width;
		int startPosZ = posZ * width;
		int endPosX = startPosX + width;
		int endPosZ = startPosZ + width;
		
		int maxDistance = (int) Math.sqrt(Math.pow(playerPosX - startPosX, 2) + Math.pow(playerPosZ - startPosZ, 2));
		maxDistance = Math.max(maxDistance, (int) Math.sqrt(Math.pow(playerPosX - startPosX, 2) + Math.pow(playerPosZ - endPosZ, 2)));
		maxDistance = Math.max(maxDistance, (int) Math.sqrt(Math.pow(playerPosX - endPosX, 2) + Math.pow(playerPosZ - startPosZ, 2)));
		maxDistance = Math.max(maxDistance, (int) Math.sqrt(Math.pow(playerPosX - endPosX, 2) + Math.pow(playerPosZ - endPosZ, 2)));
		
		return maxDistance;
	}
	
	public static int maxDistance(byte detailLevel, int posX, int posZ, int playerPosX, int playerPosZ, int xRegion, int zRegion)
	{
		int width = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		int newPosX = xRegion * width + posX;
		int newPosZ = zRegion * width + posZ;
		return maxDistance(detailLevel, newPosX, newPosZ, playerPosX, playerPosZ);
	}
	
	
	public static int minDistance(byte detailLevel, int posX, int posZ, int playerPosX, int playerPosZ)
	{
		int width = 1 << detailLevel;
		
		int startPosX = posX * width;
		int startPosZ = posZ * width;
		int endPosX = startPosX + width;
		int endPosZ = startPosZ + width;
		
		boolean inXArea = playerPosX >= startPosX && playerPosX <= endPosX;
		boolean inZArea = playerPosZ >= startPosZ && playerPosZ <= endPosZ;
		if (inXArea && inZArea)
		{
			return 0;
		}
		else if (inXArea)
		{
			return Math.min(
					Math.abs(playerPosZ - startPosZ),
					Math.abs(playerPosZ - endPosZ)
			);
		}
		else if (inZArea)
		{
			return Math.min(
					Math.abs(playerPosX - startPosX),
					Math.abs(playerPosX - endPosX)
			);
		}
		else
		{
			int minDistance = (int) Math.sqrt(Math.pow(playerPosX - startPosX, 2) + Math.pow(playerPosZ - startPosZ, 2));
			minDistance = Math.min(minDistance, (int) Math.sqrt(Math.pow(playerPosX - startPosX, 2) + Math.pow(playerPosZ - endPosZ, 2)));
			minDistance = Math.min(minDistance, (int) Math.sqrt(Math.pow(playerPosX - endPosX, 2) + Math.pow(playerPosZ - startPosZ, 2)));
			minDistance = Math.min(minDistance, (int) Math.sqrt(Math.pow(playerPosX - endPosX, 2) + Math.pow(playerPosZ - endPosZ, 2)));
			return minDistance;
		}
	}
	
	public static int minDistance(byte detailLevel, int posX, int posZ, int playerPosX, int playerPosZ, int xRegion, int zRegion)
	{
		int width = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		int newPosX = xRegion * width + posX;
		int newPosZ = zRegion * width + posZ;
		return minDistance(detailLevel, newPosX, newPosZ, playerPosX, playerPosZ);
	}
	
	public static int compareDistance(int firstDistance, int secondDistance)
	{
		return Integer.compare(
				firstDistance,
				secondDistance);
	}
	
	
	public static int compareLevelAndDistance(byte firstDetail, int firstDistance, byte secondDetail, int secondDistance)
	{
		int compareResult = Integer.compare(
				secondDetail,
				firstDetail);
		if (compareResult == 0)
		{
			compareResult = Integer.compare(
					firstDistance,
					secondDistance);
		}
		return compareResult;
	}
	
	public static String toString(int[] levelPos)
	{
		return (getDetailLevel(levelPos) + " " + getPosX(levelPos) + " " + getPosZ(levelPos));
	}
	
	public static String toString(byte detailLevel, int posX, int posZ)
	{
		return (detailLevel + " " + posX + " " + posZ);
	}
}
