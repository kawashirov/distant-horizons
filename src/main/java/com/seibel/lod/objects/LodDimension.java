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
package com.seibel.lod.objects;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.handlers.LodConfig;
import com.seibel.lod.handlers.LodDimensionFileHandler;
import com.seibel.lod.proxy.ClientProxy;
import com.seibel.lod.util.DetailUtil;
import com.seibel.lod.util.LodUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;

/**
 * This object holds all loaded LOD regions
 * for a given dimension.
 *
 * @author Leonardo Amato
 * @author James Seibel
 * @version 8-8-2021
 */
public class LodDimension
{

    public final DimensionType dimension;

    /**
     * measured in regions
     */
    private volatile int width;
    /**
     * measured in regions
     */
    private volatile int halfWidth;


    public volatile LodRegion regions[][];
    public volatile boolean isRegionDirty[][];

    private volatile RegionPos center;

    private LodDimensionFileHandler fileHandler;


    /**
     * Creates the dimension centered at (0,0)
     *
     * @param newWidth in regions
     */
    public LodDimension(DimensionType newDimension, LodWorld lodWorld, int newWidth)
    {
        dimension = newDimension;
        width = newWidth;
        halfWidth = (int) Math.floor(width / 2);

        if (newDimension != null && lodWorld != null)
        {
            try
            {
                Minecraft mc = Minecraft.getInstance();

                File saveDir;
                if (mc.hasSingleplayerServer())
                {
                    // local world

                    ServerWorld serverWorld = LodUtil.getServerWorldFromDimension(newDimension);

                    // provider needs a separate variable to prevent
                    // the compiler from complaining
                    ServerChunkProvider provider = serverWorld.getChunkSource();
                    saveDir = new File(provider.dataStorage.dataFolder.getCanonicalFile().getPath() + File.separatorChar + "lod");
                } else
                {
                    // connected to server

                    saveDir = new File(mc.gameDirectory.getCanonicalFile().getPath() +
                            File.separatorChar + "lod server data" + File.separatorChar + LodUtil.getDimensionIDFromWorld(mc.level));
                }

                fileHandler = new LodDimensionFileHandler(saveDir, this);
            } catch (IOException e)
            {
                // the file handler wasn't able to be created
                // we won't be able to read or write any files
            }
        }


        regions = new LodRegion[width][width];
        isRegionDirty = new boolean[width][width];

        // populate isRegionDirty
        for (int i = 0; i < width; i++)
            for (int j = 0; j < width; j++)
                isRegionDirty[i][j] = false;

        center = new RegionPos(0, 0);
    }


    /**
     * Move the center of this LodDimension and move all owned
     * regions over by the given x and z offset. <br><br>
     * <p>
     * Synchronized to prevent multiple moves happening on top of each other.
     */
    public synchronized void move(RegionPos regionOffset)
    {
        int xOffset = regionOffset.x;
        int zOffset = regionOffset.z;

        // if the x or z offset is equal to or greater than
        // the total size, just delete the current data
        // and update the centerX and/or centerZ
        if (Math.abs(xOffset) >= width || Math.abs(zOffset) >= width)
        {
            for (int x = 0; x < width; x++)
            {
                for (int z = 0; z < width; z++)
                {
                    regions[x][z] = null;
                }
            }

            // update the new center
            center.x += xOffset;
            center.z += zOffset;

            return;
        }


        // X
        if (xOffset > 0)
        {
            // move everything over to the left (as the center moves to the right)
            for (int x = 0; x < width; x++)
            {
                for (int z = 0; z < width; z++)
                {
                    if (x + xOffset < width)
                        regions[x][z] = regions[x + xOffset][z];
                    else
                        regions[x][z] = null;
                }
            }
        } else
        {
            // move everything over to the right (as the center moves to the left)
            for (int x = width - 1; x >= 0; x--)
            {
                for (int z = 0; z < width; z++)
                {
                    if (x + xOffset >= 0)
                        regions[x][z] = regions[x + xOffset][z];
                    else
                        regions[x][z] = null;
                }
            }
        }


        // Z
        if (zOffset > 0)
        {
            // move everything up (as the center moves down)
            for (int x = 0; x < width; x++)
            {
                for (int z = 0; z < width; z++)
                {
                    if (z + zOffset < width)
                        regions[x][z] = regions[x][z + zOffset];
                    else
                        regions[x][z] = null;
                }
            }
        } else
        {
            // move everything down (as the center moves up)
            for (int x = 0; x < width; x++)
            {
                for (int z = width - 1; z >= 0; z--)
                {
                    if (z + zOffset >= 0)
                        regions[x][z] = regions[x][z + zOffset];
                    else
                        regions[x][z] = null;
                }
            }
        }


        // update the new center
        center.x += xOffset;
        center.z += zOffset;
    }


