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

import com.seibel.lod.core.a7.util.UncheckedInterruptedException;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.enums.config.ELightGenerationMode;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.objects.DHChunkPos;
import com.seibel.lod.core.util.EventTimer;
import com.seibel.lod.core.util.gridList.ArrayGridList;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.worldGeneration.AbstractBatchGenerationEnvionmentWrapper.Steps;

import org.apache.logging.log4j.Logger;

//======================= Main Event class======================
public final class GenerationEvent
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
	private static int generationFutureDebugIDs = 0;
	final int id;
	final ThreadedParameters tParam;
	final DHChunkPos minPos;
	final int size;
	final Steps target;
	final ELightGenerationMode lightMode;
	final double runTimeRatio;
	EventTimer timer = null;
	long inQueueTime;
	long timeoutTime = -1;
	public CompletableFuture<Void> future = null;
	final Consumer<IChunkWrapper> resultConsumer;

	public GenerationEvent(DHChunkPos minPos, int size, BatchGenerationEnvironment generationGroup,
						   Steps target, double runTimeRatio, Consumer<IChunkWrapper> resultConsumer) {
		inQueueTime = System.nanoTime();
		this.id = generationFutureDebugIDs++;
		this.minPos = minPos;
		this.size = size;
		this.target = target;
		this.tParam = ThreadedParameters.getOrMake(generationGroup.params);
		this.lightMode = Config.Client.WorldGenerator.lightGenerationMode.get();
		this.runTimeRatio = runTimeRatio;
		this.resultConsumer = resultConsumer;
	}

	public static GenerationEvent startEvent(DHChunkPos minPos, int size, BatchGenerationEnvironment generationGroup,
                                             Steps target, double runTimeRatio, Consumer<IChunkWrapper> resultConsumer)
	{
		if (size % 2 == 0) size += 1; // size must be odd for vanilla world gen region to work
		GenerationEvent event = new GenerationEvent(minPos, size, generationGroup, target, runTimeRatio, resultConsumer);
		event.future = CompletableFuture.runAsync(() ->
				{
					long runStartTime = System.nanoTime();
					event.timeoutTime = runStartTime;
					event.inQueueTime = runStartTime - event.inQueueTime;
					event.timer = new EventTimer("setup");
					BatchGenerationEnvironment.isDistantGeneratorThread.set(true);
					try {
						generationGroup.generateLodFromList(event);
					} finally {
						BatchGenerationEnvironment.isDistantGeneratorThread.remove();
						if (!Thread.interrupted() && runTimeRatio < 1.0) {
							long endTime = System.nanoTime();
							try {
								long deltaMs = TimeUnit.NANOSECONDS.toMillis(endTime - runStartTime);
								Thread.sleep((long) (deltaMs/runTimeRatio - deltaMs));
							} catch (InterruptedException ignored) {}
						}
					}
				}, generationGroup.executors);
		return event;
	}

	public boolean isComplete()
	{
		return future.isDone();
	}

	public boolean hasTimeout(int duration, TimeUnit unit)
	{
		if (timeoutTime == -1) return false;
		long currentTime = System.nanoTime();
		long delta = currentTime - timeoutTime;
		return (delta > TimeUnit.NANOSECONDS.convert(duration, unit));
	}

	public boolean terminate()
	{
		LOGGER.info("======================DUMPING ALL THREADS FOR WORLD GEN=======================");
		BatchGenerationEnvironment.threadFactory.dumpAllThreadStacks();
		future.cancel(true);
		return future.isCancelled();
	}
	
	public boolean tooClose(int minX, int minZ, int w)
	{
		int aMinX = minPos.x;
		int aMinZ = minPos.z;
		int aSize = size;
		// Account for required empty chunks in the border
		aSize += 1;
		w+= 1;
		// Do a AABB to AABB intersection test
		return (aMinX + aSize >= minX &&
				aMinX <= minX + w &&
				aMinZ + aSize >= minZ &&
				aMinZ <= minZ + w);
	}
	
	public void refreshTimeout()
	{
		timeoutTime = System.nanoTime();
		UncheckedInterruptedException.throwIfInterrupted();
	}
	
	@Override
	public String toString()
	{
		return id + ":" + size + "@" + minPos + "(" + target + ")";
	}
}