package com.seibel.lod.util;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Holds data used by specific threads so 
 * the data doesn't have to be recreated every
 * time it is needed.
 * 
 * @author Leonardo Amato
 * @version 9-25-2021
 */
public class ThreadMapUtil
{
	public static final ConcurrentMap<String, long[]> threadSingleUpdateMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, long[][]> threadBuilderArrayMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, long[][]> threadBuilderVerticalArrayMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, long[]> threadVerticalAddDataMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, byte[]> saveContainer = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, short[]> projectionArrayMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, short[]> heightAndDepthMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, long[]> singleDataToMergeMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, long[][]> verticalUpdate = new ConcurrentHashMap<>();
	
	
	
	/** returns the array filled with 0's */
	public static long[] getFreshSingleUpdateArray(int arrayLength)
	{
		long[] array = getSingleUpdateArray();
		array = clearOrCreateArray(array, arrayLength);
		return array;
	}
	public static long[] getSingleUpdateArray()
	{
		if (!threadSingleUpdateMap.containsKey(Thread.currentThread().getName()) || (threadSingleUpdateMap.get(Thread.currentThread().getName()) == null))
		{
			threadSingleUpdateMap.put(Thread.currentThread().getName(), new long[4]);
		}
		return threadSingleUpdateMap.get(Thread.currentThread().getName());
	}
	
	
	/** returns the array filled with 0's */
	public static long[] getFreshBuilderArray(int arrayLength, int detailLevel)
	{
		long[] array = getBuilderArray(detailLevel);
		array = clearOrCreateArray(array, arrayLength);
		return array;
	}
	public static long[] getBuilderArray(int detailLevel)
	{
		if (!threadBuilderArrayMap.containsKey(Thread.currentThread().getName()) || (threadBuilderArrayMap.get(Thread.currentThread().getName()) == null))
		{
			long[][] array = new long[5][];
			int size = 1;
			for (int i = 0; i < 5; i++)
			{
				array[i] = new long[size * size];
				size = size << 1;
			}
			threadBuilderArrayMap.put(Thread.currentThread().getName(), array);
		}
		return threadBuilderArrayMap.get(Thread.currentThread().getName())[detailLevel];
	}
	
	
	/** returns the array filled with 0's */
	public static long[] getFreshBuilderVerticalArray(int arrayLength, int detailLevel)
	{
		long[] array = getBuilderVerticalArray(detailLevel);
		array = clearOrCreateArray(array, arrayLength);
		return array;
	}
	public static long[] getBuilderVerticalArray(int detailLevel)
	{
		if (!threadBuilderVerticalArrayMap.containsKey(Thread.currentThread().getName()) || (threadBuilderVerticalArrayMap.get(Thread.currentThread().getName()) == null))
		{
			long[][] array = new long[5][];
			threadBuilderVerticalArrayMap.put(Thread.currentThread().getName(), array);
		}
		return threadBuilderVerticalArrayMap.get(Thread.currentThread().getName())[detailLevel];
	}
	
	
	/** returns the array filled with 0's */
	public static long[] getFreshVerticalDataArray(int arrayLength)
	{
		long[] array = getVerticalDataArray();
		array = clearOrCreateArray(array, arrayLength);
		return array;
	}
	public static long[] getVerticalDataArray()
	{
		if (!threadVerticalAddDataMap.containsKey(Thread.currentThread().getName()) || (threadVerticalAddDataMap.get(Thread.currentThread().getName()) == null))
		{
			threadVerticalAddDataMap.put(Thread.currentThread().getName(), new long[0]);
		}
		return threadVerticalAddDataMap.get(Thread.currentThread().getName());
	}
	
	
	
