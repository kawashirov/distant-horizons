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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.handlers.LodQuadTreeDimensionFileHandler;
import com.seibel.lod.util.LodUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
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
public class LodQuadTreeDimension
{
	/**TODO a dimension should support two different type of quadTree.
	 * The ones that are near from the player should always be saved and can be fully generated (even at block level)
	 * The ones that are far from the player should always be non-savable and at a high level
	 * If this is not done then you could see how heavy a fully generated 64 region dimension can get.
	 * IDEA : use a mask like the "isRegionDirty" to achieve this*/
	
	public final DimensionType dimension;
	
	/** measured in regions */
	private volatile int width;
	/** measured in regions */
	private volatile int halfWidth;
	
	/**  */
	public static final Set<DistanceGenerationMode> FULL_COMPLEXITY_MASK = new HashSet<DistanceGenerationMode>();
	static
	{
		// I moved the setup here because eclipse was complaining
		FULL_COMPLEXITY_MASK.add(DistanceGenerationMode.BIOME_ONLY);
		FULL_COMPLEXITY_MASK.add(DistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT);
		FULL_COMPLEXITY_MASK.add(DistanceGenerationMode.SURFACE);
		FULL_COMPLEXITY_MASK.add(DistanceGenerationMode.FEATURES);
		FULL_COMPLEXITY_MASK.add(DistanceGenerationMode.SERVER);
	}
	
	
	public volatile LodQuadTree regions[][];
	public volatile boolean isRegionDirty[][];
	
	private volatile RegionPos center;
	
	private LodQuadTreeDimensionFileHandler fileHandler;
	
	
	/**
	 * Creates the dimension centered at (0,0)
	 * 
	 * @param newWidth in regions
	 */
	public LodQuadTreeDimension(DimensionType newDimension, LodQuadTreeWorld lodWorld, int newWidth)
	{
		dimension = newDimension;
		width = newWidth;
		halfWidth = (int)Math.floor(width / 2);
		
		if(newDimension != null && lodWorld != null)
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
				}
				else
				{
					// connected to server
					
					saveDir = new File(mc.gameDirectory.getCanonicalFile().getPath() +
							File.separatorChar + "lod server data" + File.separatorChar + LodUtil.getDimensionIDFromWorld(mc.level));
				}
				
