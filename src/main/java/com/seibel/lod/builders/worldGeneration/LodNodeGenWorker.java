/*
 *    This file is part of the LOD Mod, licensed under the GNU GPL v3 License.
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

import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.seibel.lod.builders.lodBuilding.LodBuilder;
import com.seibel.lod.builders.lodBuilding.LodBuilderConfig;
import com.seibel.lod.config.LodConfig;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.handlers.ChunkLoader;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.proxy.ClientProxy;
import com.seibel.lod.util.LodUtil;

import com.seibel.lod.wrappers.MinecraftWrapper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.palette.UpgradeData;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.blockstateprovider.WeightedBlockStateProvider;
import net.minecraft.world.gen.feature.BlockClusterFeatureConfig;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.DecoratedFeatureConfig;
import net.minecraft.world.gen.feature.FeatureSpread;
import net.minecraft.world.gen.feature.FeatureSpreadConfig;
import net.minecraft.world.gen.feature.IceAndSnowFeature;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.gen.placement.ConfiguredPlacement;
import net.minecraft.world.gen.placement.DecoratedPlacementConfig;
import net.minecraft.world.gen.placement.IPlacementConfig;
import net.minecraft.world.gen.placement.NoiseDependant;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.ServerWorldLightManager;
import net.minecraftforge.common.WorldWorkerManager.IWorker;

/**
 * This is used to generate a LodChunk at a given ChunkPos.
 * @author James Seibel
 * @version 10-15-2021
 */