	/** returns the array filled with 0's */
	public static short[] getFreshProjectionArray(int arrayLength)
	{
		short[] array = getProjectionArray();
		array = clearOrCreateArray(array, arrayLength);
		return array;
	}
	public static short[] getProjectionArray()
	{
		if (!projectionArrayMap.containsKey(Thread.currentThread().getName()) || (projectionArrayMap.get(Thread.currentThread().getName()) == null))
		{
			projectionArrayMap.put(Thread.currentThread().getName(), new short[0]);
		}
		return projectionArrayMap.get(Thread.currentThread().getName());
	}
	
	
	/** returns the array filled with 0's */
	public static short[] getFreshHeightAndDepth(int arrayLength)
	{
		short[] array = getHeightAndDepth();
		array = clearOrCreateArray(array, arrayLength);
		return array;
	}
	public static short[] getHeightAndDepth()
	{
		if (!heightAndDepthMap.containsKey(Thread.currentThread().getName()) || (heightAndDepthMap.get(Thread.currentThread().getName()) == null))
		{
			heightAndDepthMap.put(Thread.currentThread().getName(), new short[0]);
		}
		return heightAndDepthMap.get(Thread.currentThread().getName());
	}
	
	
	/** returns the array filled with 0's */
	public static byte[] getFreshSaveContainer(int arrayLength)
	{
		byte[] array = getSaveContainer();
		array = clearOrCreateArray(array, arrayLength);
		return array;
	}
	public static byte[] getSaveContainer()
	{
		if (!saveContainer.containsKey(Thread.currentThread().getName()) || (saveContainer.get(Thread.currentThread().getName()) == null))
		{
			saveContainer.put(Thread.currentThread().getName(), new byte[0]);
		}
		return saveContainer.get(Thread.currentThread().getName());
	}
	
	/** returns the array filled with 0's */
	public static long[] getFreshVerticalUpdateArray(int arrayLength, int detailLevel)
	{
		long[] array = ThreadMapUtil.getVerticalUpdateArray(detailLevel);
		array = clearOrCreateArray(array, arrayLength);
		return array;
	}
	public static long[] getVerticalUpdateArray(int detailLevel)
	{
		if (!verticalUpdate.containsKey(Thread.currentThread().getName()) || (verticalUpdate.get(Thread.currentThread().getName()) == null))
		{
			long[][] array = new long[10][];
			verticalUpdate.put(Thread.currentThread().getName(), array);
		}
		return verticalUpdate.get(Thread.currentThread().getName())[detailLevel];
	}
	
	
	/** returns the array filled with 0's */
	public static long[] getFreshSingleAddDataToMerge(int arrayLength)
	{
		long[] array = getSingleAddDataToMerge();
		array = clearOrCreateArray(array, arrayLength);
		return array;
	}
	public static long[] getSingleAddDataToMerge()
	{
		if (!singleDataToMergeMap.containsKey(Thread.currentThread().getName()) || (singleDataToMergeMap.get(Thread.currentThread().getName()) == null))
		{
			singleDataToMergeMap.put(Thread.currentThread().getName(), new long[0]);
		}
		return singleDataToMergeMap.get(Thread.currentThread().getName());
	}
	
	
	
	
	/** clears all arrays so they will have to be rebuilt */
	public static void clearMaps()
	{
		threadSingleUpdateMap.clear();
		threadBuilderArrayMap.clear();
		threadBuilderVerticalArrayMap.clear();
		threadVerticalAddDataMap.clear();
		saveContainer.clear();
		projectionArrayMap.clear();
		heightAndDepthMap.clear();
		singleDataToMergeMap.clear();
		verticalUpdate.clear();
	}
	
	
	
	
	/** returns an array filled with 0's */
	private static long[] clearOrCreateArray(long[] array, int arrayLength)
	{
		if (array == null || array.length != arrayLength)
		{
			array = new long[arrayLength];
		}
		else
			Arrays.fill(array, 0);
		
		return array;
	}
	
	/** returns an array filled with 0's */
	@SuppressWarnings("unused")
	private static int[] clearOrCreateArray(int[] array, int arrayLength)
	{
		if (array == null || array.length != arrayLength)
			array = new int[arrayLength];
		else
			Arrays.fill(array, 0);
		
		return array;
	}
	
	/** returns an array filled with 0's */
	private static short[] clearOrCreateArray(short[] array, int arrayLength)
	{
		if (array == null || array.length != arrayLength)
			array = new short[arrayLength];
		else
			Arrays.fill(array, (short) 0);
		
		return array;
	}

	/** returns an array filled with 0's */
	private static byte[] clearOrCreateArray(byte[] array, int arrayLength)
	{
		if (array == null || array.length != arrayLength)
			array = new byte[arrayLength];
		else
			Arrays.fill(array, (byte) 0);
		
		return array;
	}
	
}
