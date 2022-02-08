/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2021 Tom Lee (TomTheFurry)
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.common.wrappers.worldGeneration;

import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.builders.lodBuilding.LodBuilderConfig;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.enums.config.LightGenerationMode;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.util.GridList;
import com.seibel.lod.core.util.LodThreadFactory;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;
import com.seibel.lod.core.wrapperInterfaces.worldGeneration.AbstractBatchGenerationEnvionmentWrapper;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.seibel.lod.common.wrappers.chunk.ChunkWrapper;
import com.seibel.lod.common.wrappers.world.WorldWrapper;
import com.seibel.lod.common.wrappers.worldGeneration.mimicObject.ChunkLoader;
import com.seibel.lod.common.wrappers.worldGeneration.mimicObject.LightGetterAdaptor;
import com.seibel.lod.common.wrappers.worldGeneration.mimicObject.LightedWorldGenRegion;
import com.seibel.lod.common.wrappers.worldGeneration.mimicObject.WorldGenLevelLightEngine;
import com.seibel.lod.common.wrappers.worldGeneration.step.StepBiomes;
import com.seibel.lod.common.wrappers.worldGeneration.step.StepFeatures;
import com.seibel.lod.common.wrappers.worldGeneration.step.StepLight;
import com.seibel.lod.common.wrappers.worldGeneration.step.StepNoise;
import com.seibel.lod.common.wrappers.worldGeneration.step.StepStructureReference;
import com.seibel.lod.common.wrappers.worldGeneration.step.StepStructureStart;
import com.seibel.lod.common.wrappers.worldGeneration.step.StepSurface;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.lighting.LevelLightEngine;

/*
Total:                   3.135214124s
=====================================
Empty Chunks:            0.000558328s
StructureStart Step:     0.025177207s
StructureReference Step: 0.00189559s
Biome Step:              0.13789155s
Noise Step:              1.570347555s
Surface Step:            0.741238194s
Carver Step:             0.000009923s
Feature Step:            0.389072425s
Lod Generation:          0.269023348s
*/

public final class BatchGenerationEnvironment extends AbstractBatchGenerationEnvionmentWrapper
{
	public static final boolean ENABLE_PERF_LOGGING = false;
	public static final boolean ENABLE_EVENT_LOGGING = false;
	public static final boolean ENABLE_LOAD_EVENT_LOGGING = false;
	//TODO: Make actual proper support for StarLight
	
	public static class PrefEvent
	{
		long beginNano = 0;
		long emptyNano = 0;
		long structStartNano = 0;
		long structRefNano = 0;
		long biomeNano = 0;
		long noiseNano = 0;
		long surfaceNano = 0;
		long carverNano = 0;
		long featureNano = 0;
		long lightNano = 0;
		long endNano = 0;
		
		@Override
		public String toString()
		{
			return "beginNano: " + beginNano + ",\n" +
					"emptyNano: " + emptyNano + ",\n" +
					"structStartNano: " + structStartNano + ",\n" +
					"structRefNano: " + structRefNano + ",\n" +
					"biomeNano: " + biomeNano + ",\n" +
					"noiseNano: " + noiseNano + ",\n" +
					"surfaceNano: " + surfaceNano + ",\n" +
					"carverNano: " + carverNano + ",\n" +
					"featureNano: " + featureNano + ",\n" +
					"lightNano: " + lightNano + ",\n" +
					"endNano: " + endNano + "\n";
		}
	}
	
	public static class PerfCalculator
	{
		public static final int SIZE = 50;
		Rolling totalTime = new Rolling(SIZE);
		Rolling emptyTime = new Rolling(SIZE);
		Rolling structStartTime = new Rolling(SIZE);
		Rolling structRefTime = new Rolling(SIZE);
		Rolling biomeTime = new Rolling(SIZE);
		Rolling noiseTime = new Rolling(SIZE);
		Rolling surfaceTime = new Rolling(SIZE);
		Rolling carverTime = new Rolling(SIZE);
		Rolling featureTime = new Rolling(SIZE);
		Rolling lightTime = new Rolling(SIZE);
		Rolling lodTime = new Rolling(SIZE);
		
