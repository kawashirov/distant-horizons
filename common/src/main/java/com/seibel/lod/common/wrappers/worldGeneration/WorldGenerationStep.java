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
import com.seibel.lod.core.api.ModAccessorApi;
import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.builders.lodBuilding.LodBuilderConfig;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.enums.config.LightGenerationMode;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.util.LodThreadFactory;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.IStarlightAccessor;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.mojang.datafixers.DataFixer;
import com.seibel.lod.common.wrappers.chunk.ChunkWrapper;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.lighting.SkyLightEngine;
import net.minecraft.world.level.storage.WorldData;

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

public final class WorldGenerationStep {
	public static final boolean ENABLE_PERF_LOGGING = true;
	public static final boolean ENABLE_EVENT_LOGGING = false;
	public static final boolean ENABLE_LOAD_EVENT_LOGGING = false;
	//TODO: Make actual proper support for StarLight
	
	//FIXME: Move this outside the WorldGenerationStep thingy
	public static class Rolling {

		private final int size;
		private double total = 0d;
		private int index = 0;
		private final double[] samples;

		public Rolling(int size) {
			this.size = size;
			samples = new double[size];
			for (int i = 0; i < size; i++)
				samples[i] = 0d;
		}

		public void add(double x) {
			total -= samples[index];
			samples[index] = x;
			total += x;
			if (++index == size)
				index = 0; // cheaper than modulus
		}

		public double getAverage() {
			return total / size;
		}
	}

	public static class PrefEvent {
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
		public String toString() {
			return
					"beginNano: "+beginNano+",\n"+
					"emptyNano: "+emptyNano+",\n"+
					"structStartNano: "+structStartNano+",\n"+
					"structRefNano: "+structRefNano+",\n"+
					"biomeNano: "+biomeNano+",\n"+
					"noiseNano: "+noiseNano+",\n"+
					"surfaceNano: "+surfaceNano+",\n"+
					"carverNano: "+carverNano+",\n"+
					"featureNano: "+featureNano+",\n"+
					"lightNano: "+lightNano+",\n"+
					"endNano: "+endNano+"\n";
		}
	}

	public static class PerfCalculator {
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

		public void recordEvent(PrefEvent e) {
			long preTime = e.beginNano;
			totalTime.add(e.endNano - preTime);
			if (e.emptyNano!=0) {
				emptyTime.add(e.emptyNano - preTime);
				preTime = e.emptyNano;
			}
			if (e.structStartNano!=0) {
				structStartTime.add(e.structStartNano - preTime);
				preTime = e.structStartNano;
			}
			if (e.structRefNano!=0) {
				structRefTime.add(e.structRefNano - preTime);
				preTime = e.structRefNano;
			}
			if (e.biomeNano!=0) {
				biomeTime.add(e.biomeNano - preTime);
				preTime = e.biomeNano;
			}
			if (e.noiseNano!=0) {
				noiseTime.add(e.noiseNano - preTime);
				preTime = e.noiseNano;
			}
			if (e.surfaceNano!=0) {
				surfaceTime.add(e.surfaceNano - preTime);
				preTime = e.surfaceNano;
			}
			if (e.carverNano!=0) {
				carverTime.add(e.carverNano - preTime);
				preTime = e.carverNano;
			}
			if (e.featureNano!=0) {
				featureTime.add(e.featureNano - preTime);
				preTime = e.featureNano;
			}
			if (e.lightNano!=0) {
				lightTime.add(e.lightNano - preTime);
				preTime = e.lightNano;
			}
			if (e.endNano!=0) {
				lodTime.add(e.endNano - preTime);
				preTime = e.endNano;
			}
		}

