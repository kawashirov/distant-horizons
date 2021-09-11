package com.seibel.lod.util;

import com.seibel.lod.objects.LevelContainer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThreadMapUtil
{
	public static final ConcurrentMap<String,long[]> threadSingleAddDataMap = new ConcurrentHashMap();
	public static final ConcurrentMap<String,long[]> threadSingleGetDataMap = new ConcurrentHashMap();
	public static final ConcurrentMap<String,long[]> threadVerticalAddDataMap = new ConcurrentHashMap();
	public static final ConcurrentMap<String,long[]> threadVerticalGetDataMap = new ConcurrentHashMap();
	public static final ConcurrentMap<String,long[]> threadSingleUpdateMap = new ConcurrentHashMap();
	public static final ConcurrentMap<String,long[][][]> threadVerticalUpdateMap = new ConcurrentHashMap();
	public static final ConcurrentMap<String,int[]> threadVerticalIndexesMap = new ConcurrentHashMap();

	public static long[] getSingleAddDataArray(){
		if(!threadSingleAddDataMap.containsKey(Thread.currentThread().getName()) || (threadSingleAddDataMap.get(Thread.currentThread().getName()) == null))
		{
			threadSingleAddDataMap.put(Thread.currentThread().getName(), new long[1]);
		}
		return threadSingleAddDataMap.get(Thread.currentThread().getName());
	}

	public static long[] getSingleGetDataArray(){
		if(!threadSingleGetDataMap.containsKey(Thread.currentThread().getName()) || (threadSingleGetDataMap.get(Thread.currentThread().getName()) == null))
		{
			threadSingleGetDataMap.put(Thread.currentThread().getName(), new long[1]);
		}
		return threadSingleGetDataMap.get(Thread.currentThread().getName());
	}

	public static long[] getSingleUpdateArray(){
		if(!threadSingleUpdateMap.containsKey(Thread.currentThread().getName()) || (threadSingleUpdateMap.get(Thread.currentThread().getName()) == null))
		{
			threadSingleUpdateMap.put(Thread.currentThread().getName(), new long[4]);
		}
		return threadSingleUpdateMap.get(Thread.currentThread().getName());
	}

	public static long[] addVerticalDataArray(){
		if(!threadVerticalAddDataMap.containsKey(Thread.currentThread().getName()) || (threadVerticalAddDataMap.get(Thread.currentThread().getName()) == null))
		{
			threadVerticalAddDataMap.put(Thread.currentThread().getName(), new long[16]);
		}
		return threadVerticalAddDataMap.get(Thread.currentThread().getName());
	}

	public static long[] getVerticalGetDataArray(){
		if(!threadVerticalGetDataMap.containsKey(Thread.currentThread().getName()) || (threadVerticalGetDataMap.get(Thread.currentThread().getName()) == null))
		{
			threadVerticalGetDataMap.put(Thread.currentThread().getName(), new long[16]);
		}
		return threadVerticalGetDataMap.get(Thread.currentThread().getName());
	}

	public static long[][][] getVerticalUpdateArray(){
		if(!threadVerticalUpdateMap.containsKey(Thread.currentThread().getName()) || (threadVerticalUpdateMap.get(Thread.currentThread().getName()) == null))
		{
			threadVerticalUpdateMap.put(Thread.currentThread().getName(), new long[4][4][16]);
		}
		return threadVerticalUpdateMap.get(Thread.currentThread().getName());
	}

	public static int[] getVerticalIndexesArray(){
		if(!threadVerticalIndexesMap.containsKey(Thread.currentThread().getName()) || (threadVerticalIndexesMap.get(Thread.currentThread().getName()) == null))
		{
			threadVerticalIndexesMap.put(Thread.currentThread().getName(), new int[4]);
		}
		return threadVerticalIndexesMap.get(Thread.currentThread().getName());
	}
}