				fileHandler = new LodQuadTreeDimensionFileHandler(saveDir, this);
			}
			catch (IOException e)
			{
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
		
		center = new RegionPos(0,0);
	}
	
	
	/**
	 * Move the center of this LodDimension and move all owned
	 * regions over by the given x and z offset. <br><br>
	 * 
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
			for(int x = 0; x < width; x++)
			{
				for(int z = 0; z < width; z++)
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
		center.x += xOffset;
		center.z += zOffset;
	}
	
	
	
	
	
	
	/**
	 * Gets the region at the given X and Z
	 * <br>
	 * Returns null if the region doesn't exist
	 * or is outside the loaded area.
	 */
	public LodQuadTree getRegion(RegionPos regionPos)
	{
		int xIndex = (regionPos.x - center.x) + halfWidth;
		int zIndex = (regionPos.z - center.z) + halfWidth;
		
		if (!regionIsInRange(regionPos.x, regionPos.z))
			// out of range
			return null;
		
		if (regions[xIndex][zIndex] == null)
		{
			regions[xIndex][zIndex] = getRegionFromFile(regionPos);
			if (regions[xIndex][zIndex] == null)
			{
				regions[xIndex][zIndex] = new LodQuadTree(regionPos);
			}
		}
		
		return regions[xIndex][zIndex];
	}
	
	/**
	 * Overwrite the LodRegion at the location of newRegion with newRegion.
	 * 
	 * @throws ArrayIndexOutOfBoundsException if newRegion is outside what can be stored in this LodDimension.
	 */
	public void addOrOverwriteRegion(LodQuadTree newRegion) throws ArrayIndexOutOfBoundsException
	{
		int xIndex = (newRegion.getLodNodeData().posX - center.x) + halfWidth;
		int zIndex = (center.z - newRegion.getLodNodeData().posZ) + halfWidth;
		
		if (!regionIsInRange(newRegion.getLodNodeData().posX, newRegion.getLodNodeData().posZ))
			// out of range
			throw new ArrayIndexOutOfBoundsException();
		
		regions[xIndex][zIndex] = newRegion;
	}
	
	
	/**
	 *this method creates all null regions
	 */
	public void initializeNullRegions()
	{
		int regionX;
		int regionZ;
		RegionPos regionPos;
		LodQuadTree region;
		
		for(int x = 0; x < regions.length; x++)
		{
			for(int z = 0; z < regions.length; z++)
			{
				regionX = (x + center.x) - halfWidth;
				regionZ = (z + center.z) - halfWidth;
				regionPos = new RegionPos(regionX,regionZ);
				region = getRegion(regionPos);
				
				if (region == null)
				{
					// if no region exists, create it
					region = new LodQuadTree(regionPos);
					addOrOverwriteRegion(region);
				}
			}
		}
	}
	
	
	/**
	 * Add the given LOD to this dimension at the coordinate
	 * stored in the LOD. If an LOD already exists at the given
	 * coordinates it will be overwritten.
	 */
	public Boolean addNode(LodQuadTreeNode lodNode)
	{
		RegionPos regionPos = new RegionPos(new ChunkPos(lodNode.center.getX(), lodNode.center.getZ()));
		
		// don't continue if the region can't be saved
		if (!regionIsInRange(regionPos.x, regionPos.z))
		{
			return false;
		}
		
		LodQuadTree region = getRegion(regionPos);
		
		if (region == null)
		{
			// if no region exists, create it
			region = new LodQuadTree(regionPos);
			addOrOverwriteRegion(region);
		}
		boolean nodeAdded = region.setNodeAtLowerLevel(lodNode);
		
		// only save valid LODs to disk
		if (!lodNode.dontSave && fileHandler != null)
		{
			// mark the region as dirty so it will be saved to disk
			int xIndex = (regionPos.x - center.x) + halfWidth;
			int zIndex = (regionPos.z - center.z) + halfWidth;
			isRegionDirty[xIndex][zIndex] = true;
			fileHandler.saveDirtyRegionsToFileAsync();
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
	public LodQuadTreeNode getLodFromCoordinates(ChunkPos chunkPos)
	{
		return getLodFromCoordinates(chunkPos, LodQuadTreeNode.CHUNK_LEVEL);
	}
	
	/**
	 * Get the LodNodeData at the given X and Z coordinates
	 * in this dimension.
	 * <br>
	 * Returns null if the LodChunk doesn't exist or
	 * is outside the loaded area.
	 */
	public LodQuadTreeNode getLodFromCoordinates(ChunkPos chunkPos, int detailLevel)
	{
		if (detailLevel > LodQuadTreeNode.REGION_LEVEL)
			throw new IllegalArgumentException("getLodFromCoordinates given a level of \"" + detailLevel + "\" when \"" + LodQuadTreeNode.REGION_LEVEL + "\" is the max.");
		
		// TODO possibly put this in LodUtil
		int regionPosX = Math.floorDiv(chunkPos.x, (int) Math.pow(2,LodQuadTreeNode.REGION_LEVEL - detailLevel));
        int regionPosZ = Math.floorDiv(chunkPos.z, (int) Math.pow(2,LodQuadTreeNode.REGION_LEVEL - detailLevel));
    	LodQuadTree region = getRegion(new RegionPos(regionPosX, regionPosZ));
		
		if(region == null)
		{
			return null;
		}
		
		return region.getNodeAtChunkPos(chunkPos, detailLevel);
	}
	
	/**
	 * return true if and only if the node at that position exist
	 */
	public boolean hasThisPositionBeenGenerated(ChunkPos chunkPos, int level)
	{
		if (level > LodQuadTreeNode.REGION_LEVEL)
			throw new IllegalArgumentException("getLodFromCoordinates given a level of \"" + level + "\" when \"" + LodQuadTreeNode.REGION_LEVEL + "\" is the max.");
		
		return getLodFromCoordinates(chunkPos, level).detailLevel == level;
	}
	
	/**
	 * method to get all the nodes that have to be rendered based on the position of the player
	 * @return list of nodes
	 */
	public List<LodQuadTreeNode> getNodesToRender(BlockPos playerPos, int detailLevel, 
			Set<DistanceGenerationMode> complexityMask, int maxDistance, int minDistance)
	{
		List<LodQuadTreeNode> listOfData = new ArrayList<>();
		
		// go through every region we have stored
		for(int i = 0; i < regions.length; i++)
		{
			for(int j = 0; j < regions.length; j++)
			{
				listOfData.addAll(regions[i][j].getNodeToRender(playerPos, detailLevel, complexityMask, maxDistance, minDistance));
			}
		}
		
		return listOfData;
	}
	
	/**
	 * Returns all LodQuadTreeNodes that need to be generated based on the position of the player
	 * @return list of quadTrees
	 */
	public List<LodQuadTreeNode> getNodesToGenerate(BlockPos playerPos, byte level, DistanceGenerationMode complexity, 
			int maxDistance, int minDistance)
	{
		int regionX;
		int regionZ;
		LodQuadTree region;
		RegionPos regionPos;
		List<Map.Entry<LodQuadTreeNode,Integer>> listOfQuadTree = new ArrayList<>();
		
		// go through every region we have stored
		for(int xIndex = 0; xIndex < regions.length; xIndex++)
		{
			for(int zIndex = 0; zIndex < regions.length; zIndex++)
			{
				regionX = (xIndex + center.x) - halfWidth;
				regionZ = (zIndex + center.z) - halfWidth;
				regionPos = new RegionPos(regionX,regionZ);
				region = getRegion(regionPos);
				
				if (region == null)
				{
					region = new LodQuadTree(regionPos);
					addOrOverwriteRegion(region);
				}
				
				listOfQuadTree.addAll(region.getNodesToGenerate(playerPos, level, complexity, maxDistance, minDistance));
			}
		}
		
		// TODO why are we sorting the list?
		Collections.sort(listOfQuadTree, Map.Entry.comparingByValue());
		return listOfQuadTree.stream().map(entry -> entry.getKey()).collect(Collectors.toList());
	}
	
	/**
	 * @see LodQuadTree#getNodeListWithMask
	 */
	public List<LodQuadTreeNode> getNodesWithMask(Set<DistanceGenerationMode> complexityMask, boolean getOnlyDirty, boolean getOnlyLeaf)
	{
		List<LodQuadTreeNode> listOfNodes = new ArrayList<>();
		
		int xIndex;
		int zIndex;
		LodQuadTree region;
		
		// go through every region we have stored
		for(int xRegion = 0; xRegion < regions.length; xRegion++)
		{
			for(int zRegion = 0; zRegion < regions.length; zRegion++)
			{
				xIndex = (xRegion + center.x) - halfWidth;
				zIndex = (zRegion + center.z) - halfWidth;
				region = getRegion(new RegionPos(xIndex,zIndex));
				
				// Recursively add any children
				if (region != null)
				{
					listOfNodes.addAll(region.getNodeListWithMask(complexityMask, getOnlyDirty, getOnlyLeaf));
				}
			}
		}
		
		return listOfNodes;
	}
	
	/**
	 * Get the region at the given X and Z coordinates from the
	 * RegionFileHandler.
	 */
	public LodQuadTree getRegionFromFile(RegionPos regionPos)
	{
		if (fileHandler != null)
			return fileHandler.loadRegionFromFile(regionPos);
		else
			return null;
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
	 * 
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
				
				for(LodQuadTreeNode node : region.getNodeListWithMask(FULL_COMPLEXITY_MASK,false,true))
				{
					if (node != null && !node.voidNode)
						numbLods++;
				}
			}
		}
		
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
		}
		else
		{
			return width;
		}
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
		s += "(" + center.x + "," + center.z + ")";
		
		return s;
	}
}
