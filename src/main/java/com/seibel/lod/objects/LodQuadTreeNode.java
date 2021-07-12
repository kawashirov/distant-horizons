package com.seibel.lod.objects;

import com.seibel.lod.handlers.LodQuadTreeDimensionFileHandler;

import java.awt.*;
import java.util.*;
import java.util.List;

public class LodQuadTreeNode {
    /** This is what separates each piece of data in the toData method */
    private static final char DATA_DELIMITER = LodQuadTreeDimensionFileHandler.DATA_DELIMITER;



    /** this is how many pieces of data are exported when toData is called */
    public static final int NUMBER_OF_DELIMITERS = 9;

    private static final Color INVISIBLE = new Color(0,0,0,0);

    //Complexity indicate how complex is this node. For example a node that has been generated starting
    //from a real chunk have the maximum complexity. A node that is generated starting from a fake approximated chunk
    //has a low complexity
    public byte complexity;

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

    public LodDataPoint lodDataPoint;

    //void node is used
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
    public LodQuadTreeNode(byte level, int posX, int posZ){
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
        lodDataPoint = new LodDataPoint()
        real = false;
        dirty = true;
        voidNode = true;
    }

    /**
     * Constructor for a LodNodeData
     * @param level level of this
     * @param posX
     * @param posZ
     * @param complexity
     */
    public LodQuadTreeNode(byte level, int posX, int posZ, LodDataPoint lodDataPoint, byte complexity){
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
        this.lodDataPoint = lodDataPoint;
        this.real = real;
        dirty = true;
        voidNode = false;
    }

    public LodQuadTreeNode(String data)
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

        int complexity = Integer.parseInt(data.substring(lastIndex+1,index));
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

    public void update(LodQuadTreeNode lodQuadTreeNode){
        this.lodDataPoint = lodQuadTreeNode.lodDataPoint
        this.complexity = lodQuadTreeNode.complexity;
        this.voidNode = lodQuadTreeNode.voidNode;
        dirty = true;
    }

    public LodDataPoint getLodDataPoint(){
        return lodDataPoint;
    }
    public byte getComplexity(){
        return complexity;
    }

    public void combineData(List<LodQuadTreeNode> dataList){
        if(dataList.isEmpty()){
            lodDataPoint = new LodDataPoint();
        }else {
            short height = (short) dataList.stream().mapToInt(x -> (int) x.getLodDataPoint().height).min().getAsInt();
            short depth = (short) dataList.stream().mapToInt(x -> (int) x.getLodDataPoint().depth).max().getAsInt();
            height = height;
            depth = depth;
            int red= dataList.stream().mapToInt(x -> x.getLodDataPoint().color.getRed()).sum()/dataList.size();
            int green= dataList.stream().mapToInt(x -> x.getLodDataPoint().color.getGreen()).sum()/dataList.size();
            int blue = dataList.stream().mapToInt(x -> x.getLodDataPoint().color.getBlue()).sum()/dataList.size();
            Color color = new Color(red,green,blue);
            lodDataPoint = new LodDataPoint(height,depth,color);
            complexity = (byte) dataList.stream().mapToInt(x -> x.complexity).max().getAsInt();
            voidNode = dataList.stream().filter(x -> !x.voidNode).count() == 0;
        }
        dirty = true;
    }


    public int hashCode(){
        return Objects.hash(this.complexity, this.level, this.posX, this.posZ, this.color, this.real, this.voidNode);
    }


    public boolean equals(LodQuadTreeNode other){
        return (this.complexity == other.complexity
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
