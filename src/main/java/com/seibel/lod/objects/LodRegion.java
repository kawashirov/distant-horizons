package com.seibel.lod.objects;

import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.objects.LevelPos.LevelPos;
import com.seibel.lod.util.LodUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.io.Serializable;
import java.util.*;
import java.util.List;

/**
 * STANDARD TO FOLLOW
 * every coordinate called posX or posZ is a relative coordinate and not and absolute coordinate
 * if an array contain coordinate the order is the following
 * 0 for x, 1 for z in 2D
 * 0 for x, 1 for y, 2 for z in 3D
 */

public class LodRegion implements Serializable
{
    //x coord,
    private byte minDetailLevel;
    private static final byte POSSIBLE_LOD = 10;
    private int numberOfPoints;

    //For each of the following field the first slot is for the level of detail
    //Important: byte have a [-128, 127] range. When converting from or to int a 128 should be added or removed
    //If there is a bug with color then it's probably caused by this.
    //in the future other fields like transparency and light level could be added
    private byte[][][][] colors;

    private short[][][] height;

    private short[][][] depth;

    //a new node will have 0 as generationType
    //a node with 1 is node
    private byte[][][] generationType;

    private boolean[][][] dataExistence;

    public final int regionPosX;
    public final int regionPosZ;

    public LodRegion(LevelContainer levelContainer, RegionPos regionPos)
    {
        this.regionPosX = regionPos.x;
        this.regionPosZ = regionPos.z;
        this.minDetailLevel = levelContainer.detailLevel;

        //Array of matrices of arrays
        colors = new byte[POSSIBLE_LOD][][][];

        //Arrays of matrices
        height = new short[POSSIBLE_LOD][][];
        depth = new short[POSSIBLE_LOD][][];
        generationType = new byte[POSSIBLE_LOD][][];
        dataExistence = new boolean[POSSIBLE_LOD][][];

        colors[minDetailLevel] = levelContainer.colors;
        height[minDetailLevel] = levelContainer.height;
        depth[minDetailLevel] = levelContainer.depth;
        generationType[minDetailLevel] = levelContainer.generationType;
        dataExistence[minDetailLevel] = levelContainer.dataExistence;

        //Initialize all the different matrices
        for (byte lod = (byte) (minDetailLevel + 1); lod <= LodUtil.REGION_DETAIL_LEVEL; lod++)
        {
            int size = (short) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - lod);
            colors[lod] = new byte[size][size][3];
            height[lod] = new short[size][size];
            depth[lod] = new short[size][size];
            generationType[lod] = new byte[size][size];
            dataExistence[lod] = new boolean[size][size];
        }
        int width;
        LevelPos levelPos = new LevelPos();
        for (byte tempLod = (byte) (minDetailLevel + 1); tempLod <= LodUtil.REGION_DETAIL_LEVEL; tempLod++)
        {
            width = 1 << (LodUtil.REGION_DETAIL_LEVEL - tempLod);
            for (int x = 0; x < width; x++)
            {
                for (int z = 0; z < width; z++)
                {
                    levelPos.changeParameters(tempLod, x, z);
                    update(levelPos);
                }
            }
        }
    }

    public LodRegion(byte minDetailLevel, RegionPos regionPos)
    {
        this.minDetailLevel = minDetailLevel;
        this.regionPosX = regionPos.x;
        this.regionPosZ = regionPos.z;

        //Array of matrices of arrays
        colors = new byte[POSSIBLE_LOD][][][];

        //Arrays of matrices
        height = new short[POSSIBLE_LOD][][];
        depth = new short[POSSIBLE_LOD][][];
        generationType = new byte[POSSIBLE_LOD][][];
        dataExistence = new boolean[POSSIBLE_LOD][][];


        //Initialize all the different matrices
        for (byte lod = minDetailLevel; lod <= LodUtil.REGION_DETAIL_LEVEL; lod++)
        {
            int size = (short) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - lod);
            colors[lod] = new byte[size][size][3];
            height[lod] = new short[size][size];
            depth[lod] = new short[size][size];
            generationType[lod] = new byte[size][size];
            dataExistence[lod] = new boolean[size][size];

        }
    }

    /**
     * This method can be used to insert data into the LodRegion
     *
     * @param levelPos
     * @param dataPoint
     * @param generationType
     * @return
     */
    public boolean addData(LevelPos levelPos, short[] dataPoint, byte generationType)
    {
        levelPos.performRegionModule();
        if ((this.generationType[levelPos.detailLevel][levelPos.posX][levelPos.posZ] == 0) || (generationType >= this.generationType[levelPos.detailLevel][levelPos.posX][levelPos.posZ]))
        {

            //update the number of node present
            if (this.dataExistence[levelPos.detailLevel][levelPos.posX][levelPos.posZ]) numberOfPoints++;

            //add the node data
            this.height[levelPos.detailLevel][levelPos.posX][levelPos.posZ] = DataPoint.getHeight(dataPoint);
            this.depth[levelPos.detailLevel][levelPos.posX][levelPos.posZ] = DataPoint.getDepth(dataPoint);
            this.colors[levelPos.detailLevel][levelPos.posX][levelPos.posZ][0] = (byte) (DataPoint.getRed(dataPoint) - 128);
            this.colors[levelPos.detailLevel][levelPos.posX][levelPos.posZ][1] = (byte) (DataPoint.getGreen(dataPoint) - 128);
            this.colors[levelPos.detailLevel][levelPos.posX][levelPos.posZ][2] = (byte) (DataPoint.getBlue(dataPoint) - 128);
            this.generationType[levelPos.detailLevel][levelPos.posX][levelPos.posZ] = generationType;
            this.dataExistence[levelPos.detailLevel][levelPos.posX][levelPos.posZ] = true;
            return true;
        } else
        {
            return false;
        }
    }


    public short[] getData(ChunkPos chunkPos)
    {
        return getData(new LevelPos(LodUtil.CHUNK_DETAIL_LEVEL, chunkPos.x, chunkPos.z));
    }

    /**
     * This method will return the data in the position relative to the level of detail
     *
     * @param lod
     * @return the data at the relative pos and level
     */
    public short[] getData(byte lod, BlockPos blockPos)
    {
        int posX = Math.floorMod(blockPos.getX(), (int) Math.pow(2, lod));
        int posZ = Math.floorMod(blockPos.getZ(), (int) Math.pow(2, lod));
        return getData(new LevelPos(lod, posX, posZ));
    }

    /**
     * This method will return the data in the position relative to the level of detail
     *
     * @param levelPos
     * @return the data at the relative pos and level
     */
    public short[] getData(LevelPos levelPos)
    {
        levelPos = levelPos.getRegionModuleLevelPos();
        return new short[]{height[levelPos.detailLevel][levelPos.posX][levelPos.posZ],
                depth[levelPos.detailLevel][levelPos.posX][levelPos.posZ],
                (short) (colors[levelPos.detailLevel][levelPos.posX][levelPos.posZ][0] + 128),
                (short) (colors[levelPos.detailLevel][levelPos.posX][levelPos.posZ][1] + 128),
                (short) (colors[levelPos.detailLevel][levelPos.posX][levelPos.posZ][2] + 128)
        };
    }

    /**
     * This method will return all the levelPos that are renderable according to the requisite given in input
     *
     * @return
     */
    public List<Map.Entry<LevelPos, Integer>> getDataToGenerate(int playerPosX, int playerPosZ, int start, int end, byte generation, byte detailLevel, int dataNumber)
    {
        LevelPos levelPos = new LevelPos(LodUtil.REGION_DETAIL_LEVEL, 0, 0);
        List<Map.Entry<LevelPos, Integer>> levelPosList = getDataToGenerate(levelPos, playerPosX, playerPosZ, start, end, generation, detailLevel);
        List<Map.Entry<LevelPos, Integer>> levelMinPosList = new ArrayList<>();
        dataNumber = Math.min(dataNumber, levelPosList.size());

        for (int i = 0; i < dataNumber; i++)
        {
            Map.Entry<LevelPos, Integer> min = Collections.min(levelPosList, LevelPos.getPosComparator());
            levelPosList.remove(min);
            levelMinPosList.add(min);
        }

        return levelMinPosList;

    }

    private List<Map.Entry<LevelPos, Integer>> getDataToGenerate(LevelPos levelPos, int playerPosX, int playerPosZ, int start, int end, byte generation, byte detailLevel)
    {
        List<Map.Entry<LevelPos, Integer>> levelPosList = new ArrayList<>();

        int size = (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - levelPos.detailLevel);
        int width = (int) Math.pow(2, levelPos.detailLevel);

        //here i calculate the the LevelPos is in range
        //This is important to avoid any kind of hole in the generation
        int maxDistance = levelPos.maxDistance(playerPosX, playerPosZ, regionPosX, regionPosZ);
        int minDistance = levelPos.minDistance(playerPosX, playerPosZ, regionPosX, regionPosZ);

        if (!(start <= maxDistance && minDistance < end) || levelPos.detailLevel < detailLevel)
        {
            return levelPosList;
        }

        int childPosX = levelPos.posX * 2;
        int childPosZ = levelPos.posZ * 2;
        LevelPos childPos;

        int childSize = (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - levelPos.detailLevel + 1);
        //we have reached the target detail level
        if (detailLevel == levelPos.detailLevel)
        {
            if (generationType[levelPos.detailLevel][levelPos.posX][levelPos.posZ] < generation)
            {
                levelPosList.add(
                        new AbstractMap.SimpleEntry<LevelPos, Integer>(
                                new LevelPos(levelPos.detailLevel, levelPos.posX + regionPosX * size, levelPos.posZ + regionPosZ * size),
                                minDistance));
            }
        } else
        {
            //we want max a request per chunk. So for lod smaller than chunk we explore only the top rigth child
            if (levelPos.detailLevel > LodUtil.CHUNK_DETAIL_LEVEL)
            {
                //We take all the children that are not generated to at least the generation level taken in input
                for (int x = 0; x <= 1; x++)
                {
                    for (int z = 0; z <= 1; z++)
                    {
                        childPos = new LevelPos((byte) (levelPos.detailLevel - 1), childPosX + x, childPosZ + z);

                        minDistance = childPos.minDistance(playerPosX, playerPosZ, regionPosX, regionPosZ);

                        if (generationType[childPos.detailLevel][childPos.posX][childPos.posZ] < generation || !doesDataExist(childPos))
                        {
                            levelPosList.add(
                                    new AbstractMap.SimpleEntry<LevelPos, Integer>(
                                            new LevelPos(childPos.detailLevel, childPos.posX + regionPosX * childSize, childPos.posZ + regionPosZ * childSize),
                                            minDistance));
                        }
                    }
                }

                //only if all the children are correctly generated we go deeper
                if (levelPosList.isEmpty())
                {
                    for (int x = 0; x <= 1; x++)
                    {
                        for (int z = 0; z <= 1; z++)
                        {
                            childPos = new LevelPos((byte) (levelPos.detailLevel - 1), childPosX + x, childPosZ + z);
                            levelPosList.addAll(getDataToGenerate(childPos, playerPosX, playerPosZ, start, end, generation, detailLevel));
                        }
                    }
                }
            } else
            //now we keep exploring the top right child
            {
                childPos = levelPos.getConvertedLevelPos((byte) (levelPos.detailLevel - 1));
                if (generationType[childPos.detailLevel][childPos.posX][childPos.posZ] < generation)
                {
                    minDistance = childPos.minDistance(playerPosX, playerPosZ, regionPosX, regionPosZ);

                    levelPosList.add(
                            new AbstractMap.SimpleEntry<LevelPos, Integer>(
                                    new LevelPos(childPos.detailLevel, childPos.posX + regionPosX * childSize, childPos.posZ + regionPosZ * childSize),
                                    minDistance));
                } else
                {
                    if (childPos.detailLevel != detailLevel)
                    {
                        levelPosList.addAll(getDataToGenerate(childPos, playerPosX, playerPosZ, start, end, generation, detailLevel));
                    }
                }
            }
        }
        return levelPosList;
    }


    /**
     * @return
     */
    public List<LevelPos> getDataToRender(int playerPosX, int playerPosZ, int start, int end, byte detailLevel, boolean zFix)
    {
        LevelPos levelPos = new LevelPos(LodUtil.REGION_DETAIL_LEVEL, 0, 0);
        return getDataToRender(levelPos, playerPosX, playerPosZ, start, end, detailLevel, zFix);
    }

    /**
     * @return
     */
    private List<LevelPos> getDataToRender(LevelPos levelPos, int playerPosX, int playerPosZ, int start, int end, byte detailLevel, boolean zFix)
    {
        List<LevelPos> levelPosList = new ArrayList<>();

        int size = (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - levelPos.detailLevel);
        int width = (int) Math.pow(2, levelPos.detailLevel);

        //here i calculate the the LevelPos is in range
        //This is important to avoid any kind of hole in the rendering
        int maxDistance = levelPos.maxDistance(playerPosX, playerPosZ, regionPosX, regionPosZ);
        int minDistance = levelPos.minDistance(playerPosX, playerPosZ, regionPosX, regionPosZ);

        //To avoid z fighting: if the pos is touching the end radius at detailLevel + 1 then we stop
        //cause this area will be occupied by bigger block
        if (levelPos.detailLevel == detailLevel + 1 && end <= maxDistance && minDistance <= end && zFix)
        {
            return levelPosList;
        }

        if (!(start <= maxDistance && minDistance < end) || levelPos.detailLevel < detailLevel)
        {
            return levelPosList;
        }

        //we have reached the target detail level
        if (detailLevel == levelPos.detailLevel)
        {
            levelPosList.add(new LevelPos(levelPos.detailLevel, levelPos.posX + regionPosX * size, levelPos.posZ + regionPosZ * size));
        } else
        {
            int childPosX = levelPos.posX * 2;
            int childPosZ = levelPos.posZ * 2;
            LevelPos childPos;
            int childrenCount = 0;
            for (int x = 0; x <= 1; x++)
            {
                for (int z = 0; z <= 1; z++)
                {
                    childPos = new LevelPos((byte) (levelPos.detailLevel - 1), childPosX + x, childPosZ + z);
                    if (doesDataExist(childPos)) childrenCount++;
                }
            }

            //If all the four children exist we go deeper
            if (childrenCount == 4)
            {
                levelPosList.clear();
                for (int x = 0; x <= 1; x++)
                {
                    for (int z = 0; z <= 1; z++)
                    {
                        childPos = new LevelPos((byte) (levelPos.detailLevel - 1), childPosX + x, childPosZ + z);
                        levelPosList.addAll(getDataToRender(childPos, playerPosX, playerPosZ, start, end, detailLevel, zFix));
                    }
                }
            } else
            {
                levelPosList.add(new LevelPos(levelPos.detailLevel, levelPos.posX + regionPosX * size, levelPos.posZ + regionPosZ * size));
            }
        }
        return levelPosList;
    }

    public void updateArea(LevelPos levelPos)
    {

        LevelPos tempLevelPos;
        int width;
        int startX;
        int startZ;
        for(byte bottom = (byte) (minDetailLevel + 1); bottom <= levelPos.detailLevel ; bottom++){
            tempLevelPos = levelPos.getConvertedLevelPos(bottom);
            startX = tempLevelPos.posX;
            startZ = tempLevelPos.posZ;
            width = 1 << (levelPos.detailLevel - bottom);
            for(int x = 0; x < width; x++){
                for(int z = 0; z < width; z++) {
                    update(new LevelPos(bottom, startX+x, startZ+z));
                }
            }

        }
        for (byte tempLod = (byte) (levelPos.detailLevel + 1); tempLod <= LodUtil.REGION_DETAIL_LEVEL; tempLod++) {
            tempLevelPos = levelPos.getConvertedLevelPos(tempLod);
            update(tempLevelPos);
        }
    }

    private void update(LevelPos levelPos)
    {

        levelPos = levelPos.getRegionModuleLevelPos();
        int numberOfChildren = 0;

        byte minGenerationType = 5;
        int tempRed = 0;
        int tempGreen = 0;
        int tempBlue = 0;
        int tempHeight = 0;
        int tempDepth = 0;
        int newPosX;
        int newPosZ;
        byte newLod;
        LevelPos childPos;
        for (int x = 0; x <= 1; x++)
        {
            for (int z = 0; z <= 1; z++)
            {
                newPosX = 2 * levelPos.posX + x;
                newPosZ = 2 * levelPos.posZ + z;
                newLod = (byte) (levelPos.detailLevel - 1);
                childPos = new LevelPos(newLod, newPosX, newPosZ);
                if (hasDataBeenGenerated(childPos))
                {
                    numberOfChildren++;

                    tempRed += colors[newLod][newPosX][newPosZ][0];
                    tempGreen += colors[newLod][newPosX][newPosZ][1];
                    tempBlue += colors[newLod][newPosX][newPosZ][2];
                    tempHeight += height[newLod][newPosX][newPosZ];
                    tempDepth += depth[newLod][newPosX][newPosZ];
                    minGenerationType = (byte) Math.min(minGenerationType, generationType[newLod][newPosX][newPosZ]);
                }
            }
        }

        if (numberOfChildren > 0)
        {
            colors[levelPos.detailLevel][levelPos.posX][levelPos.posZ][0] = (byte) (tempRed / numberOfChildren);
            colors[levelPos.detailLevel][levelPos.posX][levelPos.posZ][1] = (byte) (tempGreen / numberOfChildren);
            colors[levelPos.detailLevel][levelPos.posX][levelPos.posZ][2] = (byte) (tempBlue / numberOfChildren);
            height[levelPos.detailLevel][levelPos.posX][levelPos.posZ] = (short) (tempHeight / numberOfChildren);
            depth[levelPos.detailLevel][levelPos.posX][levelPos.posZ] = (short) (tempDepth / numberOfChildren);
            generationType[levelPos.detailLevel][levelPos.posX][levelPos.posZ] = minGenerationType;
            dataExistence[levelPos.detailLevel][levelPos.posX][levelPos.posZ] = true;
        }
    }

    /**
     * @param levelPos
     *//*
    public void updateArea(LevelPos levelPos)
    {
        levelPos.performRegionModule();
        int width;
        int startX;
        int startZ;
        byte detailLevel = levelPos.detailLevel;
        for (byte bottom = (byte) (minDetailLevel + 1); bottom <= detailLevel; bottom++)
        {
            levelPos.convert(bottom);
            startX = levelPos.posX;
            startZ = levelPos.posZ;
            width = 1 << (detailLevel - bottom);
            for (int x = 0; x < width; x++)
            {
                for (int z = 0; z < width; z++)
                {
                    levelPos.changeParameters(bottom, startX + x, startZ + z);
                    update(levelPos);
                }
            }
        }
        byte tempLod = 0;
        try
        {
            for (tempLod = (byte) (detailLevel + 1); tempLod <= LodUtil.REGION_DETAIL_LEVEL; tempLod++)
            {
                levelPos.convert(tempLod);
                update(levelPos);
            }
        }catch (Exception e){
            System.out.println(tempLod);
            System.out.println(levelPos);
            e.printStackTrace();
            throw e;
        }
    }*/
    /**
     * @param levelPos
     *//*
    private void update(LevelPos levelPos)
    {
        levelPos.performRegionModule();
        int numberOfChildren = 0;

        byte minGenerationType = 5;
        int tempRed = 0;
        int tempGreen = 0;
        int tempBlue = 0;
        int tempHeight = 0;
        int tempDepth = 0;
        int newPosX;
        int newPosZ;
        byte newDetailLevel;
        LevelPos childPos;
        byte detailLevel = levelPos.detailLevel;
        int posX = levelPos.posX;
        int posZ = levelPos.posZ;
        try
        {
            if(detailLevel > 0)
            {
                for (int x = 0; x <= 1; x++)
                {
                    for (int z = 0; z <= 1; z++)
                    {
                        newPosX = 2 * posX + x;
                        newPosZ = 2 * posZ + z;
                        newDetailLevel = (byte) (detailLevel - 1);
                        levelPos.changeParameters(newDetailLevel, newPosX, newPosZ);
                        if (hasDataBeenGenerated(levelPos))
                        {
                            numberOfChildren++;

                            tempRed += colors[newDetailLevel][newPosX][newPosZ][0];
                            tempGreen += colors[newDetailLevel][newPosX][newPosZ][1];
                            tempBlue += colors[newDetailLevel][newPosX][newPosZ][2];
                            tempHeight += height[newDetailLevel][newPosX][newPosZ];
                            tempDepth += depth[newDetailLevel][newPosX][newPosZ];
                            minGenerationType = (byte) Math.min(minGenerationType, generationType[newDetailLevel][newPosX][newPosZ]);
                        }
                    }
                }
                if (numberOfChildren > 0)
                {
                    colors[detailLevel][posX][posZ][0] = (byte) (tempRed / numberOfChildren);
                    colors[detailLevel][posX][posZ][1] = (byte) (tempGreen / numberOfChildren);
                    colors[detailLevel][posX][posZ][2] = (byte) (tempBlue / numberOfChildren);
                    height[detailLevel][posX][posZ] = (short) (tempHeight / numberOfChildren);
                    depth[detailLevel][posX][posZ] = (short) (tempDepth / numberOfChildren);
                    generationType[detailLevel][posX][posZ] = minGenerationType;
                    dataExistence[detailLevel][posX][posZ] = true;
                }
            }
        }catch (Exception e){
            throw e;
        }
    }*/

    /**
     * @param levelPos
     * @return
     */
    private boolean[][] getChildren(LevelPos levelPos)
    {
        levelPos = levelPos.getRegionModuleLevelPos();
        boolean[][] children = new boolean[2][2];
        int numberOfChild = 0;
        if (minDetailLevel == levelPos.detailLevel)
        {
            return children;
        }
        for (int x = 0; x <= 1; x++)
        {
            for (int z = 0; z <= 1; z++)
            {
                children[x][z] = (dataExistence[levelPos.detailLevel - 1][2 * levelPos.posX + x][2 * levelPos.posZ + z]);
            }
        }
        return children;
    }

    /**
     * @param chunkPos
     * @return
     */
    public boolean doesDataExist(ChunkPos chunkPos)
    {
        return doesDataExist(new LevelPos(LodUtil.CHUNK_DETAIL_LEVEL, chunkPos.x, chunkPos.z));
    }

    /**
     * @param levelPos
     * @return
     */
    public boolean doesDataExist(LevelPos levelPos)
    {
        levelPos = levelPos.getRegionModuleLevelPos();
        return dataExistence[levelPos.detailLevel][levelPos.posX][levelPos.posZ];
    }

    /**
     * @param levelPos
     * @return
     */
    public DistanceGenerationMode getGenerationMode(LevelPos levelPos)
    {
        levelPos = levelPos.getRegionModuleLevelPos();
        DistanceGenerationMode generationMode = DistanceGenerationMode.NONE;
        switch (generationType[levelPos.detailLevel][levelPos.posX][levelPos.posZ])
        {
            case 0:
                generationMode = DistanceGenerationMode.NONE;
                break;
            case 1:
                generationMode = DistanceGenerationMode.BIOME_ONLY;
                break;
            case 2:
                generationMode = DistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT;
                break;
            case 3:
                generationMode = DistanceGenerationMode.SURFACE;
                break;
            case 4:
                generationMode = DistanceGenerationMode.FEATURES;
                break;
            case 5:
                generationMode = DistanceGenerationMode.SERVER;
                break;
            default:
                generationMode = DistanceGenerationMode.NONE;
                break;

        }
        return generationMode;
    }

    /**
     * @param levelPos
     * @return
     */
    public boolean hasDataBeenGenerated(LevelPos levelPos)
    {
        levelPos.performRegionModule();
        try
        {
            return (generationType[levelPos.detailLevel][levelPos.posX][levelPos.posZ] != 0);
        }catch(Exception e){
            System.out.println(levelPos);
            e.printStackTrace();
            throw e;
        }
    }

    public byte getMinDetailLevel()
    {
        return minDetailLevel;
    }

    /**
     * This will be used to save a level
     *
     * @param detailLevel
     * @return
     */
    public LevelContainer getLevel(byte detailLevel)
    {
        if (detailLevel < minDetailLevel)
        {
            throw new IllegalArgumentException("getLevel asked for a level that does not exist: minimum " + minDetailLevel + " level requested " + detailLevel);
        }
        return new LevelContainer(detailLevel, colors[detailLevel], height[detailLevel], depth[detailLevel], generationType[detailLevel], dataExistence[detailLevel]);
    }

    /**
     * @param levelContainer
     */
    public void addLevel(LevelContainer levelContainer)
    {
        if (levelContainer.detailLevel < minDetailLevel - 1)
        {
            throw new IllegalArgumentException("addLevel requires a level that is at least the minimum level of the region -1 ");
        }
        if (levelContainer.detailLevel == minDetailLevel - 1) minDetailLevel = levelContainer.detailLevel;
        colors[levelContainer.detailLevel] = levelContainer.colors;
        height[levelContainer.detailLevel] = levelContainer.height;
        depth[levelContainer.detailLevel] = levelContainer.depth;
        generationType[levelContainer.detailLevel] = levelContainer.generationType;
        dataExistence[levelContainer.detailLevel] = levelContainer.dataExistence;

    }

    /**
     * @param detailLevel
     */
    public void cutTree(byte detailLevel)
    {
        if (minDetailLevel < detailLevel)
        {
            for (byte tempLod = 0; tempLod < detailLevel; tempLod++)
            {
                colors[tempLod] = new byte[0][0][0];
                height[tempLod] = new short[0][0];
                depth[tempLod] = new short[0][0];
                generationType[tempLod] = new byte[0][0];
                dataExistence[tempLod] = new boolean[0][0];
            }
            minDetailLevel = detailLevel;
        }
    }

    /**
     * @param detailLevel
     */
    public void expand(byte detailLevel)
    {
        if (detailLevel < minDetailLevel)
        {
            for (byte tempLod = minDetailLevel; tempLod < LodUtil.REGION_DETAIL_LEVEL; tempLod++)
            {
                int size = (short) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - tempLod);
                generationType[tempLod] = new byte[size][size];
            }
            for (byte tempLod = detailLevel; tempLod < minDetailLevel; tempLod++)
            {
                int size = (short) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - tempLod);
                colors[tempLod] = new byte[size][size][3];
                height[tempLod] = new short[size][size];
                depth[tempLod] = new short[size][size];
                generationType[tempLod] = new byte[size][size];
                dataExistence[tempLod] = new boolean[size][size];
            }
            minDetailLevel = detailLevel;
        }
    }

    /**
     * return RegionPos of this lod region
     */
    public RegionPos getRegionPos()
    {
        return new RegionPos(regionPosX, regionPosZ);
    }

    /**
     * return needed memory in byte
     */
    public int getMinMemoryNeeded()
    {
        int count = 0;
        for (byte tempLod = LodUtil.REGION_DETAIL_LEVEL; tempLod > minDetailLevel; tempLod--)
        {
            //i'm doing a upper limit of the minimum
            //Color should be just 3 byte but i'm gonna calculate as 12 byte
            //Height and depth should be just 4 byte but i'm gonna calculate as 8 byte
            //count += Math.pow(2,LodUtil.REGION_DETAIL_LEVEL-tempLod) * (8 + 3 + 2 + 2 + 1 + 1)
            count += Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - tempLod) * (24 + 8 + 8 + 8 + 8 + 8);
        }
        return count;
    }

    public String toString()
    {
        return getLevel(LodUtil.REGION_DETAIL_LEVEL).toString();
    }
}
