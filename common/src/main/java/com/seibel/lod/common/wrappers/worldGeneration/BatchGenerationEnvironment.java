/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2021  Tom Lee (TomTheFurry)
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

import com.seibel.lod.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.lod.common.wrappers.world.ServerLevelWrapper;
import com.seibel.lod.core.level.IDhServerLevel;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.api.enums.config.ELightGenerationMode;
import com.seibel.lod.core.logging.ConfigBasedLogger;
import com.seibel.lod.core.logging.ConfigBasedSpamLogger;
import com.seibel.lod.core.pos.DhChunkPos;
import com.seibel.lod.core.util.objects.EventTimer;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.gridList.ArrayGridList;
import com.seibel.lod.core.util.objects.LodThreadFactory;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.worldGeneration.AbstractBatchGenerationEnvionmentWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.seibel.lod.common.wrappers.DependencySetupDoneCheck;
import com.seibel.lod.common.wrappers.chunk.ChunkWrapper;
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
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.apache.logging.log4j.LogManager;

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
	public static final ConfigBasedSpamLogger PREF_LOGGER =
			new ConfigBasedSpamLogger(LogManager.getLogger("LodWorldGen"),
					() -> Config.Client.Advanced.Debugging.DebugSwitch.logWorldGenPerformance.get(),1);
	public static final ConfigBasedLogger EVENT_LOGGER =
			new ConfigBasedLogger(LogManager.getLogger("LodWorldGen"),
					() -> Config.Client.Advanced.Debugging.DebugSwitch.logWorldGenEvent.get());
	public static final ConfigBasedLogger LOAD_LOGGER =
			new ConfigBasedLogger(LogManager.getLogger("LodWorldGen"),
					() -> Config.Client.Advanced.Debugging.DebugSwitch.logWorldGenLoadEvent.get());

	//TODO: Make actual proper support for StarLight

	public static class PerfCalculator
	{
		private static final String[] TIME_NAMES = {
				"total",
				"setup",
				"structStart",
				"structRef",
				"biome",
				"noise",
				"surface",
				"carver",
				"feature",
				"light",
				"cleanup",
				//"lodCreation" (No longer used)
		};

		public static final int SIZE = 50;
		ArrayList<Rolling> times = new ArrayList<>();

		public PerfCalculator()
		{
			for(int i = 0; i < 11; i++)
			{
				times.add(new Rolling(SIZE));
			}
		}
		
		public void recordEvent(EventTimer event)
		{
			for (EventTimer.Event e : event.events)
			{
				String name = e.name;
				int index = Arrays.asList(TIME_NAMES).indexOf(name);
				if(index == -1) continue;
				times.get(index).add(e.timeNs);
			}
			times.get(0).add(event.getTotalTimeNs());
		}
		
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < times.size(); i++)
			{
				if (times.get(i).getAverage() == 0) continue;
				sb.append(TIME_NAMES[i]).append(": ").append(times.get(i).getAverage()).append("\n");
			}
			return sb.toString();
		}
	}
	
	public static final int TIMEOUT_SECONDS = 60;
	
	//=================Generation Step===================
	
	public final LinkedList<GenerationEvent> events = new LinkedList<>();
	public final GlobalParameters params;
	public final StepStructureStart stepStructureStart = new StepStructureStart(this);
	public final StepStructureReference stepStructureReference = new StepStructureReference(this);
	public final StepBiomes stepBiomes = new StepBiomes(this);
	public final StepNoise stepNoise = new StepNoise(this);
	public final StepSurface stepSurface = new StepSurface(this);
	public final StepFeatures stepFeatures = new StepFeatures(this);
	public final StepLight stepLight = new StepLight(this);
	public boolean unsafeThreadingRecorded = false;
	//public boolean safeMode = false;
	//private static final IMinecraftClientWrapper MC = SingletonHandler.get(IMinecraftClientWrapper.class);
	public static final long EXCEPTION_TIMER_RESET_TIME = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
	public static final int EXCEPTION_COUNTER_TRIGGER = 20;
	public static final int RANGE_TO_RANGE_EMPTY_EXTENSION = 1;
	public int unknownExceptionCount = 0;
	public long lastExceptionTriggerTime = 0;
	
	public static final LodThreadFactory threadFactory = new LodThreadFactory("Gen-Worker-Thread", Thread.MIN_PRIORITY);
	
	public static ThreadLocal<Boolean> isDistantGeneratorThread = new ThreadLocal<>();
	
	public static boolean isCurrentThreadDistantGeneratorThread() {
		return (isDistantGeneratorThread.get() != null);
	}
	
	static {
		DependencySetupDoneCheck.getIsCurrentThreadDistantGeneratorThread = BatchGenerationEnvironment::isCurrentThreadDistantGeneratorThread;
	}
	
	public ExecutorService executors = Executors.newFixedThreadPool(
			Math.max(Config.Client.Advanced.Threading.numberOfWorldGenerationThreads.get().intValue(), 1),
			threadFactory);

	public <T> T joinSync(CompletableFuture<T> f) {
		if (!unsafeThreadingRecorded && !f.isDone()) {
			EVENT_LOGGER.error("Unsafe Threading in Chunk Generator: ", new RuntimeException("Concurrent future"));
			EVENT_LOGGER.error("To increase stability, it is recommended to set world generation threads count to 1.");
			unsafeThreadingRecorded = true;
		}
		return f.join();
	}
	
	public void resizeThreadPool(int newThreadCount)
	{
		executors = Executors.newFixedThreadPool(newThreadCount,
				new LodThreadFactory("Gen-Worker-Thread", Thread.MIN_PRIORITY));
	}

	public void updateAllFutures()
	{
		if (unknownExceptionCount > 0) {
			if (System.nanoTime() - lastExceptionTriggerTime >= EXCEPTION_TIMER_RESET_TIME) {
				unknownExceptionCount = 0;
			}
		}

		// Update all current out standing jobs
		Iterator<GenerationEvent> iter = events.iterator();
		while (iter.hasNext())
		{
			GenerationEvent event = iter.next();
			if (event.future.isDone())
			{
				if (event.future.isCompletedExceptionally() && !event.future.isCancelled()) {
					try {
						event.future.get(); // Should throw exception
						LodUtil.assertNotReach();
					} catch (Exception e) {
						unknownExceptionCount++;
						lastExceptionTriggerTime = System.nanoTime();
						EVENT_LOGGER.error("Batching World Generator: Event {} gotten an exception", event);
						EVENT_LOGGER.error("Exception: ", e);
					}
				}
				iter.remove();
			}
			else if (event.hasTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS))
			{
				EVENT_LOGGER.error("Batching World Generator: " + event + " timed out and terminated!");
				EVENT_LOGGER.info("Dump PrefEvent: " + event.timer);
				try {
					if (!event.terminate())
						EVENT_LOGGER.error("Failed to terminate the stuck generation event!");
				}
				finally
				{
					iter.remove();
				}
			}
		}

		if (unknownExceptionCount > EXCEPTION_COUNTER_TRIGGER) {
			EVENT_LOGGER.error("Too many exceptions in Batching World Generator! Disabling the generator.");
			unknownExceptionCount = 0;
			Config.Client.WorldGenerator.enableDistantGeneration.set(false);
		}
	}

	public BatchGenerationEnvironment(IDhServerLevel serverlevel)
	{
		super(serverlevel);
		EVENT_LOGGER.info("================WORLD_GEN_STEP_INITING=============");
		ChunkGenerator generator =  ((ServerLevelWrapper) (serverlevel.getServerLevelWrapper())).getLevel().getChunkSource().getGenerator();
		if (!(generator instanceof NoiseBasedChunkGenerator ||
				generator instanceof DebugLevelSource ||
				generator instanceof FlatLevelSource)) {
			if (generator.getClass().toString().equals("class com.terraforged.mod.chunk.TFChunkGenerator")) {
				EVENT_LOGGER.info("TerraForge Chunk Generator detected: [{}], Distant Generation will try its best to support it.", generator.getClass());
				EVENT_LOGGER.info("If it does crash, set Distant Generation to OFF or Generation Mode to None.");
			} else {
				EVENT_LOGGER.warn("Unknown Chunk Generator detected: [{}], Distant Generation May Fail!", generator.getClass());
				EVENT_LOGGER.warn("If it does crash, set Distant Generation to OFF or Generation Mode to None.");
			}
		}
		params = new GlobalParameters(serverlevel);
	}
	
	@SuppressWarnings("resource")
	public static ChunkAccess loadOrMakeChunk(ChunkPos chunkPos, ServerLevel level, LevelLightEngine lightEngine)
	{
		CompoundTag chunkData = null;
		try
		{
			#if POST_MC_1_19
			chunkData = level.getChunkSource().chunkMap.readChunk(chunkPos).get().orElse(null);
			#else
			chunkData = level.getChunkSource().chunkMap.readChunk(chunkPos);
			#endif
		}
		catch (Exception e)
		{
			LOAD_LOGGER.error("DistantHorizons: Couldn't load chunk {}", chunkPos, e);
		}
		if (chunkData == null)
		{
			return new ProtoChunk(chunkPos, UpgradeData.EMPTY
							#if POST_MC_1_17_1, level #endif
							#if POST_MC_1_18_1, level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), null #endif
			);
		}
		else
		{
			try {
			return ChunkLoader.read(level, lightEngine, chunkPos, chunkData);
			} catch (Exception e) {
				LOAD_LOGGER.error("DistantHorizons: Couldn't load chunk {}", chunkPos, e);
				return new ProtoChunk(chunkPos, UpgradeData.EMPTY
							#if POST_MC_1_17_1, level #endif
							#if POST_MC_1_18_1, level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), null #endif
				);
			}
		}
		
	}
	
	public void generateLodFromList(GenerationEvent e)
	{
		EVENT_LOGGER.debug("Lod Generate Event: " + e.minPos);
		ArrayGridList<ChunkAccess> referencedChunks;
		ArrayGridList<ChunkAccess> genChunks;
		EDhApiDistantGeneratorMode generatorDetail;
		LightedWorldGenRegion region;
		WorldGenLevelLightEngine lightEngine;
		LightGetterAdaptor adaptor;
		int refSize = e.size+2; // +2 for the border referenced chunks
		int refPosX = e.minPos.x - 1; // -1 for the border referenced chunks
		int refPosZ = e.minPos.z - 1; // -1 for the border referenced chunks

		try
		{
			adaptor = new LightGetterAdaptor(params.level);
			lightEngine = new WorldGenLevelLightEngine(adaptor);

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
				}
				if (target == null)
					target = new ProtoChunk(chunkPos, UpgradeData.EMPTY
							#if POST_MC_1_17_1, params.level #endif
							#if POST_MC_1_18_1, params.biomes, null #endif
					);
				return target;
			};

			referencedChunks = new ArrayGridList<>(refSize,
					(x,z) -> generator.generate(x + refPosX,z + refPosZ)
			);
			e.refreshTimeout();
			region = new LightedWorldGenRegion(params.level, lightEngine, referencedChunks,
					ChunkStatus.STRUCTURE_STARTS, refSize/2, e.lightMode, generator);
			adaptor.setRegion(region);
			e.threadedParam.makeStructFeat(region, params);
			genChunks = new ArrayGridList<>(referencedChunks, RANGE_TO_RANGE_EMPTY_EXTENSION,
					referencedChunks.gridSize - RANGE_TO_RANGE_EMPTY_EXTENSION);
			generateDirect(e, genChunks, e.target, region);
			e.timer.nextEvent("cleanup");
		}
		catch (StepStructureStart.StructStartCorruptedException f)
		{
			e.threadedParam.markAsInvalid();
			throw (RuntimeException)f.getCause();
		}

		for (int oy = 0; oy < genChunks.gridSize; oy++)
		{
			for (int ox = 0; ox < genChunks.gridSize; ox++)
			{
				ChunkAccess target = genChunks.get(ox, oy);
				ChunkWrapper wrappedChunk = new ChunkWrapper(target, region, null);
				if (!wrappedChunk.isLightCorrect()) {
					throw new RuntimeException("The generated chunk somehow has isLightCorrect() returning false");
				}

				boolean isFull = target.getStatus() == ChunkStatus.FULL || target instanceof LevelChunk;
				#if POST_MC_1_18_1
				boolean isPartial = target.isOldNoiseGeneration();
				#endif
				if (isFull)
				{
					LOAD_LOGGER.info("Detected full existing chunk at {}", target.getPos());
					e.resultConsumer.accept(wrappedChunk);
				}
				#if POST_MC_1_18_1
				else if (isPartial)
				{
					LOAD_LOGGER.info("Detected old existing chunk at {}", target.getPos());
					e.resultConsumer.accept(wrappedChunk);
				}
				#endif
				else if (target.getStatus() == ChunkStatus.EMPTY)
				{
					e.resultConsumer.accept(wrappedChunk);
				}
				else
				{
					e.resultConsumer.accept(wrappedChunk);
				}
				if (e.lightMode == ELightGenerationMode.FANCY || isFull)
				{
					lightEngine.retainData(target.getPos(), false);
				}
			}
		}
		e.timer.complete();
		e.refreshTimeout();
		if (PREF_LOGGER.canMaybeLog())
		{
			e.threadedParam.perf.recordEvent(e.timer);
			PREF_LOGGER.infoInc("{}", e.timer);
		}
	}
	
	public void generateDirect(GenerationEvent e, ArrayGridList<ChunkAccess> subRange,
							   Steps step, LightedWorldGenRegion region)
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
				return;
			e.timer.nextEvent("structStart");
			stepStructureStart.generateGroup(e.threadedParam, region, subRange);
			e.refreshTimeout();
			if (step == Steps.StructureStart)
				return;
			e.timer.nextEvent("structRef");
			stepStructureReference.generateGroup(e.threadedParam, region, subRange);
			e.refreshTimeout();
			if (step == Steps.StructureReference)
				return;
			e.timer.nextEvent("biome");
			stepBiomes.generateGroup(e.threadedParam, region, subRange);
			e.refreshTimeout();
			if (step == Steps.Biomes)
				return;
			e.timer.nextEvent("noise");
			stepNoise.generateGroup(e.threadedParam, region, subRange);
			e.refreshTimeout();
			if (step == Steps.Noise)
				return;
			e.timer.nextEvent("surface");
			stepSurface.generateGroup(e.threadedParam, region, subRange);
			e.refreshTimeout();
			if (step == Steps.Surface)
				return;
			e.timer.nextEvent("carver");
			if (step == Steps.Carvers)
				return;
			e.timer.nextEvent("feature");
			stepFeatures.generateGroup(e.threadedParam, region, subRange);
			e.refreshTimeout();
		}
		finally
		{
			e.timer.nextEvent("light");
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
					#if POST_MC_1_18_1
					if (p instanceof LevelChunk) {
						((LevelChunk) p).setLightCorrect(true);
						((LevelChunk) p).setClientLightReady(true);
					}
					#endif
				});
				break;
			}
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
		EVENT_LOGGER.info("Batch Chunk Generator shutting down...");
		executors.shutdownNow();
		Iterator<GenerationEvent> iter = events.iterator();
		while (iter.hasNext())
		{
			GenerationEvent event = iter.next();
			event.future.cancel(true);
			iter.remove();
		}
		if (blocking) try {
			if (!executors.awaitTermination(10, TimeUnit.SECONDS)) {
				EVENT_LOGGER.error("Batch Chunk Generator shutdown failed! Ignoring child threads...");
			}
		} catch (InterruptedException e) {
			EVENT_LOGGER.error("Batch Chunk Generator shutdown failed! Ignoring child threads...", e);
		}
	}

	@Override
	public CompletableFuture<Void> generateChunks(int minX, int minZ, int genSize, Steps targetStep, double runTimeRatio, Consumer<IChunkWrapper> resultConsumer)
	{
		// TODO: Check event overlap via e.tooClose()
		GenerationEvent e = GenerationEvent.startEvent(new DhChunkPos(minX, minZ), genSize, this, targetStep, runTimeRatio, resultConsumer);
		events.add(e);
		return e.future;
	}
}