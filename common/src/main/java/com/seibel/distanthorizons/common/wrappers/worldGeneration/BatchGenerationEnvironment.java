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

package com.seibel.distanthorizons.common.wrappers.worldGeneration;

import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiWorldGenerationStep;
import com.seibel.distanthorizons.common.wrappers.world.ServerLevelWrapper;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.mimicObject.*;
import com.seibel.distanthorizons.core.dataObjects.transformers.FullDataToRenderDataTransformer;
import com.seibel.distanthorizons.core.level.IDhServerLevel;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.api.enums.config.ELightGenerationMode;
import com.seibel.distanthorizons.core.logging.ConfigBasedLogger;
import com.seibel.distanthorizons.core.logging.ConfigBasedSpamLogger;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.util.objects.EventTimer;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.gridList.ArrayGridList;
import com.seibel.distanthorizons.core.util.objects.DhThreadFactory;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.worldGeneration.AbstractBatchGenerationEnvironmentWrapper;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.seibel.distanthorizons.common.wrappers.DependencySetupDoneCheck;
import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.step.StepBiomes;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.step.StepFeatures;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.step.StepLight;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.step.StepNoise;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.step.StepStructureReference;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.step.StepStructureStart;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.step.StepSurface;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
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
public final class BatchGenerationEnvironment extends AbstractBatchGenerationEnvironmentWrapper
{
	public static final ConfigBasedSpamLogger PREF_LOGGER =
			new ConfigBasedSpamLogger(LogManager.getLogger("LodWorldGen"),
					() -> Config.Client.Advanced.Logging.logWorldGenPerformance.get(),1);
	public static final ConfigBasedLogger EVENT_LOGGER =
			new ConfigBasedLogger(LogManager.getLogger("LodWorldGen"),
					() -> Config.Client.Advanced.Logging.logWorldGenEvent.get());
	public static final ConfigBasedLogger LOAD_LOGGER =
			new ConfigBasedLogger(LogManager.getLogger("LodWorldGen"),
					() -> Config.Client.Advanced.Logging.logWorldGenLoadEvent.get());
	
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
	
	public final LinkedList<GenerationEvent> generationEventList = new LinkedList<>();
	public final GlobalParameters params;
	public final StepStructureStart stepStructureStart = new StepStructureStart(this);
	public final StepStructureReference stepStructureReference = new StepStructureReference(this);
	public final StepBiomes stepBiomes = new StepBiomes(this);
	public final StepNoise stepNoise = new StepNoise(this);
	public final StepSurface stepSurface = new StepSurface(this);
	public final StepFeatures stepFeatures = new StepFeatures(this);
	public final StepLight stepLight = new StepLight(this);
	public boolean unsafeThreadingRecorded = false;
	public static final long EXCEPTION_TIMER_RESET_TIME = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
	public static final int EXCEPTION_COUNTER_TRIGGER = 20;
	public static final int RANGE_TO_RANGE_EMPTY_EXTENSION = 1;
	public int unknownExceptionCount = 0;
	public long lastExceptionTriggerTime = 0;

	private AtomicReference<RegionFileStorageExternalCache> regionFileStorageCacheRef = new AtomicReference<>();

	public RegionFileStorageExternalCache getOrCreateRegionFileCache(RegionFileStorage storage)
	{
		RegionFileStorageExternalCache cache = regionFileStorageCacheRef.get();
		if (cache == null)
		{
			cache = new RegionFileStorageExternalCache(storage);
			if (!regionFileStorageCacheRef.compareAndSet(null, cache))
			{
				cache = regionFileStorageCacheRef.get();
			}
		}
		return cache;
	}
	
	public static ThreadLocal<Boolean> isDistantGeneratorThread = new ThreadLocal<>();
	public static boolean isCurrentThreadDistantGeneratorThread() { return (isDistantGeneratorThread.get() != null); }
	
	public static final DhThreadFactory threadFactory = new DhThreadFactory("DH-Gen-Worker-Thread", Thread.MIN_PRIORITY);
	
	
	
	//==============//
	// constructors //
	//==============//
	
	static
	{
		DependencySetupDoneCheck.getIsCurrentThreadDistantGeneratorThread = BatchGenerationEnvironment::isCurrentThreadDistantGeneratorThread;
	}
	
