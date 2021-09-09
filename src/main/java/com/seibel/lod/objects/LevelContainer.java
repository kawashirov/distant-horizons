package com.seibel.lod.objects;

import com.seibel.lod.util.LevelPosUtil;
import com.seibel.lod.util.LodUtil;

public interface LevelContainer
{
	public static final char VERTICAL_DATA_DELIMITER = ',';
	public static final char DATA_DELIMITER = ',';

	public boolean putData(long[] data, int posX, int posZ);
	public long[] getData(int posX, int posZ);
	/**TODO could i use a static map from thread name to arrays to store these values?*/
	public long[] mergeData(long[][] dataArray, long[] newDataPoint, int[] indexes, long[] dataToCombine);
	public String toDataString();
}
