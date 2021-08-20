package com.seibel.lod.objects;

import com.seibel.lod.util.LodUtil;

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


    public LevelPos clone()
    {
        return new LevelPos(detailLevel, posX, posZ);
    }

    public int maxDistance(int playerPosX, int playerPosZ, int regionPosX, int regionPosZ)
    {
        int size = (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel);
        int width = (int) Math.pow(2, detailLevel);

        //here i calculate the the LevelPos is in range
        //This is important to avoid any kind of hole in the generation
        int blockPosX = regionPosX * 512 + posX * width + width / 2;
        int blockPosZ = regionPosZ * 512 + posZ * width + width / 2;
        int maxDistance = (int) Math.sqrt(Math.pow(playerPosX - blockPosX, 2) + Math.pow(playerPosZ - blockPosZ, 2));
        for (int x = 0; x <= 1; x++)
        {
            for (int z = 0; z <= 1; z++)
            {
                blockPosX = regionPosX * 512 + posX * width + width * x;
                blockPosZ = regionPosZ * 512 + posZ * width + width * z;
                maxDistance = Math.max(maxDistance, (int) Math.sqrt(Math.pow(playerPosX - blockPosX, 2) + Math.pow(playerPosZ - blockPosZ, 2)));
            }
        }
        return maxDistance;
    }

    public int minDistance(int playerPosX, int playerPosZ, int regionPosX, int regionPosZ)
    {
        int size = (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel);
        int width = (int) Math.pow(2, detailLevel);

        //here i calculate the the LevelPos is in range
        //This is important to avoid any kind of hole in the generation
        int blockPosX = regionPosX * 512 + posX * width + width / 2;
        int blockPosZ = regionPosZ * 512 + posZ * width + width / 2;
        int minDistance = (int) Math.sqrt(Math.pow(playerPosX - blockPosX, 2) + Math.pow(playerPosZ - blockPosZ, 2));
        for (int x = 0; x <= 1; x++)
        {
            for (int z = 0; z <= 1; z++)
            {
                blockPosX = regionPosX * 512 + posX * width + width * x;
                blockPosZ = regionPosZ * 512 + posZ * width + width * z;
                minDistance = Math.min(minDistance, (int) Math.sqrt(Math.pow(playerPosX - blockPosX, 2) + Math.pow(playerPosZ - blockPosZ, 2)));
            }
        }
        return minDistance;
    }

    public String toString()
    {
        String s = (detailLevel + " " + posX + " " + posZ);
        return s;
    }
}
