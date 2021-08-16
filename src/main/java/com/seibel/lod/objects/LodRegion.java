package com.seibel.lod.objects;

import com.seibel.lod.util.LodUtil;

import java.awt.*;
import java.io.Serializable;

public class LodRegion implements Serializable {
    //x coord,
    private byte minLevelOfDetail;

    private int numberOfPoints;

    private byte[] sizes;
    private short[] widths;

    //For each of the following field the first slot is for the level of detail
    private byte[][][][] colors;

    private short[][][] height;

    private short[][][] depth;

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


    public boolean setData(byte lod, int posX, int posZ, LodDataPoint dataPoint, byte generationType){
        return setData(lod, posX, posZ, dataPoint.color.getRed(), dataPoint.color.getGreen(), dataPoint.color.getBlue(), dataPoint.height, dataPoint.depth, generationType);
    }

    public boolean setData(byte lod, int posX, int posZ, byte red, byte green, byte blue, short height, short depth, byte generationType){
        if( (this.generationType[lod][posX][posZ] == null) || (generationType < this.generationType[lod][posX][posZ]) ) {

            //update the number of node present
            if (this.generationType[posX][posZ] == null) numberOfPoints++ ;

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
            for(int tempLod = lod+1; tempLod <= LodUtil.REGION_DETAIL_LEVEL; tempLod ++) {
                tempPosX = Math.floorDiv(tempPosX, 2);
                tempPosZ = Math.floorDiv(tempPosZ, 2);
                update(tempLod, tempPosX, tempPosZ)
            }

            return true; //added
        }else{
            return false; //not added
        }
    }

    public LodDataPoint getData(byte lod, int posX, int posZ){
        return new LodDataPoint(
                height[lod][posX][posZ],
                depth[lod][posX][posZ],
                new Color(colors[lod][posX][posZ][0],
                        colors[lod][posX][posZ][1],
                        colors[lod][posX][posZ][2]
                )
        );
    }

    private void update(byte lod, int posX, int posZ){
        for(int col = 0; col <= 2; col++) {
            colors[lod][posX][posZ][col] = (colors[lod-1][2*posX][2*posZ][col] +
                            colors[lod-1][2*posX][2*posZ+1][col] +
                            colors[lod-1][2*posX+1][2*posZ+1][col] +
                            colors[lod-1][2*posX+1][2*posZ+1][col]) / 4;
        }
        /*TODO add multiple way to combine depth, heigth and generationType*/

        depth[lod][posX][posZ] = (depth[lod-1][2*posX][2*posZ] +
                depth[lod-1][2*posX][2*posZ+1] +
                depth[lod-1][2*posX+1][2*posZ+1] +
                depth[lod-1][2*posX+1][2*posZ+1]) / 4;

        height[lod][posX][posZ] = (height[lod-1][2*posX][2*posZ] +
                height[lod-1][2*posX][2*posZ+1] +
                height[lod-1][2*posX+1][2*posZ+1] +
                height[lod-1][2*posX+1][2*posZ+1]) / 4;

        generationType[lod][posX][posZ] =
                (generationType[lod-1][2*posX][2*posZ] +
                generationType[lod-1][2*posX][2*posZ+1] +
                generationType[lod-1][2*posX+1][2*posZ+1] +
                generationType[lod-1][2*posX+1][2*posZ+1]) / 4;
    }

    private int[][] getChildren(byte lod, int posX, int posZ){
        int[][] children = new int[2][4];
        int numberOfChild=0;
        if(minLevelOfDetail=lod) return null;
        for(int x = 0; x <= 1; x++){
            for(int z = 0; z <= 1; z++){
                int newPosX = 2*posX+x;
                int newPosZ = 2*posZ+z;
                if(generationType[lod-1][newPosX][newPosZ] != null){
                    children[0]
                }
            }
        }
    }
}