		public void recordEvent(PrefEvent e)
		{
			long preTime = e.beginNano;
			totalTime.add(e.endNano - preTime);
			if (e.emptyNano != 0)
			{
				emptyTime.add(e.emptyNano - preTime);
				preTime = e.emptyNano;
			}
			if (e.structStartNano != 0)
			{
				structStartTime.add(e.structStartNano - preTime);
				preTime = e.structStartNano;
			}
			if (e.structRefNano != 0)
			{
				structRefTime.add(e.structRefNano - preTime);
				preTime = e.structRefNano;
			}
			if (e.biomeNano != 0)
			{
				biomeTime.add(e.biomeNano - preTime);
				preTime = e.biomeNano;
			}
			if (e.noiseNano != 0)
			{
				noiseTime.add(e.noiseNano - preTime);
				preTime = e.noiseNano;
			}
			if (e.surfaceNano != 0)
			{
				surfaceTime.add(e.surfaceNano - preTime);
				preTime = e.surfaceNano;
			}
			if (e.carverNano != 0)
			{
				carverTime.add(e.carverNano - preTime);
				preTime = e.carverNano;
			}
			if (e.featureNano != 0)
			{
				featureTime.add(e.featureNano - preTime);
				preTime = e.featureNano;
			}
			if (e.lightNano != 0)
			{
				lightTime.add(e.lightNano - preTime);
				preTime = e.lightNano;
			}
			if (e.endNano != 0)
			{
				lodTime.add(e.endNano - preTime);
				preTime = e.endNano;
			}
		}
		
		public String toString()
		{
			return "Total: " + Duration.ofNanos((long) totalTime.getAverage()) + ", Empty/LoadChunk: "
					+ Duration.ofNanos((long) emptyTime.getAverage()) + ", StructStart: "
					+ Duration.ofNanos((long) structStartTime.getAverage()) + ", StructRef: "
					+ Duration.ofNanos((long) structRefTime.getAverage()) + ", Biome: "
					+ Duration.ofNanos((long) biomeTime.getAverage()) + ", Noise: "
					+ Duration.ofNanos((long) noiseTime.getAverage()) + ", Surface: "
					+ Duration.ofNanos((long) surfaceTime.getAverage()) + ", Carver: "
					+ Duration.ofNanos((long) carverTime.getAverage()) + ", Feature: "
					+ Duration.ofNanos((long) featureTime.getAverage()) + ", Light: "
					+ Duration.ofNanos((long) lightTime.getAverage()) + ", Lod: "
					+ Duration.ofNanos((long) lodTime.getAverage());
		}
	}
	
	public static final int TIMEOUT_SECONDS = 30;
	
	//=================Generation Step===================
	
	public final LinkedList<GenerationEvent> events = new LinkedList<GenerationEvent>();
	public final GlobalParameters params;
	public final StepStructureStart stepStructureStart = new StepStructureStart(this);
	public final StepStructureReference stepStructureReference = new StepStructureReference(this);
	public final StepBiomes stepBiomes = new StepBiomes(this);
	public final StepNoise stepNoise = new StepNoise(this);
	public final StepSurface stepSurface = new StepSurface(this);
	public final StepFeatures stepFeatures = new StepFeatures(this);
	public final StepLight stepLight = new StepLight(this);
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	
	public static final LodThreadFactory threadFactory = new LodThreadFactory("Gen-Worker-Thread", Thread.MIN_PRIORITY);
	
	public ExecutorService executors = Executors.newFixedThreadPool(
			CONFIG.client().advanced().threading().getNumberOfWorldGenerationThreads(), threadFactory);
	
	public void resizeThreadPool(int newThreadCount)
	{
		executors = Executors.newFixedThreadPool(newThreadCount,
				new LodThreadFactory("Gen-Worker-Thread", Thread.MIN_PRIORITY));
	}
	
	public boolean tryAddPoint(int px, int pz, int range, Steps target, boolean genAllDetails)
	{
		int boxSize = range * 2 + 1;
		int x = Math.floorDiv(px, boxSize) * boxSize + range;
		int z = Math.floorDiv(pz, boxSize) * boxSize + range;
		
		for (GenerationEvent event : events)
		{
			if (event.tooClose(x, z, range))
				return false;
		}
		// System.out.println(x + ", "+z);
		events.add(new GenerationEvent(new ChunkPos(x, z), range, this, target, genAllDetails));
		return true;
	}
	
