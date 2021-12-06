package com.seibel.lod.fabric.wrappers.worldGeneration;

import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.builders.lodBuilding.LodBuilderConfig;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;
import com.seibel.lod.core.wrapperInterfaces.worldGeneration.AbstractWorldGeneratorWrapper;
import com.seibel.lod.common.wrappers.WrapperUtil;
import com.seibel.lod.common.wrappers.chunk.ChunkPosWrapper;
import com.seibel.lod.common.wrappers.chunk.ChunkWrapper;
import com.seibel.lod.common.wrappers.world.WorldWrapper;

import com.seibel.lod.fabric.wrappers.worldGeneration.LodServerWorld;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.SnowAndFreezeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

/**
 * @author James Seibel
 * @version 11-13-2021
 */
public class WorldGeneratorWrapper extends AbstractWorldGeneratorWrapper
{
    private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);

    /**
     * If a configured feature fails for whatever reason,
     * add it to this list. This will hopefully remove any
     * features that could cause issues down the line.
     */
    private static final ConcurrentHashMap<Integer, ConfiguredFeature<?, ?>> FEATURES_TO_AVOID = new ConcurrentHashMap<>();

    private static ExecutorService Executor = Executors.newSingleThreadExecutor();


    public final ServerLevel serverWorld;
    public final LodDimension lodDim;
    public final LodBuilder lodBuilder;

    public WorldGeneratorWrapper(LodBuilder newLodBuilder, LodDimension newLodDimension, IWorldWrapper worldWrapper)
    {
        super(newLodBuilder, newLodDimension, worldWrapper);

        lodBuilder = newLodBuilder;
        lodDim = newLodDimension;
        serverWorld = ((WorldWrapper) worldWrapper).getServerWorld();
    }







    /** takes about 2-5 ms */
    @Override
    public void generateBiomesOnly(AbstractChunkPosWrapper pos, DistanceGenerationMode generationMode)
    {
        /*
        List<ChunkAccess> chunkList = new LinkedList<>();
        ProtoChunk chunk = new ProtoChunk(((ChunkPosWrapper) pos).getChunkPos(), UpgradeData.EMPTY, serverWorld);
        chunkList.add(chunk);

        ServerChunkCache chunkSource = serverWorld.getChunkSource();
        ChunkGenerator chunkGen = chunkSource.getGenerator();

        // generate the terrain (this is thread safe)
        ChunkStatus.EMPTY.generate(Executor, serverWorld, chunkGen, serverWorld.getStructureManager(), (ThreadedLevelLightEngine) serverWorld.getLightEngine(), null, chunkList);
        // override the chunk status, so we can run the next generator stage
        chunk.setStatus(ChunkStatus.STRUCTURE_REFERENCES);
        chunkGen.createBiomes(serverWorld.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), chunk);
        chunk.setStatus(ChunkStatus.STRUCTURE_REFERENCES);




        // generate fake height data for this LOD
        int seaLevel = serverWorld.getSeaLevel();

        boolean simulateHeight = generationMode == DistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT;
        boolean inTheEnd = false;

        // add fake heightmap data so our LODs aren't at height 0
        Heightmap heightmap = new Heightmap(chunk, WrapperUtil.DEFAULT_HEIGHTMAP);
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

        chunk.setHeightmap(WrapperUtil.DEFAULT_HEIGHTMAP, heightmap.getRawData());


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


//		long startTime = System.currentTimeMillis();
//		long endTime = System.currentTimeMillis();
//		System.out.println(endTime - startTime);
         */
    }


    /** takes about 10 - 20 ms */
    @Override
    public void generateSurface(AbstractChunkPosWrapper pos)
    {
        /*
        List<ChunkAccess> chunkList = new LinkedList<>();
        ProtoChunk chunk = new ProtoChunk(((ChunkPosWrapper) pos).getChunkPos(), UpgradeData.EMPTY, serverWorld);
        chunkList.add(chunk);
        LodServerWorld lodServerWorld = new LodServerWorld(serverWorld, chunk);

        ServerChunkCache chunkSource = serverWorld.getChunkSource();
        ThreadedLevelLightEngine lightEngine = (ThreadedLevelLightEngine) serverWorld.getLightEngine();
        StructureManager templateManager = serverWorld.getStructureManager();
        ChunkGenerator chunkGen = chunkSource.getGenerator();


        // generate the terrain (this is thread safe)
        ChunkStatus.EMPTY.generate(Executor, serverWorld, chunkGen, templateManager, lightEngine, null, chunkList);
        // override the chunk status, so we can run the next generator stage
        chunk.setStatus(ChunkStatus.STRUCTURE_REFERENCES);
        chunkGen.createBiomes(serverWorld.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), chunk);
        ChunkStatus.NOISE.generate(Executor, serverWorld, chunkGen, templateManager, lightEngine, null, chunkList);
        // TODO: Find why this dosnt work (seems like the "Executor" is doing this)
        ChunkStatus.SURFACE.generate(Executor, serverWorld, chunkGen, templateManager, lightEngine, null, chunkList);

        // this feature has been proven to be thread safe,
        // so we will add it
        FeaturePlaceContext<NoneFeatureConfiguration> featurePlaceContext = new FeaturePlaceContext<>(lodServerWorld, chunkGen, serverWorld.random, chunk.getPos().getWorldPosition(), null);
        SnowAndFreezeFeature snowFeature = new SnowAndFreezeFeature(NoneFeatureConfiguration.CODEC);
        snowFeature.place(featurePlaceContext);


        lodBuilder.generateLodNodeFromChunk(lodDim,  new ChunkWrapper(chunk), new LodBuilderConfig(DistanceGenerationMode.SURFACE));

        /*TODO if we want to use Biome utils and terrain utils for overworld
         * lodBuilder.generateLodNodeFromChunk(lodDim, pos ,detailLevel, serverWorld.getSeed());*/
    }


    /**
     * takes about 15 - 20 ms
     * <p>
     * Causes concurrentModification Exceptions,
     * which could cause instability or world generation bugs
     */
    @Override
    public void generateFeatures(AbstractChunkPosWrapper pos)
    {
        /*
        List<ChunkAccess> chunkList = new LinkedList<>();
        ProtoChunk chunk = new ProtoChunk(((ChunkPosWrapper) pos).getChunkPos(), UpgradeData.EMPTY, serverWorld);
        chunkList.add(chunk);
        LodServerWorld lodServerWorld = new LodServerWorld(serverWorld, chunk);

        ServerChunkCache chunkSource = serverWorld.getChunkSource();
        ThreadedLevelLightEngine lightEngine = (ThreadedLevelLightEngine) serverWorld.getLightEngine();
        StructureManager templateManager = serverWorld.getStructureManager();
        ChunkGenerator chunkGen = chunkSource.getGenerator();


        // generate the terrain (this is thread safe)
        ChunkStatus.EMPTY.generate(Executor, serverWorld, chunkGen, templateManager, lightEngine, null, chunkList);
        // override the chunk status, so we can run the next generator stage
        chunk.setStatus(ChunkStatus.STRUCTURE_REFERENCES);
        chunkGen.createBiomes(serverWorld.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), chunk);
        ChunkStatus.NOISE.generate(Executor, serverWorld, chunkGen, templateManager, lightEngine, null, chunkList);
        // TODO[FABRIC]: Find whay this dosnt work
        ChunkStatus.SURFACE.generate(Executor, serverWorld, chunkGen, templateManager, lightEngine, null, chunkList);


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
                if (biome.getBiomeCategory() != Biome.BiomeCategory.JUNGLE)
                {
                    // should probably use the heightmap here instead of seaLevel,
                    // but this seems to get the job done well enough
                    biomes.add(biome);
                }
            }
        }

        boolean allowUnstableFeatures = CONFIG.client().worldGenerator().getAllowUnstableFeatureGeneration();

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
                            FEATURES_TO_AVOID.containsKey(configuredFeature.hashCode()))
                        continue;


                    try
                    {
                        configuredFeature.place(lodServerWorld, chunkGen, serverWorld.random, chunk.getPos().getWorldPosition());
                    }
                    catch (ConcurrentModificationException | UnsupportedOperationException e)
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
                            FEATURES_TO_AVOID.put(configuredFeature.hashCode(), configuredFeature);
//						ClientProxy.LOGGER.info(configuredFeaturesToAvoid.mappingCount());
                    }
                    // This will happen when the LodServerWorld
                    // isn't able to return something that a feature
                    // generator needs
                    catch (Exception e)
                    {
                        // I'm not sure what happened, print to the log

                        e.printStackTrace();

                        if (!allowUnstableFeatures)
                            FEATURES_TO_AVOID.put(configuredFeature.hashCode(), configuredFeature);
//						ClientProxy.LOGGER.info(configuredFeaturesToAvoid.mappingCount());
                    }
                }
            }
        }

        // generate a Lod like normal
        lodBuilder.generateLodNodeFromChunk(lodDim,  new ChunkWrapper(chunk), new LodBuilderConfig(DistanceGenerationMode.FEATURES));
         */
    }


    /**
     * Generates using MC's ServerWorld.
     * <p>
     * on pre generated chunks 0 - 1 ms <br>
     * on un generated chunks 0 - 50 ms <br>
     * with the median seeming to hover around 15 - 30 ms <br>
     * and outliers in the 100 - 200 ms range <br>
     * <p>
     * Note this should not be multithreaded and does cause server/simulation lag
     * (Higher lag for generating than loading)
     */
    @Override
    public void generateFull(AbstractChunkPosWrapper pos)
    {
        lodBuilder.generateLodNodeFromChunk(lodDim, new ChunkWrapper(serverWorld.getChunk(pos.getX(), pos.getZ(), ChunkStatus.FEATURES)), new LodBuilderConfig(DistanceGenerationMode.FULL));
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
     (and any other object that are needed to make biomes distinct)

     Otherwise, if snow/trees aren't necessary SURFACE is the next fastest (although not by much)
	 */
}
