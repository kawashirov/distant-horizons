package com.seibel.lod.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThreadMapUtil
{
	public static final ConcurrentMap<String, long[]> threadSingleUpdateMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, long[][]> threadBuilderArrayMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, long[][]> threadBuilderVerticalArrayMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, long[]> threadVerticalAddDataMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, byte[]> saveContainer = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, short[]> projectionShortMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, short[]> heightAndDepthMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, long[]> singleDataToMergeMap = new ConcurrentHashMap<>();

	public static long[] getSingleUpdateArray()
	{
		if (!threadSingleUpdateMap.containsKey(Thread.currentThread().getName()) || (threadSingleUpdateMap.get(Thread.currentThread().getName()) == null))
		{
			threadSingleUpdateMap.put(Thread.currentThread().getName(), new long[4]);
		}
		return threadSingleUpdateMap.get(Thread.currentThread().getName());
	}

	public static long[][] getBuilderArray()
	{
		if (!threadBuilderArrayMap.containsKey(Thread.currentThread().getName()) || (threadBuilderArrayMap.get(Thread.currentThread().getName()) == null))
		{
			long[][] array = new long[5][];
			threadBuilderArrayMap.put(Thread.currentThread().getName(), array);
		}
		return threadBuilderArrayMap.get(Thread.currentThread().getName());
	}

	public static long[][] getBuilderVerticalArray()
	{
		if (!threadBuilderVerticalArrayMap.containsKey(Thread.currentThread().getName()) || (threadBuilderVerticalArrayMap.get(Thread.currentThread().getName()) == null))
		{
			long[][] array = new long[5][];
			for(int i = 0; i < array.length; i++)
			{
				int size = 1 << i;
				array[i] = new long[size * size * DataPointUtil.WORLD_HEIGHT];
			}
			threadBuilderVerticalArrayMap.put(Thread.currentThread().getName(), array);
		}
		return threadBuilderVerticalArrayMap.get(Thread.currentThread().getName());
	}

	public static long[] verticalDataArray(int count)
	{
		if (!threadVerticalAddDataMap.containsKey(Thread.currentThread().getName()) || (threadVerticalAddDataMap.get(Thread.currentThread().getName()) == null))
		{
			threadVerticalAddDataMap.put(Thread.currentThread().getName(), new long[count]);
		}
		for(int i = 0; i < threadVerticalAddDataMap.get(Thread.currentThread().getName()).length ; i++)
		{
			threadVerticalAddDataMap.get(Thread.currentThread().getName())[i] = DataPointUtil.EMPTY_DATA;
		}
		return threadVerticalAddDataMap.get(Thread.currentThread().getName());
	}

	public static short[] getProjectionShort(int size){
		if(!projectionShortMap.containsKey(Thread.currentThread().getName()) || (projectionShortMap.get(Thread.currentThread().getName()) == null) || (projectionShortMap.get(Thread.currentThread().getName()).length != size))
		{
			projectionShortMap.put(Thread.currentThread().getName(), new short[size]);
		}
		return projectionShortMap.get(Thread.currentThread().getName());
	}

	public static short[] getHeightAndDepth(int size){
		if(!heightAndDepthMap.containsKey(Thread.currentThread().getName()) || (heightAndDepthMap.get(Thread.currentThread().getName()) == null) || (heightAndDepthMap.get(Thread.currentThread().getName()).length != size))
		{
			heightAndDepthMap.put(Thread.currentThread().getName(), new short[size]);
		}
		return heightAndDepthMap.get(Thread.currentThread().getName());
	}

	public static byte[] getSaveContainer(int size){
		if(!saveContainer.containsKey(Thread.currentThread().getName()) || (saveContainer.get(Thread.currentThread().getName()) == null) || (saveContainer.get(Thread.currentThread().getName()).length != size))
		{
			saveContainer.put(Thread.currentThread().getName(), new byte[size]);
		}
		return saveContainer.get(Thread.currentThread().getName());
	}

	public static long[] getSingleAddDataToMerge(int size){
		if(!singleDataToMergeMap.containsKey(Thread.currentThread().getName()) || (singleDataToMergeMap.get(Thread.currentThread().getName()) == null) || (singleDataToMergeMap.get(Thread.currentThread().getName()).length != size))
		{
			singleDataToMergeMap.put(Thread.currentThread().getName(), new long[size]);
		}
		return singleDataToMergeMap.get(Thread.currentThread().getName());
	}
}
