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
package com.seibel.lod.builders;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.seibel.lod.util.DetailUtil;
import org.lwjgl.opengl.GL11;

import com.seibel.lod.builders.worldGeneration.LodNodeGenWorker;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.enums.LodDetail;
import com.seibel.lod.handlers.LodConfig;
import com.seibel.lod.objects.LevelPos;
import com.seibel.lod.objects.LodDataPoint;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.objects.RegionPos;
import com.seibel.lod.proxy.ClientProxy;
import com.seibel.lod.render.LodRenderer;
import com.seibel.lod.util.LodThreadFactory;
import com.seibel.lod.util.LodUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.WorldWorkerManager;

/**
 * This object is used to create NearFarBuffer objects.
 *
 * @author James Seibel
 * @version 8-21-2021
 */
public class LodBufferBuilder
{
    private Minecraft mc;

    /**
     * This holds the thread used to generate new LODs off the main thread.
     */
    private ExecutorService mainGenThread = Executors.newSingleThreadExecutor(new LodThreadFactory(this.getClass().getSimpleName() + " - main"));

    private LodBuilder LodQuadTreeNodeBuilder;

    /**
     * The buffers that are used to create LODs using far fog
     */
    public volatile BufferBuilder[][] buildableBuffers;

    /**
     * Used when building new VBOs
     */
    public volatile VertexBuffer[][] buildableVbos;

    /**
     * VBOs that are sent over to the LodNodeRenderer
     */
    public volatile VertexBuffer[][] drawableVbos;

    /**
     * if this is true the LOD buffers are currently being
     * regenerated.
     */
    public boolean generatingBuffers = false;

    /**
     * if this is true the LOD buffers are currently being
     * regenerated.
     */
    public Set<ChunkPos> positionWaitingToBeGenerated = new HashSet<>();

    /**
     * if this is true new LOD buffers have been generated
     * and are waiting to be swapped with the drawable buffers
     */
    private boolean switchVbos = false;

    /**
     * This keeps track of how many chunk generation requests are on going.
     * This is to prevent chunks from being generated for a long time in an area
     * the player is no longer in.
     */
    public AtomicInteger numberOfChunksWaitingToGenerate = new AtomicInteger(0);


    /**
     * how many chunks to generate outside of the player's
     * view distance at one time. (or more specifically how
     * many requests to make at one time).
     * I multiply by 8 to make sure there is always a buffer of chunk requests,
     * to make sure the CPU is always busy and we can generate LODs as quickly as
     * possible.
     */
    public int maxChunkGenRequests = LodConfig.CLIENT.numberOfWorldGenerationThreads.get() * 8;


    public LodBufferBuilder(LodBuilder newLodBuilder)
    {
        mc = Minecraft.getInstance();
        LodQuadTreeNodeBuilder = newLodBuilder;
    }


    private LodDimension previousDimension = null;