public class LodNodeGenWorker implements IWorker
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
	
	
	
	public LodNodeGenWorker(ChunkPos newPos, DistanceGenerationMode newGenerationMode,
			LodBuilder newLodBuilder,
			LodDimension newLodDimension, ServerWorld newServerWorld)
	{
		// just a few sanity checks
		if (newPos == null)
			throw new IllegalArgumentException("LodChunkGenWorker must have a non-null ChunkPos");
		
		if (newLodBuilder == null)
			throw new IllegalArgumentException("LodChunkGenThread requires a non-null LodChunkBuilder");
		
		if (newLodDimension == null)
			throw new IllegalArgumentException("LodChunkGenThread requires a non-null LodDimension");
		
		if (newServerWorld == null)
			throw new IllegalArgumentException("LodChunkGenThread requires a non-null ServerWorld");
		
		
		
		thread = new LodChunkGenThread(newPos, newGenerationMode,
				newLodBuilder,
				newLodDimension, newServerWorld);
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
		public final ServerWorld serverWorld;
		public final LodDimension lodDim;
		public final DistanceGenerationMode generationMode;
		public final LodBuilder lodBuilder;
		
		private final ChunkPos pos;
		
		public LodChunkGenThread(ChunkPos newPos, DistanceGenerationMode newGenerationMode,
				LodBuilder newLodBuilder,
				LodDimension newLodDimension, ServerWorld newServerWorld)
		{
			pos = newPos;
			generationMode = newGenerationMode;
			lodBuilder = newLodBuilder;
			lodDim = newLodDimension;
			serverWorld = newServerWorld;
		}
		
		@Override
		public void run()
		{
			try
			{
				// only generate LodChunks if they can
				// be added to the current LodDimension
				
				/* TODO I must disable this 'if', if I will find a way to replace it */
				if (lodDim.regionIsInRange(pos.x / LodUtil.REGION_WIDTH_IN_CHUNKS, pos.z / LodUtil.REGION_WIDTH_IN_CHUNKS))
				{
					//
					//{
					//	lodBuilder.generateLodNodeFromChunk(lodDim, loadedChunk, new LodBuilderConfig(DistanceGenerationMode.SERVER));
					//}
					//else
					//{
					/*
					IChunk loadedChunk = null;
					if (lodDim.isChunkPreGenerated(pos.x, pos.z) && LodConfig.CLIENT.worldGenerator.useExperimentalPreGenLoading.get())
					{
						// generate a Lod like normal
						loadedChunk = ChunkLoader.getChunkFromFile(pos);
						if(loadedChunk != null)
							lodBuilder.generateLodNodeFromChunk(lodDim, loadedChunk, new LodBuilderConfig(DistanceGenerationMode.SERVER));
						else
						{
							switch (generationMode)
							{
							case NONE:
								// don't generate
								break;
							case BIOME_ONLY:
							case BIOME_ONLY_SIMULATE_HEIGHT:
								// fastest
								generateUsingBiomesOnly();
								break;
							case SURFACE:
								// faster
								generateUsingSurface();
								break;
							case FEATURES:
								// fast
								generateUsingFeatures();
								break;
							case SERVER:
								// very slow
								lodBuilder.generateLodNodeFromChunk(lodDim, serverWorld.getChunk(pos.x, pos.z, ChunkStatus.FEATURES), new LodBuilderConfig(DistanceGenerationMode.SERVER));
								//generateWithServer();
								break;
							}
						}
					}
					else
					{*/
						switch (generationMode)
						{
						case NONE:
							// don't generate
							break;
						case BIOME_ONLY:
						case BIOME_ONLY_SIMULATE_HEIGHT:
							// fastest
							generateUsingBiomesOnly();
							break;
						case SURFACE:
							// faster
							generateUsingSurface();
							break;
						case FEATURES:
							// fast
							generateUsingFeatures();
							break;
						case SERVER:
							// very slow
							lodBuilder.generateLodNodeFromChunk(lodDim, serverWorld.getChunk(pos.x, pos.z, ChunkStatus.FEATURES), new LodBuilderConfig(DistanceGenerationMode.SERVER));
							//generateWithServer();
							break;
						}
					//}
					
					//lodRenderer.regenerateLODsNextFrame();
					
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
			List<IChunk> chunkList = new LinkedList<>();
			ChunkPrimer chunk = new ChunkPrimer(pos, UpgradeData.EMPTY);
			chunkList.add(chunk);
			
			ServerChunkProvider chunkSource = serverWorld.getChunkSource();
			ChunkGenerator chunkGen = chunkSource.generator;
			
			// generate the terrain (this is thread safe)
			ChunkStatus.EMPTY.generate(serverWorld, chunkGen, serverWorld.getStructureManager(), (ServerWorldLightManager) serverWorld.getLightEngine(), null, chunkList);
			// override the chunk status, so we can run the next generator stage
			chunk.setStatus(ChunkStatus.STRUCTURE_REFERENCES);
			chunkGen.createBiomes(serverWorld.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), chunk);
			chunk.setStatus(ChunkStatus.STRUCTURE_REFERENCES);
			
			
			
			
			// generate fake height data for this LOD
			int seaLevel = serverWorld.getSeaLevel();
			
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
							heightmap.setHeight(x, z, serverWorld.getHeight() / 2);
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
				lodBuilder.generateLodNodeFromChunk(lodDim, chunk, new LodBuilderConfig(true, true, false));
			}
			else
			{
				// if we are in the end, don't generate any chunks.
				// Since we don't know where the islands are, everything
				// generates the same, and it looks awful.
				//TODO it appears that 'if' can be collapsed, but comment says that it should not be a case
				lodBuilder.generateLodNodeFromChunk(lodDim, chunk, new LodBuilderConfig(true, true, false));
			}


//			long startTime = System.currentTimeMillis();
//			long endTime = System.currentTimeMillis();
//			System.out.println(endTime - startTime);
		}
		
		
		/**
		 * takes about 10 - 20 ms
		 */
		private void generateUsingSurface()
		{
			List<IChunk> chunkList = new LinkedList<>();
			ChunkPrimer chunk = new ChunkPrimer(pos, UpgradeData.EMPTY);
			chunkList.add(chunk);
			LodServerWorld lodServerWorld = new LodServerWorld(serverWorld, chunk);
			
			ServerChunkProvider chunkSource = serverWorld.getChunkSource();
			ServerWorldLightManager lightEngine = (ServerWorldLightManager) serverWorld.getLightEngine();
			TemplateManager templateManager = serverWorld.getStructureManager();
			ChunkGenerator chunkGen = chunkSource.generator;
			
			
			// generate the terrain (this is thread safe)
			ChunkStatus.EMPTY.generate(serverWorld, chunkGen, templateManager, lightEngine, null, chunkList);
			// override the chunk status, so we can run the next generator stage
			chunk.setStatus(ChunkStatus.STRUCTURE_REFERENCES);
			chunkGen.createBiomes(serverWorld.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), chunk);
			ChunkStatus.NOISE.generate(serverWorld, chunkGen, templateManager, lightEngine, null, chunkList);
			ChunkStatus.SURFACE.generate(serverWorld, chunkGen, templateManager, lightEngine, null, chunkList);
			
			// this feature has been proven to be thread safe,
			// so we will add it
			IceAndSnowFeature snowFeature = new IceAndSnowFeature(NoFeatureConfig.CODEC);
			snowFeature.place(lodServerWorld, chunkGen, serverWorld.random, chunk.getPos().getWorldPosition(), null);
			
			
			lodBuilder.generateLodNodeFromChunk(lodDim, chunk, new LodBuilderConfig(DistanceGenerationMode.SURFACE));
			
			/*TODO if we want to use Biome utils and terrain utils for overworld
			 * lodBuilder.generateLodNodeFromChunk(lodDim, pos ,detailLevel, serverWorld.getSeed());*/
		}
		
		
		/**
		 * takes about 15 - 20 ms
		 * <p>
		 * Causes concurrentModification Exceptions,
		 * which could cause instability or world generation bugs
		 */
		private void generateUsingFeatures()
		{
			List<IChunk> chunkList = new LinkedList<>();
			ChunkPrimer chunk = new ChunkPrimer(pos, UpgradeData.EMPTY);
			chunkList.add(chunk);
			LodServerWorld lodServerWorld = new LodServerWorld(serverWorld, chunk);
			
			ServerChunkProvider chunkSource = serverWorld.getChunkSource();
			ServerWorldLightManager lightEngine = (ServerWorldLightManager) serverWorld.getLightEngine();
			TemplateManager templateManager = serverWorld.getStructureManager();
			ChunkGenerator chunkGen = chunkSource.generator;
			
			
			// generate the terrain (this is thread safe)
			ChunkStatus.EMPTY.generate(serverWorld, chunkGen, templateManager, lightEngine, null, chunkList);
			// override the chunk status, so we can run the next generator stage
			chunk.setStatus(ChunkStatus.STRUCTURE_REFERENCES);
			chunkGen.createBiomes(serverWorld.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), chunk);
			ChunkStatus.NOISE.generate(serverWorld, chunkGen, templateManager, lightEngine, null, chunkList);
			ChunkStatus.SURFACE.generate(serverWorld, chunkGen, templateManager, lightEngine, null, chunkList);
			
			
			// get all the biomes in the chunk
			HashSet<Biome> biomes = new HashSet<>();
			for (int x = 0; x < LodUtil.CHUNK_WIDTH; x++)
			{
				for (int z = 0; z < LodUtil.CHUNK_WIDTH; z++)
				{
					Biome biome = chunk.getBiomes().getNoiseBiome(x >> 2, serverWorld.getSeaLevel() >> 2, z >> 2);
					
					// Issue #35
					// For some reason Jungle biomes cause incredible lag
					// the features here must be interacting with each other
					// in unpredictable ways (specifically tree feature generation).
					// When generating Features my CPU usage generally hovers around 30 - 40%
					// when generating Jungles it spikes to 100%.
					if (biome.getBiomeCategory() != Biome.Category.JUNGLE)
					{
						// should probably use the heightmap here instead of seaLevel,
						// but this seems to get the job done well enough
						biomes.add(biome);
					}
				}
			}
			
			boolean allowUnstableFeatures = LodConfig.CLIENT.worldGenerator.allowUnstableFeatureGeneration.get();
			
			// generate all the features related to this chunk.
			// this may or may not be thread safe
			for (Biome biome : biomes)
			{
				List<List<Supplier<ConfiguredFeature<?, ?>>>> featuresForState = biome.generationSettings.features();
				
				for (List<Supplier<ConfiguredFeature<?, ?>>> suppliers : featuresForState)
				{
					for (Supplier<ConfiguredFeature<?, ?>> featureSupplier : suppliers)
					{
						ConfiguredFeature<?, ?> configuredFeature = featureSupplier.get();
						
						if (!allowUnstableFeatures &&
									configuredFeaturesToAvoid.containsKey(configuredFeature.hashCode()))
							continue;
						
						
						try
						{
							configuredFeature.place(lodServerWorld, chunkGen, serverWorld.random, chunk.getPos().getWorldPosition());
						}
						catch (ConcurrentModificationException e)
						{
							// This will happen. I'm not sure what to do about it
							// except pray that it doesn't affect the normal world generation
							// in any harmful way.
							// Update: this can cause crashes and high CPU usage.
							
							// Issue #35
							// I tried cloning the config for each feature, but that
							// path was blocked since I can't clone lambda methods.
							// I tried using a deep cloning library and discovered
							// the problem there.
							// ( https://github.com/kostaskougios/cloning
							//   and
							//   https://github.com/EsotericSoftware/kryo )
							
							if (!allowUnstableFeatures)
								configuredFeaturesToAvoid.put(configuredFeature.hashCode(), configuredFeature);
//							ClientProxy.LOGGER.info(configuredFeaturesToAvoid.mappingCount());
						}
						catch (UnsupportedOperationException e)
						{
							// This will happen when the LodServerWorld
							// isn't able to return something that a feature
							// generator needs
							
							if (!allowUnstableFeatures)
								configuredFeaturesToAvoid.put(configuredFeature.hashCode(), configuredFeature);
//							ClientProxy.LOGGER.info(configuredFeaturesToAvoid.mappingCount());
						}
						catch (Exception e)
						{
							// I'm not sure what happened, print to the log
							
							e.printStackTrace();
							
							if (!allowUnstableFeatures)
								configuredFeaturesToAvoid.put(configuredFeature.hashCode(), configuredFeature);
//							ClientProxy.LOGGER.info(configuredFeaturesToAvoid.mappingCount());
						}
					}
				}
			}
			
			// generate a Lod like normal
			lodBuilder.generateLodNodeFromChunk(lodDim, chunk, new LodBuilderConfig(DistanceGenerationMode.FEATURES));
		}
		
		
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
			lodBuilder.generateLodNodeAsync(serverWorld.getChunk(pos.x, pos.z, ChunkStatus.FEATURES), ClientProxy.getLodWorld(), serverWorld);
		}
		
		
		
		
		
		
		//================//
		// Unused methods //
		//================//
		
		// Sadly I wasn't able to get these to work,
		// they are here for documentation purposes
		
		@SuppressWarnings({ "rawtypes", "unchecked", "unused" })
		private DecoratedFeatureConfig cloneDecoratedFeatureConfig(DecoratedFeatureConfig config)
		{
			IPlacementConfig placementConfig;
			
			Class oldConfigClass = config.decorator.config().getClass();
			
			if (oldConfigClass == FeatureSpreadConfig.class)
			{
				FeatureSpreadConfig oldPlacementConfig = (FeatureSpreadConfig) config.decorator.config();
				FeatureSpread oldSpread = oldPlacementConfig.count();
				
				placementConfig = new FeatureSpreadConfig(oldSpread);
			}
			else if (oldConfigClass == DecoratedPlacementConfig.class)
			{
				DecoratedPlacementConfig oldPlacementConfig = (DecoratedPlacementConfig) config.decorator.config();
				placementConfig = new DecoratedPlacementConfig(oldPlacementConfig.inner(), oldPlacementConfig.outer());
			}
			else if (oldConfigClass == NoiseDependant.class)
			{
				NoiseDependant oldPlacementConfig = (NoiseDependant) config.decorator.config();
				placementConfig = new NoiseDependant(oldPlacementConfig.noiseLevel, oldPlacementConfig.belowNoise, oldPlacementConfig.aboveNoise);
			}
			else
			{
//				ClientProxy.LOGGER.debug("unknown decorated placement config: \"" + config.decorator.config().getClass() + "\"");
				return config;
			}
			
			
			ConfiguredPlacement<?> newPlacement = new ConfiguredPlacement(config.decorator.decorator, placementConfig);
			return new DecoratedFeatureConfig(config.feature, newPlacement);
		}
		
		
		@SuppressWarnings("unused")
		private BlockClusterFeatureConfig cloneBlockClusterFeatureConfig(BlockClusterFeatureConfig config)
		{
			WeightedBlockStateProvider provider = new WeightedBlockStateProvider();
			provider.weightedList.entries.addAll(((WeightedBlockStateProvider) config.stateProvider).weightedList.entries);
			
			HashSet<Block> whitelist = new HashSet<>(config.whitelist);
			
			HashSet<BlockState> blacklist = new HashSet<>(config.blacklist);
			
			
			BlockClusterFeatureConfig.Builder builder = new BlockClusterFeatureConfig.Builder(provider, config.blockPlacer);
			builder.whitelist(whitelist);
			builder.blacklist(blacklist);
			builder.xspread(config.xspread);
			builder.yspread(config.yspread);
			builder.zspread(config.zspread);
			if (config.canReplace)
			{
				builder.canReplace();
			}
			if (config.needWater)
			{
				builder.needWater();
			}
			if (config.project)
			{
				builder.noProjection();
			}
			builder.tries(config.tries);
			
			
			return builder.build();
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
	 * serverWorld.getChunk(x, z, ChunkStatus. *** )

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
