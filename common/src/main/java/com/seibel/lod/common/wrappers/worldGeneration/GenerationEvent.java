
package com.seibel.lod.common.wrappers.worldGeneration;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.seibel.lod.common.wrappers.worldGeneration.BatchGenerationEnvironment.PrefEvent;
import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.enums.config.LightGenerationMode;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.worldGeneration.AbstractBatchGenerationEnvionmentWrapper.Steps;

import net.minecraft.world.level.ChunkPos;

//======================= Main Event class======================
public final class GenerationEvent
{
	static private final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	
	private static int generationFutureDebugIDs = 0;
	final ThreadedParameters tParam;
	final ChunkPos pos;
	final int range;
	final Future<?> future;
	long nanotime;
	final int id;
	final Steps target;
	final LightGenerationMode lightMode;
	final PrefEvent pEvent = new PrefEvent();
	final boolean genAllDetails;
	
	public GenerationEvent(ChunkPos pos, int range, BatchGenerationEnvironment generationGroup, Steps target, boolean genAllDetails)
	{
		nanotime = System.nanoTime();
		this.pos = pos;
		this.range = range;
		id = generationFutureDebugIDs++;
		this.target = target;
		this.tParam = ThreadedParameters.getOrMake(generationGroup.params);
		LightGenerationMode mode = CONFIG.client().worldGenerator().getLightGenerationMode();
		
		this.lightMode = mode;
		this.genAllDetails = genAllDetails;
		
		future = generationGroup.executors.submit(() ->
		{
			BatchGenerationEnvironment.isDistantGeneratorThread.set(true);
			try {
				generationGroup.generateLodFromList(this);
			} finally {
				BatchGenerationEnvironment.isDistantGeneratorThread.remove();
			}
		});
	}
	
	public boolean isCompleted()
	{
		return future.isDone();
	}
	
	public boolean hasTimeout(int duration, TimeUnit unit)
	{
		long currentTime = System.nanoTime();
		long delta = currentTime - nanotime;
		return (delta > TimeUnit.NANOSECONDS.convert(duration, unit));
	}
	
	public boolean terminate()
	{
		future.cancel(true);
		ClientApi.LOGGER.info("======================DUMPING ALL THREADS FOR WORLD GEN=======================");
		BatchGenerationEnvironment.threadFactory.dumpAllThreadStacks();
		return future.isCancelled();
	}
	
	public void join()
	{
		try
		{
			future.get();
		}
		catch (InterruptedException | ExecutionException e)
		{
			throw new RuntimeException(e.getCause()==null? e : e.getCause());
		}
	}
	
	public boolean tooClose(int cx, int cz, int cr)
	{
		int distX = Math.abs(cx - pos.x);
		int distZ = Math.abs(cz - pos.z);
		int minRange = cr + range + 1; // Need one to account for the center
		minRange += 1 + 1; // Account for required empty chunks
		return distX < minRange && distZ < minRange;
	}
	
	public void refreshTimeout()
	{
		nanotime = System.nanoTime();
	}
	
	@Override
	public String toString()
	{
		return id + ":" + range + "@" + pos + "(" + target + ")";
	}
}