package com.seibel.lod.common.wrappers.worldGeneration;

import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.builders.lodBuilding.LodBuilderConfig;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.objects.lod.LodDimension;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.seibel.lod.common.wrappers.chunk.ChunkWrapper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.*;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.lighting.LevelLightEngine;

public class WorldGenerationStep {
	
	private static <T> T joinAsync(CompletableFuture<T> f) {
		//while (!f.isDone()) Thread.yield();
		return f.join();
	}
	
	ServerLevel level;
	ChunkGenerator generator;
	StructureManager structures;
	ThreadedLevelLightEngine lightEngine;
    LodBuilder lodBuilder;
    LodDimension lodDim;
    Registry<Biome> biomes;
    //public ExecutorService executors = Executors.newWorkStealingPool();
    public ExecutorService executors = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("Gen-Worker-Thread-%d").build());
    
	//public ExecutorService executors = Executors.newFixedThreadPool(8, new ThreadFactoryBuilder().setNameFormat("Gen-Worker-Thread-%d").build());
	
	public WorldGenerationStep(ServerLevel level, LodBuilder lodBuilder, LodDimension lodDim) {
		System.out.println("================WORLD_GEN_STEP_INITING=============");
		this.level = level;
		this.lodBuilder = lodBuilder;
		this.lodDim = lodDim;
		lightEngine = (ThreadedLevelLightEngine) level.getLightEngine();
		biomes = level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
		generator = level.getChunkSource().getGenerator();
		structures = level.getStructureManager();
		StepStructureStart.onLevelLoad(level.getServer().getWorldData().worldGenSettings());
		StepBiomes.onLevelLoad(level.registryAccess());
		// Note: This will be problematic...
		StepLight.onLevelLoad(lightEngine);
		
	}

	public static final class GridList<T> extends ArrayList<T> implements List<T> {

		private static final long serialVersionUID = 1585978374811888116L;
		public final int gridCentreToEdge;
		public final int gridSize;

		public GridList(int gridCentreToEdge) {
			super((gridCentreToEdge * 2 + 1) * (gridCentreToEdge * 2 + 1));
			gridSize = gridCentreToEdge * 2 + 1;
			this.gridCentreToEdge = gridCentreToEdge;
		}

		public final int offsetOf(int index, int x, int y) {
			return index + x + y * gridSize;
		}

		public GridList<T> subGrid(int centreIndex, int gridCentreToEdge) {
			GridList<T> subGrid = new GridList<T>(gridCentreToEdge);
			for (int oy = -gridCentreToEdge; oy <= gridCentreToEdge; oy++) {
				int begin = offsetOf(centreIndex, -gridCentreToEdge, oy);
				int end = offsetOf(centreIndex, gridCentreToEdge, oy);
				subGrid.addAll(this.subList(begin, end+1));
			}
			return subGrid;
		}
		
		@Override
		public String toString() {
			return "GridList "+gridSize+"*"+gridSize+"["+size()+"]";
		}
	}

	public static class ChunkSynconizer {
		
		private ReentrantLock uniqueOwnerLock = new ReentrantLock();
		ChunkAccess chunk;
		Steps completedStep = Steps.Empty;

		public ChunkSynconizer(ChunkPos pos, ServerLevel level, Registry<Biome> biomes) {
			chunk = new ProtoChunk(pos, UpgradeData.EMPTY, level, biomes, null);
		}

		public boolean tryClaimOwnerLock() {
			return uniqueOwnerLock.tryLock();
		}

		public void releaseOwnerLock() {
			uniqueOwnerLock.unlock();
		}

		public boolean hasCompletedStep(Steps step) {
			return step.compareTo(completedStep) <= 0;
		}

		public void set(ChunkAccess newChunk, Steps newStep) {
			chunk = newChunk;
			completedStep = newStep;
		}

		public void set(Steps newStep) {
			completedStep = newStep;
		}
	}

	ConcurrentHashMap<Long, ChunkSynconizer> chunks = new ConcurrentHashMap<Long, ChunkSynconizer>();
	// No longer using Long2ObjectLinkedOpenHashMap as I doubt it is multithread
	// safe.

	private static final long toLongPos(int cx, int cy) {
		return ChunkPos.asLong(cx, cy);
	}

	private final ChunkSynconizer getChunkSynconizer(long pos) {
		ChunkSynconizer chunk = chunks.get(pos);
		if (chunk != null)
			return chunk;
		chunk = new ChunkSynconizer(new ChunkPos(pos), level, biomes);
		ChunkSynconizer oldVal = chunks.putIfAbsent(pos, chunk);
		if (oldVal != null)
			return oldVal;
		return chunk;
	}

	public void generateLodFromList(int i, ChunkPos pos, int range, Steps step) {
		System.out.println(i+": generateLodFromList("+pos.toString()+", "+range+", "+step+")");
		GridList<ChunkSynconizer> referencedChunks;
		DistanceGenerationMode generationMode;
		switch (step) {
		case Empty:
			return;
		case StructureStart:
			referencedChunks = generateStructureStart(pos, range);
			generationMode = DistanceGenerationMode.NONE;
			break;
		case StructureReference:
			referencedChunks = generateStructureReference(pos, range);
			generationMode = DistanceGenerationMode.NONE;
			break;
		case Biomes:
			referencedChunks = generateBiomes(pos, range);
			generationMode = DistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT;
			break;
		case Noise:
			referencedChunks = generateNoise(pos, range);
			generationMode = DistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT;
			break;
		case Surface:
			referencedChunks = generateSurface(pos, range);
			generationMode = DistanceGenerationMode.SURFACE;
			break;
		case Carvers:
			referencedChunks = generateCarvers(pos, range);
			generationMode = DistanceGenerationMode.SURFACE;
			break;
		case Features:
			referencedChunks = generateFeatures(pos, range);
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

		for (int ox = -range; ox <= range; ox++) {
			for (int oy = -range; oy <= range; oy++) {
				int targetIndex = referencedChunks.offsetOf(centreIndex, ox, oy);
				ChunkSynconizer target = referencedChunks.get(targetIndex);
				lodBuilder.generateLodNodeFromChunk(lodDim, new ChunkWrapper(target.chunk), new LodBuilderConfig(generationMode));
			}
		}
		for (ChunkSynconizer sync : referencedChunks) {
			chunks.remove(sync.chunk.getPos().toLong());
		}
		
		System.out.println(i+": EXIT: generateLodFromList("+pos.toString()+", "+range+", "+step+")");
	} 
	
	public GridList<ChunkSynconizer> generateStructureStart(ChunkPos pos, int range) {
		//System.out.println("generateStructureStart("+pos.toString()+", "+range+")");
		int cx = pos.x;
		int cy = pos.z;
		GridList<ChunkSynconizer> chunks = new GridList<ChunkSynconizer>(range);
		
		
		for (int ox = -range; ox <= range; ox++) {
			for (int oy = -range; oy <= range; oy++) {
				ChunkSynconizer target = getChunkSynconizer(toLongPos(cx + ox, cy + oy));
				chunks.add(target);
				if (!target.hasCompletedStep(Steps.StructureStart)) {
					
					boolean owned = target.tryClaimOwnerLock();
					if (owned) {
						try {
							ChunkAccess access = target.chunk;
								target.set(StepStructureStart.generate(level, generator, structures, access),
										Steps.StructureStart);
						} finally {
							target.releaseOwnerLock();
						}
					}
					
					
					
				}
			}
		}
		//System.out.println("EXIT: generateStructureStart("+pos.toString()+", "+range+") -> "+chunks);
		return chunks;
	}

	public GridList<ChunkSynconizer> generateStructureReference(ChunkPos pos, int range) {
		int prestepRange = range + StepStructureReference.RANGE;
		GridList<ChunkSynconizer> referencedChunks = generateStructureStart(pos, prestepRange);
		//System.out.println("generateStructureReference(" + pos.toString() + ", " + range + ")");
		int centreIndex = referencedChunks.size() / 2;

		for (int ox = -range; ox <= range; ox++) {
			for (int oy = -range; oy <= range; oy++) {
				int targetIndex = referencedChunks.offsetOf(centreIndex, ox, oy);
				ChunkSynconizer target = referencedChunks.get(targetIndex);
				if (!target.hasCompletedStep(Steps.StructureReference)) {
					boolean owned = target.tryClaimOwnerLock();
					if (owned) {
						try {
							GridList<ChunkSynconizer> reference = referencedChunks.subGrid(targetIndex,
									StepStructureReference.RANGE);
							ArrayList<ChunkAccess> referenceAccess = new ArrayList<ChunkAccess>(reference.size());
							for (ChunkSynconizer ref : reference) {
								referenceAccess.add(ref.chunk);
							}
							StepStructureReference.generate(level, generator, referenceAccess, target.chunk);
							target.set(Steps.StructureReference);
						} finally {
							target.releaseOwnerLock();
						}
					}
				}
			}
		}
		//System.out.println(
		//		"EXIT: generateStructureReference(" + pos.toString() + ", " + range + ") -> " + referencedChunks);
		return referencedChunks;
	}

	public GridList<ChunkSynconizer> generateBiomes(ChunkPos pos, int range) {
		int prestepRange = range + 1;
		GridList<ChunkSynconizer> referencedChunks = generateStructureReference(pos, prestepRange);
		//System.out.println("generateBiomes("+pos.toString()+", "+range+")");
		int centreIndex = referencedChunks.size() / 2;

		for (int ox = -range; ox <= range; ox++) {
			for (int oy = -range; oy <= range; oy++) {
				int targetIndex = referencedChunks.offsetOf(centreIndex, ox, oy);
				ChunkSynconizer target = referencedChunks.get(targetIndex);
				if (!target.hasCompletedStep(Steps.Biomes)) {
					boolean owned = target.tryClaimOwnerLock();
					if (owned) {
						try {
							GridList<ChunkSynconizer> reference = referencedChunks.subGrid(targetIndex,
									StepBiomes.RANGE);
							ArrayList<ChunkAccess> referenceAccess = new ArrayList<ChunkAccess>(reference.size());
							for (ChunkSynconizer ref : reference) {
								referenceAccess.add(ref.chunk);
							}
							target.set(StepBiomes.generate(level, generator, referenceAccess, target.chunk, executors), Steps.Biomes);
						} finally {
							target.releaseOwnerLock();
						}
					}
				}
			}
		}
		//System.out.println("EXIT: generateBiomes("+pos.toString()+", "+range+") -> "+referencedChunks);
		return referencedChunks;
	}

	public GridList<ChunkSynconizer> generateNoise(ChunkPos pos, int range) {
		// System.out.println("generateNoise("+pos.toString()+", "+range+")");
		int prestepRange = range + 1;
		GridList<ChunkSynconizer> referencedChunks = generateBiomes(pos, prestepRange);
		//System.out.println("generateNoise("+pos.toString()+", "+range+")");
		int centreIndex = referencedChunks.size() / 2;

		for (int ox = -range; ox <= range; ox++) {
			for (int oy = -range; oy <= range; oy++) {
				int targetIndex = referencedChunks.offsetOf(centreIndex, ox, oy);
				ChunkSynconizer target = referencedChunks.get(targetIndex);
				if (!target.hasCompletedStep(Steps.Noise)) {
					boolean owned = target.tryClaimOwnerLock();
					if (owned) {
						try {
							GridList<ChunkSynconizer> reference = referencedChunks.subGrid(targetIndex,
									StepNoise.RANGE);
							ArrayList<ChunkAccess> referenceAccess = new ArrayList<ChunkAccess>(reference.size());
							for (ChunkSynconizer ref : reference) {
								referenceAccess.add(ref.chunk);
							}
							target.set(StepNoise.generate(level, generator, referenceAccess, target.chunk, executors),
									Steps.Noise);
						} finally {
							target.releaseOwnerLock();
						}
					}
				}
			}
		}
		//System.out.println("EXIT: generateNoise(" + pos.toString() + ", " + range + ") -> " + referencedChunks);
		return referencedChunks;
	}

	public GridList<ChunkSynconizer> generateSurface(ChunkPos pos, int range) {
		int prestepRange = range + 1;
		GridList<ChunkSynconizer> referencedChunks = generateNoise(pos, prestepRange);
		//System.out.println("generateSurface("+pos.toString()+", "+range+")");
		int centreIndex = referencedChunks.size() / 2;

		for (int ox = -range; ox <= range; ox++) {
			for (int oy = -range; oy <= range; oy++) {
				int targetIndex = referencedChunks.offsetOf(centreIndex, ox, oy);
				ChunkSynconizer target = referencedChunks.get(targetIndex);
				if (!target.hasCompletedStep(Steps.Surface)) {
					boolean owned = target.tryClaimOwnerLock();
					if (owned) {
						try {
							GridList<ChunkSynconizer> reference = referencedChunks.subGrid(targetIndex,
									StepSurface.RANGE);
							ArrayList<ChunkAccess> referenceAccess = new ArrayList<ChunkAccess>(reference.size());
							for (ChunkSynconizer ref : reference) {
								referenceAccess.add(ref.chunk);
							}
							
							target.set(StepSurface.generate(level, generator, referenceAccess, target.chunk),
									Steps.Surface);
						} finally {
							target.releaseOwnerLock();
						}
					}
				}
			}
		}
		//System.out.println("EXIT: generateNoise(" + pos.toString() + ", " + range + ") -> " + referencedChunks);
		return referencedChunks;
	}


	public GridList<ChunkSynconizer> generateCarvers(ChunkPos pos, int range) {
		int prestepRange = range + 1;
		GridList<ChunkSynconizer> referencedChunks = generateSurface(pos, prestepRange);
		//System.out.println("generateCarvers("+pos.toString()+", "+range+")");
		int centreIndex = referencedChunks.size() / 2;

		for (int ox = -range; ox <= range; ox++) {
			for (int oy = -range; oy <= range; oy++) {
				int targetIndex = referencedChunks.offsetOf(centreIndex, ox, oy);
				ChunkSynconizer target = referencedChunks.get(targetIndex);
				if (!target.hasCompletedStep(Steps.Carvers)) {
					boolean owned = target.tryClaimOwnerLock();
					if (owned) {
						try {
							GridList<ChunkSynconizer> reference = referencedChunks.subGrid(targetIndex,
									StepCarvers.RANGE);
							ArrayList<ChunkAccess> referenceAccess = new ArrayList<ChunkAccess>(reference.size());
							for (ChunkSynconizer ref : reference) {
								referenceAccess.add(ref.chunk);
							}
							target.set(StepCarvers.generate(level, generator, referenceAccess, target.chunk),
									Steps.Carvers);
						} finally {
							target.releaseOwnerLock();
						}
					}
				}
			}
		}
		//System.out.println("EXIT: generateNoise(" + pos.toString() + ", " + range + ") -> " + referencedChunks);
		return referencedChunks;
	}


	public GridList<ChunkSynconizer> generateFeatures(ChunkPos pos, int range) {
		int prestepRange = range + 1;
		GridList<ChunkSynconizer> referencedChunks = generateCarvers(pos, prestepRange);
		//System.out.println("generateFeatures("+pos.toString()+", "+range+")");
		int centreIndex = referencedChunks.size() / 2;

		for (int ox = -range; ox <= range; ox++) {
			for (int oy = -range; oy <= range; oy++) {
				int targetIndex = referencedChunks.offsetOf(centreIndex, ox, oy);
				ChunkSynconizer target = referencedChunks.get(targetIndex);
				if (!target.hasCompletedStep(Steps.Features)) {
					boolean owned = target.tryClaimOwnerLock();
					if (owned) {
						try {
							GridList<ChunkSynconizer> reference = referencedChunks.subGrid(targetIndex,
									StepFeatures.RANGE);
							ArrayList<ChunkAccess> referenceAccess = new ArrayList<ChunkAccess>(reference.size());
							for (ChunkSynconizer ref : reference) {
								referenceAccess.add(ref.chunk);
							}
							target.set(StepFeatures.generate(level, generator, lightEngine, referenceAccess, target.chunk),
									Steps.Features);
						} finally {
							target.releaseOwnerLock();
						}
					}
				}
			}
		}
		//System.out.println("EXIT: generateNoise(" + pos.toString() + ", " + range + ") -> " + referencedChunks);
		return referencedChunks;
	}
	
	
	
	
	enum Steps {
		Empty, StructureStart, StructureReference, Biomes, Noise, Surface, Carvers, LiquidCarvers, Features, Light,
	}

	public static class StepStructureStart {
		public static final ChunkStatus STATUS = ChunkStatus.STRUCTURE_STARTS;
		public static final int RANGE = STATUS.getRange();
		public static final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();
		static boolean doGenerateFeatures = true;

		public final static void onLevelLoad(WorldGenSettings genSettings) {
			doGenerateFeatures = genSettings.generateFeatures();
		}

		public final static ChunkAccess generate(ServerLevel level, ChunkGenerator generator,
				StructureManager structures, ChunkAccess chunk) {

			if (doGenerateFeatures) {
				// Should be thread safe
				generator.createStructures(level.registryAccess(), level.structureFeatureManager(), chunk, structures,
						level.getSeed());
			}
			((ProtoChunk) chunk).setStatus(STATUS);
			level.onStructureStartsAvailable(chunk);
			return chunk;
		}

		static ChunkAccess load(ServerLevel level, ChunkAccess chunk) {
			((ProtoChunk) chunk).setStatus(STATUS);
			level.onStructureStartsAvailable(chunk);
			return chunk;
		}
	}

	public static class StepStructureReference {
		public static final ChunkStatus STATUS = ChunkStatus.STRUCTURE_REFERENCES;
		public static final int RANGE = STATUS.getRange();
		public static final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();

		public static final void generate(ServerLevel level, ChunkGenerator generator, List<ChunkAccess> chunkList,
				ChunkAccess chunk) {
			WorldGenRegion worldGenRegion = new WorldGenRegion(level, chunkList, STATUS, -1);
			// Note: Not certain StructureFeatureManager.forWorldGenRegion(...) is thread
			// safe
			generator.createReferences((WorldGenLevel) worldGenRegion,
					level.structureFeatureManager().forWorldGenRegion(worldGenRegion), chunk);
			((ProtoChunk) chunk).setStatus(STATUS);
		}

		static ChunkAccess load(ChunkAccess chunk) {
			((ProtoChunk) chunk).setStatus(STATUS);
			return chunk;
		}
	}

	public static class StepBiomes {
		public static final ChunkStatus STATUS = ChunkStatus.BIOMES;
		public static final int RANGE = STATUS.getRange();
		public static final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();
		public static Registry<Biome> biomeRegistry;

		public final static void onLevelLoad(RegistryAccess registry) {
			biomeRegistry = registry.registryOrThrow(Registry.BIOME_REGISTRY);
		}

		public static final ChunkAccess generate(ServerLevel level, ChunkGenerator generator,
				List<ChunkAccess> chunkList, ChunkAccess chunk, Executor worker) {
			WorldGenRegion worldGenRegion = new WorldGenRegion(level, chunkList, STATUS, -1);
				chunk = joinAsync(generator.createBiomes(biomeRegistry, worker, Blender.of(worldGenRegion),
						level.structureFeatureManager().forWorldGenRegion(worldGenRegion), chunk));
			((ProtoChunk) chunk).setStatus(STATUS);
			return chunk;
		}

		static ChunkAccess load(ChunkAccess chunk) {
			((ProtoChunk) chunk).setStatus(STATUS);
			return chunk;
		}
	}

	public static class StepNoise {
		public static final ChunkStatus STATUS = ChunkStatus.NOISE;
		public static final int RANGE = STATUS.getRange();
		public static final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();

		public static final ChunkAccess generate(ServerLevel level, ChunkGenerator generator,
				List<ChunkAccess> chunkList, ChunkAccess chunk, Executor worker) {
			WorldGenRegion worldGenRegion = new WorldGenRegion(level, chunkList, STATUS, 0);
			chunk = joinAsync(generator.fillFromNoise(worker, Blender.of(worldGenRegion),
					level.structureFeatureManager().forWorldGenRegion(worldGenRegion), chunk));
			((ProtoChunk) chunk).setStatus(STATUS);
			return chunk;
		}

		static ChunkAccess load(ChunkAccess chunk) {
			((ProtoChunk) chunk).setStatus(STATUS);
			return chunk;
		}
	}

	public static class StepSurface {
		public static final ChunkStatus STATUS = ChunkStatus.SURFACE;
		public static final int RANGE = STATUS.getRange();
		public static final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();

		public static final ChunkAccess generate(ServerLevel level, ChunkGenerator generator,
				List<ChunkAccess> chunkList, ChunkAccess chunk) {
			WorldGenRegion worldGenRegion = new WorldGenRegion(level, chunkList, STATUS, 0);
			generator.buildSurface(worldGenRegion, level.structureFeatureManager().forWorldGenRegion(worldGenRegion),
					chunk);
			((ProtoChunk) chunk).setStatus(STATUS);
			return chunk;
		}

		static ChunkAccess load(ChunkAccess chunk) {
			((ProtoChunk) chunk).setStatus(STATUS);
			return chunk;
		}
	}

	public static class StepCarvers {
		public static final ChunkStatus STATUS = ChunkStatus.CARVERS;
		public static final int RANGE = STATUS.getRange();
		public static final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();

		public static final ChunkAccess generate(ServerLevel level, ChunkGenerator generator,
				List<ChunkAccess> chunkList, ChunkAccess chunk) {
			WorldGenRegion worldGenRegion = new WorldGenRegion(level, chunkList, STATUS, 0);
			Blender.addAroundOldChunksCarvingMaskFilter((WorldGenLevel) worldGenRegion, (ProtoChunk) chunk);
			generator.applyCarvers(worldGenRegion, level.getSeed(), level.getBiomeManager(),
					level.structureFeatureManager().forWorldGenRegion(worldGenRegion), chunk,
					GenerationStep.Carving.AIR);
			((ProtoChunk) chunk).setStatus(STATUS);
			return chunk;
		}

		static ChunkAccess load(ChunkAccess chunk) {
			((ProtoChunk) chunk).setStatus(STATUS);
			return chunk;
		}
	}

	public static class StepLiquidCarvers {
		public static final ChunkStatus STATUS = ChunkStatus.LIQUID_CARVERS;
		public static final int RANGE = STATUS.getRange();
		public static final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();

		public static final ChunkAccess generate(ServerLevel level, ChunkGenerator generator,
				List<ChunkAccess> chunkList, ChunkAccess chunk, Executor worker) {
			// FIXME: I think the decompiler failed on this one. Find the actual body and
			// put it here.
			((ProtoChunk) chunk).setStatus(STATUS);
			return chunk;
		}

		static ChunkAccess load(ChunkAccess chunk) {
			((ProtoChunk) chunk).setStatus(STATUS);
			return chunk;
		}
	}

	public static class StepFeatures {
		public static final ChunkStatus STATUS = ChunkStatus.FEATURES;
		public static final int RANGE = STATUS.getRange();
		public static final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();

		public static final ChunkAccess generate(ServerLevel level, ChunkGenerator generator,
				ThreadedLevelLightEngine lightEngine, List<ChunkAccess> chunkList, ChunkAccess chunk) {
			ProtoChunk protoChunk = (ProtoChunk) chunk;
			protoChunk.setLightEngine((LevelLightEngine) lightEngine);

			Heightmap.primeHeightmaps(chunk,
					EnumSet.of(Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
							Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE));

			// This could be problematic. May need to lock the 8 surrounding chunks then.
			WorldGenRegion worldGenRegion = new WorldGenRegion(level, chunkList, STATUS, 1);

			generator.applyBiomeDecoration((WorldGenLevel) worldGenRegion, chunk,
					level.structureFeatureManager().forWorldGenRegion(worldGenRegion));
			Blender.generateBorderTicks(worldGenRegion, chunk);

			protoChunk.setStatus(STATUS);
			return chunk;
		}

		static ChunkAccess load(ChunkAccess chunk) {
			((ProtoChunk) chunk).setStatus(STATUS);
			return chunk;
		}
	}

	public static class StepLight {
		public static final ChunkStatus STATUS = ChunkStatus.LIGHT;
		public static final int RANGE = STATUS.getRange();
		public static final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();
		private static ThreadedLevelLightEngine lightEngine;

		public final static void onLevelLoad(ThreadedLevelLightEngine engine) {
			lightEngine = engine;
		}

		public static final ChunkAccess generate(ChunkAccess chunk) {
			((ProtoChunk) chunk).setStatus(STATUS);
			return joinAsync(lightEngine.lightChunk(chunk, chunk.isLightCorrect()));
		}

		public static final ChunkAccess load(ChunkAccess chunk) {
			((ProtoChunk) chunk).setStatus(STATUS);
			return joinAsync(lightEngine.lightChunk(chunk, chunk.isLightCorrect()));
		}
	}

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
