/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
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

package com.seibel.lod.builders.worldGeneration;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.seibel.lod.builders.lodBuilding.LodBuilder;
import com.seibel.lod.builders.lodBuilding.LodBuilderConfig;
import com.seibel.lod.config.LodConfig;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.proxy.ClientProxy;
import com.seibel.lod.util.LodUtil;
import com.seibel.lod.wrappers.Chunk.ChunkPosWrapper;
import com.seibel.lod.wrappers.Chunk.ChunkWrapper;

import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraftforge.common.WorldWorkerManager.IWorker;

/**
 * This is used to generate a LodChunk at a given ChunkPos.
 * 
 * @author James Seibel
 * @version 10-22-2021
 */
public class LodGenWorker implements IWorker
{
	public static ExecutorService genThreads = Executors.newFixedThreadPool(LodConfig.CLIENT.advancedModOptions.threading.numberOfWorldGenerationThreads.get(), new ThreadFactoryBuilder().setNameFormat("Gen-Worker-Thread-%d").build());
	
	private boolean threadStarted = false;
	private final LodChunkGenThread thread;
	
	
	/**
	 * If a configured feature fails for whatever reason,
	 * add it to this list, this is to hopefully remove any
	 * features that could cause issues down the line.
	 */
	private static final ConcurrentHashMap<Integer, ConfiguredFeature<?, ?>> configuredFeaturesToAvoid = new ConcurrentHashMap<>();
	
	
	
	public LodGenWorker(ChunkPosWrapper newPos, DistanceGenerationMode newGenerationMode,
			LodBuilder newLodBuilder,
			LodDimension newLodDimension, ServerLevel newServerLevel)
	{
		// just a few sanity checks
		if (newPos == null)
			throw new IllegalArgumentException("LodChunkGenWorker must have a non-null ChunkPos");
		
		if (newLodBuilder == null)
			throw new IllegalArgumentException("LodChunkGenThread requires a non-null LodChunkBuilder");
		
		if (newLodDimension == null)
			throw new IllegalArgumentException("LodChunkGenThread requires a non-null LodDimension");
		
		if (newServerLevel == null)
			throw new IllegalArgumentException("LodChunkGenThread requires a non-null ServerLevel");
		
		
		
		thread = new LodChunkGenThread(newPos, newGenerationMode,
				newLodBuilder,
				newLodDimension, newServerLevel);
	}
	
	@Override
	public boolean doWork()
	{
		if (!threadStarted)
		{
			if (LodConfig.CLIENT.worldGenerator.distanceGenerationMode.get() == DistanceGenerationMode.SERVER)
			{
				// if we are using SERVER generation that has to be done
				// synchronously to prevent crashing and harmful
				// interactions with the normal world generator
				thread.run();
			}
			else
			{
				// Every other method can
				// be done asynchronously
				Thread newThread = new Thread(thread);
				newThread.setPriority(5);
				genThreads.execute(newThread);
			}
			
			threadStarted = true;
			
			// useful for debugging
//        	ClientProxy.LOGGER.info(thread.lodDim.getNumberOfLods());
//        	ClientProxy.LOGGER.info(genThreads.toString());
		}
		
		return false;
	}
	
	@Override
	public boolean hasWork()
	{
		return !threadStarted;
	}
	
	
	
	
	private static class LodChunkGenThread implements Runnable
	{
		public final ServerLevel serverLevel;
		public final LodDimension lodDim;
		public final DistanceGenerationMode generationMode;
		public final LodBuilder lodBuilder;
		
		private final ChunkPosWrapper pos;
		
		public LodChunkGenThread(ChunkPosWrapper newPos, DistanceGenerationMode newGenerationMode,
				LodBuilder newLodBuilder,
				LodDimension newLodDimension, ServerLevel newServerLevel)
		{
			pos = newPos;
			generationMode = newGenerationMode;
			lodBuilder = newLodBuilder;
			lodDim = newLodDimension;
			serverLevel = newServerLevel;
		}
		
