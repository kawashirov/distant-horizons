package com.seibel.lod.objects;

import java.io.Serializable;

import com.seibel.lod.util.LodUtil;

public class LevelContainer implements Serializable
{

	/** This is here so that Eclipse doesn't complain */
	private static final long serialVersionUID = -4930855068717998385L;

	public static final char DATA_DELIMITER = ',';

	public final byte detailLevel;

	public final long[][] data;

	public LevelContainer(byte detailLevel, long[][] data)
	{
		this.detailLevel = detailLevel;
		this.data = data;
	}

	public LevelContainer(String inputString)
	{

		int index = 0;
		int lastIndex = 0;


		index = inputString.indexOf(DATA_DELIMITER, 0);
		this.detailLevel = (byte) Integer.parseInt(inputString.substring(0, index));
		int size = (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel);

		this.data = new long[size][size];
		for (int x = 0; x < size; x++)
		{
			for (int z = 0; z < size; z++)
			{
				lastIndex = index;
				index = inputString.indexOf(DATA_DELIMITER, lastIndex + 1);
				data[x][z] = Long.parseLong(inputString.substring(lastIndex + 1, index), 16);
			}
		}

	}

	@Override
	public String toString()
	{
		StringBuilder stringBuilder = new StringBuilder();
		int size = (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		stringBuilder.append(detailLevel);
		stringBuilder.append(DATA_DELIMITER);
		for (int x = 0; x < size; x++)
		{
			for (int z = 0; z < size; z++)
			{
				//Converting the dataToHex
				stringBuilder.append(Long.toHexString(data[x][z]));
				stringBuilder.append(DATA_DELIMITER);
			}
		}
		return stringBuilder.toString();
	}
}
