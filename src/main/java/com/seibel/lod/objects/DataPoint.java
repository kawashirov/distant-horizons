package com.seibel.lod.objects;

public class DataPoint
{

    public static short[] createDataPoint(int height, int depth, int red, int green, int blue){
        return new short[]{(short) height, (short) depth, (short) red, (short) green, (short) blue};
    }

    public static short getHeight(short[] dataPoint){
        return dataPoint[0];
    }

    public static short getDepth(short[] dataPoint){
        return dataPoint[1];
    }

    public static short getRed(short[] dataPoint){
        return dataPoint[2];
    }

    public static short getGreen(short[] dataPoint){
        return dataPoint[3];
    }

    public static short getBlue(short[] dataPoint){
        return dataPoint[4];
    }

    public static short[] getHeightDepth(short[] dataPoint){
        return new short[]{dataPoint[0], dataPoint[1]};
    }

    public static int getColor(short[] dataPoint){
        int R = (dataPoint[2] << 16) & 0x00FF0000;
        int G = (dataPoint[3] << 8) & 0x0000FF00;
        int B = dataPoint[4] & 0x000000FF;
        return 0xFF000000 | R | G | B;
    }
}
