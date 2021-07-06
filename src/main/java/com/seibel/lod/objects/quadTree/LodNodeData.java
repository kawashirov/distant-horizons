package com.seibel.lod.objects.quadTree;

import com.seibel.lod.handlers.LodQuadTreeDimensionFileHandler;

import java.awt.*;
import java.util.*;
import java.util.List;

public class LodNodeData {
    /** This is what separates each piece of data in the toData method */
    private static final char DATA_DELIMITER = LodQuadTreeDimensionFileHandler.DATA_DELIMITER;



    /** this is how many pieces of data are exported when toData is called */
    public static final int NUMBER_OF_DELIMITERS = 9;

    private static final Color INVISIBLE = new Color(0,0,0,0);

    //level height goes from 0 to 9 with 0 the deepest (block size) and 9 the highest (region size)
    public final byte level;
    public static final byte REGION_LEVEL = 9; //at level 9 we reach the dimension of a single region
    public static final byte CHUNK_LEVEL = 4; //at level 4 we reach the dimension of a single chunk
    public static final byte BLOCK_LEVEL = 0; //at level 0 we reach the dimension of a single block

    //indicate the width in block of this node (goes from 1 to 512)
    public final short width;
    public static final short REGION_WIDTH = 512; //at level 9 we reach the dimension of a single region
    public static final short CHUNK_WIDTH = 16; //at level 4 we reach the dimension of a single chunk
    public static final short BLOCK_WIDTH = 1; //at level 0 we reach the dimension of a single block

    //this 2 values indicate the position of the LOD in the relative Level
    //this will be useful in the generation process
    public final int posX;
    public final int posZ;

    //these 4 value indicate the corner of the LOD block
    //they can be named SW, SE, NW, NE as the cardinal direction.
    //the start values should always be smaller than the end values.
    //All this value could be calculated from level and levelWidth
    //so they could be removed and replaced with just a getter
    public final int startX;
    public final int startZ;
    public final int endX;
    public final int endZ;
    //these 2 value indicate the center of the LodNode in real coordinate. This
    //can be used to calculate the distance from the player
    public final int centerX;
    public final int centerZ;

    /** highest point */
    public short height;

    /** lowest point */
    public short depth;

    /** The average color for the 6 cardinal directions */
    public Color color;

    public boolean real;
    public boolean voidNode;
    //if dirty is true, then this node have unsaved changes
    public boolean dirty;


    /**
     * Creates and empty LodDataPoint
     * This LodDataPoint only contains the position data
     * @param level of the node
     * @param posX position x in the level
     * @param posZ posizion z in the level
     */
    public LodNodeData(byte level, int posX, int posZ){
        this.level = level;
        this.posX = posX;
        this.posZ = posZ;
        width = (short) Math.pow(2, level);
        startX = posX * width;
        startZ = posZ * width;
        endX = startX + width - 1;
        endZ = startZ + width - 1;
        centerX = startX + width/2;
        centerZ = startZ + width/2;
        height = -1;
        depth = -1;
        color = INVISIBLE;
        real = false;
        dirty = true;
        voidNode = true;
    }

    /**
     * Constructor for a LodNodeData
     * @param level level of this
     * @param posX
     * @param posZ
     * @param height
     * @param depth
     * @param color
     * @param real
     */
    public LodNodeData(byte level, int posX, int posZ, short height, short depth, Color color, boolean real){
        this.level = level;
        this.posX = posX;
        this.posZ = posZ;
        width = (short) Math.pow(2, level);
        startX = posX * width;
        startZ = posZ * width;
        endX = startX + width - 1;
        endZ = startZ + width - 1;
        centerX = startX + width/2;
        centerZ = startZ + width/2;
        this.height = height;
        this.depth = depth;
        this.color = color;
        this.real = real;
        dirty = true;
        voidNode = false;
    }

    public LodNodeData(byte level, int posX, int posZ, int height, int depth, Color color, boolean real)    {
        this(level, posX, posZ, (short) height,(short) depth, color, real);
    }

