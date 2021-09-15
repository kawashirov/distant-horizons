package com.seibel.lod;

import com.google.common.primitives.UnsignedLong;
import com.seibel.lod.objects.PosToGenerateContainer;
import com.seibel.lod.util.DataPointUtil;

import javax.xml.crypto.Data;
import java.math.BigInteger;

public class Main
{
	public static void main(String[] args)
	{
		try
		{
			long[][] dataToMerge = new long[][]{
					{DataPointUtil.createDataPoint(10, 5, 0, 0, 0, 0)},
					{DataPointUtil.createDataPoint(15, 5, 0, 0, 0, 0)},
					{DataPointUtil.createDataPoint(40, 20, 0, 0, 0, 0)},
					{DataPointUtil.createDataPoint(1, 0, 0, 0, 0, 0)}};
			long[] data = DataPointUtil.mergeVerticalData(dataToMerge);
			for (long dataPoint : data)
			{
				System.out.println("depth " + DataPointUtil.getDepth(dataPoint));
				System.out.println("height " + DataPointUtil.getHeight(dataPoint));
			}
		}catch (Exception e){
			e.printStackTrace();
		}
	}
}