	public BatchGenerationEnvironment(IDhServerLevel serverlevel)
	{
		super(serverlevel);
		EVENT_LOGGER.info("================WORLD_GEN_STEP_INITING=============");
		
		ChunkGenerator generator = ((ServerLevelWrapper) (serverlevel.getServerLevelWrapper())).getLevel().getChunkSource().getGenerator();
		if (!(generator instanceof NoiseBasedChunkGenerator ||
				generator instanceof DebugLevelSource ||
				generator instanceof FlatLevelSource))
		{
			if (generator.getClass().toString().equals("class com.terraforged.mod.chunk.TFChunkGenerator"))
			{
				EVENT_LOGGER.info("TerraForge Chunk Generator detected: ["+generator.getClass()+"], Distant Generation will try its best to support it.");
				EVENT_LOGGER.info("If it does crash, turn Distant Generation off or set it to to ["+EDhApiWorldGenerationStep.EMPTY+"].");
			}
			else
			{
				EVENT_LOGGER.warn("Unknown Chunk Generator detected: ["+generator.getClass()+"], Distant Generation May Fail!");
				EVENT_LOGGER.warn("If it does crash, disable Distant Generation or set the Generation Mode to ["+EDhApiWorldGenerationStep.EMPTY+"].");
			}
		}
		
		this.params = new GlobalParameters(serverlevel);
	}
	
	
	
	
	public <T> T joinSync(CompletableFuture<T> future)
	{
		if (!unsafeThreadingRecorded && !future.isDone())
		{
			EVENT_LOGGER.error("Unsafe MultiThreading in Chunk Generator: ", new RuntimeException("Concurrent future"));
			EVENT_LOGGER.error("To increase stability, it is recommended to set world generation threads count to 1.");
			unsafeThreadingRecorded = true;
		}
		
		return future.join();
	}
	