		@Override
		public void run()
		{
			try
			{
				// only generate LodChunks if they can
				// be added to the current LodDimension
				
				if (lodDim.regionIsInRange(pos.getX() / LodUtil.REGION_WIDTH_IN_CHUNKS, pos.getZ() / LodUtil.REGION_WIDTH_IN_CHUNKS))
				{
					// TODO test if anything has changed vs MC 1.16.5
					generateWithServer();
					
//					switch (generationMode)
//					{
//					case NONE:
//						// don't generate
//						break;
//					case BIOME_ONLY:
//					case BIOME_ONLY_SIMULATE_HEIGHT:
//						// fastest
//						generateUsingBiomesOnly();
//						break;
//					case SURFACE:
//						// faster
//						generateUsingSurface();
//						break;
//					case FEATURES:
//						// fast
//						generateUsingFeatures();
//						break;
//					case SERVER:
//						// very slow
//						generateWithServer();
//						break;
//					}
					
/*
					boolean dataExistence = lodDim.doesDataExist(new LevelPos((byte) 3, pos.x, pos.z));
					if (dataExistence)
						ClientProxy.LOGGER.info(pos.x + " " + pos.z + " Success!");
					else
						ClientProxy.LOGGER.info(pos.x + " " + pos.z);
 */
					// shows the pool size, active threads, queued tasks and completed tasks
//					ClientProxy.LOGGER.info(genThreads.toString());

//					long endTime = System.currentTimeMillis();
//					System.out.println(endTime - startTime);
					
				}// if in range
			}
			catch (Exception e)
			{
				ClientProxy.LOGGER.error(LodChunkGenThread.class.getSimpleName() + ": ran into an error: " + e.getMessage());
				e.printStackTrace();
			}
			finally
			{
				// decrement how many threads are running
				LodWorldGenerator.INSTANCE.numberOfChunksWaitingToGenerate.addAndGet(-1);
				
				// this position is no longer being generated
				LodWorldGenerator.INSTANCE.positionsWaitingToBeGenerated.remove(pos);
			}
			
		}// run
		
		
		
		/**
		 * takes about 2-5 ms
		 */
		private void generateUsingBiomesOnly()
		{
			List<ChunkAccess> chunkList = new LinkedList<>();
			ProtoChunk chunk = new ProtoChunk(pos.getChunkPos(), UpgradeData.EMPTY, serverLevel);
			chunkList.add(chunk);
			
			ServerChunkCache chunkSource = serverLevel.getChunkSource();
			ChunkGenerator chunkGen = chunkSource.generator;
			
			// generate the terrain (this is thread safe)
			//ChunkStatus.EMPTY.generate(serverLevel, chunkGen, serverLevel.getStructureManager(), (ServerLevelLightManager) serverLevel.getLightEngine(), null, chunkList);
			// override the chunk status, so we can run the next generator stage
			chunk.setStatus(ChunkStatus.STRUCTURE_REFERENCES);
			chunkGen.createBiomes(serverLevel.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), chunk);
			chunk.setStatus(ChunkStatus.STRUCTURE_REFERENCES);
			
			
			
			
			// generate fake height data for this LOD
			int seaLevel = serverLevel.getSeaLevel();
			
			boolean simulateHeight = generationMode == DistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT;
			boolean inTheEnd = false;
			
