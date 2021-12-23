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

import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.builders.lodBuilding.LodBuilderConfig;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.objects.lod.LodDimension;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.datafixers.DataFixer;
import com.seibel.lod.common.wrappers.chunk.ChunkWrapper;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.storage.ChunkScanAccess;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.ReportedException;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.storage.WorldData;

public final class WorldGenerationStep {
	
	enum Steps {
		Empty, StructureStart, StructureReference, Biomes, Noise, Surface, Carvers, LiquidCarvers, Features, Light,
	}

	public static final class GridList<T> extends ArrayList<T> implements List<T> {
		
		public static class Pos {
			public int x;
			public int y;
			public Pos(int xx, int yy) {x=xx;y=yy;}
		}

		private static final long serialVersionUID = 1585978374811888116L;
		public final int gridCentreToEdge;
		public final int gridSize;

		public GridList(int gridCentreToEdge) {
			super((gridCentreToEdge * 2 + 1) * (gridCentreToEdge * 2 + 1));
			gridSize = gridCentreToEdge * 2 + 1;
			this.gridCentreToEdge = gridCentreToEdge;
		}

		public final T getOffsetOf(int index, int x, int y) {
			return get(index + x + y * gridSize);
		}
		
		public final int offsetOf(int index, int x, int y) {
			return index + x + y * gridSize;
		}
		
		public final Pos posOf(int index) {
			return new Pos(index%gridSize, index/gridSize);
		}
		public final int calculateOffset(int x, int y) {
			return x + y * gridSize;
		}

		public GridList<T> subGrid(int gridCentreToEdge) {
			int centreIndex = size()/2;
			GridList<T> subGrid = new GridList<T>(gridCentreToEdge);
			for (int oy = -gridCentreToEdge; oy <= gridCentreToEdge; oy++) {
				int begin = offsetOf(centreIndex, -gridCentreToEdge, oy);
				int end = offsetOf(centreIndex, gridCentreToEdge, oy);
				subGrid.addAll(this.subList(begin, end+1));
			}
			//System.out.println("========================================\n"+
			//this.toDetailString() + "\nTOOOOOOOOOOOOO\n"+subGrid.toDetailString()+
			//"==========================================\n");
			return subGrid;
		}
		
		@Override
		public String toString() {
			return "GridList "+gridSize+"*"+gridSize+"["+size()+"]";
		}
		public String toDetailString() {
			StringBuilder str = new StringBuilder("\n");
			int i = 0;
			for (T t : this) {
				str.append(t.toString());
				str.append(", ");
				i++;
				if (i%gridSize == 0) {
					str.append("\n");
				}
			}
			return str.toString();
		}
	}

