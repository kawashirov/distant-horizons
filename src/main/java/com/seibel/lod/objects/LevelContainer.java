package com.seibel.lod.objects;

import java.io.Serializable;

import com.seibel.lod.util.LodUtil;

public class LevelContainer implements Serializable
{

    public static final char DATA_DELIMITER = ',';

    public final byte detailLevel;

    public final byte[][][] colors;

    public final short[][] height;

    public final short[][] depth;

    public final byte[][] generationType;

    public final boolean[][] dataExistence;

    public LevelContainer(byte detailLevel, byte[][][] colors, short[][] height, short[][] depth, byte[][] generationType, boolean[][] dataExistence)
    {
        this.detailLevel = detailLevel;
        this.colors = colors;
        this.height = height;
        this.depth = depth;
        this.generationType = generationType;
        this.dataExistence = dataExistence;
    }

    public LevelContainer(String data)
    {

        int index = 0;
        int lastIndex = 0;


        index = data.indexOf(DATA_DELIMITER, 0);
        this.detailLevel = (byte) Integer.parseInt(data.substring(0, index));
        int size = (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel);

        this.colors = new byte[size][size][3];
        this.height = new short[size][size];
        this.depth = new short[size][size];
        this.generationType = new byte[size][size];
        this.dataExistence = new boolean[size][size];
        int intCol;
        for (int x = 0; x < size; x++)
        {
            for (int z = 0; z < size; z++)
            {
                lastIndex = index;
                index = data.indexOf(DATA_DELIMITER, lastIndex + 1);
                intCol = Integer.parseInt(data.substring(lastIndex + 1, index), 16);
                colors[x][z][0] = (byte) ((intCol >> 16) - 128);
                colors[x][z][1] = (byte) ((intCol >> 8) - 128);
                colors[x][z][2] = (byte) (intCol - 128);

                lastIndex = index;
                index = data.indexOf(DATA_DELIMITER, lastIndex + 1);
                height[x][z] = Short.parseShort(data.substring(lastIndex + 1, index), 16);

                lastIndex = index;
                index = data.indexOf(DATA_DELIMITER, lastIndex + 1);
                depth[x][z] = Short.parseShort(data.substring(lastIndex + 1, index), 16);

                lastIndex = index;
                index = data.indexOf(DATA_DELIMITER, lastIndex + 1);
                generationType[x][z] = Byte.parseByte(data.substring(lastIndex + 1, index), 16);

                lastIndex = index;
                index = data.indexOf(DATA_DELIMITER, lastIndex + 1);
                dataExistence[x][z] = Boolean.parseBoolean(data.substring(lastIndex + 1, index));
            }
        }

    }

    @Override
    public String toString()
    {
        StringBuilder stringBuilder = new StringBuilder();
        int combinedCol;
        int size = (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel);
        stringBuilder.append(detailLevel);
        stringBuilder.append(DATA_DELIMITER);
        for (int x = 0; x < size; x++)
        {
            for (int z = 0; z < size; z++)
            {
                //Converting the colors to intColor and then to HEX
                combinedCol = ((colors[x][z][0] + 128) << 16) | ((colors[x][z][1] + 128) << 8) | ((colors[x][z][2] + 128));
                stringBuilder.append(Integer.toHexString(combinedCol));
                stringBuilder.append(DATA_DELIMITER);
                stringBuilder.append(Integer.toHexString(height[x][z]));
                stringBuilder.append(DATA_DELIMITER);
                stringBuilder.append(Integer.toHexString(depth[x][z]));
                stringBuilder.append(DATA_DELIMITER);
                stringBuilder.append(Integer.toHexString(generationType[x][z]));
                stringBuilder.append(DATA_DELIMITER);
                stringBuilder.append(dataExistence[x][z]);
                stringBuilder.append(DATA_DELIMITER);
            }
        }
        return stringBuilder.toString();
    }
}