			// add fake heightmap data so our LODs aren't at height 0
			Heightmap heightmap = new Heightmap(chunk, LodUtil.DEFAULT_HEIGHTMAP);
			for (int x = 0; x < LodUtil.CHUNK_WIDTH && !inTheEnd; x++)
			{
				for (int z = 0; z < LodUtil.CHUNK_WIDTH && !inTheEnd; z++)
				{
					if (simulateHeight)
					{
						// these heights are of course aren't super accurate,
						// they are just to simulate height data where there isn't any
						switch (chunk.getBiomes().getNoiseBiome(x >> 2, seaLevel >> 2, z >> 2).getBiomeCategory())
						{
						case NETHER:
							heightmap.setHeight(x, z, serverLevel.getHeight() / 2);
							break;
						
						case EXTREME_HILLS:
							heightmap.setHeight(x, z, seaLevel + 30);
							break;
						case MESA:
							heightmap.setHeight(x, z, seaLevel + 20);
							break;
						case JUNGLE:
							heightmap.setHeight(x, z, seaLevel + 20);
							break;
						case BEACH:
							heightmap.setHeight(x, z, seaLevel + 5);
							break;
						case NONE:
							heightmap.setHeight(x, z, 0);
							break;
						
						case OCEAN:
						case RIVER:
							heightmap.setHeight(x, z, seaLevel);
							break;
						
						case THEEND:
							inTheEnd = true;
							break;
						
						// DESERT
						// FOREST
						// ICY
						// MUSHROOM
						// SAVANNA
						// SWAMP
						// TAIGA
						// PLAINS
						default:
							heightmap.setHeight(x, z, seaLevel + 10);
							break;
						}// heightmap switch
					}
					else
					{
						// we aren't simulating height
						// always use sea level
						heightmap.setHeight(x, z, seaLevel);
					}
				}// z
			}// x
			