	public void updateAllFutures()
	{
		// Update all current out standing jobs
		Iterator<GenerationEvent> iter = events.iterator();
		while (iter.hasNext())
		{
			GenerationEvent event = iter.next();
			if (event.isCompleted())
			{
				try
				{
					event.join();
				}
				catch (Throwable e)
				{
					e.printStackTrace();
					while (e.getCause() != null)
					{
						e = e.getCause();
						e.printStackTrace();
					}
				}
				finally
				{
					iter.remove();
				}
			}
			else if (event.hasTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS))
			{
				ClientApi.LOGGER.error("Batching World Generator: " + event + " timed out and terminated!");
				ClientApi.LOGGER.info("Dump PrefEvent: " + event.pEvent);
				try
				{
					if (!event.terminate())
						ClientApi.LOGGER.error("Failed to terminate the stuck generation event!");
				}
				finally
				{
					iter.remove();
				}
			}
		}
	}
	
	public BatchGenerationEnvironment(IWorldWrapper serverlevel, LodBuilder lodBuilder, LodDimension lodDim)
	{
		super(serverlevel, lodBuilder, lodDim);
		ClientApi.LOGGER.info("================WORLD_GEN_STEP_INITING=============");
		params = new GlobalParameters((ServerLevel) ((WorldWrapper) serverlevel).getWorld(), lodBuilder, lodDim);
	}
	
	public void startLoadingAllRegionsFromFile(LodDimension lodDim)
	{
		ServerLevel level = params.level;
		level.getChunkSource();
		
	}
	
	@SuppressWarnings("resource")
	public static ChunkAccess loadOrMakeChunk(ChunkPos chunkPos, ServerLevel level, LevelLightEngine lightEngine)
	{
		CompoundTag chunkData = null;
		try
		{
			chunkData = level.getChunkSource().chunkMap.readChunk(chunkPos);
		}
		catch (IOException e)
		{
			ClientApi.LOGGER.error("DistantHorizons: Couldn't load chunk {}", chunkPos, e);
		}
		if (chunkData == null)
		{
			return new ProtoChunk(chunkPos, UpgradeData.EMPTY, level, level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), null);
		}
		else
		{
			return ChunkLoader.read(level, lightEngine, chunkPos, chunkData);
		}
		
	}
	
	
	
	public void generateLodFromList(GenerationEvent e)
	{
		if (ENABLE_EVENT_LOGGING)
			ClientApi.LOGGER.info("Lod Generate Event: " + e.pos);
		e.pEvent.beginNano = System.nanoTime();
		GridList<ChunkAccess> referencedChunks;
		DistanceGenerationMode generationMode;
		LightedWorldGenRegion region;
		WorldGenLevelLightEngine lightEngine;
		LightGetterAdaptor adaptor;
		
		try
		{
			adaptor = new LightGetterAdaptor(params.level);
			lightEngine = new WorldGenLevelLightEngine(adaptor);
			
			int cx = e.pos.x;
			int cy = e.pos.z;
			int rangeEmpty = e.range + 1;
			GridList<ChunkAccess> chunks = new GridList<ChunkAccess>(rangeEmpty);
			
			@SuppressWarnings("resource")
			EmptyChunkGenerator generator = (int x, int z) ->
			{
				ChunkPos chunkPos = new ChunkPos(x, z);
				ChunkAccess target = null;
				try
				{
					target = loadOrMakeChunk(chunkPos, params.level, lightEngine);
				}
				catch (RuntimeException e2)
				{
					// Continue...
					e2.printStackTrace();
				}
				if (target == null)
					target = new ProtoChunk(chunkPos, UpgradeData.EMPTY, params.level,
							params.biomes, null);
				return target;
			};
			
			for (int oy = -rangeEmpty; oy <= rangeEmpty; oy++)
			{
				for (int ox = -rangeEmpty; ox <= rangeEmpty; ox++)
				{
					ChunkAccess target = generator.generate(cx + ox, cy + oy);
					chunks.add(target);
				}
			}
			e.pEvent.emptyNano = System.nanoTime();
			e.refreshTimeout();
			region = new LightedWorldGenRegion(params.level, lightEngine, e.tParam.structFeat, chunks, ChunkStatus.STRUCTURE_STARTS, rangeEmpty, e.lightMode, generator);
			adaptor.setRegion(region);
			referencedChunks = chunks.subGrid(e.range);
			referencedChunks = generateDirect(e, referencedChunks, e.target, region);
			
		}
		catch (StepStructureStart.StructStartCorruptedException f)
		{
			e.tParam.markAsInvalid();
			return;
		}
		
		switch (e.target)
		{
		case Empty:
		case StructureStart:
		case StructureReference:
			generationMode = DistanceGenerationMode.NONE;
			break;
		case Biomes:
			generationMode = DistanceGenerationMode.BIOME_ONLY;
		case Noise:
			generationMode = DistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT;
			break;
		case Surface:
		case Carvers:
			generationMode = DistanceGenerationMode.SURFACE;
			break;
		case Features:
			generationMode = DistanceGenerationMode.FEATURES;
			break;
		case Light:
		case LiquidCarvers:
		default:
			return;
		}
		int centreIndex = referencedChunks.size() / 2;
		
		for (int oy = -e.range; oy <= e.range; oy++)
		{
			for (int ox = -e.range; ox <= e.range; ox++)
			{
				int targetIndex = referencedChunks.offsetOf(centreIndex, ox, oy);
				ChunkAccess target = referencedChunks.get(targetIndex);
				target.setLightCorrect(true);
				if (target instanceof LevelChunk)
					((LevelChunk) target).setClientLightReady(true);
				boolean isFull = target.getStatus() == ChunkStatus.FULL || target instanceof LevelChunk;
				boolean isPartial = target.isOldNoiseGeneration();
				if (isFull)
				{
					if (ENABLE_LOAD_EVENT_LOGGING)
						ClientApi.LOGGER.info("Detected full existing chunk at {}", target.getPos());
					params.lodBuilder.generateLodNodeFromChunk(params.lodDim, new ChunkWrapper(target, region),
							new LodBuilderConfig(DistanceGenerationMode.FULL), true, e.genAllDetails);
				}
				else if (isPartial)
				{
					if (ENABLE_LOAD_EVENT_LOGGING)
						ClientApi.LOGGER.info("Detected old existing chunk at {}", target.getPos());
					params.lodBuilder.generateLodNodeFromChunk(params.lodDim, new ChunkWrapper(target, region),
							new LodBuilderConfig(generationMode), true, e.genAllDetails);
				}
				else if (target.getStatus() == ChunkStatus.EMPTY && generationMode == DistanceGenerationMode.NONE)
				{
					params.lodBuilder.generateLodNodeFromChunk(params.lodDim, new ChunkWrapper(target, region),
							LodBuilderConfig.getFillVoidConfig(), true, e.genAllDetails);
				}
				else
				{
					params.lodBuilder.generateLodNodeFromChunk(params.lodDim, new ChunkWrapper(target, region),
							new LodBuilderConfig(generationMode), true, e.genAllDetails);
				}
				if (e.lightMode == LightGenerationMode.FANCY || isFull)
				{
					lightEngine.retainData(target.getPos(), false);
				}
				
			}
		}
		e.pEvent.endNano = System.nanoTime();
		e.refreshTimeout();
		if (ENABLE_PERF_LOGGING)
		{
			e.tParam.perf.recordEvent(e.pEvent);
			ClientApi.LOGGER.info(e.tParam.perf);
		}
	}
	
	public GridList<ChunkAccess> generateDirect(GenerationEvent e, GridList<ChunkAccess> subRange, Steps step,
			LightedWorldGenRegion region)
	{
		try
		{
			subRange.forEach((chunk) ->
			{
				if (chunk instanceof ProtoChunk)
				{
					((ProtoChunk) chunk).setLightEngine(region.getLightEngine());
					region.getLightEngine().retainData(chunk.getPos(), true);
				}
			});
			if (step == Steps.Empty)
				return subRange;
			stepStructureStart.generateGroup(e.tParam, region, subRange);
			e.pEvent.structStartNano = System.nanoTime();
			e.refreshTimeout();
			if (step == Steps.StructureStart)
				return subRange;
			stepStructureReference.generateGroup(e.tParam, region, subRange);
			e.pEvent.structRefNano = System.nanoTime();
			e.refreshTimeout();
			if (step == Steps.StructureReference)
				return subRange;
			stepBiomes.generateGroup(e.tParam, region, subRange);
			e.pEvent.biomeNano = System.nanoTime();
			e.refreshTimeout();
			if (step == Steps.Biomes)
				return subRange;
			stepNoise.generateGroup(e.tParam, region, subRange);
			e.pEvent.noiseNano = System.nanoTime();
			e.refreshTimeout();
			if (step == Steps.Noise)
				return subRange;
			stepSurface.generateGroup(e.tParam, region, subRange);
			e.pEvent.surfaceNano = System.nanoTime();
			e.refreshTimeout();
			if (step == Steps.Surface)
				return subRange;
			if (step == Steps.Carvers)
				return subRange;
			stepFeatures.generateGroup(e.tParam, region, subRange);
			e.pEvent.featureNano = System.nanoTime();
			e.refreshTimeout();
			return subRange;
		}
		finally
		{
			switch (region.lightMode)
			{
			case FANCY:
				stepLight.generateGroup(region.getLightEngine(), subRange);
				break;
			case FAST:
				subRange.forEach((p) ->
				{
					if (p instanceof ProtoChunk)
						((ProtoChunk) p).setLightCorrect(true);
				});
				break;
			}
			e.pEvent.lightNano = System.nanoTime();
			e.refreshTimeout();
		}
	}
	
	public interface EmptyChunkGenerator
	{
		ChunkAccess generate(int x, int z);
	}

	@Override
	public int getEventCount() {
		return events.size();
	}

	@Override
	public void stop(boolean blocking) {
		ClientApi.LOGGER.info("Batch Chunk Generator shutting down...");
		executors.shutdownNow();
		if (blocking) try {
			if (!executors.awaitTermination(10, TimeUnit.SECONDS)) {
				ClientApi.LOGGER.error("Batch Chunk Generator shutdown failed! Ignoring child threads...");
			}
		} catch (InterruptedException e) {
			ClientApi.LOGGER.error("Batch Chunk Generator shutdown failed! Ignoring child threads...", e);
		}
	}
}