    /**
     * Gets the region at the given X and Z
     * <br>
     * Returns null if the region doesn't exist
     * or is outside the loaded area.
     */
    public LodRegion getRegion(LevelPos levelPos)
    {
        RegionPos regionPos = levelPos.getRegionPos();
        int xIndex = (regionPos.x - center.x) + halfWidth;
        int zIndex = (regionPos.z - center.z) + halfWidth;

        if (!regionIsInRange(regionPos.x, regionPos.z))
            // out of range
            return null;

        if (regions[xIndex][zIndex] == null || regions[xIndex][zIndex].getMinDetailLevel() > levelPos.detailLevel)
        {

            regions[xIndex][zIndex] = getRegionFromFile(levelPos);
            if (regions[xIndex][zIndex] == null)
            {
                /**TODO the value is currently 0 but should be determinated by the distance of the player)*/
                regions[xIndex][zIndex] = new LodRegion(LodConfig.CLIENT.maxGenerationDetail.get().detailLevel, regionPos);
            }

            if (regions[xIndex][zIndex].getMinDetailLevel() > levelPos.detailLevel)
            {
                regions[xIndex][zIndex].expand(levelPos.detailLevel);
            }
        }

        return regions[xIndex][zIndex];
    }

    /**
     * Overwrite the LodRegion at the location of newRegion with newRegion.
     *
     * @throws ArrayIndexOutOfBoundsException if newRegion is outside what can be stored in this LodDimension.
     */
    public synchronized void addOrOverwriteRegion(LodRegion newRegion) throws ArrayIndexOutOfBoundsException
    {
        int xIndex = (newRegion.regionPosX - center.x) + halfWidth;
        int zIndex = (center.z - newRegion.regionPosZ) + halfWidth;

        if (!regionIsInRange(newRegion.regionPosX, newRegion.regionPosZ))
            // out of range
            throw new ArrayIndexOutOfBoundsException();

        regions[xIndex][zIndex] = newRegion;
    }


    /**
     * this method creates all null regions
     */
    public void treeCutter(int posX, int posZ)
    {
        int regionX;
        int regionZ;
        LevelPos levelPos;
        LodRegion region;

        for (int x = 0; x < regions.length; x++)
        {
            for (int z = 0; z < regions.length; z++)
            {
                regionX = (x + center.x) - halfWidth;
                regionZ = (z + center.z) - halfWidth;
                levelPos = new LevelPos(LodUtil.REGION_DETAIL_LEVEL, regionX, regionZ);
                for(byte index = LodUtil.BLOCK_DETAIL_LEVEL; index <= LodUtil.REGION_DETAIL_LEVEL; index++){
                    if(DetailUtil.getDistanceGeneration(index+1) > levelPos.minDistance(posX, posZ)){
                        region = getRegion(levelPos.convert(DetailUtil.getLodDetail(index).detailLevel));
                        region.cuteTree(DetailUtil.getLodDetail(index-1).detailLevel);
                        break;
                    }
                }
            }
        }
    }


    /**
     * Add the given LOD to this dimension at the coordinate
     * stored in the LOD. If an LOD already exists at the given
     * coordinates it will be overwritten.
     */
    public synchronized Boolean addData(LevelPos levelPos, LodDataPoint lodDataPoint, DistanceGenerationMode generationMode, boolean update, boolean dontSave)
    {

        // don't continue if the region can't be saved
        RegionPos regionPos = levelPos.getRegionPos();
        if (!regionIsInRange(regionPos.x, regionPos.z))
        {
            return false;
        }

        LodRegion region = getRegion(levelPos);

        boolean nodeAdded = region.setData(levelPos, lodDataPoint, generationMode.complexity, true);
        // only save valid LODs to disk
        if (!dontSave && fileHandler != null)
        {
            try
            {
                // mark the region as dirty so it will be saved to disk
                int xIndex = (regionPos.x - center.x) + halfWidth;
                int zIndex = (regionPos.z - center.z) + halfWidth;
                isRegionDirty[xIndex][zIndex] = true;
            } catch (ArrayIndexOutOfBoundsException e)
            {
                // This method was probably called when the dimension was changing size.
                // Hopefully this shouldn't be an issue.
            }
        }
        return nodeAdded;
    }


