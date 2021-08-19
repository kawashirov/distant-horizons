package com.seibel.lod.objects;

import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.util.LodUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.awt.*;
import java.io.Serializable;

/**
 * STANDARD TO FOLLOW
 * every coordinate called posX or posZ is a relative coordinate and not and absolute coordinate
 * if an array contain coordinate the order is the following
 * 0 for x, 1 for z in 2D
 * 0 for x, 1 for y, 2 for z in 3D
 */

public class LodRegion implements Serializable {
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

    public LodRegion(LevelContainer levelContainer, RegionPos regionPos) {
        /**TODO there is some error here in the update*/
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
        for (byte lod = (byte) (minDetailLevel + 1); lod <= LodUtil.REGION_DETAIL_LEVEL; lod++) {
            int size = (short) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - lod);
            colors[lod] = new byte[size][size][3];
            height[lod] = new short[size][size];
            depth[lod] = new short[size][size];
            generationType[lod] = new byte[size][size];
            dataExistence[lod] = new boolean[size][size];
        }
        int sizeDiff;
        LevelPos levelPos;
        for (byte tempLod = (byte) (minDetailLevel + 1); tempLod <= LodUtil.REGION_DETAIL_LEVEL; tempLod++) {
            sizeDiff = (int) Math.pow(2,LodUtil.REGION_DETAIL_LEVEL - tempLod);
            for (int x = 0; x < sizeDiff; x++) {
                for (int z = 0; z < sizeDiff; z++) {
                    levelPos = new LevelPos(tempLod, x, z);
                    update(levelPos);
                }
            }
        }
    }

    public LodRegion(byte minDetailLevel, RegionPos regionPos) {
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
        for (byte lod = minDetailLevel; lod <= LodUtil.REGION_DETAIL_LEVEL; lod++) {
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
     * @param update
     * @return
     */
    public boolean setData(LevelPos levelPos, LodDataPoint dataPoint, byte generationType, boolean update) {
        levelPos = levelPos.regionModule();
        if ((this.generationType[levelPos.detailLevel][levelPos.posX][levelPos.posZ] == 0) || (generationType >= this.generationType[levelPos.detailLevel][levelPos.posX][levelPos.posZ])) {

            //update the number of node present
            //if (this.generationType[lod][posX][posZ] == 0) numberOfPoints++;

            //add the node data
            this.colors[levelPos.detailLevel][levelPos.posX][levelPos.posZ][0] = (byte) (dataPoint.color.getRed() - 128);
            this.colors[levelPos.detailLevel][levelPos.posX][levelPos.posZ][1] = (byte) (dataPoint.color.getGreen() - 128);
            this.colors[levelPos.detailLevel][levelPos.posX][levelPos.posZ][2] = (byte) (dataPoint.color.getBlue() - 128);
            this.height[levelPos.detailLevel][levelPos.posX][levelPos.posZ] = dataPoint.height;
            this.depth[levelPos.detailLevel][levelPos.posX][levelPos.posZ] = dataPoint.depth;
            this.generationType[levelPos.detailLevel][levelPos.posX][levelPos.posZ] = generationType;
            this.dataExistence[levelPos.detailLevel][levelPos.posX][levelPos.posZ] = true;

            //update could be stopped and a single big update could be done at the end
            LevelPos tempLevelPos = levelPos;
            if (update) {
                for (byte tempLod = (byte) (levelPos.detailLevel + 1); tempLod <= LodUtil.REGION_DETAIL_LEVEL; tempLod++) {
                    tempLevelPos = tempLevelPos.convert(tempLod);
                    update(tempLevelPos);
                }
            }
            return true;
        } else {
            return false;
        }
    }


    public LodDataPoint getData(ChunkPos chunkPos) {
        return getData(new LevelPos(LodUtil.CHUNK_DETAIL_LEVEL, chunkPos.x, chunkPos.z));
    }

    /**
     * This method will return the data in the position relative to the level of detail
     *
     * @param lod
     * @return the data at the relative pos and level
     */
    public LodDataPoint getData(byte lod, BlockPos blockPos) {
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
    public LodDataPoint getData(LevelPos levelPos) {
        levelPos = levelPos.regionModule();
        return new LodDataPoint(
                height[levelPos.detailLevel][levelPos.posX][levelPos.posZ],
                depth[levelPos.detailLevel][levelPos.posX][levelPos.posZ],
                new Color(colors[levelPos.detailLevel][levelPos.posX][levelPos.posZ][0] + 128,
                        colors[levelPos.detailLevel][levelPos.posX][levelPos.posZ][1] + 128,
                        colors[levelPos.detailLevel][levelPos.posX][levelPos.posZ][2] + 128
                )
        );
    }

    /**
     * @return
     */
        /*
    public List<LevelPos> getDataToGenerate(int playerPosX, int playerPosZ, int start, int end, byte generetion, byte detailLevel) {
        if(detailLevel < minDetailLevel) detailLevel = minDetailLevel;
        LevelPos levelPos
        int size;
        int width;
        int posX;
        int posZ;
        int distance;
        for(int tempLod = detailLevel; tempLod <= LodUtil.REGION_DETAIL_LEVEL; tempLod++){
            size = (int) Math.pow(2,LodUtil.REGION_DETAIL_LEVEL-tempLod);
            width = (int) Math.pow(2,tempLod);
            for(int x = 0; x < size; x++){
                for(int z = 0; z < size; z++){
                    posX = regionPosX * 512 + x * width + width/2;
                    posZ = regionPosZ * 512 + z * width + width/2;
                    distance = (int) Math.sqrt(Math.pow(playerPosX - posX, 2) + Math.pow(playerPosZ - posZ, 2))
                    if(distance >= start && distance <= end){

                    }
                }
            }
        }
    }
         */

    /**
     * @return
     */
        /*
    public List<LevelPos> getDataToRender(int playerPosX, int playerPosZ, int start, int end, byte detailLevel) {
        if(detailLevel < minDetailLevel) detailLevel = minDetailLevel;
        int size;
        int width;
        int posX;
        int posZ;
        for(int tempLod = detailLevel; tempLod <= LodUtil.REGION_DETAIL_LEVEL; tempLod++){
            size = (int) Math.pow(2,LodUtil.REGION_DETAIL_LEVEL-tempLod);
            width = (int) Math.pow(2,tempLod);
            for(int x = 0; x < size; x++){
                for(int z = 0; z < size; z++){
                    dataExistence[][]
                }
            }
        }
    }
         */

    /**TODO a method to update a whole area, to be used as a single big update*/
    /**
     * @param levelPos
     */
    private void updateArea(LevelPos levelPos) {
        /*
        LevelPos tempLevelPos = levelPos;
        int sizeDiff;
        int startX;
        int startZ;
        for(int bottom = minLevelOfDetail + 1 ; bottom < levelPos.detailLevel ; bottom ++){
            tempLevelPos = levelPos.convert(bottom);
            startX = tempLevelPos.posX;
            startZ = tempLevelPos.posZ;
            sizeDiff = (int) Math.pow(2, levelPos.detailLevel - bottom);
            for(int x = 0; x < sizeDiff; x++){
                for(int z = 0; z < sizeDiff; z++) {
                    update(new LevelPos(bottom, startX+x, startZ+z));
                }
            }

        }

         */
    }

    /**
     * @param levelPos
     */
    private void update(LevelPos levelPos) {

        levelPos = levelPos.regionModule();
        boolean[][] children = getChildren(levelPos);
        int numberOfChildren = 0;

        /**TODO add the ability to change how the heigth and depth are determinated (for example min or max)**/
        byte minGenerationType = 10;
        int tempRed = 0;
        int tempGreen = 0;
        int tempBlue = 0;
        int tempHeight = 0;
        int tempDepth = 0;
        int newPosX;
        int newPosZ;
        byte newLod;
        LevelPos childPos;
        for (int x = 0; x <= 1; x++) {
            for (int z = 0; z <= 1; z++) {
                newPosX = 2 * levelPos.posX + x;
                newPosZ = 2 * levelPos.posZ + z;
                newLod = (byte) (levelPos.detailLevel - 1);
                childPos = new LevelPos(newLod, newPosX, newPosZ);
                if (hasDataBeenGenerated(childPos)) {
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

        if (numberOfChildren > 0) {
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
     * @return
     */
    private boolean[][] getChildren(LevelPos levelPos) {
        levelPos = levelPos.regionModule();
        boolean[][] children = new boolean[2][2];
        int numberOfChild = 0;
        if (minDetailLevel == levelPos.detailLevel) {
            return children;
        }
        for (int x = 0; x <= 1; x++) {
            for (int z = 0; z <= 1; z++) {
                children[x][z] = (dataExistence[levelPos.detailLevel - 1][2 * levelPos.posX + x][2 * levelPos.posZ + z]);
            }
        }
        return children;
    }

    /**
     * @param chunkPos
     * @return
     */
    public boolean doesDataExist(ChunkPos chunkPos) {
        return doesDataExist(new LevelPos(LodUtil.CHUNK_DETAIL_LEVEL, chunkPos.x, chunkPos.z));
    }

    /**
     * @param levelPos
     * @return
     */
    public boolean doesDataExist(LevelPos levelPos) {
        levelPos = levelPos.regionModule();
        return dataExistence[levelPos.detailLevel][levelPos.posX][levelPos.posZ];
    }

    /**
     * @param levelPos
     * @return
     */
    public DistanceGenerationMode getGenerationMode(LevelPos levelPos) {
        levelPos = levelPos.regionModule();
        DistanceGenerationMode generationMode = DistanceGenerationMode.NONE;
        switch (generationType[levelPos.detailLevel][levelPos.posX][levelPos.posZ]) {
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
    public boolean hasDataBeenGenerated(LevelPos levelPos) {
        levelPos = levelPos.regionModule();
        return (generationType[levelPos.detailLevel][levelPos.posX][levelPos.posZ] != 0);
    }

    public byte getMinDetailLevel() {
        return minDetailLevel;
    }

    /**
     * This will be used to save a level
     *
     * @param lod
     * @return
     */
    public LevelContainer getLevel(byte lod) {
        return new LevelContainer(lod, colors[lod], height[lod], depth[lod], generationType[lod], dataExistence[lod]);
    }

    /**
     * @param levelContainer
     */
    public void addLevel(LevelContainer levelContainer) {
        if (levelContainer.detailLevel < minDetailLevel - 1) {
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
     * @param lod
     */
    public void removeDetailLevel(byte lod) {
        for (byte tempLod = 0; tempLod <= lod; tempLod++) {
            colors[tempLod] = new byte[0][0][0];
            height[tempLod] = new short[0][0];
            depth[tempLod] = new short[0][0];
            generationType[tempLod] = new byte[0][0];
            dataExistence[tempLod] = new boolean[0][0];
        }
    }

    public String toString(){
        return getLevel(LodUtil.REGION_DETAIL_LEVEL).toString();
    }
}
