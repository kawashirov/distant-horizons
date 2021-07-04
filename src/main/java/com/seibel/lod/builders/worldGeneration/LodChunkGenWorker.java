package com.seibel.lod.builders.worldGeneration;

import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import com.seibel.lod.builders.LodBufferBuilder;
import com.seibel.lod.builders.LodBuilderConfig;
import com.seibel.lod.builders.LodChunkBuilder;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.handlers.LodConfig;
import com.seibel.lod.objects.LodChunk;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.objects.LodRegion;
import com.seibel.lod.proxy.ClientProxy;
import com.seibel.lod.render.LodRenderer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.WeightedList.Entry;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.palette.UpgradeData;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeContainer;
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
 * 
 * @author James Seibel
 * @version 7-4-2021
 */
public class LodChunkGenWorker implements IWorker
{
    public static final ExecutorService genThreads = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    
    private boolean threadStarted = false;
    private LodChunkGenThread thread;
    
    /** If a configured feature fails for whatever reason,
     * add it to this list, this is to hopefully remove any
     * features that could cause issues down the line. */
    private static ConcurrentHashMap<Integer, ConfiguredFeature<?, ?>> configuredFeaturesToAvoid = new ConcurrentHashMap<>();
    
    
    
    public LodChunkGenWorker(ChunkPos newPos, LodRenderer newLodRenderer, 
    		LodChunkBuilder newLodBuilder, LodBufferBuilder newLodBufferBuilder, 
    		LodDimension newLodDimension, ServerWorld newServerWorld,
    		BiomeContainer newBiomeContainer)
    {
        if (newServerWorld == null)
        	throw new IllegalArgumentException("LodChunkGenWorker must have a non-null ServerWorld"); 
        	
        thread = new LodChunkGenThread(newPos, newLodRenderer, 
        		newLodBuilder, newLodBufferBuilder, 
        		newLodDimension, newServerWorld);
    }
    
    @Override
    public boolean doWork()
    {
        if (!threadStarted)
        {
        	// make sure we don't generate this chunk again
        	thread.lodDim.addLod(new LodChunk(thread.pos));
        	
        	thread.lodBufferBuilder.numberOfChunksWaitingToGenerate--;
        	
        	if (LodConfig.CLIENT.distanceGenerationMode.get() == DistanceGenerationMode.SERVER)
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
        		genThreads.execute(thread);
        	}
        	
        	threadStarted = true;
        	
    		// useful for debugging
//        	ClientProxy.LOGGER.info(thread.lodDim.getNumberOfLods());
        }
        
