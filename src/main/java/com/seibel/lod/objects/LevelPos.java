package com.seibel.lod.objects;

import com.seibel.lod.util.LodUtil;

import java.util.Comparator;
import java.util.Map;

public class LevelPos implements Cloneable
{
    public final byte detailLevel;
    public final int posX;
    public final int posZ;

    public LevelPos(byte detailLevel, int posX, int posZ)
    {
        this.posX = posX;
        this.posZ = posZ;
        this.detailLevel = detailLevel;
    }

    public LevelPos convert(byte newDetailLevel)
    {
        if (newDetailLevel >= detailLevel)
        {
            return new LevelPos(
                    newDetailLevel,
                    Math.floorDiv(posX, (int) Math.pow(2, newDetailLevel - detailLevel)),
                    Math.floorDiv(posZ, (int) Math.pow(2, newDetailLevel - detailLevel)));
        } else
        {
            return new LevelPos(
                    newDetailLevel,
                    posX * (int) Math.pow(2, detailLevel - newDetailLevel),
                    posZ * (int) Math.pow(2, detailLevel - newDetailLevel));
        }
    }

    public LevelPos regionModule()
    {
        return new LevelPos(
                detailLevel,
                Math.floorMod(posX, (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel)),
                Math.floorMod(posZ, (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel)));
    }

    public RegionPos getRegionPos()
    {
        return new RegionPos(
                Math.floorDiv(posX, (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel)),
                Math.floorDiv(posZ, (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel)));
    }


    @Override
    public LevelPos clone()
    {
        return new LevelPos(detailLevel, posX, posZ);
    }


    public int maxDistance(int playerPosX, int playerPosZ, int regionPosX, int regionPosZ)
    {
        int width = (int) Math.pow(2, detailLevel);

        int startPosX = regionPosX * 512 + posX * width;
        int startPosZ = regionPosZ * 512 + posZ * width;
        int endPosX = startPosX + width;
        int endPosZ = startPosZ + width;

        int maxDistance = (int) Math.sqrt(Math.pow(playerPosX - startPosX, 2) + Math.pow(playerPosZ - startPosZ, 2));
        maxDistance = Math.max(maxDistance, (int) Math.sqrt(Math.pow(playerPosX - startPosX, 2) + Math.pow(playerPosZ - endPosZ, 2)));
        maxDistance = Math.max(maxDistance, (int) Math.sqrt(Math.pow(playerPosX - endPosX, 2) + Math.pow(playerPosZ - startPosZ, 2)));
        maxDistance = Math.max(maxDistance, (int) Math.sqrt(Math.pow(playerPosX - endPosX, 2) + Math.pow(playerPosZ - endPosZ, 2)));

        return maxDistance;
    }

    public int maxDistance(int playerPosX, int playerPosZ)
    {
        int width = (int) Math.pow(2, detailLevel);

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

    public int minDistance(int playerPosX, int playerPosZ, int regionPosX, int regionPosZ)
    {
        int width = (int) Math.pow(2, detailLevel);

        int startPosX = regionPosX * 512 + posX * width;
        int startPosZ = regionPosZ * 512 + posZ * width;
        int endPosX = startPosX + width;
        int endPosZ = startPosZ + width;

        boolean inXArea = playerPosX >= startPosX && playerPosX <= endPosX;
        boolean inZArea = playerPosZ >= startPosZ && playerPosZ <= endPosZ;
        if (inXArea && inZArea)
        {
            return 0;
        } else if (inXArea)
        {
            return Math.min(
                    Math.abs(playerPosZ - startPosZ),
                    Math.abs(playerPosZ - endPosZ)
            );
        } else if (inZArea)
        {
            return Math.min(
                    Math.abs(playerPosX - startPosX),
                    Math.abs(playerPosX - endPosX)
            );
        } else
        {
            int minDistance = (int) Math.sqrt(Math.pow(playerPosX - startPosX, 2) + Math.pow(playerPosZ - startPosZ, 2));
            minDistance = Math.min(minDistance, (int) Math.sqrt(Math.pow(playerPosX - startPosX, 2) + Math.pow(playerPosZ - endPosZ, 2)));
            minDistance = Math.min(minDistance, (int) Math.sqrt(Math.pow(playerPosX - endPosX, 2) + Math.pow(playerPosZ - startPosZ, 2)));
            minDistance = Math.min(minDistance, (int) Math.sqrt(Math.pow(playerPosX - endPosX, 2) + Math.pow(playerPosZ - endPosZ, 2)));
            return minDistance;
        }
    }

    public int minDistance(int playerPosX, int playerPosZ)
    {
        int width = (int) Math.pow(2, detailLevel);

        int startPosX = posX * width;
        int startPosZ = posZ * width;
        int endPosX = startPosX + width;
        int endPosZ = startPosZ + width;

        boolean inXArea = playerPosX >= startPosX && playerPosX <= endPosX;
        boolean inZArea = playerPosZ >= startPosZ && playerPosZ <= endPosZ;

        if (inXArea && inZArea)
        {
            return 0;
        } else if (inXArea)
        {
            return Math.min(
                    Math.abs(playerPosZ - startPosZ),
                    Math.abs(playerPosZ - endPosZ)
            );
        } else if (inZArea)
        {
            return Math.min(
                    Math.abs(playerPosX - startPosX),
                    Math.abs(playerPosX - endPosX)
            );
        } else
        {
            int minDistance = (int) Math.sqrt(Math.pow(playerPosX - startPosX, 2) + Math.pow(playerPosZ - startPosZ, 2));
            minDistance = Math.min(minDistance, (int) Math.sqrt(Math.pow(playerPosX - startPosX, 2) + Math.pow(playerPosZ - endPosZ, 2)));
            minDistance = Math.min(minDistance, (int) Math.sqrt(Math.pow(playerPosX - endPosX, 2) + Math.pow(playerPosZ - startPosZ, 2)));
            minDistance = Math.min(minDistance, (int) Math.sqrt(Math.pow(playerPosX - endPosX, 2) + Math.pow(playerPosZ - endPosZ, 2)));
            return minDistance;
        }
    }

    public static LevelPosComparator getPosComparator()
    {
        return new LevelPosComparator();
    }

    public static LevelPosDetailComparator getPosAndDetailComparator()
    {
        return new LevelPosDetailComparator();
    }

    public static class LevelPosComparator implements Comparator<Map.Entry<LevelPos, Integer>>
    {

        @Override
        public int compare(Map.Entry<LevelPos, Integer> first, Map.Entry<LevelPos, Integer> second)
        {
            return Integer.compare(first.getValue(), second.getValue());
        }
    }

    public static class LevelPosDetailComparator implements Comparator<Map.Entry<LevelPos, Integer>>
    {

        @Override
        public int compare(Map.Entry<LevelPos, Integer> first, Map.Entry<LevelPos, Integer> second)
        {
            Integer compareResult = Integer.compare(first.getKey().detailLevel, second.getKey().detailLevel);
            if (compareResult != 0)
            {
                compareResult = Integer.compare(first.getValue(), second.getValue());
            }
            return compareResult;
        }
    }

    public int hashCode()
    {
        int hash = 7;
        hash = 31 * hash + (int) detailLevel;
        hash = 31 * hash + posX;
        hash = 31 * hash + posZ;
        return hash;
    }

    public boolean equals(LevelPos other)
    {
        return (this.detailLevel == other.detailLevel &&
                this.posX == other.posX &&
                this.posZ == other.posZ);
    }

    @Override
    public String toString()
    {
        String s = (detailLevel + " " + posX + " " + posZ);
        return s;
    }
}