    /**
     * Create a thread to asynchronously generate LOD buffers
     * centered around the given camera X and Z.
     * <br>
     * This method will write to the drawable near and far buffers.
     * <br>
     * After the buildable buffers have been generated they must be
     * swapped with the drawable buffers in the LodRenderer to be drawn.
     */
    public void generateLodBuffersAsync(LodRenderer renderer, LodDimension lodDim,
                                        BlockPos playerBlockPos, int numbChunksWide)
    {
        // only allow one generation process to happen at a time
        if (generatingBuffers)
            return;

        if (buildableBuffers == null)
            throw new IllegalStateException("\"generateLodBuffersAsync\" was called before the \"setupBuffers\" method was called.");

        if (previousDimension != lodDim)
        {
            previousDimension = lodDim;
        }


        generatingBuffers = true;


        // round the player's block position down to the nearest chunk BlockPos
        ChunkPos playerChunkPos = new ChunkPos(playerBlockPos);
        BlockPos playerBlockPosRounded = playerChunkPos.getWorldPosition();

        // this is where we will start drawing squares
        // (exactly half the total width)
        BlockPos startBlockPos = new BlockPos(-(numbChunksWide * 16 / 2) + playerBlockPosRounded.getX(), 0, -(numbChunksWide * 16 / 2) + playerBlockPosRounded.getZ());
        ChunkPos startChunkPos = new ChunkPos(startBlockPos);


        Thread thread = new Thread(() ->
        {
            try
            {
                long startTime = System.currentTimeMillis();

                ArrayList<ChunkPos> chunksToGen = new ArrayList<>(maxChunkGenRequests);
                // if we don't have a full number of chunks to generate in chunksToGen
                // we can top it off from the reserve
                ArrayList<ChunkPos> chunksToGenReserve = new ArrayList<>(maxChunkGenRequests);

                startBuffers();

                // used when determining which chunks are closer when queuing distance
                // generation
                int minChunkDist = Integer.MAX_VALUE;

                int width;
                List<LevelPos> posListToRender = new ArrayList<>();
                LodDataPoint lodData;
                for (int xRegion = 0; xRegion < lodDim.regions.length; xRegion++)
                {
                    for (int zRegion = 0; zRegion < lodDim.regions.length; zRegion++)
                    {
                        RegionPos regionPos = new RegionPos(xRegion + lodDim.getCenterX() - lodDim.getWidth() / 2, zRegion + lodDim.getCenterZ() - lodDim.getWidth() / 2);

                        // local position in the vbo and bufferBuilder arrays
                        BufferBuilder currentBuffer = buildableBuffers[xRegion][zRegion];

                        // make sure the buffers weren't
                        // changed while we were running this method
                        if (currentBuffer == null || (currentBuffer != null && !currentBuffer.building()))
                            return;

                        /**TODO make this automatic and config dependent*/
                        for (byte detail = LodUtil.BLOCK_DETAIL_LEVEL; detail <= LodUtil.REGION_DETAIL_LEVEL; detail++)
                        {
                            posListToRender.addAll(lodDim.getDataToRender(
                                    regionPos,
                                    playerBlockPosRounded.getX(),
                                    playerBlockPosRounded.getZ(),
                                    DetailUtil.getDistanceRendering(detail),
                                    DetailUtil.getDistanceRendering(detail + 1),
                                    detail));
                        }
                        for (LevelPos pos : posListToRender)
                        {
                            LevelPos chunkPos = pos.convert(LodUtil.CHUNK_DETAIL_LEVEL);
                            int chunkX = chunkPos.posX + startChunkPos.x;
                            int chunkZ = chunkPos.posZ + startChunkPos.z;
                            // skip any chunks that Minecraft is going to render
                            if (isCoordInCenterArea(chunkPos.posX, chunkPos.posZ, (numbChunksWide / 2)) || renderer.vanillaRenderedChunks.contains(new ChunkPos(chunkX, chunkZ)))
                            {
                                continue;
                            }

                            if (lodDim.doesDataExist(pos))
                            {
                                try
                                {
                                    width = (int) Math.pow(2, pos.detailLevel);
                                    lodData = lodDim.getData(pos);
                                    LodConfig.CLIENT.lodTemplate.get().template.addLodToBuffer(currentBuffer, lodDim, lodData,
                                            pos.posX * width, 0, pos.posZ * width, renderer.debugging, pos.detailLevel);
                                } catch (ArrayIndexOutOfBoundsException e)
                                {
                                    ClientProxy.LOGGER.warn("LodBufferBuilder ran into trouble and had to start over.");
                                    closeBuffers();
                                    return;
                                }
                            }

                        }

                        posListToRender.clear();
                    }
                }

                /**TODO make this automatic and config dependent*/
                List<LevelPos> posListToGenerate = new ArrayList<>();

                /**TODO this order can be inverted*/
                /*
                for (byte detail = LodUtil.BLOCK_DETAIL_LEVEL; detail <= LodUtil.REGION_DETAIL_LEVEL; detail++)
                {
                    if (!posListToGenerate.isEmpty()) break;
                    for (byte detailGen = LodUtil.BLOCK_DETAIL_LEVEL; detailGen <= LodUtil.REGION_DETAIL_LEVEL; detailGen++)
                    {
                        if (!posListToGenerate.isEmpty()) break;
                        posListToGenerate.addAll(lodDim.getDataToGenerate(
                                playerBlockPosRounded.getX(),
                                playerBlockPosRounded.getZ(),
                                (int) (distancesLinear[detailGen]*1.5),
                                (int) (distancesLinear[detailGen+1]*1.5),
                                (byte) distancesGenerators[detailGen].complexity,
                                detail,
                                16));
                        System.out.println("HERE");
                    }
                }
*/
                int requesting = maxChunkGenRequests;

                for (byte detailGen = LodUtil.BLOCK_DETAIL_LEVEL; detailGen <= LodUtil.REGION_DETAIL_LEVEL; detailGen++)
                {
                    if (requesting == 0) break;
                    posListToGenerate.addAll(lodDim.getDataToGenerate(
                            playerBlockPosRounded.getX(),
                            playerBlockPosRounded.getZ(),
                            DetailUtil.getDistanceGeneration(detailGen),
                            DetailUtil.getDistanceGeneration(detailGen + 1),
                            DetailUtil.getDistanceGenerationMode(detailGen).complexity,
                            (byte) 8,
                            requesting));
                    requesting = maxChunkGenRequests - posListToGenerate.size();

                    for (LevelPos levelPos : posListToGenerate)
                    {
                    }

                }
                for (byte detailGen = LodUtil.BLOCK_DETAIL_LEVEL; detailGen <= LodUtil.REGION_DETAIL_LEVEL; detailGen++)
                {
                    if (requesting == 0) break;
                    posListToGenerate.addAll(lodDim.getDataToGenerate(
                            playerBlockPosRounded.getX(),
                            playerBlockPosRounded.getZ(),
                            DetailUtil.getDistanceGeneration(detailGen),
                            DetailUtil.getDistanceGeneration(detailGen + 1),
                            DetailUtil.getDistanceGenerationMode(detailGen).complexity,
                            (byte) 0,
                            maxChunkGenRequests));
                    requesting = maxChunkGenRequests - posListToGenerate.size();
                }


                if (LodConfig.CLIENT.distanceGenerationMode.get() != DistanceGenerationMode.NONE)
                {
                    // determine which points in the posListToGenerate
                    // should actually be queued up
                    for (LevelPos levelPos : posListToGenerate)
                    {
                        LevelPos chunkLevelPos = levelPos.convert(LodUtil.CHUNK_DETAIL_LEVEL);
                        int chunkX = chunkLevelPos.posX;
                        int chunkZ = chunkLevelPos.posZ;

                        if (numberOfChunksWaitingToGenerate.get() < maxChunkGenRequests)
                        {
                            ChunkPos pos = new ChunkPos(chunkX, chunkZ);

                            if (positionWaitingToBeGenerated.contains(pos))
                            {
                                ClientProxy.LOGGER.debug(pos + " asked to be generated again.");
                                continue;
                            }

                            // determine if this position is closer to the player
                            // than the previous
                            int newDistance = playerChunkPos.getChessboardDistance(pos);

                            if (newDistance < minChunkDist)
                            {
                                // this chunk is closer, clear any previous
                                // positions and update the new minimum distance
                                minChunkDist = newDistance;

                                // move all the old chunks into the reserve
                                ArrayList<ChunkPos> oldReserve = new ArrayList<>(chunksToGenReserve);
                                chunksToGenReserve.clear();
                                chunksToGenReserve.addAll(chunksToGen);
                                // top off reserve with whatever was in oldReerve
                                for (int i = 0; i < oldReserve.size(); i++)
                                {
                                    if (chunksToGenReserve.size() < maxChunkGenRequests)
                                        chunksToGenReserve.add(oldReserve.get(i));
                                    else
                                        break;
                                }

                                chunksToGen.clear();
                                chunksToGen.add(pos);
                            } else if (newDistance == minChunkDist)
                            {
                                // this chunk position as close as the minimum distance
                                if (chunksToGen.size() < maxChunkGenRequests)
                                {
                                    // we are still under the number of chunks to generate
                                    // add this position to the list
                                    chunksToGen.add(pos);
                                }
                            } else
                            {
                                // this chunk is farther away than the minimum distance,
                                // add it to the reserve to make sure we always have a full reserve
                                chunksToGenReserve.add(pos);
                            }

                        } // lod null and can generate more chunks
                    } // positions to generate


                    // queue up chunks to be generated
                    if (mc.hasSingleplayerServer())
                    {
                        // issue #19
                        // TODO add a way for a server side mod to generate chunks requested here
                        ServerWorld serverWorld = LodUtil.getServerWorldFromDimension(lodDim.dimension);

                        // make sure we have as many chunks to generate as we are allowed
                        if (chunksToGen.size() < maxChunkGenRequests)
                        {
                            Iterator<ChunkPos> reserveIterator = chunksToGenReserve.iterator();
                            while (chunksToGen.size() < maxChunkGenRequests && reserveIterator.hasNext())
                            {
                                chunksToGen.add(reserveIterator.next());
                            }
                        }

                        // start chunk generation
                        for (ChunkPos chunkPos : chunksToGen)
                        {
                            // don't add null chunkPos (which shouldn't happen anyway)
                            // or add more to the generation queue
                            if (chunkPos == null || numberOfChunksWaitingToGenerate.get() >= maxChunkGenRequests)
                                continue;

                            positionWaitingToBeGenerated.add(chunkPos);
                            numberOfChunksWaitingToGenerate.addAndGet(1);
                            LodNodeGenWorker genWorker = new LodNodeGenWorker(chunkPos, DistanceGenerationMode.SURFACE, LodDetail.FULL, renderer, LodQuadTreeNodeBuilder, this, lodDim, serverWorld);
                            WorldWorkerManager.addWorker(genWorker);
                        }
                    }
                } // if distanceGenerationMode != DistanceGenerationMode.NONE


                // finish the buffer building
                closeBuffers();

                // upload the new buffers
                uploadBuffers();


                long endTime = System.currentTimeMillis();
                long buildTime = endTime - startTime;
                //ClientProxy.LOGGER.info("Buffer Build time: " + buildTime + " ms");

                // mark that the buildable buffers as ready to swap
                switchVbos = true;
            } catch (Exception e)
            {
                ClientProxy.LOGGER.warn("\"LodNodeBufferBuilder.generateLodBuffersAsync\" ran into trouble: ");
                e.printStackTrace();
            } finally
            {
                // regardless of if we successfully created the buffers
                // we are done generating.
                generatingBuffers = false;


                // clean up any potentially open resources
                if (buildableBuffers != null)
                    closeBuffers();
            }

        });

        mainGenThread.execute(thread);

        return;
    }


