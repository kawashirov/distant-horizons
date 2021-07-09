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
import java.util.*;
import java.util.stream.Collectors;

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


    public LodQuadTreeDimension(DimensionType newDimension, LodQuadTreeWorld lodWorld, int newMaxWidth)
    {
        dimension = newDimension;
        width = newMaxWidth;
        if(newDimension != null && lodWorld != null) {
            try {
                Minecraft mc = Minecraft.getInstance();

                File saveDir;
                if (mc.hasSingleplayerServer()) {
                    // local world

                    ServerWorld serverWorld = LodUtil.getServerWorldFromDimension(newDimension);
                    seed = serverWorld.getSeed();
                    // provider needs a separate variable to prevent
                    // the compiler from complaining
                    ServerChunkProvider provider = serverWorld.getChunkSource();
                    saveDir = new File(provider.dataStorage.dataFolder.getCanonicalFile().getPath() + File.separatorChar + "lod");
                } else {
                    // connected to server

                    saveDir = new File(mc.gameDirectory.getCanonicalFile().getPath() +
                            File.separatorChar + "lod server data" + File.separatorChar + LodUtil.getDimensionIDFromWorld(mc.level));
                }

                fileHandler = new LodQuadTreeDimensionFileHandler(saveDir, this);

            } catch (IOException e) {
                // the file handler wasn't able to be created
                // we won't be able to read or write any files
            }
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
                regions[xIndex][zIndex] = new LodQuadTree(regionZ, regionX);
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
     *this method create all the regions that are null
     */
    public void initializeNullRegions(){
        int n = regions.length;
        int xIndex;
        int zIndex;
        LodQuadTree region;
        for(int xRegion=0; xRegion<n; xRegion++){
            for(int zRegion=0; zRegion<n; zRegion++){
                xIndex = (xRegion + centerX) - halfWidth;
                zIndex = (zRegion + centerZ) - halfWidth;
                region = getRegion(xIndex,zIndex);
                if (region == null)
                {
                    // if no region exists, create it
                    region = new LodQuadTree(zIndex, xIndex);
                    setRegion(region);
                }
            }
        }
    }


    /**
     * Add the given LOD to this dimension at the coordinate
     * stored in the LOD. If an LOD already exists at the given
     * coordinates it will be overwritten.
     */
    public Boolean addNode(LodNodeData lodNodeData)
    {
        RegionPos pos = new RegionPos(
                lodNodeData.startX / 512,
                lodNodeData.startZ / 512
        );

        // don't continue if the region can't be saved
        if (!regionIsInRange(pos.x, pos.z))
        {
            return false;
        }

        LodQuadTree region = getRegion(pos.x, pos.z);

        if (region == null)
        {
            // if no region exists, create it
            region = new LodQuadTree(pos.z, pos.x);
            setRegion(region);
        }
        boolean coorectlyAdded = region.setNodeAtLowerLevel(lodNodeData, true);

        // don't save empty place holders to disk
        if (lodNodeData.real && fileHandler != null)
        {
            // mark the region as dirty so it will be saved to disk
            int xIndex = (pos.x - centerX) + halfWidth;
            int zIndex = (pos.z - centerZ) + halfWidth;
            isRegionDirty[xIndex][zIndex] = true;
            fileHandler.saveDirtyRegionsToFileAsync();
        }
        return coorectlyAdded;
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
        LodQuadTree region = getRegion(posX/(512/Math.pow(level,2)),posZ/(512/Math.pow(level,2)));
        if(region == null)
            return null;
        return region.getLodFromCoordinate(posX, posZ, level);
        /*
        RegionPos pos = LodUtil.convertChunkPosToRegionPos(new ChunkPos(chunkX, chunkZ));

        LodQuadTree region = getRegion(pos.x, pos.z);


        return region.getNode(chunkX, chunkZ);

         */
    }

    /**
     * method to get all the nodes that have to be rendered based on the position of the player
     * @return list of nodes
     */
    public List<LodNodeData> getNodeToRender(int x, int z, byte level, int maxDistance, int minDistance){
        int n = regions.length;
        List<LodNodeData> listOfData = new ArrayList<>();
        for(int i=0; i<n; i++){
            for(int j=0; j<n; j++){
                listOfData.addAll(regions[i][j].getNodeToRender(x,z,level,maxDistance,minDistance));
            }
        }
        return listOfData;
    }

    /**
     * method to get all the quadtree level that have to be generated based on the position of the player
     * @return list of quadTrees
     */
    public List<LodQuadTree> getNodeToGenerate(int x, int z, byte level, int maxDistance, int minDistance){

        int n = regions.length;
        int xIndex;
        int zIndex;
        LodQuadTree region;
        List<Map.Entry<LodQuadTree,Integer>> listOfQuadTree = new ArrayList<>();
        for(int xRegion=0; xRegion<n; xRegion++){
            for(int zRegion=0; zRegion<n; zRegion++){
                xIndex = (xRegion + centerX) - halfWidth;
                zIndex = (zRegion + centerZ) - halfWidth;
                region = getRegion(xIndex,zIndex);
                if (region == null){
                    region = new LodQuadTree(zIndex, xIndex);
                    setRegion(region);
                }
                listOfQuadTree.addAll(region.getLevelToGenerate(x,z,level,maxDistance,minDistance));
            }
        }
        Collections.sort(listOfQuadTree,Map.Entry.comparingByValue());
        return listOfQuadTree.stream().map(entry -> entry.getKey()).collect(Collectors.toList());
    }

    /**
     * getNodes
     * @return list of quadTrees
     */
    public List<LodNodeData> getNodes(boolean getOnlyReal, boolean getOnlyDirty, boolean getOnlyLeaf){
        int n = regions.length;
        List<LodNodeData> listOfNodes = new ArrayList<>();
        int xIndex;
        int zIndex;
        LodQuadTree region;
        for(int xRegion=0; xRegion<n; xRegion++){
            for(int zRegion=0; zRegion<n; zRegion++){
                xIndex = (xRegion + centerX) - halfWidth;
                zIndex = (zRegion + centerZ) - halfWidth;
                region = getRegion(xIndex,zIndex);
                if (region != null){
                    listOfNodes.addAll(region.getNodeList(getOnlyReal, getOnlyDirty, getOnlyLeaf));
                }
            }
        }
        return listOfNodes;
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
     * TODO THIS METHOD HAVE TO BE CHANGES. IS NOT THE SAME AS NUMER OF CHUNK
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