		public String toString() {
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

	enum Steps {
		Empty, StructureStart, StructureReference, Biomes, Noise, Surface, Carvers, LiquidCarvers, Features, Light,
	}

	//FIXME: Remove this and use the Utils one
	public static final class GridList<T> extends ArrayList<T> implements List<T> {

		public static class Pos {
			public int x;
			public int y;

			public Pos(int xx, int yy) {
				x = xx;
				y = yy;
			}
		}

		private static final long serialVersionUID = 1585978374811888116L;
		public final int gridCentreToEdge;
		public final int gridSize;

		public GridList(int gridCentreToEdge) {
			super((gridCentreToEdge * 2 + 1) * (gridCentreToEdge * 2 + 1));
			gridSize = gridCentreToEdge * 2 + 1;
			this.gridCentreToEdge = gridCentreToEdge;
		}

		public T getOffsetOf(int index, int x, int y) {
			return get(index + x + y * gridSize);
		}

		public int offsetOf(int index, int x, int y) {
			return index + x + y * gridSize;
		}

		public Pos posOf(int index) {
			return new Pos(index % gridSize, index / gridSize);
		}

		public int calculateOffset(int x, int y) {
			return x + y * gridSize;
		}

		public GridList<T> subGrid(int gridCentreToEdge) {
			int centreIndex = size() / 2;
			GridList<T> subGrid = new GridList<T>(gridCentreToEdge);
			for (int oy = -gridCentreToEdge; oy <= gridCentreToEdge; oy++) {
				int begin = offsetOf(centreIndex, -gridCentreToEdge, oy);
				int end = offsetOf(centreIndex, gridCentreToEdge, oy);
				subGrid.addAll(this.subList(begin, end + 1));
			}
			// System.out.println("========================================\n"+
			// this.toDetailString() + "\nTOOOOOOOOOOOOO\n"+subGrid.toDetailString()+
			// "==========================================\n");
			return subGrid;
		}

		@Override
		public String toString() {
			return "GridList " + gridSize + "*" + gridSize + "[" + size() + "]";
		}

		public String toDetailString() {
			StringBuilder str = new StringBuilder("\n");
			int i = 0;
			for (T t : this) {
				str.append(t.toString());
				str.append(", ");
				i++;
				if (i % gridSize == 0) {
					str.append("\n");
				}
			}
			return str.toString();
		}
	}

	public static final class GlobalParameters {
		final ChunkGenerator generator;
		final StructureManager structures;
		//final BiomeManager biomeManager;
		final WorldGenSettings worldGenSettings;
		final ThreadedLevelLightEngine lightEngine;
		final LodBuilder lodBuilder;
		final LodDimension lodDim;
		final Registry<Biome> biomes;
		final RegistryAccess registry;
		final long worldSeed;
		final ServerLevel level; // TODO: Figure out a way to remove this. Maybe ClientLevel also works?
		final DataFixer fixerUpper;

		public GlobalParameters(ServerLevel level, LodBuilder lodBuilder, LodDimension lodDim) {
			this.lodBuilder = lodBuilder;
			this.lodDim = lodDim;
			this.level = level;
			lightEngine = (ThreadedLevelLightEngine) level.getLightEngine();
			MinecraftServer server = level.getServer();
			WorldData worldData = server.getWorldData();
			worldGenSettings = worldData.worldGenSettings();
			registry = server.registryAccess();
			biomes = registry.registryOrThrow(Registry.BIOME_REGISTRY);
			worldSeed = worldGenSettings.seed();
			//biomeManager = new BiomeManager(level, BiomeManager.obfuscateSeed(worldSeed));
			structures = server.getStructureManager();
			generator = level.getChunkSource().getGenerator();
			fixerUpper = server.getFixerUpper();
		}
	}

	public static final class ThreadedParameters {
		private static final ThreadLocal<ThreadedParameters> localParam = new ThreadLocal<ThreadedParameters>();
		final ServerLevel level;
		public WorldGenStructFeatManager structFeat = null;
		boolean isValid = true;
		public final PerfCalculator perf = new PerfCalculator();

		public static ThreadedParameters getOrMake(GlobalParameters param) {
			ThreadedParameters tParam = localParam.get();
			if (tParam != null && tParam.isValid && tParam.level == param.level)
				return tParam;
			tParam = new ThreadedParameters(param);
			localParam.set(tParam);
			return tParam;
		}
		public void markAsInvalid() {
			isValid = false;
		}

		private ThreadedParameters(GlobalParameters param) {
			level = param.level;
		}
		
		public void makeStructFeat(WorldGenLevel genLevel, WorldGenSettings worldGenSettings) {
			structFeat = new WorldGenStructFeatManager(level, worldGenSettings, genLevel);
		}
	}

	//======================= Main Event class======================
	public static final class GenerationEvent {
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

		public GenerationEvent(ChunkPos pos, int range, WorldGenerationStep generationGroup, Steps target) {
			nanotime = System.nanoTime();
			this.pos = pos;
			this.range = range;
			id = generationFutureDebugIDs++;
			this.target = target;
			this.tParam = ThreadedParameters.getOrMake(generationGroup.params);
			LightGenerationMode mode = CONFIG.client().worldGenerator().getLightGenerationMode();
			
			
			this.lightMode = mode;
			
			future = generationGroup.executors.submit(() -> {
				generationGroup.generateLodFromList(this);
			});
		}

		public boolean isCompleted() {
			return future.isDone();
		}

		public boolean hasTimeout(int duration, TimeUnit unit) {
			long currentTime = System.nanoTime();
			long delta = currentTime - nanotime;
			return (delta > TimeUnit.NANOSECONDS.convert(duration, unit));
		}

		public boolean terminate() {
			future.cancel(true);
			ClientApi.LOGGER.info("======================DUMPING ALL THREADS FOR WORLD GEN=======================");
			threadFactory.dumpAllThreadStacks();
			return future.isCancelled();
		}

		public void join() {
			try {
				future.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}

		public boolean tooClose(int cx, int cz, int cr) {
			int distX = Math.abs(cx - pos.x);
			int distZ = Math.abs(cz - pos.z);
			int minRange = cr+range+1; //Need one to account for the center
			minRange += 1+1; // Account for required empty chunks
			return distX < minRange && distZ < minRange;
		}

		public void refreshTimeout() {
			nanotime = System.nanoTime();
		}

		@Override
		public String toString() {
			return id + ":" + range + "@" + pos + "(" + target + ")";
		}
	}
	
	//=================Generation Step===================

	private static <T> T joinSync(CompletableFuture<T> f) {
		if (!f.isDone()) throw new RuntimeException("The future is concurrent!");
		return f.join();
	}

	final LinkedList<GenerationEvent> events = new LinkedList<GenerationEvent>();
	final GlobalParameters params;
	final StepStructureStart stepStructureStart = new StepStructureStart();
	final StepStructureReference stepStructureReference = new StepStructureReference();
	final StepBiomes stepBiomes = new StepBiomes();
	final StepNoise stepNoise = new StepNoise();
	final StepSurface stepSurface = new StepSurface();
	//final StepCarvers stepCarvers = new StepCarvers();
	final StepFeatures stepFeatures = new StepFeatures();
	final StepLight stepLight = new StepLight();
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);

	public static final LodThreadFactory threadFactory = new LodThreadFactory("Gen-Worker-Thread", Thread.MIN_PRIORITY);
	
	//public final ExecutorService executors = Executors
	//		.newCachedThreadPool(new LodThreadFactory("Gen-Worker-Thread", Thread.MIN_PRIORITY));
	public ExecutorService executors = Executors
			.newFixedThreadPool(CONFIG.client().advanced().threading().getNumberOfWorldGenerationThreads(),
					threadFactory);

	public void resizeThreadPool(int newThreadCount)
	{
		executors = Executors.newFixedThreadPool(newThreadCount,
			new LodThreadFactory("Gen-Worker-Thread", Thread.MIN_PRIORITY));
	}
	
	public boolean tryAddPoint(int px, int pz, int range, Steps target) {
		int boxSize = range * 2 + 1;
		int x = Math.floorDiv(px, boxSize) * boxSize + range;
		int z = Math.floorDiv(pz, boxSize) * boxSize + range;

		for (GenerationEvent event : events) {
			if (event.tooClose(x, z, range))
				return false;
		}
		// System.out.println(x + ", "+z);
		events.add(new GenerationEvent(new ChunkPos(x, z), range, this, target));
		return true;
	}

	public void updateAllFutures() {
		// Update all current out standing jobs
		Iterator<GenerationEvent> iter = events.iterator();
		while (iter.hasNext()) {
			GenerationEvent event = iter.next();
			if (event.isCompleted()) {
				try {
					event.join();
				} catch (Throwable e) {
					e.printStackTrace();
					while (e.getCause() != null) {
						e = e.getCause();
						e.printStackTrace();
					}
				} finally {
					iter.remove();
				}
			} else if (event.hasTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				ClientApi.LOGGER.error("Batching World Generator: " + event + " timed out and terminated!");
				ClientApi.LOGGER.info("Dump PrefEvent: "+event.pEvent);
				try {
					if (!event.terminate()) ClientApi.LOGGER.error("Failed to terminate the stuck generation event!");
				} finally {
					iter.remove();
				}
			}
		}
	}

	public WorldGenerationStep(ServerLevel level, LodBuilder lodBuilder, LodDimension lodDim) {
		ClientApi.LOGGER.info("================WORLD_GEN_STEP_INITING=============");
		params = new GlobalParameters(level, lodBuilder, lodDim);
	}
	
	public void startLoadingAllRegionsFromFile(LodDimension lodDim) {
		ServerLevel level = params.level;
		level.getChunkSource();
		
	}
	
	@SuppressWarnings("resource")
	public static ChunkAccess loadOrMakeChunk(ChunkPos chunkPos, ServerLevel level, LevelLightEngine lightEngine) {
		CompoundTag chunkData = null;
		try
		{
			chunkData = level.getChunkSource().chunkMap.readChunk(chunkPos);
		}
		catch (IOException e)
		{
        	ClientApi.LOGGER.error("DistantHorizons: Couldn't load chunk {}", chunkPos, e);
		}
		if (chunkData == null) {
			return new ProtoChunk(chunkPos, UpgradeData.EMPTY);
		} else {
			return ChunkLoader.read(level, lightEngine, chunkPos, chunkData);
		}
		
	}
	
	
	
	public void generateLodFromList(GenerationEvent e) {
		if (ENABLE_EVENT_LOGGING)
			ClientApi.LOGGER.info("Lod Generate Event: "+e.pos);
		e.pEvent.beginNano = System.nanoTime();
		GridList<ChunkAccess> referencedChunks;
		DistanceGenerationMode generationMode;
		LightedWorldGenRegion region;
		WorldGenLevelLightEngine lightEngine;
		LightGetterAdaptor adaptor;
		
		try {
			adaptor = new LightGetterAdaptor(params.level);
			lightEngine = new WorldGenLevelLightEngine(adaptor);
			
			int cx = e.pos.x;
			int cy = e.pos.z;
			int rangeEmpty = e.range + 1;
			GridList<ChunkAccess> chunks = new GridList<ChunkAccess>(rangeEmpty);
			
			@SuppressWarnings("resource")
			EmptyChunkGenerator generator = (int x, int z) -> {
				ChunkPos chunkPos = new ChunkPos(x, z);
				ChunkAccess target = null;
				try {
					target = loadOrMakeChunk(chunkPos, params.level, lightEngine);
				} catch (RuntimeException e2) {
					// Continue...
					e2.printStackTrace();
				}
				if (target == null)
					target = new ProtoChunk(chunkPos, UpgradeData.EMPTY);
				return target;
			};

			for (int oy = -rangeEmpty; oy <= rangeEmpty; oy++) {
				for (int ox = -rangeEmpty; ox <= rangeEmpty; ox++) {
					ChunkAccess target = generator.generate(cx + ox, cy + oy);
					chunks.add(target);
				}
			}
			e.pEvent.emptyNano = System.nanoTime();
			e.refreshTimeout();
			region = new LightedWorldGenRegion(params.level, lightEngine, chunks, ChunkStatus.STRUCTURE_STARTS, rangeEmpty, e.lightMode, generator);
			adaptor.setRegion(region);
			e.tParam.makeStructFeat(region, params.worldGenSettings);
			referencedChunks = chunks.subGrid(e.range);
			referencedChunks = generateDirect(e, referencedChunks, e.target, region);
			
		} catch (StructStartCorruptedException f) {
			e.tParam.markAsInvalid();
			return;
		}

		switch (e.target) {
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
				boolean isFull = target.getStatus() == ChunkStatus.FULL || target instanceof LevelChunk;
				if (isFull) {
					if (ENABLE_LOAD_EVENT_LOGGING) ClientApi.LOGGER.info("Detected full existing chunk at {}", target.getPos());
					params.lodBuilder.generateLodNodeFromChunk(params.lodDim, new ChunkWrapper(target, region),
							new LodBuilderConfig(DistanceGenerationMode.FULL), true);
				} else if (target.getStatus() == ChunkStatus.EMPTY && generationMode == DistanceGenerationMode.NONE) {
					params.lodBuilder.generateLodNodeFromChunk(params.lodDim, new ChunkWrapper(target, region),
							LodBuilderConfig.getFillVoidConfig(), true);
				} else {
					params.lodBuilder.generateLodNodeFromChunk(params.lodDim, new ChunkWrapper(target, region),
							new LodBuilderConfig(generationMode), true);
				}
				if (e.lightMode == LightGenerationMode.FANCY || isFull) {
					lightEngine.retainData(target.getPos(), false);
				} 
				
			}
		}
		e.pEvent.endNano = System.nanoTime();
		e.refreshTimeout();
		if (ENABLE_PERF_LOGGING) {
			e.tParam.perf.recordEvent(e.pEvent);
			ClientApi.LOGGER.info(e.tParam.perf);
		}
	}