	public static final class GlobalParameters {
		final ChunkGenerator generator;
		final StructureManager structures;
		final BiomeManager biomeManager;
		final WorldGenSettings worldGenSettings;
		final ThreadedLevelLightEngine lightEngine;
		final LodBuilder lodBuilder;
		final LodDimension lodDim;
		final  Registry<Biome> biomes;
		final RegistryAccess registry;
		final long worldSeed;
		final ChunkScanAccess chunkScanner;
		final ServerLevel level; //TODO: Figure out a way to remove this. Maybe ClientLevel also works?
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
			biomeManager = new BiomeManager(level, BiomeManager.obfuscateSeed(worldSeed));
			structures = server.getStructureManager();
			// TODO: Get the current level dimension
			MappedRegistry<LevelStem> mappedRegistry = worldGenSettings.dimensions();
			LevelStem levelStem = (LevelStem) mappedRegistry.get(LevelStem.OVERWORLD);
			if (levelStem == null)
				throw new RuntimeException("There should already be a level.... Right???");
			generator = levelStem.generator();
			chunkScanner = level.getChunkSource().chunkScanner();
			fixerUpper = server.getFixerUpper();
	    }
	}
	
	public static final class ThreadedParameters {
		final StructureFeatureManager structFeat;
		final StructureCheck structCheck;
	    public ThreadedParameters(GlobalParameters param) {
	    	structCheck = new StructureCheck(param.chunkScanner, param.registry, param.structures,
					Level.OVERWORLD, param.generator, param.level, param.generator.getBiomeSource(), param.worldSeed, param.fixerUpper);
	    	structFeat = new StructureFeatureManager(param.level, param.worldGenSettings, structCheck);
	    }
	}

	public static final class GenerationEvent {
		private static int generationFutureDebugIDs = 0;
		final ThreadedParameters tParam;
		final ChunkPos pos;
		final int range;
		final Future<?> future;
		long nanotime;
		final int id;
		final Steps target;
		
		public GenerationEvent(ChunkPos pos, int range, WorldGenerationStep generationGroup, Steps target) {
			nanotime = System.nanoTime();
			this.pos = pos;
			this.range = range;
			id = generationFutureDebugIDs++;
			this.target = target;
			this.tParam = new ThreadedParameters(generationGroup.params);
			future = generationGroup.executors.submit(() -> {
				generationGroup.generateLodFromList(this);
				});
		}
		public final boolean isCompleted() {
			return future.isDone();
		}
		public final boolean hasTimeout(int duration, TimeUnit unit) {
			long currentTime = System.nanoTime();
			long delta = currentTime - nanotime;
			return (delta > TimeUnit.NANOSECONDS.convert(duration, unit));
		}
		public final void terminate() {
			future.cancel(true);
		}
		public final void join() {
			try {
				future.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		public final boolean tooClose(int cx, int cz, int cr) {
			int dist = Math.min(Math.abs(cx - pos.x), Math.abs(cz - pos.z));
			return dist<range+cr;
		}
		public final void refreshTimeout() {
			nanotime = System.nanoTime();
		}
		
		@Override
		public String toString() {
			return id + ":"+ range + "@"+ pos+"("+target+")";
		}
	}
	
	private final static <T> T joinAsync(CompletableFuture<T> f) {
		//while (!f.isDone()) Thread.yield();
		return f.join();
	}
	
	final LinkedList<GenerationEvent> events = new LinkedList<GenerationEvent>();
	final GlobalParameters params;
	final StepStructureStart stepStructureStart = new StepStructureStart();
	final StepStructureReference stepStructureReference = new StepStructureReference();
	final StepBiomes stepBiomes = new StepBiomes();
	final StepNoise stepNoise = new StepNoise();
	final StepSurface stepSurface = new StepSurface();
	final StepCarvers stepCarvers = new StepCarvers();
	final StepLiquidCarvers stepLiquidCarvers = new StepLiquidCarvers();
	final StepFeatures stepFeatures = new StepFeatures();
	
    //public ExecutorService executors = Executors.newWorkStealingPool();
    public final ExecutorService executors = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("Gen-Worker-Thread-%d").build());
	//public ExecutorService executors = Executors.newFixedThreadPool(8, new ThreadFactoryBuilder().setNameFormat("Gen-Worker-Thread-%d").build());

	public final boolean tryAddPoint(int x, int z, int range, Steps target) {
		x = Math.floorDiv(x, range) * range;
		z = Math.floorDiv(z, range) * range;
		
		for (GenerationEvent event : events) {
			if (event.tooClose(x, z, range)) return false;
		}
		events.add(new GenerationEvent(new ChunkPos(x,z), range, this, target));
		return true;
	}
	
	public final void updateAllFutures() {
		// Update all current out standing jobs
		Iterator<GenerationEvent> iter = events.iterator();
		while (iter.hasNext()) {
			GenerationEvent event = iter.next();
			if (event.isCompleted()) {
				try {
					event.join();
				} catch (RuntimeException e) {
					// Ignore.
				} finally {
					iter.remove();
				}
			} else if (event.hasTimeout(5, TimeUnit.SECONDS)) {
				System.err.println(event.id+": Timed out and terminated!");
				try {
					event.terminate();
				} finally {
					iter.remove();
				}
			}
		}
	}
	
	public WorldGenerationStep(ServerLevel level, LodBuilder lodBuilder, LodDimension lodDim) {
		System.out.println("================WORLD_GEN_STEP_INITING=============");
		params = new GlobalParameters(level, lodBuilder, lodDim);
	}
	
	//ConcurrentHashMap<Long, ChunkAccess> chunks = new ConcurrentHashMap<Long, ChunkAccess>();
	// No longer using Long2ObjectLinkedOpenHashMap as I doubt it is multithread
	// safe.
/*
	private final ChunkAccess getCachedChunk(ChunkPos pos) {
		ChunkAccess chunk = chunks.get(pos.toLong());
		if (chunk != null)
			return chunk;
		chunk = new ProtoChunk(pos, UpgradeData.EMPTY, params.level, params.biomes, null);
		ChunkAccess oldVal = chunks.putIfAbsent(pos.toLong(), chunk);
		if (oldVal != null)
			return oldVal;
		return chunk;
	}*/

	public final void generateLodFromList(GenerationEvent event) {
		try {
		//System.out.println("Started event: "+event);
		GridList<ChunkAccess> referencedChunks;
		DistanceGenerationMode generationMode;
		switch (event.target) {
		case Empty:
			return;
		case StructureStart:
			referencedChunks = generateStructureStart(event, event.range);
			generationMode = DistanceGenerationMode.NONE;
			break;
		case StructureReference:
			referencedChunks = generateStructureReference(event, event.range);
			generationMode = DistanceGenerationMode.NONE;
			break;
		case Biomes:
			referencedChunks = generateBiomes(event, event.range);
			generationMode = DistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT;
			break;
		case Noise:
			referencedChunks = generateNoise(event, event.range);
			generationMode = DistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT;
			break;
		case Surface:
			referencedChunks = generateSurface(event, event.range);
			generationMode = DistanceGenerationMode.SURFACE;
			break;
		case Carvers:
			referencedChunks = generateCarvers(event, event.range);
			generationMode = DistanceGenerationMode.SURFACE;
			break;
		case Features:
			referencedChunks = generateFeatures(event, event.range);
			generationMode = DistanceGenerationMode.FEATURES;
			break;
		case LiquidCarvers:
			return;
		case Light:
			return;
		default:
			return;
		}
		int centreIndex = referencedChunks.size() / 2;

		for (int oy = -event.range; oy <= event.range; oy++) {
			for (int ox = -event.range; ox <= event.range; ox++) {
				int targetIndex = referencedChunks.offsetOf(centreIndex, ox, oy);
				ChunkAccess target = referencedChunks.get(targetIndex);
				params.lodBuilder.generateLodNodeFromChunk(params.lodDim, new ChunkWrapper(target), new LodBuilderConfig(generationMode));
			}
		}
		event.refreshTimeout();
		//for (ChunkAccess sync : referencedChunks) {
		//	chunks.remove(sync.getPos().toLong());
		//}
		//System.out.println("Ended event: "+event);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
	} 
	
	public final GridList<ChunkAccess> generateEmpty(GenerationEvent e, int range) {
		int cx = e.pos.x;
		int cy = e.pos.z;
		GridList<ChunkAccess> chunks = new GridList<ChunkAccess>(range);
		
		for (int oy = -range; oy <= range; oy++) {
			for (int ox = -range; ox <= range; ox++) {
				//ChunkAccess target = getCachedChunk(new ChunkPos(cx+ox, cy+oy));
				ChunkAccess target = new ProtoChunk(new ChunkPos(cx+ox, cy+oy), UpgradeData.EMPTY, params.level, params.biomes, null);
				chunks.add(target);
			}
		}
		e.refreshTimeout();
		return chunks;
	}
	
	public final GridList<ChunkAccess> generateStructureStart(GenerationEvent e, int range) {
		int prestepRange = range+8;
		GridList<ChunkAccess> chunks = generateEmpty(e, prestepRange);
		WorldGenRegion region = new WorldGenRegion(params.level, chunks, ChunkStatus.STRUCTURE_STARTS, range);
		//System.out.println("DEBUG: StructureStart:"+pos);
		//System.out.println("DEBUG: StructureStart:\n"+referencedChunks.toDetailString());
		//System.out.println("to:\n"+referencedChunks.subGrid(centreIndex, range).toDetailString());
		stepStructureStart.generateGroup(e.tParam, region, chunks.subGrid(range));
		e.refreshTimeout();
		return chunks;
	}

	public final GridList<ChunkAccess> generateStructureReference(GenerationEvent e, int range) {
		int prestepRange = range;
		GridList<ChunkAccess> chunks = generateStructureStart(e, prestepRange);
		WorldGenRegion region = new WorldGenRegion(params.level, chunks, ChunkStatus.STRUCTURE_REFERENCES, range);
		//System.out.println("DEBUG: StructureReference:"+pos);
		//System.out.println("DEBUG: StructureReference:\n"+referencedChunks.toDetailString());
		//System.out.println("to:\n"+referencedChunks.subGrid(centreIndex, range).toDetailString());
		stepStructureReference.generateGroup(e.tParam, region, chunks.subGrid(range));
		e.refreshTimeout();
		return chunks;
	}

	public final GridList<ChunkAccess> generateBiomes(GenerationEvent e, int range) {
		int prestepRange = range;
		GridList<ChunkAccess> chunks = generateStructureReference(e, prestepRange);
		WorldGenRegion region = new WorldGenRegion(params.level, chunks, ChunkStatus.BIOMES, range);
		//System.out.println("DEBUG: Biomes:"+pos);
		//System.out.println("DEBUG: Biomes:\n"+referencedChunks.toDetailString());
		//System.out.println("to:\n"+referencedChunks.subGrid(centreIndex, range).toDetailString());
		stepBiomes.generateGroup(e.tParam, region, chunks.subGrid(range), executors);
		e.refreshTimeout();
		return chunks;
	}

	public final GridList<ChunkAccess> generateNoise(GenerationEvent e, int range) {
		int prestepRange = range;
		GridList<ChunkAccess> chunks = generateBiomes(e, prestepRange);
		WorldGenRegion region = new WorldGenRegion(params.level, chunks, ChunkStatus.NOISE, range);
		//System.out.println("DEBUG: Noise:"+pos);
		//System.out.println("DEBUG: Noise:\n"+referencedChunks.toDetailString());
		//System.out.println("to:\n"+referencedChunks.subGrid(centreIndex, range).toDetailString());
		stepNoise.generateGroup(e.tParam, region, chunks.subGrid(range), executors);
		e.refreshTimeout();
		return chunks;
	}

	public final GridList<ChunkAccess> generateSurface(GenerationEvent e, int range) {
		int prestepRange = range;
		GridList<ChunkAccess> chunks = generateNoise(e, prestepRange);
		WorldGenRegion region = new WorldGenRegion(params.level, chunks, ChunkStatus.SURFACE, range);
		//System.out.println("DEBUG: Surface:"+pos);
		//System.out.println("DEBUG: Surface:\n"+referencedChunks.toDetailString());
		//System.out.println("to:\n"+referencedChunks.subGrid(centreIndex, range).toDetailString());
		stepSurface.generateGroup(e.tParam, region, chunks.subGrid(range));
		e.refreshTimeout();
		return chunks;
	}


	public final GridList<ChunkAccess> generateCarvers(GenerationEvent e, int range) {
		int prestepRange = range;
		GridList<ChunkAccess> chunks = generateSurface(e, prestepRange);
		WorldGenRegion region = new WorldGenRegion(params.level, chunks, ChunkStatus.CARVERS, range);
		//System.out.println("DEBUG: Carvers:"+pos);
		//System.out.println("DEBUG: Carvers:\n"+referencedChunks.toDetailString());
		//System.out.println("to:\n"+referencedChunks.subGrid(centreIndex, range).toDetailString());
		stepCarvers.generateGroup(e.tParam, region, chunks.subGrid(range));
		e.refreshTimeout();
		return chunks;
	}


	public final GridList<ChunkAccess> generateFeatures(GenerationEvent e, int range) {
		int prestepRange = range;
		GridList<ChunkAccess> chunks = generateCarvers(e, prestepRange);
		WorldGenRegion region = new WorldGenRegion(params.level, chunks, ChunkStatus.FEATURES, range + 1);
		//System.out.println("DEBUG: Features:"+pos+" range:"+range);
		//System.out.println("DEBUG: Features:\n"+referencedChunks.toDetailString());
		//System.out.println("to:\n"+referencedChunks.subGrid(centreIndex, range).toDetailString());
		stepFeatures.generateGroup(e.tParam, region, chunks.subGrid(range));
		e.refreshTimeout();
		return chunks;
	}
	
	public final class StepStructureStart {
		public final ChunkStatus STATUS = ChunkStatus.STRUCTURE_STARTS;
		public final int RANGE = STATUS.getRange();
		public final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();

		public final void generateGroup(ThreadedParameters tParams, WorldGenRegion worldGenRegion, List<ChunkAccess> chunks) {
			// Note: Not certain StructureFeatureManager.forWorldGenRegion(...) is thread safe
			for (ChunkAccess chunk : chunks) {
				if (params.worldGenSettings.generateFeatures()) {
					//System.out.println("StepStructureStart: "+chunk.getPos());
					// Should be thread safe
					params.generator.createStructures(params.registry, tParams.structFeat, chunk, params.structures, params.worldSeed);
				}
				((ProtoChunk) chunk).setStatus(STATUS);
				tParams.structCheck.onStructureLoad(chunk.getPos(), chunk.getAllStarts());
			}
		}
		/*
		static ChunkAccess load(ServerLevel level, ChunkAccess chunk) {
			((ProtoChunk) chunk).setStatus(STATUS);
			structCheck.onStructureLoad(chunk.getPos(), chunk.getAllStarts());
			return chunk;
		}*/
	}

	public final class StepStructureReference {
		public final ChunkStatus STATUS = ChunkStatus.STRUCTURE_REFERENCES;
		public final int RANGE = STATUS.getRange();
		public final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();
		
		public final void generateGroup(ThreadedParameters tParams, WorldGenRegion worldGenRegion, List<ChunkAccess> chunks) {
			// Note: Not certain StructureFeatureManager.forWorldGenRegion(...) is thread safe
			for (ChunkAccess chunk : chunks) {
				//System.out.println("StepStructureReference: "+chunk.getPos());
				params.generator.createReferences(worldGenRegion, tParams.structFeat.forWorldGenRegion(worldGenRegion), chunk);
				((ProtoChunk) chunk).setStatus(STATUS);
			}
		}
		/*
		static ChunkAccess load(ChunkAccess chunk) {
			((ProtoChunk) chunk).setStatus(STATUS);
			return chunk;
		}*/
	}

	public final class StepBiomes {
		public final ChunkStatus STATUS = ChunkStatus.BIOMES;
		public final int RANGE = STATUS.getRange();
		public final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();
		
		public final void generateGroup(ThreadedParameters tParams, WorldGenRegion worldGenRegion, List<ChunkAccess> chunks, Executor worker) {
			for (ChunkAccess chunk : chunks) {
				//System.out.println("StepBiomes: "+chunk.getPos());
				chunk = joinAsync(params.generator.createBiomes(params.biomes, worker, Blender.of(worldGenRegion),
						tParams.structFeat.forWorldGenRegion(worldGenRegion), chunk));
				((ProtoChunk) chunk).setStatus(STATUS);
			}
		}
		/*
		static ChunkAccess load(ChunkAccess chunk) {
			((ProtoChunk) chunk).setStatus(STATUS);
			return chunk;
		}*/
	}

	public final class StepNoise {
		public final ChunkStatus STATUS = ChunkStatus.NOISE;
		public final int RANGE = STATUS.getRange();
		public final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();

		public final void generateGroup(ThreadedParameters tParams, WorldGenRegion worldGenRegion, List<ChunkAccess> chunks, Executor worker) {
			for (ChunkAccess chunk : chunks) {
				//System.out.println("StepNoise: "+chunk.getPos());
				chunk = joinAsync(params.generator.fillFromNoise(worker, Blender.of(worldGenRegion),
						tParams.structFeat.forWorldGenRegion(worldGenRegion), chunk));
				((ProtoChunk) chunk).setStatus(STATUS);
			}
		}
		/*
		static ChunkAccess load(ChunkAccess chunk) {
			((ProtoChunk) chunk).setStatus(STATUS);
			return chunk;
		}*/
	}

	public final class StepSurface {
		public final ChunkStatus STATUS = ChunkStatus.SURFACE;
		public final int RANGE = STATUS.getRange();
		public final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();
		
		public final void generateGroup(ThreadedParameters tParams, WorldGenRegion worldGenRegion, List<ChunkAccess> chunks) {
			for (ChunkAccess chunk : chunks) {
				//System.out.println("StepSurface: "+chunk.getPos());
				params.generator.buildSurface(worldGenRegion, tParams.structFeat.forWorldGenRegion(worldGenRegion),
						chunk);
				((ProtoChunk) chunk).setStatus(STATUS);
			}
		}
		/*
		static ChunkAccess load(ChunkAccess chunk) {
			((ProtoChunk) chunk).setStatus(STATUS);
			return chunk;
		}*/
	}

	public final class StepCarvers {
		public final ChunkStatus STATUS = ChunkStatus.CARVERS;
		public final int RANGE = STATUS.getRange();
		public final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();

		public final void generateGroup(ThreadedParameters tParams, WorldGenRegion worldGenRegion, List<ChunkAccess> chunks) {
			for (ChunkAccess chunk : chunks) {
				//System.out.println("StepCarvers: "+chunk.getPos());
				Blender.addAroundOldChunksCarvingMaskFilter((WorldGenLevel) worldGenRegion, (ProtoChunk) chunk);
				params.generator.applyCarvers(worldGenRegion, params.worldSeed, params.biomeManager, tParams.structFeat.forWorldGenRegion(worldGenRegion), chunk,
						GenerationStep.Carving.AIR);
				((ProtoChunk) chunk).setStatus(STATUS);
			}
		}
		/*
		static ChunkAccess load(ChunkAccess chunk) {
			((ProtoChunk) chunk).setStatus(STATUS);
			return chunk;
		}*/
	}
	
	public final class StepLiquidCarvers {
		public final ChunkStatus STATUS = ChunkStatus.LIQUID_CARVERS;
		public final int RANGE = STATUS.getRange();
		public final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();

		public final void generateGroup(ThreadedParameters tParams, WorldGenRegion worldGenRegion, List<ChunkAccess> chunks) {
			for (ChunkAccess chunk : chunks) {
				Blender.addAroundOldChunksCarvingMaskFilter((WorldGenLevel) worldGenRegion, (ProtoChunk) chunk);
				params.generator.applyCarvers(worldGenRegion, params.worldSeed, params.biomeManager, tParams.structFeat.forWorldGenRegion(worldGenRegion), chunk,
						GenerationStep.Carving.AIR);
				((ProtoChunk) chunk).setStatus(STATUS);
			}
		}
		/*
		static ChunkAccess load(ChunkAccess chunk) {
			((ProtoChunk) chunk).setStatus(STATUS);
			return chunk;
		}*/
	}

	public final class StepFeatures {
		public final ChunkStatus STATUS = ChunkStatus.FEATURES;
		public final int RANGE = STATUS.getRange();
		public final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();

		public final void generateGroup(ThreadedParameters tParams, WorldGenRegion worldGenRegion, GridList<ChunkAccess> chunks) {
			for (int i=0; i<chunks.size(); i++) {
				ChunkAccess chunk = chunks.get(i);
				ProtoChunk protoChunk = (ProtoChunk) chunk;
				try {
					protoChunk.setLightEngine(params.lightEngine);
					params.generator.applyBiomeDecoration(worldGenRegion, chunk, tParams.structFeat.forWorldGenRegion(worldGenRegion));
					Blender.generateBorderTicks(worldGenRegion, chunk);
				} catch (ReportedException e) {
					//e.printStackTrace();
					// FIXME: Features concurrent modification issue. Something about cocobeans just aren't happy
					// For now just retry.
				} finally {
					Heightmap.primeHeightmaps(chunk,
							EnumSet.of(Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
									Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE));
					protoChunk.setStatus(STATUS);
				}
			}
		}
		/*
		static ChunkAccess load(ChunkAccess chunk) {
			((ProtoChunk) chunk).setStatus(STATUS);
			return chunk;
		}*/
	}
/*
	public static class StepLight {
		public static final ChunkStatus STATUS = ChunkStatus.LIGHT;
		public static final int RANGE = STATUS.getRange();
		public static final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();

		public static final ChunkAccess generate(ChunkAccess chunk) {
			((ProtoChunk) chunk).setStatus(STATUS);
			return joinAsync(lightEngine.lightChunk(chunk, chunk.isLightCorrect()));
		}

		public static final ChunkAccess load(ChunkAccess chunk) {
			((ProtoChunk) chunk).setStatus(STATUS);
			return joinAsync(lightEngine.lightChunk(chunk, chunk.isLightCorrect()));
		}
	}
*/
	// The following may not be needed
	/*
	 * public static class Spawn implements SimpleGen {
	 * 
	 * @Override public EnumSet<Types> getHeightmapTypes() { return POST_FEATURES; }
	 * 
	 * @Override public int getDependencyRange() { return 0; }
	 * 
	 * @Override public final void doSimpleWork(ChunkStatus targetStatus,
	 * ServerLevel level, ChunkGenerator generator, List<ChunkAccess> chunkList,
	 * ChunkAccess chunk) { if (!chunk.isUpgrading())
	 * generator.spawnOriginalMobs(new WorldGenRegion(level, chunkList,
	 * targetStatus, -1)); } } public static class Heightmaps implements SimpleGen {
	 * 
	 * @Override public EnumSet<Types> getHeightmapTypes() { return POST_FEATURES; }
	 * 
	 * @Override public int getDependencyRange() { return 0; }
	 * 
	 * @Override public final void doSimpleWork(ChunkStatus targetStatus,
	 * ServerLevel level, ChunkGenerator generator, List<ChunkAccess> chunkList,
	 * ChunkAccess chunk) { // Apearently nothing again??? Decompiler Error? } }
	 * 
	 * public static class Full implements Gen {
	 * 
	 * @Override public EnumSet<Types> getHeightmapTypes() { return POST_FEATURES; }
	 * 
	 * @Override public int getDependencyRange() { return 0; }
	 * 
	 * @Override public final ChunkAccess doWork(ChunkStatus targetStatus, Executor
	 * worker, ServerLevel level, ChunkGenerator generator, StructureManager
	 * structures, ThreadedLevelLightEngine lightEngine, Mutator function,
	 * List<ChunkAccess> chunkList, ChunkAccess chunk, boolean alwaysRegenerate) {
	 * return function.call(chunk); }
	 * 
	 * @Override public final ChunkAccess load(ChunkStatus targetStatus, ServerLevel
	 * level, StructureManager structures, ThreadedLevelLightEngine lightEngine,
	 * Mutator function, ChunkAccess chunk) { return function.call(chunk); } }
	 * 
	 * 
	 */

}
