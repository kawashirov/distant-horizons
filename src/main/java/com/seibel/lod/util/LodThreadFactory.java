package com.seibel.lod.util;

import java.util.concurrent.ThreadFactory;

/**
 * Just a simple ThreadFactory to name ExecutorService
 * threads, which can be helpful when debugging.
 * 
 * @author James Seibel
 * @version 8-15-2021
 */
public class LodThreadFactory implements ThreadFactory
{
	public String threadName;
	
	
	public LodThreadFactory(String newThreadName)
	{
		threadName = newThreadName + " Thread";
	}
	
	@Override
	public Thread newThread(Runnable r)
	{
            return new Thread(r, threadName);
	}
	
}