    //====================//
    // generation helpers //
    //====================//

    /**
     * Returns if the given coordinate is in the loaded area of the world.
     *
     * @param centerCoordinate the center of the loaded world
     */
    private boolean isCoordInCenterArea(int i, int j, int centerCoordinate)
    {
        return (i >= centerCoordinate - mc.options.renderDistance
                && i <= centerCoordinate + mc.options.renderDistance)
                &&
                (j >= centerCoordinate - mc.options.renderDistance
                        && j <= centerCoordinate + mc.options.renderDistance);
    }


    //===============================//
    // BufferBuilder related methods //
    //===============================//


    /**
     * Called from the LodRenderer to create the
     * BufferBuilders.
     */
    public void setupBuffers(int numbRegionsWide, int bufferMaxCapacity)
    {
        buildableBuffers = new BufferBuilder[numbRegionsWide][numbRegionsWide];

        buildableVbos = new VertexBuffer[numbRegionsWide][numbRegionsWide];
        drawableVbos = new VertexBuffer[numbRegionsWide][numbRegionsWide];

        for (int x = 0; x < numbRegionsWide; x++)
        {
            for (int z = 0; z < numbRegionsWide; z++)
            {
                buildableBuffers[x][z] = new BufferBuilder(bufferMaxCapacity);
                buildableVbos[x][z] = new VertexBuffer(LodRenderer.LOD_VERTEX_FORMAT);
                drawableVbos[x][z] = new VertexBuffer(LodRenderer.LOD_VERTEX_FORMAT);
            }
        }
    }