    /**
     * Get the LodNodeData at the given X and Z coordinates
     * in this dimension.
     * <br>
     * Returns null if the LodChunk doesn't exist or
     * is outside the loaded area.
     */
    public LodDataPoint getData(ChunkPos chunkPos)
    {
        LevelPos levelPos = new LevelPos(LodUtil.CHUNK_DETAIL_LEVEL, chunkPos.x, chunkPos.z);
        return getData(levelPos);
    }


    /**
     * method to get all the quadtree level that have to be generated based on the position of the player
     *
     * @return list of quadTrees
     */
    public List<LevelPos> getDataToGenerate(int playerPosX, int playerPosZ, int start, int end, byte generation, byte detailLevel, int dataNumber)
    {

        int n = regions.length;
        int xIndex;
        int zIndex;
        LodRegion region;
        List<Map.Entry<LevelPos, Integer>> listOfData = new ArrayList<>();
        for (int xRegion = 0; xRegion < n; xRegion++)
        {
            for (int zRegion = 0; zRegion < n; zRegion++)
            {
                xIndex = (xRegion + center.x) - halfWidth;
                zIndex = (zRegion + center.z) - halfWidth;
                RegionPos regionPos = new RegionPos(xIndex, zIndex);
                region = getRegion(new LevelPos(LodUtil.REGION_DETAIL_LEVEL, regionPos.x, regionPos.z).convert(detailLevel));
                listOfData.addAll(region.getDataToGenerate(playerPosX, playerPosZ, start, end, generation, detailLevel, dataNumber));
            }
        }

        List<Map.Entry<LevelPos,Integer>> levelMinPosList = new ArrayList<>();
        dataNumber = Math.min(dataNumber, listOfData.size());

        for(int i=0; i<dataNumber; i++)
        {
            Map.Entry<LevelPos,Integer> min = Collections.min(listOfData, LevelPos.getPosComparator());
            listOfData.remove(min);
            levelMinPosList.add(min);
        }
        return levelMinPosList.stream().map(entry -> entry.getKey()).collect(Collectors.toList()).subList(0, dataNumber);
    }


    /**
     * method to get all the nodes that have to be rendered based on the position of the player
     *
     * @return list of nodes
     */
    public List<LevelPos> getDataToRender(int playerPosX, int playerPosZ, int start, int end, byte detailLevel)
    {
        int n = regions.length;
        List<LevelPos> listOfData = new ArrayList<>();
        int xIndex;
        int zIndex;
        LodRegion region;
        for (int xRegion = 0; xRegion < n; xRegion++)
        {
            for (int zRegion = 0; zRegion < n; zRegion++)
            {
                xIndex = (xRegion + center.x) - halfWidth;
                zIndex = (zRegion + center.z) - halfWidth;
                RegionPos regionPos = new RegionPos(xIndex, zIndex);
                region = getRegion(new LevelPos(LodUtil.REGION_DETAIL_LEVEL, regionPos.x, regionPos.z).convert(detailLevel));
                if (region == null)
                {
                    //region = new LodRegion(DetailUtil.getLodDetail(detailLevel).detailLevel, regionPos);
                    //addOrOverwriteRegion(region);
                } else
                {
                    listOfData.addAll(region.getDataToRender(playerPosX, playerPosZ, start, end, detailLevel));
                }
            }
        }
        return listOfData;
    }

    /**
     * method to get all the nodes that have to be rendered based on the position of the player
     *
     * @return list of nodes
     */
    public List<LevelPos> getDataToRender(RegionPos regionPos, int playerPosX, int playerPosZ, int start, int end, byte detailLevel)
    {
        List<LevelPos> listOfData = new ArrayList<>();
        LodRegion region = getRegion(new LevelPos(LodUtil.REGION_DETAIL_LEVEL, regionPos.x, regionPos.z).convert(detailLevel));
        if (region == null)
        {
            /*
        	try
        	{
	            region = new LodRegion(DetailUtil.getLodDetail(detailLevel).detailLevel, regionPos);
	            addOrOverwriteRegion(region);
        	}
        	catch (ArrayIndexOutOfBoundsException e)
        	{
        		ClientProxy.LOGGER.warn("getDataToRender was unable to add the region at the pos [" + regionPos.x + ", " + regionPos.z + "]");
        		return listOfData; // this list should be empty
        	}
             */
            return listOfData;
        }
        else
        {
            listOfData.addAll(region.getDataToRender(playerPosX, playerPosZ, start, end, detailLevel));
        }
        return listOfData;
    }

