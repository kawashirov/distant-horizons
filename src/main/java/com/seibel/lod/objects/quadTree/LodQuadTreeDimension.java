package com.seibel.lod.objects.quadTree;

import com.seibel.lod.handlers.LodQuadTreeDimensionFileHandler;
import com.seibel.lod.objects.LodWorld;
import com.seibel.lod.objects.RegionPos;
import com.seibel.lod.util.LodUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.DimensionType;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class LodQuadTreeDimension {
    public final DimensionType dimension;

    private volatile int width;
    private volatile int halfWidth;
    public long seed;

    public volatile LodQuadTree regions[][];
    public volatile boolean isRegionDirty[][];

    private int centerX;
    private int centerZ;

    private LodQuadTreeDimensionFileHandler fileHandler;


    public LodQuadTreeDimension(DimensionType newDimension, LodWorld lodWorld, int newMaxWidth)
    {
        dimension = newDimension;
        width = newMaxWidth;

        try
        {
            Minecraft mc = Minecraft.getInstance();

            File saveDir;
            if(mc.isIntegratedServerRunning())
            {
                // local world

                ServerWorld serverWorld = LodUtil.getServerWorldFromDimension(newDimension);
                seed = serverWorld.getSeed();
                // provider needs a separate variable to prevent
                // the compiler from complaining
                ServerChunkProvider provider = serverWorld.getChunkProvider();
                saveDir = new File(provider.getSavedData().folder.getCanonicalFile().getPath() + File.separatorChar + "lod");
            }
            else
            {
                // connected to server

                saveDir = new File(mc.gameDir.getCanonicalFile().getPath() +
                        File.separatorChar + "lod server data" + File.separatorChar + LodUtil.getDimensionIDFromWorld(mc.world));
            }

            fileHandler = new LodQuadTreeDimensionFileHandler(saveDir, this);
        }
        catch(IOException e)
        {
            // the file handler wasn't able to be created
            // we won't be able to read or write any files
        }


        regions = new LodQuadTree[width][width];
        isRegionDirty = new boolean[width][width];

        // populate isRegionDirty
        for(int i = 0; i < width; i++)
            for(int j = 0; j < width; j++)
                isRegionDirty[i][j] = false;

        centerX = 0;
        centerZ = 0;

        halfWidth = (int)Math.floor(width / 2);
    }


    /**
     * Move the center of this LodDimension and move all owned
     * regions over by the given x and z offset.
     */
    public void move(int xOffset, int zOffset)
    {
        // if the x or z offset is equal to or greater than
        // the total size, just delete the current data
        // and update the centerX and/or centerZ
        if (Math.abs(xOffset) >= width || Math.abs(zOffset) >= width)
        {
            for(int x = 0; x < width; x++)
            {
                for(int z = 0; z < width; z++)
                {
                    regions[x][z] = null;
                }
            }

            // update the new center
            centerX += xOffset;
            centerZ += zOffset;

            return;
        }


        // X
        if(xOffset > 0)
        {
            // move everything over to the left (as the center moves to the right)
            for(int x = 0; x < width; x++)
            {
                for(int z = 0; z < width; z++)
                {
                    if(x + xOffset < width)
                        regions[x][z] = regions[x + xOffset][z];
                    else
                        regions[x][z] = null;
                }
            }
        }
        else
        {
            // move everything over to the right (as the center moves to the left)
            for(int x = width - 1; x >= 0; x--)
            {
                for(int z = 0; z < width; z++)
                {
                    if(x + xOffset >= 0)
                        regions[x][z] = regions[x + xOffset][z];
                    else
                        regions[x][z] = null;
                }
            }
        }



        // Z
        if(zOffset > 0)
        {
            // move everything up (as the center moves down)
            for(int x = 0; x < width; x++)
            {
                for(int z = 0; z < width; z++)
                {
                    if(z + zOffset < width)
                        regions[x][z] = regions[x][z + zOffset];
                    else
                        regions[x][z] = null;
                }
            }
        }
        else
        {
            // move everything down (as the center moves up)
            for(int x = 0; x < width; x++)
            {
                for(int z = width - 1; z >= 0; z--)
                {
                    if(z + zOffset >= 0)
                        regions[x][z] = regions[x][z + zOffset];
                    else
                        regions[x][z] = null;
                }
            }
        }



        // update the new center
        centerX += xOffset;
        centerZ += zOffset;
    }






    /**
     * Gets the region at the given X and Z
     * <br>
     * Returns null if the region doesn't exist
     * or is outside the loaded area.
     */
    public LodQuadTree getRegion(int regionX, int regionZ)
    {
        int xIndex = (regionX - centerX) + halfWidth;
        int zIndex = (regionZ - centerZ) + halfWidth;

        if (!regionIsInRange(regionX, regionZ))
            // out of range
            return null;

        if (regions[xIndex][zIndex] == null)
        {
            regions[xIndex][zIndex] = getRegionFromFile(regionX, regionZ);
            if (regions[xIndex][zIndex] == null)
            {
                regions[xIndex][zIndex] = new LodQuadTree(regionX, regionZ);
            }
        }

        return regions[xIndex][zIndex];
    }

    /**
     * Overwrite the LodRegion at the location of newRegion with newRegion.
     * @throws ArrayIndexOutOfBoundsException if newRegion is outside what can be stored in this LodDimension.
     */
    public void setRegion(LodQuadTree newRegion) throws ArrayIndexOutOfBoundsException
    {
        int xIndex = (newRegion.getLodNodeData().posX - centerX) + halfWidth;
        int zIndex = (centerZ - newRegion.getLodNodeData().posZ) + halfWidth;

        if (!regionIsInRange(newRegion.getLodNodeData().posX, newRegion.getLodNodeData().posZ))
            // out of range
            throw new ArrayIndexOutOfBoundsException();

        regions[xIndex][zIndex] = newRegion;
    }





    /**
     * Add the given LOD to this dimension at the coordinate
     * stored in the LOD. If an LOD already exists at the given
     * coordinates it will be overwritten.
     */
    public void addNode(LodNodeData lodNodeData)
    {
        RegionPos pos = new RegionPos(
                lodNodeData.posX / lodNodeData.width,
                lodNodeData.posZ / lodNodeData.width
        );

        // don't continue if the region can't be saved
        if (!regionIsInRange(pos.x, pos.z))
        {
            return;
        }

        LodQuadTree region = getRegion(pos.x, pos.z);

        if (region == null)
        {
            // if no region exists, create it
            region = new LodQuadTree(pos.x, pos.z);
            setRegion(region);
        }

        region.setNodeAtLowerLevel(lodNodeData, true);

        // don't save empty place holders to disk
        if (!lodNodeData.real && fileHandler != null)
        {
            // mark the region as dirty so it will be saved to disk
            int xIndex = (pos.x - centerX) + halfWidth;
            int zIndex = (pos.z - centerZ) + halfWidth;
            isRegionDirty[xIndex][zIndex] = true;
            fileHandler.saveDirtyRegionsToFileAsync();
        }
    }

    /**
     * Get the LodNodeData at the given X and Z position in the level
     * in this dimension.
     * <br>
     * Returns null if the LodChunk doesn't exist or
     * is outside the loaded area.
     */
    public LodNodeData getLodFromCoordinates(int posX, int posZ, byte level)
    {
        /*TODO */
        return null;
        /*
        RegionPos pos = LodUtil.convertChunkPosToRegionPos(new ChunkPos(chunkX, chunkZ));

        LodQuadTree region = getRegion(pos.x, pos.z);

        if(region == null)
            return null;

        return region.getNode(chunkX, chunkZ);

         */
    }

    /**
     * method to get all the nodes that have to be rendered based on the position of the player
     * @return list of nodes
     */
    public List getNodeToRender(int x, int z){
        //BASIC IDEA
        //Get all the node to render from every region
        //combine them toghether
        return null;
    }

    /**
     * method to get all the nodes that have to be generated based on the position of the player
     * @return list of nodes
     */
    public List getNodeToGenerate(int x, int z){
        //BASIC IDEA
        //Get all the node to generate from every region
        //combine them toghether
        //sort them based on the distance to player
        //this way
        return null;
    }

    /**
     * Get the region at the given X and Z coordinates from the
     * RegionFileHandler.
     */
    public LodQuadTree getRegionFromFile(int regionX, int regionZ)
    {
        if (fileHandler != null)
            return fileHandler.loadRegionFromFile(regionX, regionZ);
        else
            return null;
    }


    /**
     * Returns whether the region at the given X and Z coordinates
     * is within the loaded range.
     */
    public boolean regionIsInRange(int regionX, int regionZ)
    {
        int xIndex = (regionX - centerX) + halfWidth;
        int zIndex = (regionZ - centerZ) + halfWidth;

        return xIndex >= 0 && xIndex < width && zIndex >= 0 && zIndex < width;
    }







    public int getCenterX()
    {
        return centerX;
    }

    public int getCenterZ()
    {
        return centerZ;
    }


    /**
     * Returns how many non-null LodChunks
     * are stored in this LodDimension.
     */
    public int getNumberOfLods()
    {
        int numbLods = 0;
        for (LodQuadTree[] regions : regions)
        {
            if(regions == null)
                continue;

            for (LodQuadTree region : regions)
            {
                if(region == null)
                    continue;

                numbLods= region.getNodeList(false,false,true).size();
            }
        }

        return numbLods;
    }


    public int getWidth()
    {
        return width;
    }

    public void setRegionWidth(int newWidth)
    {
        width = newWidth;
        halfWidth = (int)Math.floor(width / 2);

        regions = new LodQuadTree[width][width];
        isRegionDirty = new boolean[width][width];

        // populate isRegionDirty
        for(int i = 0; i < width; i++)
            for(int j = 0; j < width; j++)
                isRegionDirty[i][j] = false;
    }


    @Override
    public String toString()
    {
        String s = "";

        s += "dim: " + dimension.toString() + "\t";
        s += "(" + centerX + "," + centerZ + ")";

        return s;
    }
}