			chunk.setHeightmap(LodUtil.DEFAULT_HEIGHTMAP, heightmap.getRawData());
			
			
			if (!inTheEnd)
			{
				lodBuilder.generateLodNodeFromChunk(lodDim, new ChunkWrapper(chunk), new LodBuilderConfig(true, true, false));
			}
			else
			{
				// if we are in the end, don't generate any chunks.
				// Since we don't know where the islands are, everything
				// generates the same, and it looks awful.
				//TODO it appears that 'if' can be collapsed, but comment says that it should not be a case
				lodBuilder.generateLodNodeFromChunk(lodDim, new ChunkWrapper(chunk), new LodBuilderConfig(true, true, false));
			}


//			long startTime = System.currentTimeMillis();
//			long endTime = System.currentTimeMillis();
//			System.out.println(endTime - startTime);
		}
		
		
//		/**
//		 * takes about 10 - 20 ms
//		 */
//		private void generateUsingSurface()
//		{
//			List<ChunkAccess> chunkList = new LinkedList<>();
//			ProtoChunk chunk = new ProtoChunk(pos.getChunkPos(), UpgradeData.EMPTY, serverLevel);
//			chunkList.add(chunk);
//			//LodServerLevel lodServerLevel = new LodServerLevel(serverLevel, chunk);
//			
//			ServerChunkCache chunkSource = serverLevel.getChunkSource();
//			ChunkGenerator chunkGen = chunkSource.generator;
//			
//			LevelLightEngine lightEngine = serverLevel.getLightEngine();
//			StructureManager templateManager = serverLevel.getStructureManager();
//			
//			
//			// generate the terrain (this is thread safe)
//			//ChunkStatus.EMPTY.generate(serverLevel, chunkGen, templateManager, lightEngine, null, chunkList);
//			// override the chunk status, so we can run the next generator stage
//			chunk.setStatus(ChunkStatus.STRUCTURE_REFERENCES);
//			chunkGen.createBiomes(serverLevel.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), chunk);
//			ChunkStatus.NOISE.generate(serverLevel, chunkGen, templateManager, lightEngine, null, chunkList);
//			ChunkStatus.SURFACE.generate(serverLevel, chunkGen, templateManager, lightEngine, null, chunkList);
//			
//			// this feature has been proven to be thread safe,
//			// so we will add it
//			SnowAndFreezeFeature snowFeature = new SnowAndFreezeFeature(NoneFeatureConfiguration.CODEC);
//			snowFeature.place(serverLevel, chunkGen, serverLevel.random, chunk.getPos().getWorldPosition(), null);
//			
//			
//			lodBuilder.generateLodNodeFromChunk(lodDim,  new ChunkWrapper(chunk), new LodBuilderConfig(DistanceGenerationMode.SURFACE));
//		}
//		
//		
//		/**
//		 * takes about 15 - 20 ms
//		 * <p>
//		 * Causes concurrentModification Exceptions,
//		 * which could cause instability or world generation bugs
//		 */
//		private void generateUsingFeatures()
//		{
//			List<ChunkAccess> chunkList = new LinkedList<>();
//			ProtoChunk chunk = new ProtoChunk(pos.getChunkPos(), UpgradeData.EMPTY, serverLevel);
//			chunkList.add(chunk);
//			LodServerLevel lodServerLevel = new LodServerLevel(serverLevel, chunk);
//			
//			ServerChunkCache chunkSource = serverLevel.getChunkSource();
//			ChunkGenerator chunkGen = chunkSource.generator;
//			
//			ServerLevelLightManager lightEngine = (ServerLevelLightManager) serverLevel.getLightEngine();
//			TemplateManager templateManager = serverLevel.getStructureManager();
//			
//			
//			// generate the terrain (this is thread safe)
//			ChunkStatus.EMPTY.generate(serverLevel, chunkGen, templateManager, lightEngine, null, chunkList);
//			// override the chunk status, so we can run the next generator stage
//			chunk.setStatus(ChunkStatus.STRUCTURE_REFERENCES);
//			chunkGen.createBiomes(serverLevel.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), chunk);
//			ChunkStatus.NOISE.generate(serverLevel, chunkGen, templateManager, lightEngine, null, chunkList);
//			ChunkStatus.SURFACE.generate(serverLevel, chunkGen, templateManager, lightEngine, null, chunkList);
//			
//			
//			// get all the biomes in the chunk
//			HashSet<Biome> biomes = new HashSet<>();
//			for (int x = 0; x < LodUtil.CHUNK_WIDTH; x++)
//			{
//				for (int z = 0; z < LodUtil.CHUNK_WIDTH; z++)
//				{
//					Biome biome = chunk.getBiomes().getNoiseBiome(x >> 2, serverLevel.getSeaLevel() >> 2, z >> 2);
//					
//					// Issue #35
//					// For some reason Jungle biomes cause incredible lag
//					// the features here must be interacting with each other
//					// in unpredictable ways (specifically tree feature generation).
//					// When generating Features my CPU usage generally hovers around 30 - 40%
//					// when generating Jungles it spikes to 100%.
//					if (biome.getBiomeCategory() != Biome.Category.JUNGLE)
//					{
//						// should probably use the heightmap here instead of seaLevel,
//						// but this seems to get the job done well enough
//						biomes.add(biome);
//					}
//				}
//			}
//			
//			boolean allowUnstableFeatures = LodConfig.CLIENT.worldGenerator.allowUnstableFeatureGeneration.get();
//			
//			// generate all the features related to this chunk.
//			// this may or may not be thread safe
//			for (Biome biome : biomes)
//			{
//				List<List<Supplier<ConfiguredFeature<?, ?>>>> featuresForState = biome.generationSettings.features();
//				
//				for (List<Supplier<ConfiguredFeature<?, ?>>> suppliers : featuresForState)
//				{
//					for (Supplier<ConfiguredFeature<?, ?>> featureSupplier : suppliers)
//					{
//						ConfiguredFeature<?, ?> configuredFeature = featureSupplier.get();
//						
//						if (!allowUnstableFeatures &&
//									configuredFeaturesToAvoid.containsKey(configuredFeature.hashCode()))
//							continue;
//						
//						
//						try
//						{
//							configuredFeature.place(lodServerLevel, chunkGen, serverLevel.random, chunk.getPos().getWorldPosition());
//						}
//						catch (ConcurrentModificationException e)
//						{
//							// This will happen. I'm not sure what to do about it
//							// except pray that it doesn't affect the normal world generation
//							// in any harmful way.
//							// Update: this can cause crashes and high CPU usage.
//							
//							// Issue #35
//							// I tried cloning the config for each feature, but that
//							// path was blocked since I can't clone lambda methods.
//							// I tried using a deep cloning library and discovered
//							// the problem there.
//							// ( https://github.com/kostaskougios/cloning
//							//   and
//							//   https://github.com/EsotericSoftware/kryo )
//							
//							if (!allowUnstableFeatures)
//								configuredFeaturesToAvoid.put(configuredFeature.hashCode(), configuredFeature);
////							ClientProxy.LOGGER.info(configuredFeaturesToAvoid.mappingCount());
//						}
//						catch (UnsupportedOperationException e)
//						{
//							// This will happen when the LodServerLevel
//							// isn't able to return something that a feature
//							// generator needs
//							
//							if (!allowUnstableFeatures)
//								configuredFeaturesToAvoid.put(configuredFeature.hashCode(), configuredFeature);
////							ClientProxy.LOGGER.info(configuredFeaturesToAvoid.mappingCount());
//						}
//						catch (Exception e)
//						{
//							// I'm not sure what happened, print to the log
//							
//							e.printStackTrace();
//							
//							if (!allowUnstableFeatures)
//								configuredFeaturesToAvoid.put(configuredFeature.hashCode(), configuredFeature);
////							ClientProxy.LOGGER.info(configuredFeaturesToAvoid.mappingCount());
//						}
//					}
//				}
//			}
//			
//			// generate a Lod like normal
//			lodBuilder.generateLodNodeFromChunk(lodDim,  new ChunkWrapper(chunk), new LodBuilderConfig(DistanceGenerationMode.FEATURES));
//		}
		
		
		/**
		 * on pre generated chunks 0 - 1 ms
		 * on un generated chunks 0 - 50 ms
		 * with the median seeming to hover around 15 - 30 ms
		 * and outliers in the 100 - 200 ms range
		 * <p>
		 * Note this should not be multithreaded and does cause server/simulation lag
		 * (Higher lag for generating than loading)
		 */
		private void generateWithServer()
		{
			lodBuilder.generateLodNodeFromChunk(lodDim,  new ChunkWrapper(serverLevel.getChunk(pos.getX(), pos.getZ(), ChunkStatus.FEATURES)), new LodBuilderConfig(DistanceGenerationMode.FEATURES));
		}
		
		
		
		
		
	}
	
	
	/**
	 * Stops the current genThreads if they are running
	 * and then recreates the Executor service. <br><br>
	 * <p>
	 * This is done to clear any outstanding tasks
	 * that may exist after the player leaves their current world.
	 * If this isn't done unfinished tasks may be left in the queue
	 * preventing new LodChunks form being generated.
	 */
	public static void restartExecutorService()
	{
		if (genThreads != null && !genThreads.isShutdown())
		{
			genThreads.shutdownNow();
		}
		genThreads = Executors.newFixedThreadPool(LodConfig.CLIENT.advancedModOptions.threading.numberOfWorldGenerationThreads.get(), new ThreadFactoryBuilder().setNameFormat("Gen-Worker-Thread-%d").build());
	}
	
	
	
	
	
	/*
	 * performance/generation tests related to
	 * ServerLevel.getChunk(x, z, ChunkStatus. *** )

     true/false is whether they generated blocks or not
     the time is how long it took to generate

     ChunkStatus.EMPTY					0  - 1  ms	false	(empty, what did you expect? :P)
     ChunkStatus.STRUCTURE_REFERENCES	1  - 2  ms  false	(no height, only generates some chunks)
     ChunkStatus.BIOMES 				1  - 10 ms	false	(no height)
     ChunkStatus.NOISE					4  - 15 ms	true	(all blocks are stone)
     ChunkStatus.LIQUID_CARVERS			6  - 12 ms	true	(no snow/trees, just grass)
     ChunkStatus.SURFACE				5  - 15 ms	true	(no snow/trees, just grass)
     ChunkStatus.CARVERS				5  - 30 ms	true	(no snow/trees, just grass)
     ChunkStatus.FEATURES				7  - 25 ms	true
     ChunkStatus.HEIGHTMAPS 			20 - 40 ms	true
     ChunkStatus.LIGHT					20 - 40 ms	true
     ChunkStatus.FULL 					30 - 50 ms	true
     ChunkStatus.SPAWN			   		50 - 80 ms	true


     At this point I would suggest using FEATURES, as it generates snow and trees
     (and any other object that is needed to make biomes distinct)

     Otherwise, if snow/trees aren't necessary SURFACE is the next fastest (although not by much)
	 */
}
