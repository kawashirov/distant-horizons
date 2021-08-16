package com.seibel.lod.objects;

import com.seibel.lod.util.LodUtil;

public class LodRegion {
    //x coord,
    private byte levelOfDetail;

    private short size;

    private short width;

    private int numberOfPoints;

    //For each of the following field the first slot is for the level of detail
    private byte[][][][] colors;

    private short[][][] height;

    private short[][][] depth;

    private byte[][][] generationType;

    public LodRegion(byte levelOfDetail){
        size = (short) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - levelOfDetail);
        width = (short) Math.pow(2, levelOfDetail);
        colors = new byte[size][size][size][3];
        height = new short[size][size][size];
        depth = new short[size][size];
        generationType = new byte[size][size];
    }

    public boolean addData(int posX, int posZ, byte red, byte green, byte blue, short height, short depth, byte generationType){
        if( (this.generationType[posX][posZ] == null) || (generationType < this.generationType[posX][posZ]) ) {
            if (this.generationType[posX][posZ] == null) numberOfPoints++ ;
            this.colors[posX][posZ][0] = red;
            this.colors[posX][posZ][1] = green;
            this.colors[posX][posZ][2] = blue;
            this.height[posX][posZ] = height;
            this.depth[posX][posZ] = depth;
            this.generationType[posX][posZ] = generationType;
            return true; //added
        }else{
            return false; //not added
        }
    }

    public short[] getDataAtPos(int posX, int posZ, byte newLevelOfDetail){
        boolean fastConversion = true;
        short[] data = new short[5]; //this array will contains all the data: red, green, blue, height, depth
        if(fastConversion){

        }else{

        }
    }

    public short[] cornerMean(short startX, short startZ, short endX, short endZ, byte newLevelOfDetail){
        short[] dataToCompute = new short[5][4]; //this array will contains all the data: red, green, blue, height, depth
        if(){

        }
    }

    public short[] fullMean(short startX, short startZ, short endX, short endZ, byte newLevelOfDetail){
        short[] dataToCompute = new short[5][4]; //this array will contains all the data: red, green, blue, height, depth

    }

    public short[] combine(short startX, short startZ, short endX, short endZ, byte newLevelOfDetail){

    }
}
