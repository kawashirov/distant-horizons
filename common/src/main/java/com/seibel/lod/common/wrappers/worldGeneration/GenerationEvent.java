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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.seibel.lod.common.wrappers.worldGeneration.BatchGenerationEnvironment.PrefEvent;
import com.seibel.lod.core.enums.config.ELightGenerationMode;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.worldGeneration.AbstractBatchGenerationEnvionmentWrapper.Steps;

import net.minecraft.world.level.ChunkPos;
import org.apache.logging.log4j.Logger;

//======================= Main Event class======================
public final class GenerationEvent
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	
	private static int generationFutureDebugIDs = 0;
	final ThreadedParameters tParam;
	final ChunkPos pos;
	final int range;
	final Future<?> future;
	long creationNanotime;
	final int id;
	final Steps target;
	final ELightGenerationMode lightMode;
	final PrefEvent pEvent = new PrefEvent();
	final boolean genAllDetails;

	final double runTimeRatio;
	
	public GenerationEvent(ChunkPos pos, int range, BatchGenerationEnvironment generationGroup,
						   Steps target, boolean genAllDetails, double runTimeRatio)
	{
		creationNanotime = System.nanoTime();
		this.pos = pos;
		this.range = range;
		id = generationFutureDebugIDs++;
		this.target = target;
		this.tParam = ThreadedParameters.getOrMake(generationGroup.params);
		ELightGenerationMode mode = CONFIG.client().worldGenerator().getLightGenerationMode();
		
		this.lightMode = mode;
		this.genAllDetails = genAllDetails;
		this.runTimeRatio = runTimeRatio;
		
		future = generationGroup.executors.submit(() ->
		{
			long startTime = System.nanoTime();
			BatchGenerationEnvironment.isDistantGeneratorThread.set(true);
			try {
				generationGroup.generateLodFromList(this);
			} finally {
				BatchGenerationEnvironment.isDistantGeneratorThread.remove();
				if (!Thread.interrupted() && runTimeRatio < 1.0) {
					long endTime = System.nanoTime();
					try {
						long deltaMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
						Thread.sleep((long) (deltaMs/runTimeRatio - deltaMs));
					} catch (InterruptedException ignored) {
					}
				}
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
		long delta = currentTime - creationNanotime;
		return (delta > TimeUnit.NANOSECONDS.convert(duration, unit));
	}
	
	public boolean terminate()
	{
		LOGGER.info("======================DUMPING ALL THREADS FOR WORLD GEN=======================");
		BatchGenerationEnvironment.threadFactory.dumpAllThreadStacks();
		future.cancel(true);
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
		creationNanotime = System.nanoTime();
		LodUtil.checkInterruptsUnchecked();
	}
	
	@Override
	public String toString()
	{
		return id + ":" + range + "@" + pos + "(" + target + ")";
	}
}