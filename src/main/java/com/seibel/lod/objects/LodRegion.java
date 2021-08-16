package com.seibel.lod.objects;

import com.seibel.lod.util.LodUtil;

import java.awt.*;
import java.io.Serializable;

/**STANDARD TO FOLLOW
 * every coordinate called posX or posZ is a relative coordinate and not and absolute coordinate
 * if an array contain coordinate the order is the following
 * 0 for x, 1 for z in 2D
 * 0 for x, 1 for y, 2 for z in 3D
 */

public class LodRegion{
    //x coord,
    private byte minLevelOfDetail;
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

    private int regionPosX;
    private int regionPosZ;

    public LodRegion(byte minimumLevelOfDetail, RegionPos regionPos){
        this.regionPosX = regionPos.x;
        this.regionPosZ = regionPos.z;

        //Array of matrices of arrays
        colors =            new byte[POSSIBLE_LOD][][][];

        //Arrays of matrices
        height =            new short[POSSIBLE_LOD][][];
        depth =             new short[POSSIBLE_LOD][][];
        generationType =    new byte[POSSIBLE_LOD][][];


        //Initialize all the different matrices
        for(byte lod = minimumLevelOfDetail; lod <= LodUtil.REGION_DETAIL_LEVEL; lod ++){
            int size = (short) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - lod);
            colors[lod] = new byte[size][size][3];
            height[lod] = new short[size][size];
            depth[lod] = new short[size][size];
            generationType[lod] = new byte[size][size];

        }
    }

    /**
     * This method can be used to insert data into the LodRegion
     * @param lod
     * @param posX
     * @param posZ
     * @param dataPoint
     * @param generationType
     * @param update
     * @return
     */
    public boolean setData(byte lod, int posX, int posZ, LodDataPoint dataPoint, byte generationType, boolean update){
        return setData(lod, posX, posZ, (byte) (dataPoint.color.getRed() - 128), (byte) (dataPoint.color.getGreen() - 128), (byte) (dataPoint.color.getBlue() - 128), dataPoint.height, dataPoint.depth, generationType, update);
    }

    /**
     * This method can be used to insert data into the LodRegion
     * @param lod
     * @param posX
     * @param posZ
     * @param red
     * @param green
     * @param blue
     * @param height
     * @param depth
     * @param generationType
     * @param update
     * @return
     */
    public boolean setData(byte lod, int posX, int posZ, byte red, byte green, byte blue, short height, short depth, byte generationType, boolean update){
        posX = Math.floorMod(posX, (int) Math.pow(2,lod));
        posZ = Math.floorMod(posZ, (int) Math.pow(2,lod));
        if( (this.generationType[lod][posX][posZ] == 0) || (generationType < this.generationType[lod][posX][posZ]) ) {

            //update the number of node present
            if (this.generationType[lod][posX][posZ] == 0) numberOfPoints++ ;

            //add the node data
            this.colors[lod][posX][posZ][0] = red;
            this.colors[lod][posX][posZ][1] = green;
            this.colors[lod][posX][posZ][2] = blue;
            this.height[lod][posX][posZ] = height;
            this.depth[lod][posX][posZ] = depth;
            this.generationType[lod][posX][posZ] = generationType;

            //update all the higher level
            int tempPosX = posX;
            int tempPosZ = posZ;

            //update could be stopped and a single big update could be done at the end
            if(update) {
                for (byte tempLod = (byte) (lod + 1); tempLod <= LodUtil.REGION_DETAIL_LEVEL; tempLod++) {
                    tempPosX = Math.floorDiv(tempPosX, 2);
                    tempPosZ = Math.floorDiv(tempPosZ, 2);
                    update(tempLod, tempPosX, tempPosZ);
                }
            }
            return true; //added
        }else{
            return false; //not added
        }
    }

    /**
     * This method will return the data in the position relative to the level of detail
     * @param lod
     * @param posX
     * @param posZ
     * @return the data at the relative pos and level
     */
    public LodDataPoint getData(byte lod, int posX, int posZ){
        return new LodDataPoint(
                height[lod][posX][posZ],
                depth[lod][posX][posZ],
                new Color(colors[lod][posX][posZ][0] + 128,
                        colors[lod][posX][posZ][1] + 128,
                        colors[lod][posX][posZ][2] + 128
                )
        );
    }
/*
    private void updateArea(byte lod, int posX, int posZ){
    }
*/
    private void update(byte lod, int posX, int posZ){
        posX = Math.floorMod(posX, (int) Math.pow(2,lod));
        posZ = Math.floorMod(posZ, (int) Math.pow(2,lod));
        boolean[][] children = getChildren(lod, posX, posZ);
        int numberOfChild = 0;

        for(int x = 0; x <= 1; x++) {
            for (int z = 0; z <= 1; z++) {
                if(children[x][z]){
                    numberOfChild++;
                }
            }
        }

        if(numberOfChild>0) {

            //int minDepth = Integer.MAX_VALUE;
            //int maxDepth = Integer.MIN_VALUE;
            //int minHeight = Integer.MAX_VALUE;
            //int maxHeight = Integer.MIN_VALUE;

            byte minGenerationType = 0;
            for (int x = 0; x <= 1; x++) {
                for (int z = 0; z <= 1; z++) {
                    if (children[x][z]) {
                        int newPosX = 2 * posX + x;
                        int newPosZ = 2 * posZ + z;
                        for (int col = 0; col <= 2; col++) {
                            colors[lod][posX][posZ][col] += (byte) (colors[lod - 1][newPosX][newPosZ][col] / numberOfChild);
                        }

                        //TODO ability to change between mean, max and min.

                        height[lod][posX][posZ] += (short) (height[lod - 1][newPosX][newPosZ] / numberOfChild);
                        //minHeight = Math.min( height[lod - 1][newPosX][newPosZ] , maxHeight);
                        //maxHeight = Math.max( height[lod - 1][newPosX][newPosZ] , minHeight);

                        depth[lod][posX][posZ] += (short) (depth[lod - 1][newPosX][newPosZ] / numberOfChild);
                        //minDepth = Math.min( depth[lod - 1][newPosX][newPosZ] , maxDepth);
                        //maxDepth = Math.max( depth[lod - 1][newPosX][newPosZ] , minDepth);

                        minGenerationType = (byte) Math.max(minGenerationType, generationType[lod - 1][newPosX][newPosZ]);
                    }
                }
            }
            //height[lod][posX][posZ] = minHeight;
            //depth[lod][posX][posZ] = maxDepth;
            generationType[lod][posX][posZ] = minGenerationType;
        }
    }

    private boolean[][] getChildren(byte lod, int posX, int posZ){
        posX = Math.floorMod(posX, (int) Math.pow(2,lod));
        posZ = Math.floorMod(posZ, (int) Math.pow(2,lod));
        boolean[][] children = new boolean[2][2];
        int numberOfChild=0;
        if(minLevelOfDetail == lod){
            return children;
        }
        for(int x = 0; x <= 1; x++) {
            for (int z = 0; z <= 1; z++) {
                children[x][z] = (generationType[lod-1][2*posX+x][2*posZ+z] != 0);
            }
        }
        return children;
    }

    private void removeDetailLevel(byte lod, byte[][][] colors, short[][] height, short[][] depth, byte[][] generationType){
    }

    private void addDetailLevel(byte lod, int posX, int posZ){
    }
}