	public void updateAllFutures()
	{
		if (unknownExceptionCount > 0)
		{
			if (System.nanoTime() - lastExceptionTriggerTime >= EXCEPTION_TIMER_RESET_TIME)
			{
				unknownExceptionCount = 0;
			}
		}
		
		// Update all current out standing jobs
		Iterator<GenerationEvent> iter = generationEventList.iterator();
		while (iter.hasNext())
		{
			GenerationEvent event = iter.next();
			if (event.future.isDone())
			{
				if (event.future.isCompletedExceptionally() && !event.future.isCancelled())
				{
					try
					{
						event.future.get(); // Should throw exception
						LodUtil.assertNotReach();
					}
					catch (Exception e)
					{
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
				try
				{
					if (!event.terminate())
					{
						EVENT_LOGGER.error("Failed to terminate the stuck generation event!");
					}
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
			Config.Client.Advanced.WorldGenerator.enableDistantGeneration.set(false);
		}
	}

	public ChunkAccess loadOrMakeChunk(ChunkPos chunkPos, WorldGenLevelLightEngine lightEngine)
	{
		ServerLevel level = params.level;

		CompoundTag chunkData = null;
		try
		{
			#if POST_MC_1_19
			chunkData = level.getChunkSource().chunkMap.readChunk(chunkPos).get().orElse(null);
			#else
			// Warning: if multiple threads attempt to access this method at the same time,
			// it can throw EOFExceptions that are caught and logged by Minecraft
			//chunkData = level.getChunkSource().chunkMap.readChunk(chunkPos);
			RegionFileStorage storage = params.level.getChunkSource().chunkMap.worker.storage;
			RegionFileStorageExternalCache cache = getOrCreateRegionFileCache(storage);
			chunkData = cache.read(chunkPos);
			#endif
		}
		catch (Exception e)
		{
			LOAD_LOGGER.error("DistantHorizons: Couldn't load or make chunk "+chunkPos+". Error: "+e.getMessage(), e);
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
			try
			{
				LOAD_LOGGER.info("DistantHorizons: Loading chunk "+chunkPos+" from disk.");
				return ChunkLoader.read(level, lightEngine, chunkPos, chunkData);
			}
			catch (Exception e)
			{
				LOAD_LOGGER.error("DistantHorizons: Couldn't load or make chunk "+chunkPos+". Returning an empty chunk. Error: "+e.getMessage(), e);
				return new ProtoChunk(chunkPos, UpgradeData.EMPTY
							#if POST_MC_1_17_1 , level #endif
							#if POST_MC_1_18_1 , level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), null #endif
				);
			}
		}
	}
	
	public void generateLodFromList(GenerationEvent genEvent) throws InterruptedException
	{
		EVENT_LOGGER.debug("Lod Generate Event: "+genEvent.minPos);
		
		ArrayGridList<ChunkAccess> referencedChunks;
		ArrayGridList<ChunkAccess> genChunks;
		LightedWorldGenRegion region;
		WorldGenLevelLightEngine lightEngine;
		LightGetterAdaptor adaptor;
		
		int refSize = genEvent.size+2; // +2 for the border referenced chunks
		int refPosX = genEvent.minPos.x - 1; // -1 for the border referenced chunks
		int refPosZ = genEvent.minPos.z - 1; // -1 for the border referenced chunks
		
		try
		{
			adaptor = new LightGetterAdaptor(params.level);
			lightEngine = new WorldGenLevelLightEngine(adaptor);
			
			EmptyChunkGenerator generator = (int x, int z) ->
			{
				ChunkPos chunkPos = new ChunkPos(x, z);
				ChunkAccess target = null;
				try
				{
					target = loadOrMakeChunk(chunkPos, lightEngine);
				}
				catch (RuntimeException e2)
				{
					// Continue...
				}
				
				if (target == null)
				{
					target = new ProtoChunk(chunkPos, UpgradeData.EMPTY
							#if POST_MC_1_17_1 , params.level #endif
							#if POST_MC_1_18_1 , params.biomes, null #endif
					);
				}
				return target;
			};
			
			referencedChunks = new ArrayGridList<>(refSize, (x,z) -> generator.generate(x + refPosX,z + refPosZ));
			
			genEvent.refreshTimeout();
			region = new LightedWorldGenRegion(params.level, lightEngine, referencedChunks,
					ChunkStatus.STRUCTURE_STARTS, refSize/2, generator);
			adaptor.setRegion(region);
			genEvent.threadedParam.makeStructFeat(region, params);
			genChunks = new ArrayGridList<>(referencedChunks, RANGE_TO_RANGE_EMPTY_EXTENSION,
					referencedChunks.gridSize - RANGE_TO_RANGE_EMPTY_EXTENSION);
			this.generateDirect(genEvent, genChunks, genEvent.targetGenerationStep, region);
			genEvent.timer.nextEvent("cleanup");
		}
		catch (StepStructureStart.StructStartCorruptedException f)
		{
			genEvent.threadedParam.markAsInvalid();
			throw (RuntimeException)f.getCause();
		}
		
		for (int offsetY = 0; offsetY < genChunks.gridSize; offsetY++)
		{
			for (int offsetX = 0; offsetX < genChunks.gridSize; offsetX++)
			{
				ChunkAccess target = genChunks.get(offsetX, offsetY);
				ChunkWrapper wrappedChunk = new ChunkWrapper(target, region, null);
				if (target instanceof LevelChunk) {
					((LevelChunk) target).loaded = true;
				}
				if (!wrappedChunk.isLightCorrect())
				{
					throw new RuntimeException("The generated chunk somehow has isLightCorrect() returning false");
				}
				
				boolean isFull = target.getStatus() == ChunkStatus.FULL || target instanceof LevelChunk;
				#if POST_MC_1_18_1
				boolean isPartial = target.isOldNoiseGeneration();
				#endif
				if (isFull)
				{
					LOAD_LOGGER.info("Detected full existing chunk at {}", target.getPos());
					genEvent.resultConsumer.accept(wrappedChunk);
				}
				#if POST_MC_1_18_1
				else if (isPartial)
				{
					LOAD_LOGGER.info("Detected old existing chunk at {}", target.getPos());
					genEvent.resultConsumer.accept(wrappedChunk);
				}
				#endif
				else if (target.getStatus() == ChunkStatus.EMPTY)
				{
					genEvent.resultConsumer.accept(wrappedChunk);
				}
				else
				{
					genEvent.resultConsumer.accept(wrappedChunk);
				}
				if (isFull)
				{
					lightEngine.retainData(target.getPos(), false);
				}
			}
		}
		
		genEvent.timer.complete();
		genEvent.refreshTimeout();
		if (PREF_LOGGER.canMaybeLog())
		{
			genEvent.threadedParam.perf.recordEvent(genEvent.timer);
			PREF_LOGGER.infoInc("{}", genEvent.timer);
		}
	}
	
	public void generateDirect(GenerationEvent genEvent, ArrayGridList<ChunkAccess> chunksToGenerate,
								EDhApiWorldGenerationStep step, LightedWorldGenRegion region) throws InterruptedException
	{
		if (Thread.interrupted())
		{
			return;	
		}
		
		try
		{
			chunksToGenerate.forEach((chunk) ->
			{
				if (chunk instanceof ProtoChunk)
				{
					ProtoChunk protoChunk = ((ProtoChunk) chunk);
					
					protoChunk.setLightEngine(region.getLightEngine());
					region.getLightEngine().retainData(protoChunk.getPos(), true);
				}
			});
			
			if (step == EDhApiWorldGenerationStep.EMPTY)
			{
				return;
			}
			
			genEvent.timer.nextEvent("structStart");
			throwIfThreadInterrupted();
			stepStructureStart.generateGroup(genEvent.threadedParam, region, chunksToGenerate);
			genEvent.refreshTimeout();
			if (step == EDhApiWorldGenerationStep.STRUCTURE_START)
			{
				return;
			}
			
			genEvent.timer.nextEvent("structRef");
			throwIfThreadInterrupted();
			stepStructureReference.generateGroup(genEvent.threadedParam, region, chunksToGenerate);
			genEvent.refreshTimeout();
			if (step == EDhApiWorldGenerationStep.STRUCTURE_REFERENCE)
			{
				return;
			}
			
			genEvent.timer.nextEvent("biome");
			throwIfThreadInterrupted();
			stepBiomes.generateGroup(genEvent.threadedParam, region, chunksToGenerate);
			genEvent.refreshTimeout();
			if (step == EDhApiWorldGenerationStep.BIOMES)
			{
				return;
			}
			
			genEvent.timer.nextEvent("noise");
			throwIfThreadInterrupted();
			stepNoise.generateGroup(genEvent.threadedParam, region, chunksToGenerate);
			genEvent.refreshTimeout();
			if (step == EDhApiWorldGenerationStep.NOISE)
			{
				return;
			}
			
			genEvent.timer.nextEvent("surface");
			throwIfThreadInterrupted();
			stepSurface.generateGroup(genEvent.threadedParam, region, chunksToGenerate);
			genEvent.refreshTimeout();
			if (step == EDhApiWorldGenerationStep.SURFACE)
			{
				return;
			}
			
			genEvent.timer.nextEvent("carver");
			throwIfThreadInterrupted();
			if (step == EDhApiWorldGenerationStep.CARVERS)
			{
				return;
			}
			
			genEvent.timer.nextEvent("feature");
			throwIfThreadInterrupted();
			stepFeatures.generateGroup(genEvent.threadedParam, region, chunksToGenerate);
			genEvent.refreshTimeout();
		}
		finally
		{
			genEvent.timer.nextEvent("light");
			
			boolean useMinecraftLightingEngine = Config.Client.Advanced.WorldGenerator.lightingEngine.get() == ELightGenerationMode.MINECRAFT;
			if (useMinecraftLightingEngine)
			{
				// generates chunk lighting using MC's methods
				
				if (!Thread.interrupted())
				{
					this.stepLight.generateGroup(region.getLightEngine(), chunksToGenerate);
				}
			}
			else
			{
				// ignores lighting
				
				chunksToGenerate.forEach((chunk) ->
				{
					if (chunk instanceof ProtoChunk)
					{
						chunk.setLightCorrect(true); // TODO why are we checking instanceof ProtoChunk?
						// TODO: This is due to old times where it may return actual live chunks, which is LevelChunk.
						//   that though is no longer needed...
					}
				
				#if POST_MC_1_18_1
					if (chunk instanceof LevelChunk)
					{
						LevelChunk levelChunk = (LevelChunk) chunk;
						levelChunk.setLightCorrect(true);
						levelChunk.setClientLightReady(true);
						levelChunk.loaded = true;
					}
				#endif
				});
			}
			
			genEvent.refreshTimeout();
		}
	}
	
	public interface EmptyChunkGenerator { ChunkAccess generate(int x, int z); }
	
	@Override
	public int getEventCount() { return this.generationEventList.size(); }
	
	@Override
	public void stop()
	{
		EVENT_LOGGER.info(BatchGenerationEnvironment.class.getSimpleName()+" shutting down...");
		
		EVENT_LOGGER.info("Canceling in progress generation event futures...");
		Iterator<GenerationEvent> iter = this.generationEventList.iterator();
		while (iter.hasNext())
		{
			GenerationEvent event = iter.next();
			event.future.cancel(true);
			iter.remove();
		}
		
		// clear the chunk cache
		var regionStorage = this.regionFileStorageCacheRef.get();
		if (regionStorage != null)
		{
			try
			{
				regionStorage.close();
			}
			catch (IOException e)
			{
				EVENT_LOGGER.error("Failed to close region file storage cache!", e);
			}
		}
		
		EVENT_LOGGER.info(BatchGenerationEnvironment.class.getSimpleName()+" shutdown complete.");
	}
	
	@Override
	public CompletableFuture<Void> generateChunks(
			int minX, int minZ, int genSize, EDhApiWorldGenerationStep targetStep,
			ExecutorService worldGeneratorThreadPool, Consumer<IChunkWrapper> resultConsumer)
	{
		//System.out.println("GenerationEvent: "+genSize+"@"+minX+","+minZ+" "+targetStep);
		
		// TODO: Check event overlap via e.tooClose()
		GenerationEvent genEvent = GenerationEvent.startEvent(new DhChunkPos(minX, minZ), genSize, this, targetStep, resultConsumer, worldGeneratorThreadPool);
		this.generationEventList.add(genEvent);
		return genEvent.future;
	}
	
	/**
	 * Called before code that may run for an extended period of time. <br>
	 * This is necessary to allow canceling world gen since waiting
	 * for some world gen requests to finish can take a while.
	 */
	private static void throwIfThreadInterrupted() throws InterruptedException
	{
		if (Thread.interrupted())
		{
			throw new InterruptedException(FullDataToRenderDataTransformer.class.getSimpleName()+" task interrupted.");
		}
	}
	
}