	public GridList<ChunkAccess> generateDirect(GenerationEvent e, GridList<ChunkAccess> subRange, Steps step,
			LightedWorldGenRegion region) {
		try {
			subRange.forEach((chunk) -> {
				if (chunk instanceof ProtoChunk) {
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
			//stepCarvers.generateGroup(e.tParam, region, subRange);
			//e.pEvent.carverNano = System.nanoTime();
			//e.refreshTimeout();
			if (step == Steps.Carvers)
				return subRange;
			stepFeatures.generateGroup(e.tParam, region, subRange);
			e.pEvent.featureNano = System.nanoTime();
			e.refreshTimeout();
			return subRange;
		} finally {
			switch (region.lightMode) {
			case FANCY:
				stepLight.generateGroup(region.getLightEngine(), subRange);
				break;
			case FAST:
				subRange.forEach((p) -> {
					if (p instanceof ProtoChunk) ((ProtoChunk)p).setLightCorrect(true);
				});
				break;
			}
			e.pEvent.lightNano = System.nanoTime();
			e.refreshTimeout();
		}
	}
	
	

	public static class StructStartCorruptedException extends RuntimeException {
		private static final long serialVersionUID = -8987434342051563358L;

		public StructStartCorruptedException(ArrayIndexOutOfBoundsException e) {
			super("StructStartCorruptedException");
			super.initCause(e);
			fillInStackTrace();
		}
	}

	public final class StepStructureStart {
		public final ChunkStatus STATUS = ChunkStatus.STRUCTURE_STARTS;
		

		public void generateGroup(ThreadedParameters tParams, WorldGenRegion worldGenRegion,
				List<ChunkAccess> chunks) {

			ArrayList<ChunkAccess> chunksToDo = new ArrayList<ChunkAccess>();
			
			for (ChunkAccess chunk : chunks) {
				if (chunk.getStatus().isOrAfter(STATUS)) continue;
				((ProtoChunk) chunk).setStatus(STATUS);
				chunksToDo.add(chunk);
			}
			
			if (params.worldGenSettings.generateFeatures()) {
				for (ChunkAccess chunk : chunksToDo) {
					// System.out.println("StepStructureStart: "+chunk.getPos());
					params.generator.createStructures(params.registry, tParams.structFeat, chunk, params.structures,
							params.worldSeed);
					//try {
						params.generator.createStructures(params.registry, tParams.structFeat, chunk, params.structures, params.worldSeed);
						//tParams.structCheck.onStructureLoad(chunk.getPos(), chunk.getAllStarts());
					/*} catch (ArrayIndexOutOfBoundsException e) {
						// There's a rare issue with StructStart where it throws ArrayIndexOutOfBounds
						// This means the structFeat is corrupted (For some reason) and I need to reset it.
						// TODO: Figure out in the future why this happens even though I am using new structFeat
						throw new StructStartCorruptedException(e);
					}*/
				}
			}
		}
	}

	public final class StepStructureReference {
		public final ChunkStatus STATUS = ChunkStatus.STRUCTURE_REFERENCES;

		private void createReferences(WorldGenRegion worldGenLevel, StructureFeatureManager structureFeatureManager,
				ChunkAccess chunkAccess) {
			ChunkPos chunkPos = chunkAccess.getPos();
			int j = chunkPos.x;
			int k = chunkPos.z;
			int l = chunkPos.getMinBlockX();
			int m = chunkPos.getMinBlockZ();

			SectionPos sectionPos = SectionPos.of(chunkAccess.getPos(), 0);

			for (int n = j - 8; n <= j + 8; n++) {
				for (int o = k - 8; o <= k + 8; o++) {
					if (!worldGenLevel.hasChunk(n, o))
						continue;
					long p = ChunkPos.asLong(n, o);
					for (StructureStart<?> structureStart : worldGenLevel.getChunk(n, o).getAllStarts().values()) {
						try {
							if (structureStart.isValid()
									&& structureStart.getBoundingBox().intersects(l, m, l + 15, m + 15)) {
								structureFeatureManager.addReferenceForFeature(sectionPos, structureStart.getFeature(),
										p, chunkAccess);
							}
						} catch (Exception exception) {
							CrashReport crashReport = CrashReport.forThrowable(exception,
									"Generating structure reference");
							CrashReportCategory crashReportCategory = crashReport.addCategory("Structure");
							crashReportCategory.setDetail("Id",
									() -> Registry.STRUCTURE_FEATURE.getKey(structureStart.getFeature()).toString());
							crashReportCategory.setDetail("Name", () -> structureStart.getFeature().getFeatureName());
							crashReportCategory.setDetail("Class",
									() -> structureStart.getFeature().getClass().getCanonicalName());
							throw new ReportedException(crashReport);
						}
					}
				}
			}
		}

		public void generateGroup(ThreadedParameters tParams, WorldGenRegion worldGenRegion,
				List<ChunkAccess> chunks) {

			ArrayList<ChunkAccess> chunksToDo = new ArrayList<ChunkAccess>();
			
			for (ChunkAccess chunk : chunks) {
				if (chunk.getStatus().isOrAfter(STATUS)) continue;
				((ProtoChunk) chunk).setStatus(STATUS);
				chunksToDo.add(chunk);
			}
			
			for (ChunkAccess chunk : chunksToDo) {
				// System.out.println("StepStructureReference: "+chunk.getPos());
				createReferences(worldGenRegion, tParams.structFeat.forWorldGenRegion(worldGenRegion), chunk);
			}
		}
	}

	public final class StepBiomes {
		public final ChunkStatus STATUS = ChunkStatus.BIOMES;
	    
		public void generateGroup(ThreadedParameters tParams, WorldGenRegion worldGenRegion,
				List<ChunkAccess> chunks) {

			ArrayList<ChunkAccess> chunksToDo = new ArrayList<ChunkAccess>();
			
			for (ChunkAccess chunk : chunks) {
				if (chunk.getStatus().isOrAfter(STATUS)) continue;
				((ProtoChunk) chunk).setStatus(STATUS);
				chunksToDo.add(chunk);
			}
			
			for (ChunkAccess chunk : chunksToDo) {
				// System.out.println("StepBiomes: "+chunk.getPos());
				params.generator.createBiomes(params.biomes, chunk);
			}
		}
	}

	public final class StepNoise {
		public final ChunkStatus STATUS = ChunkStatus.NOISE;
		
		public void generateGroup(ThreadedParameters tParams, WorldGenRegion worldGenRegion,
				List<ChunkAccess> chunks) {

			ArrayList<ChunkAccess> chunksToDo = new ArrayList<ChunkAccess>();
			
			for (ChunkAccess chunk : chunks) {
				if (chunk.getStatus().isOrAfter(STATUS)) continue;
				((ProtoChunk) chunk).setStatus(STATUS);
				chunksToDo.add(chunk);
			}
			
			for (ChunkAccess chunk : chunksToDo) {
				// System.out.println("StepNoise: "+chunk.getPos());
				 params.generator.fillFromNoise(worldGenRegion, tParams.structFeat, chunk);
			}
		}
	}

	public final class StepSurface {
		public final ChunkStatus STATUS = ChunkStatus.SURFACE;

		public void generateGroup(ThreadedParameters tParams, WorldGenRegion worldGenRegion,
				List<ChunkAccess> chunks) {
			ArrayList<ChunkAccess> chunksToDo = new ArrayList<ChunkAccess>();
			
			for (ChunkAccess chunk : chunks) {
				if (chunk.getStatus().isOrAfter(STATUS)) continue;
				((ProtoChunk) chunk).setStatus(STATUS);
				chunksToDo.add(chunk);
			}
			
			for (ChunkAccess chunk : chunksToDo) {
				// System.out.println("StepSurface: "+chunk.getPos());
				params.generator.buildSurfaceAndBedrock(worldGenRegion, chunk);
			}
		}
	}

	public final class StepFeatures {
		public final ChunkStatus STATUS = ChunkStatus.FEATURES;

		public void applyBiomeDecoration(ChunkGenerator generator, WorldGenRegion worldGenRegion,
				StructureFeatureManager structureFeatureManager, ChunkAccess chunk) {
			int i = chunk.getPos().x;
			int j = chunk.getPos().z;
			int k = i * 16;
			int l = j * 16;
			BlockPos blockPos = new BlockPos(k, 0, l);
			Biome biome = generator.biomeSource.getNoiseBiome((i << 2) + 2, 2, (j << 2) + 2);
			WorldgenRandom worldgenRandom = new WorldgenRandom();
			long m = worldgenRandom.setDecorationSeed(worldGenRegion.getSeed(), k, l);
			try {
				synchronized(this) {
					biome.generate(structureFeatureManager, generator, worldGenRegion, m, worldgenRandom, blockPos);
				}
			} catch (Exception exception) {
				CrashReport crashReport = CrashReport.forThrowable(exception, "Biome decoration");
				crashReport.addCategory("Generation")

						.setDetail("CenterX", Integer.valueOf(i)).setDetail("CenterZ", Integer.valueOf(j))
						.setDetail("Seed", Long.valueOf(m)).setDetail("Biome", biome);
				throw new ReportedException(crashReport);
			}
		}
		
		public void generateGroup(ThreadedParameters tParams, WorldGenRegion worldGenRegion,
				GridList<ChunkAccess> chunks) {
			ArrayList<ChunkAccess> chunksToDo = new ArrayList<ChunkAccess>();
			
			for (ChunkAccess chunk : chunks) {
				if (chunk.getStatus().isOrAfter(STATUS)) continue;
				((ProtoChunk) chunk).setStatus(STATUS);
				chunksToDo.add(chunk);
			}
			
			for (ChunkAccess chunk : chunksToDo) {
				try {
					applyBiomeDecoration(params.generator, worldGenRegion, tParams.structFeat, chunk);
				} catch (ReportedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public final class StepLight {
		public final ChunkStatus STATUS = ChunkStatus.LIGHT;
		
		public void generateGroup(LevelLightEngine lightEngine,
				GridList<ChunkAccess> chunks) {
			//ArrayList<ChunkAccess> chunksToDo = new ArrayList<ChunkAccess>();
			
			for (ChunkAccess chunk : chunks) {
				if (chunk.getStatus().isOrAfter(STATUS)) continue;
				((ProtoChunk) chunk).setStatus(STATUS);
			}
			
			for (ChunkAccess chunk : chunks) {
				boolean hasCorrectBlockLight = (chunk instanceof LevelChunk && chunk.isLightCorrect());
				try {
					if (lightEngine == null) {
						// Do nothing
					} else if (lightEngine instanceof WorldGenLevelLightEngine) {
						((WorldGenLevelLightEngine)lightEngine).lightChunk(chunk, !hasCorrectBlockLight);
					} else if (lightEngine instanceof ThreadedLevelLightEngine) {
						((ThreadedLevelLightEngine) lightEngine).lightChunk(chunk, !hasCorrectBlockLight).join();
					} else {
						assert(false);
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				chunk.setLightCorrect(true);
			}
			lightEngine.runUpdates(Integer.MAX_VALUE, true, true);
		}
	}
	
	public interface EmptyChunkGenerator {
		ChunkAccess generate(int x, int z);
	}
	
	public static class WorldGenStructFeatManager extends StructureFeatureManager {
		WorldGenLevel genLevel;
		WorldGenSettings worldGenSettings;
		public WorldGenStructFeatManager(LevelAccessor levelAccessor, WorldGenSettings worldGenSettings, WorldGenLevel genLevel) {
			super(levelAccessor, worldGenSettings);
			this.genLevel = genLevel;
			this.worldGenSettings = worldGenSettings;
		}
		
		@Override
	    public WorldGenStructFeatManager forWorldGenRegion(WorldGenRegion worldGenRegion) {
	        return new WorldGenStructFeatManager(worldGenRegion, worldGenSettings, worldGenRegion);
	    }

		@Override
	    public Stream<? extends StructureStart<?>> startsForFeature(SectionPos sectionPos2, StructureFeature<?> structureFeature) {
	        return genLevel.getChunk(sectionPos2.x(), sectionPos2.z(), ChunkStatus.STRUCTURE_REFERENCES).getReferencesForFeature(structureFeature)
	        		.stream().map(long_ -> SectionPos.of(new ChunkPos((long)long_), 0)).map(
	        				sectionPos -> this.getStartForFeature((SectionPos)sectionPos, structureFeature,
	        						genLevel.getChunk(sectionPos.x(), sectionPos.z(), ChunkStatus.STRUCTURE_STARTS))).filter(
	        								structureStart -> structureStart != null && structureStart.isValid());
	    }
	}
	
	public static class LightedWorldGenRegion extends WorldGenRegion {
		final WorldGenLevelLightEngine light;
		final LightGenerationMode lightMode;
		final EmptyChunkGenerator generator;
		final int writeRadius;
		final int size;
	    private final ChunkPos firstPos;
	    private final List<ChunkAccess> cache;
		Long2ObjectOpenHashMap<ChunkAccess> chunkMap = new Long2ObjectOpenHashMap<ChunkAccess>();
		public LightedWorldGenRegion(ServerLevel serverLevel, WorldGenLevelLightEngine lightEngine, List<ChunkAccess> list, ChunkStatus chunkStatus, int i,
				LightGenerationMode lightMode, EmptyChunkGenerator generator) {
			super(serverLevel, list);
			this.lightMode = lightMode;
	        this.firstPos = list.get(0).getPos();
			this.generator = generator;
			light = lightEngine;
			writeRadius = i;
			cache = list;
			size = Mth.floor(Math.sqrt(list.size()));
		}

		// Skip updating the related tile entities
	    @Override
	    public boolean setBlock(BlockPos blockPos, BlockState blockState, int i, int j) {
	        ChunkAccess chunkAccess = this.getChunk(blockPos);
	        if (chunkAccess instanceof LevelChunk) return true;
	        chunkAccess.setBlockState(blockPos, blockState, false);
	        //This is for post ticking for water on gen and stuff like that. Not enabled for now.
	        //if (blockState.hasPostProcess(this, blockPos)) this.getChunk(blockPos).markPosForPostprocessing(blockPos); 
	        return true;
	    }

	    // Skip Dropping the item on destroy
	    @Override
	    public boolean destroyBlock(BlockPos blockPos, boolean bl, @Nullable Entity entity, int i) {
	        BlockState blockState = this.getBlockState(blockPos);
	        if (blockState.isAir()) {
	            return false;
	        }
	        return this.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 3, i);
	    }

	    // Skip BlockEntity stuff. It aren't really needed
	    @Override
	    public BlockEntity getBlockEntity(BlockPos blockPos) {
	        return null;
	    }

	    // Skip BlockEntity stuff. It aren't really needed
	    @Override
	    public boolean addFreshEntity(Entity entity) {
	        return true;
	    }
	    
	    // Allays have empty chunks even if it's outside the worldGenRegion
	    //@Override
	    //public boolean hasChunk(int i, int j) {
	    //	return true;
	    //}
	    
	    // Override to ensure no other mod mixins cause skipping the overrided getChunk(...)
	    @Override
	    public ChunkAccess getChunk(int i, int j) {
	        return this.getChunk(i, j, ChunkStatus.EMPTY);
	    }

	    // Override to ensure no other mod mixins cause skipping the overrided getChunk(...)
	    @Override
	    public ChunkAccess getChunk(int i, int j, ChunkStatus chunkStatus) {
	        return this.getChunk(i, j, chunkStatus, true);
	    }

	    // Use this instead of super.getChunk() to bypass C2ME concurrency checks
	    private ChunkAccess superGetChunk(int x, int z, ChunkStatus cs) {
	    	int k = x - firstPos.x;
            int l = z - firstPos.z;
            return cache.get(k + l * size);
	    }
	    // Use this instead of super.hasChunk() to bypass C2ME concurrency checks
	    private boolean superHasChunk(int x, int z) {
	    	int k = x - firstPos.x;
            int l = z - firstPos.z;
	        return l>=0 && l<size && k>=0 && k<size;
	    }
	    
	    // Allow creating empty chunks even if it's outside the worldGenRegion
		@Override
	    @Nullable
	    public ChunkAccess getChunk(int i, int j, ChunkStatus chunkStatus, boolean bl) {
	    	ChunkAccess chunk = getChunkAccess(i, j, chunkStatus, bl);
	    	if (chunk instanceof LevelChunk) {
	    		chunk = new ImposterProtoChunk((LevelChunk) chunk);
	    	}
	    	return chunk;
	    }
	    
	    private static ChunkStatus debugTriggeredForStatus = null;
	    private ChunkAccess getChunkAccess(int i, int j, ChunkStatus chunkStatus, boolean bl) {
	    	ChunkAccess chunk = superHasChunk(i, j) ? superGetChunk(i, j, ChunkStatus.EMPTY) : null;
	    	if (chunk != null && chunk.getStatus().isOrAfter(chunkStatus)) {
                return chunk;
            }
	    	if (!bl) return null;
	    	if (chunk == null) {
		    	chunk = chunkMap.get(ChunkPos.asLong(i, j));
		    	if (chunk == null) {
			    	chunk = generator.generate(i, j);
			    	if (chunk == null) throw new NullPointerException("The provided generator should not return null!");
			    	chunkMap.put(ChunkPos.asLong(i, j), chunk);
		    	}
	    	}
	    	if (chunkStatus != ChunkStatus.EMPTY && chunkStatus != debugTriggeredForStatus) {
	    		ClientApi.LOGGER.info(
	    				"WorldGen requiring "+chunkStatus+" outside expected range detected. Force passing EMPTY chunk and seeing if it works.");
	    		debugTriggeredForStatus = chunkStatus;
	    	}
	    	return chunk;
	    }
	    
	    // Override force use of my own light engine
		@Override
		public LevelLightEngine getLightEngine() {
			return light;
		}

	    // Override force use of my own light engine
		@Override
		public int getBrightness(LightLayer lightLayer, BlockPos blockPos) {
			if (lightMode != LightGenerationMode.FAST) {
				return light.getLayerListener(lightLayer).getLightValue(blockPos);
			}
			if (lightLayer == LightLayer.BLOCK) return 0;
			BlockPos p = super.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockPos);
			return (p.getY()<=blockPos.getY()) ? getMaxLightLevel() : 0;
		}

	    // Override force use of my own light engine
		@Override
		public int getRawBrightness(BlockPos blockPos, int i) {
			if (lightMode != LightGenerationMode.FAST) {
				return light.getRawBrightness(blockPos, i);
			}
			BlockPos p = super.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockPos);
			return (p.getY()<=blockPos.getY()) ? getMaxLightLevel() : 0;
		}

	    // Override force use of my own light engine
		@Override
		public boolean canSeeSky(BlockPos blockPos) {
			return (getBrightness(LightLayer.SKY, blockPos) >= getMaxLightLevel());
		}

		
		
	}
	
	public static class LightGetterAdaptor implements LightChunkGetter {
		private final BlockGetter heightGetter;
		public LightedWorldGenRegion genRegion = null;
		final boolean shouldReturnNull;
		public LightGetterAdaptor(BlockGetter heightAccessor) {
			this.heightGetter = heightAccessor;
			shouldReturnNull = ModAccessorApi.get(IStarlightAccessor.class)!=null;
		}
		public void setRegion(LightedWorldGenRegion region) {
			genRegion = region;
		}
		
		@Override
		public BlockGetter getChunkForLighting(int chunkX, int chunkZ) {
			if (genRegion == null) throw new IllegalStateException("World Gen region has not been set!");
			// May be null
			return genRegion.getChunk(chunkX, chunkZ, ChunkStatus.EMPTY, false);
		}
		@Override
		public BlockGetter getLevel() {
			return shouldReturnNull ? null : (genRegion!=null ? genRegion : heightGetter);
		}
	}

	public static class WorldGenLevelLightEngine extends LevelLightEngine {
		public static final int MAX_SOURCE_LEVEL = 15;
	    public static final int LIGHT_SECTION_PADDING = 1;
	    @Nullable
	    public final BlockLightEngine blockEngine;
	    @Nullable
	    public final SkyLightEngine skyEngine;

	    public WorldGenLevelLightEngine(LightGetterAdaptor genRegion) {
	    	super(genRegion, false, false);
	        this.blockEngine = new BlockLightEngine(genRegion);
	        this.skyEngine = new SkyLightEngine(genRegion);
	    }

	    @Override
	    public void checkBlock(BlockPos blockPos) {
	        if (this.blockEngine != null) {
	            this.blockEngine.checkBlock(blockPos);
	        }
	        if (this.skyEngine != null) {
	            this.skyEngine.checkBlock(blockPos);
	        }
	    }

	    @Override
	    public void onBlockEmissionIncrease(BlockPos blockPos, int i) {
	        if (this.blockEngine != null) {
	            this.blockEngine.onBlockEmissionIncrease(blockPos, i);
	        }
	    }

	    @Override
	    public boolean hasLightWork() {
	        if (this.skyEngine != null && this.skyEngine.hasLightWork()) {
	            return true;
	        }
	        return this.blockEngine != null && this.blockEngine.hasLightWork();
	    }

	    @Override
	    public int runUpdates(int i, boolean bl, boolean bl2) {
	        if (this.blockEngine != null && this.skyEngine != null) {
	            int j = i / 2;
	            int k = this.blockEngine.runUpdates(j, bl, bl2);
	            int l = i - j + k;
	            int m = this.skyEngine.runUpdates(l, bl, bl2);
	            if (k == 0 && m > 0) {
	                return this.blockEngine.runUpdates(m, bl, bl2);
	            }
	            return m;
	        }
	        if (this.blockEngine != null) {
	            return this.blockEngine.runUpdates(i, bl, bl2);
	        }
	        if (this.skyEngine != null) {
	            return this.skyEngine.runUpdates(i, bl, bl2);
	        }
	        return i;
	    }

	    @Override
	    public void updateSectionStatus(SectionPos sectionPos, boolean bl) {
	        if (this.blockEngine != null) {
	            this.blockEngine.updateSectionStatus(sectionPos, bl);
	        }
	        if (this.skyEngine != null) {
	            this.skyEngine.updateSectionStatus(sectionPos, bl);
	        }
	    }

	    @Override
	    public void enableLightSources(ChunkPos chunkPos, boolean bl) {
	        if (this.blockEngine != null) {
	            this.blockEngine.enableLightSources(chunkPos, bl);
	        }
	        if (this.skyEngine != null) {
	            this.skyEngine.enableLightSources(chunkPos, bl);
	        }
	    }

	    @Override
	    public LayerLightEventListener getLayerListener(LightLayer lightLayer) {
	        if (lightLayer == LightLayer.BLOCK) {
	            if (this.blockEngine == null) {
	                return LayerLightEventListener.DummyLightLayerEventListener.INSTANCE;
	            }
	            return this.blockEngine;
	        }
	        if (this.skyEngine == null) {
	            return LayerLightEventListener.DummyLightLayerEventListener.INSTANCE;
	        }
	        return this.skyEngine;
	    }

	    @Override
	    public int getRawBrightness(BlockPos blockPos, int i) {
	        int j = this.skyEngine == null ? 0 : this.skyEngine.getLightValue(blockPos) - i;
	        int k = this.blockEngine == null ? 0 : this.blockEngine.getLightValue(blockPos);
	        return Math.max(k, j);
	    }

	    public void lightChunk(ChunkAccess chunkAccess, boolean needLightBlockUpdate) {
	    	ChunkPos chunkPos = chunkAccess.getPos();
	        chunkAccess.setLightCorrect(false);

            LevelChunkSection[] levelChunkSections = chunkAccess.getSections();
            for (int i = 0; i < 16; i++) {
                LevelChunkSection levelChunkSection = levelChunkSections[i];
                if (!LevelChunkSection.isEmpty(levelChunkSection)) {
                    updateSectionStatus(SectionPos.of(chunkPos, i), false); 
                }
            }
            enableLightSources(chunkPos, true);
            if (needLightBlockUpdate) {
                chunkAccess.getLights().forEach(blockPos ->
                onBlockEmissionIncrease(blockPos, chunkAccess.getLightEmission(blockPos)));
            }
            
            chunkAccess.setLightCorrect(true);
	    }

	    @Override
	    public String getDebugData(LightLayer lightLayer, SectionPos sectionPos) {
	    	throw new UnsupportedOperationException("This should never be used!");
	    }
	    @Override
	    public void queueSectionData(LightLayer lightLayer, SectionPos sectionPos, @Nullable DataLayer dataLayer, boolean bl) {
	        if (lightLayer == LightLayer.BLOCK) {
	            if (this.blockEngine != null) {
	                this.blockEngine.queueSectionData(sectionPos.asLong(), dataLayer, bl);
	            }
	        } else if (this.skyEngine != null) {
	            this.skyEngine.queueSectionData(sectionPos.asLong(), dataLayer, bl);
	        }
	    }
	    @Override
	    public void retainData(ChunkPos chunkPos, boolean bl) {
	        if (this.blockEngine != null) {
	            this.blockEngine.retainData(chunkPos, bl);
	        }
	        if (this.skyEngine != null) {
	            this.skyEngine.retainData(chunkPos, bl);
	        }
	    }
	}




	
}