    /**
     * Calls begin on each of the buildable BufferBuilders.
     */
    public void startBuffers()
    {
        for (int x = 0; x < buildableBuffers.length; x++)
            for (int z = 0; z < buildableBuffers.length; z++)
                buildableBuffers[x][z].begin(GL11.GL_QUADS, LodRenderer.LOD_VERTEX_FORMAT);
    }

    /**
     * Calls end on each of the buildable BufferBuilders.
     */
    public void closeBuffers()
    {
        for (int x = 0; x < buildableBuffers.length; x++)
            for (int z = 0; z < buildableBuffers.length; z++)
                if (buildableBuffers[x][z] != null && buildableBuffers[x][z].building())
                    buildableBuffers[x][z].end();
    }

    /**
     * Called from the LodRenderer to create the
     * BufferBuilders at the right size.
     *
     * @param bufferMaxCapacity
     */
    public void uploadBuffers()
    {
        for (int x = 0; x < buildableVbos.length; x++)
        {
            for (int z = 0; z < buildableVbos.length; z++)
            {
                buildableVbos[x][z].upload(buildableBuffers[x][z]);
            }
        }
    }


    /**
     * Get the newly created VBOs
     */
    public VertexBuffer[][] getVertexBuffers()
    {
        VertexBuffer[][] tmp = drawableVbos;
        drawableVbos = buildableVbos;
        buildableVbos = tmp;

        // the vbos have been swapped
        switchVbos = false;

        return drawableVbos;
    }

    /**
     * If this is true the buildable near and far
     * buffers have been generated and are ready to be
     * sent to the LodRenderer.
     */
    public boolean newBuffersAvaliable()
    {
        return switchVbos;
    }


}