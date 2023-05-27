/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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

package com.seibel.lod.common.wrappers.worldGeneration;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.seibel.lod.api.enums.worldGeneration.EDhApiWorldGenerationStep;
import com.seibel.lod.core.util.objects.UncheckedInterruptedException;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.api.enums.config.ELightGenerationMode;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.DhChunkPos;
import com.seibel.lod.core.util.objects.EventTimer;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;

import org.apache.logging.log4j.Logger;

public final class GenerationEvent
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
	private static int generationFutureDebugIDs = 0;
	
	final int id;
	final ThreadedParameters threadedParam;
	final DhChunkPos minPos;
	final int size;
	final EDhApiWorldGenerationStep targetGenerationStep;
	final ELightGenerationMode lightMode;
	final double runTimeRatio;
	EventTimer timer = null;
	long inQueueTime;
	long timeoutTime = -1;
	public CompletableFuture<Void> future = null;
	final Consumer<IChunkWrapper> resultConsumer;
	
	
	
	public GenerationEvent(DhChunkPos minPos, int size, BatchGenerationEnvironment generationGroup,
							EDhApiWorldGenerationStep targetGenerationStep, double runTimeRatio, Consumer<IChunkWrapper> resultConsumer)
	{
		this.inQueueTime = System.nanoTime();
		this.id = generationFutureDebugIDs++;
		this.minPos = minPos;
		this.size = size;
		this.targetGenerationStep = targetGenerationStep;
		this.threadedParam = ThreadedParameters.getOrMake(generationGroup.params);
		this.lightMode = Config.Client.WorldGenerator.lightGenerationMode.get();
		this.runTimeRatio = runTimeRatio;
		this.resultConsumer = resultConsumer;
	}
	
	
	
	public static GenerationEvent startEvent(DhChunkPos minPos, int size, BatchGenerationEnvironment generationGroup,
											EDhApiWorldGenerationStep target, double runTimeRatio, Consumer<IChunkWrapper> resultConsumer)
	{
		if (size % 2 == 0)
		{
			size += 1; // size must be odd for vanilla world gen regions to work
		}
		
		GenerationEvent generationEvent = new GenerationEvent(minPos, size, generationGroup, target, runTimeRatio, resultConsumer);
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
				generationGroup.generateLodFromList(generationEvent);
			}
			catch (InterruptedException ignored) { }
			finally
			{
				BatchGenerationEnvironment.isDistantGeneratorThread.remove();
				if (!Thread.interrupted() && runTimeRatio < 1.0)
				{
					long endTime = System.nanoTime();
					try
					{
						long deltaMs = TimeUnit.NANOSECONDS.toMillis(endTime - runStartTime);
						Thread.sleep((long) (deltaMs / runTimeRatio - deltaMs));
					}
					catch (InterruptedException ignored)
					{
					}
				}
			}
		}, generationGroup.executorService);
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
		BatchGenerationEnvironment.threadFactory.dumpAllThreadStacks();
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
	public String toString() { return this.id+":"+this.size+"@"+this.minPos+"("+this.targetGenerationStep +")"; }
	
}