    public LodNodeData(String data)
    {
        int index = 0;
        int lastIndex = 0;

        index = data.indexOf(DATA_DELIMITER, 0);
        this.level = (byte) Integer.parseInt(data.substring(0,index));

        lastIndex = index;
        index = data.indexOf(DATA_DELIMITER, lastIndex+1);
        this.posX = Integer.parseInt(data.substring(lastIndex+1,index));

        lastIndex = index;
        index = data.indexOf(DATA_DELIMITER, lastIndex+1);
        this.posZ = Integer.parseInt(data.substring(lastIndex+1,index));

        lastIndex = index;
        index = data.indexOf(DATA_DELIMITER, lastIndex+1);
        this.height = (short) Integer.parseInt(data.substring(lastIndex+1,index));

        lastIndex = index;
        index = data.indexOf(DATA_DELIMITER, lastIndex+1);
        this.depth = (short) Integer.parseInt(data.substring(lastIndex+1,index));

        lastIndex = index;
        index = data.indexOf(DATA_DELIMITER, lastIndex+1);
        int r = Integer.parseInt(data.substring(lastIndex+1,index));
        lastIndex = index;
        index = data.indexOf(DATA_DELIMITER, lastIndex+1);
        int g = Integer.parseInt(data.substring(lastIndex+1,index));
        lastIndex = index;
        index = data.indexOf(DATA_DELIMITER, lastIndex+1);
        int b = Integer.parseInt(data.substring(lastIndex+1,index));
        lastIndex = index;
        index = data.indexOf(DATA_DELIMITER, lastIndex+1);
        int a = Integer.parseInt(data.substring(lastIndex+1,index));
        this.color = new Color(r,g,b,a);

        int val = Integer.parseInt(data.substring(lastIndex+1,index));
        this.real = (val == 1);
        width = (short) Math.pow(2, level);

        val = Integer.parseInt(data.substring(lastIndex+1,index));
        this.voidNode = (val == 1);
        startX = posX * width;
        startZ = posZ * width;
        endX = startX + width - 1;
        endZ = startZ + width - 1;
        centerX = startX + width/2;
        centerZ = startZ + width/2;
        dirty = false;
    }

    public void update(LodNodeData lodNodeData){
        this.height = lodNodeData.height;
        this.depth = lodNodeData.depth;
        this.color = lodNodeData.color;
        this.real = lodNodeData.real;
        dirty = true;
    }

    public void combineData(List<LodNodeData> dataList){
        if(dataList.isEmpty()){
            height = -1;
            depth = -1;
            color = INVISIBLE;
        }else {
            short height = (short) dataList.stream().mapToInt(x -> (int) x.height).min().getAsInt();
            short depth = (short) dataList.stream().mapToInt(x -> (int) x.depth).max().getAsInt();
            height = height;
            depth = depth;
            color = dataList.get(0).color;
            real = dataList.stream().filter(x -> x.real).count() == 4;
            voidNode = dataList.stream().filter(x -> !x.voidNode).count() == 0;
        }
        dirty = true;
    }



    public int hashCode(){
        return Objects.hash(this.real, this.level, this.posX, this.posZ, this.color, this.real, this.voidNode);
    }


    public boolean equals(LodNodeData other){
        return (this.real == other.real
                && this.level == other.level
                && this.posX == other.posX
                && this.posZ == other.posZ
                && this.color.equals(other.color)
                && this.real == other.real
                && this.voidNode == other.voidNode);
    }


    /**
     * Outputs all data in a csv format
     */
    public String toData(){
        String s = Integer.toString(level) + DATA_DELIMITER
                + posX + DATA_DELIMITER
                + posZ + DATA_DELIMITER
                + Integer.toString(height) + DATA_DELIMITER
                + Integer.toString(depth) + DATA_DELIMITER
                + color.getRed() + DATA_DELIMITER
                + color.getGreen() + DATA_DELIMITER
                + color.getBlue() + DATA_DELIMITER
                + color.getAlpha() + DATA_DELIMITER;
        int val = real ? 1 : 0;
        s += val + DATA_DELIMITER;
        val = voidNode ? 1 : 0;
        s += val + DATA_DELIMITER;
        return s;
    }



    public String toString()
    {
        return this.toData();
    }
}