        return false;
    }
    
    @Override
    public boolean hasWork()
    {
        return !threadStarted;
    }
    
    
    
    
    private class LodChunkGenThread implements Runnable
    {
    	public final ServerWorld serverWorld;
        public final LodDimension lodDim;
        public final LodChunkBuilder lodChunkBuilder;
        public final LodRenderer lodRenderer;
        private LodBufferBuilder lodBufferBuilder;
    	
    	private ChunkPos pos;
    	
    	public LodChunkGenThread(ChunkPos newPos, LodRenderer newLodRenderer, 
        		LodChunkBuilder newLodBuilder, LodBufferBuilder newLodBufferBuilder, 
        		LodDimension newLodDimension, ServerWorld newServerWorld)
    	{
    		pos = newPos;
    		lodRenderer = newLodRenderer;
    		lodChunkBuilder = newLodBuilder;
    		lodBufferBuilder = newLodBufferBuilder;
    		lodDim = newLodDimension;
    		serverWorld = newServerWorld;
    	}
    	
		@Override
		public void run()
		{
			// only generate LodChunks if they can
            // be added to the current LodDimension
			if (lodDim.regionIsInRange(pos.x / LodRegion.SIZE, pos.z / LodRegion.SIZE))
			{
//				long startTime = System.currentTimeMillis();
				
				switch(LodConfig.CLIENT.distanceGenerationMode.get())
				{
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
					generateWithServer();
					break;
				}
				
				lodRenderer.regenerateLODsNextFrame();
				
				
//				if (lodDim.getLodFromCoordinates(pos.x, pos.z) != null)
//					ClientProxy.LOGGER.info(pos.x + " " + pos.z + " Success!");
//				else
//					ClientProxy.LOGGER.info(pos.x + " " + pos.z);
				
//				long endTime = System.currentTimeMillis();
//				System.out.println(endTime - startTime);
				
			}// if in range
			
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
			// override the chunk status so we can run the next generator stage
			chunk.setStatus(ChunkStatus.STRUCTURE_REFERENCES);
			ChunkStatus.BIOMES.generate(serverWorld, chunkGen, serverWorld.getStructureManager(), (ServerWorldLightManager) serverWorld.getLightEngine(), null, chunkList);
			chunk.setStatus(ChunkStatus.STRUCTURE_REFERENCES);
			
			
			// generate fake height data for this LOD
			int seaLevel = serverWorld.getSeaLevel();
			
			boolean simulateHeight = LodConfig.CLIENT.distanceGenerationMode.get() == DistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT;
			boolean inTheEnd = false;
			
			// add fake heightmap data so our LODs aren't at height 0
			Heightmap heightmap = new Heightmap(chunk, LodChunk.DEFAULT_HEIGHTMAP);
			for(int x = 0; x < LodChunk.WIDTH && !inTheEnd; x++)
			{
				for(int z = 0; z < LodChunk.WIDTH && !inTheEnd; z++)
				{
					if (simulateHeight)
					{
						// TODO use the biomes around each block to smooth out the transition
						
						// these heights are of course aren't super accurate,
						// they are just to simulate height data where there isn't any
						switch(chunk.getBiomes().getNoiseBiome(x, seaLevel, z).getBiomeCategory())
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
			
			chunk.setHeightmap(LodChunk.DEFAULT_HEIGHTMAP, heightmap.getRawData());
			
			
			LodChunk lod;
			if (!inTheEnd)
			{
				lod = lodChunkBuilder.generateLodFromChunk(chunk, new LodBuilderConfig(true, true, false));
			}
			else
			{
				// if we are in the end, don't generate any chunks.
				// Since we don't know where the islands are, everything
				// generates the same and it looks really bad.
				lod = new LodChunk(chunk.getPos().x, chunk.getPos().z);
			}
			lodDim.addLod(lod);
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
			ChunkGenerator chunkGen = chunkSource.generator;
			
			
			// generate the terrain (this is thread safe)
			ChunkStatus.EMPTY.generate(serverWorld, chunkGen, serverWorld.getStructureManager(), (ServerWorldLightManager) serverWorld.getLightEngine(), null, chunkList);
			// override the chunk status so we can run the next generator stage
			chunk.setStatus(ChunkStatus.STRUCTURE_REFERENCES);
			ChunkStatus.BIOMES.generate(serverWorld, chunkGen, serverWorld.getStructureManager(), (ServerWorldLightManager) serverWorld.getLightEngine(), null, chunkList);
			ChunkStatus.NOISE.generate(serverWorld, chunkGen, serverWorld.getStructureManager(), (ServerWorldLightManager) serverWorld.getLightEngine(), null, chunkList);
			ChunkStatus.SURFACE.generate(serverWorld, chunkGen, serverWorld.getStructureManager(), (ServerWorldLightManager) serverWorld.getLightEngine(), null, chunkList);
			
			// this feature has proved to be thread safe
			// so we will add it
			IceAndSnowFeature snowFeature = new IceAndSnowFeature(NoFeatureConfig.CODEC);
			snowFeature.place(lodServerWorld, chunkGen, serverWorld.random, chunk.getPos().getWorldPosition(), null);
			
			
			LodChunk lod = lodChunkBuilder.generateLodFromChunk(chunk, new LodBuilderConfig(false, true, true));
			lodDim.addLod(lod);
		}
		
		
		/**
		 * takes about 15 - 20 ms
		 * 
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
			ChunkGenerator chunkGen = chunkSource.generator;
			
			
			// generate the terrain (this is thread safe)
			ChunkStatus.EMPTY.generate(serverWorld, chunkGen, serverWorld.getStructureManager(), (ServerWorldLightManager) serverWorld.getLightEngine(), null, chunkList);
			// override the chunk status so we can run the next generator stage
			chunk.setStatus(ChunkStatus.STRUCTURE_REFERENCES);
			ChunkStatus.BIOMES.generate(serverWorld, chunkGen, serverWorld.getStructureManager(), (ServerWorldLightManager) serverWorld.getLightEngine(), null, chunkList);
			ChunkStatus.NOISE.generate(serverWorld, chunkGen, serverWorld.getStructureManager(), (ServerWorldLightManager) serverWorld.getLightEngine(), null, chunkList);
			ChunkStatus.SURFACE.generate(serverWorld, chunkGen, serverWorld.getStructureManager(), (ServerWorldLightManager) serverWorld.getLightEngine(), null, chunkList);
			
			
			// get all the biomes in the chunk
			HashSet<Biome> biomes = new HashSet<>();
			for (int x = 0; x < LodChunk.WIDTH; x++)
			{
				for (int z = 0; z < LodChunk.WIDTH; z++)
				{
					// should probably use the heightmap here instead of seaLevel,
					// but this seems to get the job done well enough
					biomes.add(chunk.getBiomes().getNoiseBiome(x, serverWorld.getSeaLevel(), z));
				}
			}
			
			
			// generate all the features related to this chunk.
			// this may or may not be thread safe
			for (Biome biome : biomes)
			{
				List<List<Supplier<ConfiguredFeature<?, ?>>>> featuresForState = biome.generationSettings.features();
				
				for(int featureStateToGenerate = 0; featureStateToGenerate < featuresForState.size(); featureStateToGenerate++)
				{
					for(Supplier<ConfiguredFeature<?, ?>> featureSupplier : featuresForState.get(featureStateToGenerate))
					{
						ConfiguredFeature<?, ?> configuredfeature = featureSupplier.get();
						
						if (configuredFeaturesToAvoid.containsKey(configuredfeature.hashCode()))
							continue;
						
						/*
						// clone any items that aren't thread safe to prevent
						// them from causing issues
						if(configuredfeature.config.getClass() == BlockClusterFeatureConfig.class)
						{
							config = cloneBlockClusterFeatureConfig((BlockClusterFeatureConfig) configuredfeature.config);
						}
						else if (configuredfeature.config.getClass() == DecoratedFeatureConfig.class)
						{
							config = cloneDecoratedFeatureConfig((DecoratedFeatureConfig) configuredfeature.config);
						}
						*/
						
						try
						{
							configuredfeature.place(lodServerWorld, chunkGen, serverWorld.random, chunk.getPos().getWorldPosition());
						}
						catch(ConcurrentModificationException e)
						{
							// This will happen. I'm not sure what to do about it
							// except pray that it doesn't effect the normal world generation
							// in any harmful way
							
							// I tried cloning the config for each feature, but that
							// path was blocked since I can't clone lambda methods.
							// I tried using a deep cloning library and discovered
							// the problem there.
							// ( https://github.com/kostaskougios/cloning )
							
							configuredFeaturesToAvoid.put(configuredfeature.hashCode(), configuredfeature);
//							ClientProxy.LOGGER.info(configuredFeaturesToAvoid.mappingCount());
						}
						catch(UnsupportedOperationException e)
						{
							// This will happen when the LodServerWorld
							// isn't able to return something that a feature
							// generator needs
							
							configuredFeaturesToAvoid.put(configuredfeature.hashCode(), configuredfeature);
//							ClientProxy.LOGGER.info(configuredFeaturesToAvoid.mappingCount());
						}
						catch(Exception e)
						{
							// I'm not sure what happened, print to the log
							
							System.out.println();
							System.out.println();
							e.printStackTrace();
							System.out.println();
							//ClientProxy.LOGGER.error("error class: \"" + configuredfeature.config.getClass() + "\"");
							System.out.println();
							
							configuredFeaturesToAvoid.put(configuredfeature.hashCode(), configuredfeature);
//							ClientProxy.LOGGER.info(configuredFeaturesToAvoid.mappingCount());
						}
					}
				}
			}
			
			// generate a Lod like normal
			LodChunk lod = lodChunkBuilder.generateLodFromChunk(chunk);
			lodDim.addLod(lod);
		}
		
		
		/**
		 * on pre generated chunks 0 - 1 ms
		 * on un generated chunks 0 - 50 ms 
		 * 	with the median seeming to hover around 15 - 30 ms
		 * 	and outliers in the 100 - 200 ms range
		 * 
		 * Note this should not be multithreaded and does cause server/simulation lag
		 * (Higher lag for generating than loading)
		 */
		private void generateWithServer()
		{
			lodChunkBuilder.generateLodChunkAsync(serverWorld.getChunk(pos.x, pos.z, ChunkStatus.FEATURES), ClientProxy.getLodWorld(), serverWorld);
		}
		
		
		
		
		
		
		//================//
		// Unused methods //
		//================//
		
		// Sadly I wasn't able to get these to work,
		// they are here for documentation purposes
		
		@SuppressWarnings({ "rawtypes", "unchecked", "unused" })
		private DecoratedFeatureConfig cloneDecoratedFeatureConfig(DecoratedFeatureConfig config)
		{
			IPlacementConfig placementConfig = null;
			
			Class oldConfigClass = config.decorator.config().getClass();
			
			if (oldConfigClass == FeatureSpreadConfig.class)
			{
				FeatureSpreadConfig oldPlacementConfig = (FeatureSpreadConfig) config.decorator.config();
				FeatureSpread oldSpread = oldPlacementConfig.count();
				
				placementConfig = new FeatureSpreadConfig(oldSpread);
			}
			else if(oldConfigClass == DecoratedPlacementConfig.class)
			{
				DecoratedPlacementConfig oldPlacementConfig = (DecoratedPlacementConfig) config.decorator.config();
				placementConfig = new DecoratedPlacementConfig(oldPlacementConfig.inner(), oldPlacementConfig.outer());
			}
			else if(oldConfigClass == NoiseDependant.class)
			{
				NoiseDependant oldPlacementConfig = (NoiseDependant) config.decorator.config();
				placementConfig = new NoiseDependant(oldPlacementConfig.noiseLevel, oldPlacementConfig.belowNoise, oldPlacementConfig.aboveNoise);
			}
			else
			{
//				ClientProxy.LOGGER.debug("unkown decorated placement config: \"" + config.decorator.config().getClass() + "\"");
				return config;
			}
			
			
			ConfiguredPlacement<?> newPlacement = new ConfiguredPlacement(config.decorator.decorator, placementConfig);
			return new DecoratedFeatureConfig(config.feature, newPlacement);
		}
		
		
		@SuppressWarnings("unused")
		private BlockClusterFeatureConfig cloneBlockClusterFeatureConfig(BlockClusterFeatureConfig config)
		{
			WeightedBlockStateProvider provider = new WeightedBlockStateProvider();
			for(Entry<BlockState> state : ((WeightedBlockStateProvider) config.stateProvider).weightedList.entries)
				provider.weightedList.entries.add(state);
			
			HashSet<Block> whitelist = new HashSet<>();
			for(Block block : config.whitelist)
				whitelist.add(block);
			
			HashSet<BlockState> blacklist = new HashSet<>();
			for(BlockState state : config.blacklist)
				blacklist.add(state); 
			
			
			BlockClusterFeatureConfig.Builder builder = new BlockClusterFeatureConfig.Builder(provider, config.blockPlacer);
			builder.whitelist(whitelist);
			builder.blacklist(blacklist);
			builder.xspread(config.xspread);
			builder.yspread(config.yspread);
			builder.zspread(config.zspread);
			if(config.canReplace) { builder.canReplace(); }
			if(config.needWater) { builder.needWater(); }
			if(config.project) { builder.noProjection(); }
			builder.tries(config.tries);
			
			
			return builder.build();
		}
		
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
     
     Otherwise if snow/trees aren't necessary SURFACE is the next fastest (although not by much)
     */
}
