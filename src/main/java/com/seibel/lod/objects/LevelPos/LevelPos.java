package com.seibel.lod.objects.LevelPos;

import com.seibel.lod.objects.RegionPos;
import com.seibel.lod.util.LodUtil;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

import java.util.Comparator;
import java.util.Map;

public class LevelPos implements Cloneable, ImmutableLevelPos, MutableLevelPos
{
    public byte detailLevel;
    public int posX;
    public int posZ;


    public LevelPos()
    {
    }

    public LevelPos(byte detailLevel, int posX, int posZ)
    {
        this.posX = posX;
        this.posZ = posZ;
        this.detailLevel = detailLevel;
    }

    /**
     * this operation does not change the state
     */
    public LevelPos getConvertedLevelPos(byte newDetailLevel)
    {
        if (newDetailLevel >= detailLevel)
        {
            int width = 1 << (newDetailLevel - detailLevel);
            return new LevelPos(
                    newDetailLevel,
                    Math.floorDiv(posX, width),
                    Math.floorDiv(posZ, width));
        } else
        {
            int width = 1 << (detailLevel - newDetailLevel);
            return new LevelPos(
                    newDetailLevel,
                    posX * width,
                    posZ * width);
        }
    }

    /**
     * this operation does not change the state
     */
    public LevelPos getRegionModuleLevelPos()
    {
        int width = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
        return new LevelPos(
                detailLevel,
                Math.floorMod(posX, width),
                Math.floorMod(posZ, width));
    }

    /**
     * this operation changes the state
     */
    public void convert(byte newDetailLevel)
    {
        if (newDetailLevel >= detailLevel)
        {
            int width = 1 << (newDetailLevel - detailLevel);
            detailLevel = newDetailLevel;
            posX = Math.floorDiv(posX, width);
            posZ = Math.floorDiv(posZ, width);
        } else
        {
            int width = 1 << (detailLevel - newDetailLevel);
            detailLevel = newDetailLevel;
            posX = posX * width;
            posZ = posZ * width;
        }
    }

    /**
     * this operation changes the state
     */
    public void performRegionModule()
    {
        int width = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
        posX = Math.floorMod(posX, width);
        posZ = Math.floorMod(posZ, width);
    }

    /**
     * this operation changes the state
     */
    public void applyOffset(int xOffset, int zOffset)
    {
        posX = posX + xOffset;
        posX = posZ + zOffset;
    }

    /**
     * this operation changes the state
     */
    public void changeParameters(byte newDetailLevel, int newPosX, int newPosZ)
    {
        detailLevel = newDetailLevel;
        posX = newPosX;
        posZ = newPosZ;
    }

    public RegionPos getRegionPos()
    {
        int width = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
        return new RegionPos(
                Math.floorDiv(posX, width),
                Math.floorDiv(posZ, width));
    }

    public ChunkPos getChunkPos()
    {
        if (LodUtil.CHUNK_DETAIL_LEVEL >= detailLevel)
        {
            int width = 1 << (LodUtil.CHUNK_DETAIL_LEVEL - detailLevel);
            return new ChunkPos(
                    Math.floorDiv(posX, width),
                    Math.floorDiv(posZ, width));
        } else
        {
            int width = 1 << (detailLevel - LodUtil.CHUNK_DETAIL_LEVEL);
            return new ChunkPos(
                    posX * width,
                    posZ * width);
        }
    }

    /**
     * TODO fix the region disappearing for a second
     */

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

    public static LevelPosComparator getPosComparator(int playerPosX, int playerPosZ)
    {
        return new LevelPosComparator(playerPosX,playerPosZ);
    }

    public static LevelPosDetailComparator getPosAndDetailComparator(int playerPosX, int playerPosZ)
    {
        return new LevelPosDetailComparator(playerPosX,playerPosZ);
    }

    public static class LevelPosComparator implements Comparator<LevelPos>
    {
        int playerPosX;
        int playerPosZ;
        public LevelPosComparator(int playerPosX, int playerPosZ){
            this.playerPosX = playerPosX;
            this.playerPosZ = playerPosZ;
        }

        @Override
        public int compare(LevelPos first, LevelPos second)
        {
            return Integer.compare(first.getRegionModuleLevelPos().minDistance(playerPosX,playerPosZ), second.getRegionModuleLevelPos().minDistance(playerPosX,playerPosZ));
        }
    }

    public static class LevelPosDetailComparator implements Comparator<LevelPos>
    {
        int playerPosX;
        int playerPosZ;
        public LevelPosDetailComparator(int playerPosX, int playerPosZ){
            this.playerPosX = playerPosX;
            this.playerPosZ = playerPosZ;
        }

        @Override
        public int compare(LevelPos first, LevelPos second)
        {
            int compareResult = Integer.compare(first.detailLevel, second.detailLevel);
            if (compareResult != 0)
            {
                compareResult = Integer.compare(
                        first.getRegionModuleLevelPos().minDistance(playerPosX,playerPosZ),
                        second.getRegionModuleLevelPos().minDistance(playerPosX,playerPosZ));
            }
            return compareResult;
        }
    }


    @Override
    public LevelPos clone()
    {
        return new LevelPos(detailLevel, posX, posZ);
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 31 * hash + (int) detailLevel;
        hash = 31 * hash + posX;
        hash = 31 * hash + posZ;
        return hash;
    }

    @Override
    public boolean equals(Object other)
    {
        return (this.detailLevel == ((LevelPos) other).detailLevel &&
                this.posX == ((LevelPos) other).posX &&
                this.posZ == ((LevelPos) other).posZ);
    }

    @Override
    public String toString()
    {
        String s = (detailLevel + " " + posX + " " + posZ);
        return s;
    }
}
