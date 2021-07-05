package com.seibel.lod.builders;

import com.seibel.lod.enums.LodDetail;
import com.seibel.lod.handlers.LodConfig;
import com.seibel.lod.objects.LodChunk;
import com.seibel.lod.objects.LodDataPoint;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.objects.LodWorld;
import com.seibel.lod.objects.quadTree.LodNodeData;
import com.seibel.lod.objects.quadTree.LodQuadTreeWorld;
import kaptainwutax.biomeutils.source.BiomeSource;
import kaptainwutax.biomeutils.source.OverworldBiomeSource;
import kaptainwutax.mcutils.state.Dimension;
import kaptainwutax.mcutils.version.MCVersion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DimensionType;
import net.minecraft.world.IWorld;
import net.minecraft.world.chunk.IChunk;

import java.awt.*;
import java.util.Iterator;
import java.util.OptionalLong;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LodNodeBuilder {
    private ExecutorService lodGenThreadPool = Executors.newSingleThreadExecutor();
    private long seed;
    private DimensionType dimension;

    /** Default size of any LOD regions we use */
    public int regionWidth = 5;


    /** fast biome calculator */
    private BiomeSource biomeSource;
    //Biome biome=biomeSource.getBiome(x,y,z); // here y is always 0 no matter what you pass

    public LodNodeBuilder(){

    }
    public setApproxGenerator(long seed){
        //Dimension.OVERWORLD;
        //Dimension.END;
        //Dimension.NETHER;
        biomeSource = BiomeSource.of(Dimension.OVERWORLD ,MCVersion.v1_16_4, seed);
    }

    public void generateLodNodeAsync(List<LodNodeData> dataList){
        Thread thread = new Thread(() ->{
            for(LodNodeData data : dataList){

            }
        });
        thread.setPriority(4);
        lodGenThreadPool.execute(thread);

        return;
    }

    public void generateLodNodeAsync(IChunk chunk, LodWorld lodWorld, IWorld world)
    {
        if (lodWorld == null || !lodWorld.getIsWorldLoaded())
            return;

        // don't try to create an LOD object
        // if for some reason we aren't
        // given a valid chunk object
        if (chunk == null)
            return;

        Thread thread = new Thread(() ->
        {
            try
            {
                DimensionType dim = world.getDimensionType();

                LodChunk lod = generateLodFromChunk(chunk, config);

                LodDimension lodDim;

                if (lodWorld.getLodDimension(dim) == null)
                {
                    lodDim = new LodDimension(dim, lodWorld, regionWidth);
                    lodWorld.addLodDimension(lodDim);
                }
                else
                {
                    lodDim = lodWorld.getLodDimension(dim);
                }

                lodDim.addLod(lod);
            }
            catch(IllegalArgumentException | NullPointerException e)
            {
                // if the world changes while LODs are being generated
                // they will throw errors as they try to access things that no longer
                // exist.
            }
        });
        lodGenThreadPool.execute(thread);

        return;
    }





    /**
     * Creates a LodChunk for a chunk in the given world.
     *
     * @throws IllegalArgumentException
     * thrown if either the chunk or world is null.
     */
    public LodChunk generateLodFromChunk(IChunk chunk) throws IllegalArgumentException
    {
        return generateLodFromChunk(chunk, new LodBuilderConfig());
    }

    /**
     * Creates a LodChunk for a chunk in the given world.
     *
     * @throws IllegalArgumentException
     * thrown if either the chunk or world is null.
     */
    public LodChunk generateLodFromChunk(IChunk chunk, LodBuilderConfig config) throws IllegalArgumentException
    {
        if(chunk == null)
            throw new IllegalArgumentException("generateLodFromChunk given a null chunk");


        LodDetail detail = LodConfig.CLIENT.lodDetail.get();
        LodDataPoint[][] dataPoints = new LodDataPoint[detail.lengthCount][detail.lengthCount];

        for(int i = 0; i < detail.lengthCount * detail.lengthCount; i++)
        {
            int startX = detail.startX[i];
            int startZ = detail.startZ[i];
            int endX = detail.endX[i];
            int endZ = detail.endZ[i];

            Color color;

            color = generateLodColorForArea(chunk, config, startX, startZ, endX, endZ);


            short height;
            short depth;

            if (!config.useHeightmap)
            {
                height = determineHeightPointForArea(chunk.getSections(), startX, startZ, endX, endZ);
                depth = determineBottomPointForArea(chunk.getSections(), startX, startZ, endX, endZ);
            }
            else
            {
                height = determineHeightPoint(chunk.getHeightmap(LodChunk.DEFAULT_HEIGHTMAP), startX, startZ, endX, endZ);
                depth = 0;
            }

            int x = i / detail.lengthCount;
            int z = i % detail.lengthCount;

            dataPoints[x][z] = new LodDataPoint(height, depth, color);
        }

        return new LodChunk(chunk.getPos(), dataPoints, detail);
    }


}
