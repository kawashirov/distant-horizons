/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2023 James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.distanthorizons.common.wrappers.worldGeneration;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiWorldGenerationStep;
import com.seibel.distanthorizons.core.generation.WorldGenerationQueue;
import com.seibel.distanthorizons.core.util.ThreadUtil;
import com.seibel.distanthorizons.core.util.objects.UncheckedInterruptedException;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.util.objects.EventTimer;
import com.seibel.distanthorizons.core.util.threading.ThreadPools;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;

import org.apache.logging.log4j.Logger;

public final class GenerationEvent
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
	private static int generationFutureDebugIDs = 0;
	
	public final int id;
	public final ThreadedParameters threadedParam;
	public final DhChunkPos minPos;
	public final int size;
	public final EDhApiWorldGenerationStep targetGenerationStep;
	public EventTimer timer = null;
	public long inQueueTime;
	public long timeoutTime = -1;
	public CompletableFuture<Void> future = null;
	public final Consumer<IChunkWrapper> resultConsumer;
	
	
	
	public GenerationEvent(
			DhChunkPos minPos, int size, BatchGenerationEnvironment generationGroup,
			EDhApiWorldGenerationStep targetGenerationStep, Consumer<IChunkWrapper> resultConsumer)
	{
		this.inQueueTime = System.nanoTime();
		this.id = generationFutureDebugIDs++;
		this.minPos = minPos;
		this.size = size;
		this.targetGenerationStep = targetGenerationStep;
		this.threadedParam = ThreadedParameters.getOrMake(generationGroup.params);
		this.resultConsumer = resultConsumer;
	}
	
	
	
	public static GenerationEvent startEvent(
			DhChunkPos minPos, int size, BatchGenerationEnvironment genEnvironment,
			EDhApiWorldGenerationStep target, Consumer<IChunkWrapper> resultConsumer,
			ExecutorService worldGeneratorThreadPool)
	{
		if (size % 2 == 0)
		{
			size += 1; // size must be odd for vanilla world gen regions to work
		}
		
		
		GenerationEvent generationEvent = new GenerationEvent(minPos, size, genEnvironment, target, resultConsumer);
		generationEvent.future = CompletableFuture.runAsync(() ->
		{
			long runStartTime = System.nanoTime();
			generationEvent.timeoutTime = runStartTime;
			generationEvent.inQueueTime = runStartTime - generationEvent.inQueueTime;
			generationEvent.timer = new EventTimer("setup");
			
			BatchGenerationEnvironment.isDistantGeneratorThread.set(true);
			try
			{
				//LOGGER.info("generating [{}]", event.minPos);
				genEnvironment.generateLodFromList(generationEvent);
			}
			catch (InterruptedException ignored)
			{
			}
			finally
			{
				BatchGenerationEnvironment.isDistantGeneratorThread.remove();
			}
		}, worldGeneratorThreadPool);
		return generationEvent;
	}
	
	public boolean isComplete() { return this.future.isDone(); }
	
	public boolean hasTimeout(int duration, TimeUnit unit)
	{
		if (this.timeoutTime == -1)
		{
			return false;
		}
		
		long currentTime = System.nanoTime();
		long delta = currentTime - this.timeoutTime;
		return (delta > TimeUnit.NANOSECONDS.convert(duration, unit));
	}
	
	public boolean terminate()
	{
		LOGGER.info("======================DUMPING ALL THREADS FOR WORLD GEN=======================");
		ThreadPools.WORLD_GEN_THREAD_FACTORY.dumpAllThreadStacks();
		this.future.cancel(true);
		return this.future.isCancelled();
	}
	
	public boolean tooClose(int minX, int minZ, int width)
	{
		int aMinX = this.minPos.x;
		int aMinZ = this.minPos.z;
		int aSize = this.size;
		// Account for required empty chunks in the border
		aSize += 1;
		width += 1;
		// Do a AABB to AABB intersection test
		return (aMinX + aSize >= minX &&
				aMinX <= minX + width &&
				aMinZ + aSize >= minZ &&
				aMinZ <= minZ + width);
	}
	
	public void refreshTimeout()
	{
		this.timeoutTime = System.nanoTime();
		UncheckedInterruptedException.throwIfInterrupted();
	}
	
	@Override
	public String toString() { return this.id + ":" + this.size + "@" + this.minPos + "(" + this.targetGenerationStep + ")"; }
	
}