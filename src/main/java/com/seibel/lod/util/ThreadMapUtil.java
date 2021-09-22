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
	public static final ConcurrentMap<String, long[][]> verticalUpdate = new ConcurrentHashMap<>();

	public static long[] getSingleUpdateArray()
	{
		if (!threadSingleUpdateMap.containsKey(Thread.currentThread().getName()) || (threadSingleUpdateMap.get(Thread.currentThread().getName()) == null))
		{
			threadSingleUpdateMap.put(Thread.currentThread().getName(), new long[0]);
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
			threadBuilderVerticalArrayMap.put(Thread.currentThread().getName(), array);
		}
		return threadBuilderVerticalArrayMap.get(Thread.currentThread().getName());
	}

	public static long[] verticalDataArray()
	{
		if (!threadVerticalAddDataMap.containsKey(Thread.currentThread().getName()) || (threadVerticalAddDataMap.get(Thread.currentThread().getName()) == null))
		{
			threadVerticalAddDataMap.put(Thread.currentThread().getName(), new long[0]);
		}
		return threadVerticalAddDataMap.get(Thread.currentThread().getName());
	}

	public static short[] getProjectionShort(){
		if(!projectionShortMap.containsKey(Thread.currentThread().getName()) || (projectionShortMap.get(Thread.currentThread().getName()) == null))
		{
			projectionShortMap.put(Thread.currentThread().getName(), new short[0]);
		}
		return projectionShortMap.get(Thread.currentThread().getName());
	}

	public static short[] getHeightAndDepth(){
		if(!heightAndDepthMap.containsKey(Thread.currentThread().getName()) || (heightAndDepthMap.get(Thread.currentThread().getName()) == null))
		{
			heightAndDepthMap.put(Thread.currentThread().getName(), new short[0]);
		}
		return heightAndDepthMap.get(Thread.currentThread().getName());
	}

	public static byte[] getSaveContainer(){
		if(!saveContainer.containsKey(Thread.currentThread().getName()) || (saveContainer.get(Thread.currentThread().getName()) == null))
		{
			saveContainer.put(Thread.currentThread().getName(), new byte[0]);
		}
		return saveContainer.get(Thread.currentThread().getName());
	}

	public static long[][] getVerticalUpdateArray(){
		if(!verticalUpdate.containsKey(Thread.currentThread().getName()) || (verticalUpdate.get(Thread.currentThread().getName()) == null))
		{
			long[][] array = new long[10][];
			verticalUpdate.put(Thread.currentThread().getName(), array);
		}
		return verticalUpdate.get(Thread.currentThread().getName());
	}

	public static long[] getSingleAddDataToMerge(){
		if(!singleDataToMergeMap.containsKey(Thread.currentThread().getName()) || (singleDataToMergeMap.get(Thread.currentThread().getName()) == null))
		{
			singleDataToMergeMap.put(Thread.currentThread().getName(), new long[0]);
		}
		return singleDataToMergeMap.get(Thread.currentThread().getName());
	}

	public static void clearMaps(){
		threadSingleUpdateMap.clear();
		threadBuilderArrayMap.clear();
		threadBuilderVerticalArrayMap.clear();
		threadVerticalAddDataMap.clear();
		saveContainer.clear();
		projectionShortMap.clear();
		heightAndDepthMap.clear();
		singleDataToMergeMap.clear();
		verticalUpdate.clear();
	}
}