    /**
     * Get the data point at the given X and Z coordinates
     * in this dimension.
     * <br>
     * Returns null if the LodChunk doesn't exist or
     * is outside the loaded area.
     */
    public LodDataPoint getData(LevelPos levelPos)
    {
        if (levelPos.detailLevel > LodUtil.REGION_DETAIL_LEVEL)
            throw new IllegalArgumentException("getLodFromCoordinates given a level of \"" + levelPos.detailLevel + "\" when \"" + LodUtil.REGION_DETAIL_LEVEL + "\" is the max.");

        LodRegion region = getRegion(levelPos);


        if (region == null)
        {
            return null;
        }

        return region.getData(levelPos);
    }


    /**
     * Get the data point at the given X and Z coordinates
     * in this dimension.
     * <br>
     * Returns null if the LodChunk doesn't exist or
     * is outside the loaded area.
     */
    public void updateData(LevelPos levelPos)
    {
        if (levelPos.detailLevel > LodUtil.REGION_DETAIL_LEVEL)
            throw new IllegalArgumentException("getLodFromCoordinates given a level of \"" + levelPos.detailLevel + "\" when \"" + LodUtil.REGION_DETAIL_LEVEL + "\" is the max.");

        LodRegion region = getRegion(levelPos);


        if (region == null)
        {
            return;
        }
        region.updateArea(levelPos);
    }

    /**
     * return true if and only if the node at that position exist
     */

    public boolean hasThisPositionBeenGenerated(LevelPos levelPos)
    {
        LodRegion region = getRegion(levelPos);

        if (region == null)
        {
            return false;
        }

        return region.hasDataBeenGenerated(levelPos);
    }

    /**
     * return true if and only if the node at that position exist
     */
    public boolean doesDataExist(LevelPos levelPos)
    {
        LodRegion region = getRegion(levelPos);

        if (region == null)
        {
            return false;
        }

        return region.doesDataExist(levelPos);
    }

    /**
     * return true if and only if the node at that position exist
     */
    public DistanceGenerationMode getGenerationMode(LevelPos levelPos)
    {
        LodRegion region = getRegion(levelPos);

        if (region == null)
        {
            return DistanceGenerationMode.NONE;
        }

        return region.getGenerationMode(levelPos);
    }

    /**
     * Get the region at the given X and Z coordinates from the
     * RegionFileHandler.
     */
    public LodRegion getRegionFromFile(LevelPos levelPos)
    {
        if (fileHandler != null)
            return fileHandler.loadRegionFromFile(levelPos);
        else
            return null;
    }

    /**
     * Save all dirty regions in this LodDimension to file.
     */
    public void saveDirtyRegionsToFileAsync()
    {
        fileHandler.saveDirtyRegionsToFileAsync();
    }


    /**
     * Returns whether the region at the given X and Z coordinates
     * is within the loaded range.
     */
    public boolean regionIsInRange(int regionX, int regionZ)
    {
        int xIndex = (regionX - center.x) + halfWidth;
        int zIndex = (regionZ - center.z) + halfWidth;

        return xIndex >= 0 && xIndex < width && zIndex >= 0 && zIndex < width;
    }


    public int getCenterX()
    {
        return center.x;
    }

    public int getCenterZ()
    {
        return center.z;
    }


    /**
     * TODO Double check that this method works as expected
     * <p>
     * Returns how many non-null LodChunks
     * are stored in this LodDimension.
     */
    public int getNumberOfLods()
    {
        /**TODO **/
        int numbLods = 0;
        return numbLods;
    }


    public int getWidth()
    {
        if (regions != null)
        {
            // we want to get the length directly from the
            // source to make sure it is in sync with region
            // and isRegionDirty
            return regions.length;
        } else
        {
            return width;
        }
    }

    public void setRegionWidth(int newWidth)
    {
        width = newWidth;
        halfWidth = (int) Math.floor(width / 2);

        regions = new LodRegion[width][width];
        isRegionDirty = new boolean[width][width];

        // populate isRegionDirty
        for (int i = 0; i < width; i++)
            for (int j = 0; j < width; j++)
                isRegionDirty[i][j] = false;
    }


    @Override
    public String toString()
    {
        String s = "";

        s += "dim: " + dimension.toString() + "\t";
        s += "(" + center.x + "," + center.z + ")";

        return s;
    }
}
