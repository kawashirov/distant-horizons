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

public class LodRegion implements Serializable {
    //x coord,
    private byte minLevelOfDetail;

    private int numberOfPoints;

    private byte[] sizes;
    private short[] widths;

    //For each of the following field the first slot is for the level of detail
    //Important: byte have a [-128, 127] range. When converting from or to int a 128 should be added or removed
    private byte[][][][] colors;

    private short[][][] height;

    private short[][][] depth;

    //a new node will have 0 as generationType
    private byte[][][] generationType;

    private final RegionPos regionPos;

    public LodRegion(byte levelOfDetail, RegionPos regionPos){
        this.regionPos = regionPos;

        sizes =             new byte[LodUtil.REGION_DETAIL_LEVEL];
        widths =            new short[LodUtil.REGION_DETAIL_LEVEL];

        //Array of matrices of arrays
        colors =            new byte[LodUtil.REGION_DETAIL_LEVEL][][][];

        //Arrays of matrices
        height =            new short[LodUtil.REGION_DETAIL_LEVEL][][];
        depth =             new short[LodUtil.REGION_DETAIL_LEVEL][][];
        generationType =    new byte[LodUtil.REGION_DETAIL_LEVEL][][];


        //Initialize all the different matrices
        for(int lod = levelOfDetail; lod <= LodUtil.REGION_DETAIL_LEVEL; lod ++){
            int size = (short) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - lod);
            int width = (short) Math.pow(2, lod);
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
                for (int tempLod = lod + 1; tempLod <= LodUtil.REGION_DETAIL_LEVEL; tempLod++) {
                    tempPosX = Math.floorDiv(tempPosX, 2);
                    tempPosZ = Math.floorDiv(tempPosZ, 2);
                    update((byte) tempLod, tempPosX, tempPosZ);
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
            //int maxDepth = Integer.MIN_VALUE;
            //int minHeight = Integer.MAX_VALUE;
            byte minGenerationType = 0;
            for (int x = 0; x <= 1; x++) {
                for (int z = 0; z <= 1; z++) {
                    if (children[x][z]) {
                        int newPosX = 2 * posX + x;
                        int newPosZ = 2 * posZ + z;
                        for (int col = 0; col <= 2; col++) {
                            colors[lod][posX][posZ][col] = (byte) (colors[lod - 1][newPosX][newPosZ][col] / numberOfChild);
                        }

                        depth[lod][posX][posZ] = (short) (depth[lod - 1][newPosX][newPosZ] / numberOfChild);

                        height[lod][posX][posZ] = (short) (height[lod - 1][newPosX][newPosZ] / numberOfChild);
                        minGenerationType = (byte) Math.min(minGenerationType, generationType[lod][posX][posZ]);
                    }
                }
            }
            generationType[lod][posX][posZ] = minGenerationType;
        }
    }

    private boolean[][] getChildren(byte lod, int posX, int posZ){
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
}
