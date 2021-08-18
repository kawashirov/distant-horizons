package com.seibel.lod.objects;

import java.io.Serializable;

public class LevelContainer implements Serializable {

    public final byte detailLevel;

    public final byte[][][] colors;

    public final short[][] height;

    public final short[][] depth;

    public final byte[][] generationType;

    public final boolean[][] dataExistence;

    public LevelContainer(byte detailLevel, byte[][][] colors, short[][] height, short[][] depth, byte[][] generationType, boolean[][] dataExistence){
        this.detailLevel = detailLevel;
        this.colors = colors;
        this.height = height;
        this.depth = depth;
        this.generationType = generationType;
        this.dataExistence = dataExistence;
    }
/*
    public LevelContainer(String data, byte detailLevel){
        int size = detailLevel*detailLevel;
    }

 */
}
