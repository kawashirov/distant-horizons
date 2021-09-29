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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.seibel.lod.config.LodConfig;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.enums.GenerationPriority;
import com.seibel.lod.enums.LodTemplate;
import com.seibel.lod.enums.VerticalQuality;
import com.seibel.lod.handlers.LodDimensionFileHandler;
import com.seibel.lod.util.DataPointUtil;
import com.seibel.lod.util.DetailDistanceUtil;
import com.seibel.lod.util.LevelPosUtil;
import com.seibel.lod.util.LodThreadFactory;
import com.seibel.lod.util.LodUtil;
import com.seibel.lod.wrappers.MinecraftWrapper;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;


/**
 * This object holds all loaded LOD regions
 * for a given dimension. <Br><Br>
 * 
 * <strong>Coordinate Standard: </strong><br>
 * Coordinate called posX or posZ are relative LevelPos coordinates <br>
 * unless stated otherwise. <br>
 * 
 * @author Leonardo Amato
 * @author James Seibel
 * @version 9-27-2021
 */
public class LodDimension
{
	public final DimensionType dimension;
	
	/** measured in regions */
	private volatile int width;
	/** measured in regions */
	private volatile int halfWidth;
	
	// these three variables are private to force use of the getWidth() method
	// which is a safer way to get the width then directly asking the arrays
	/** stores all the regions in this dimension */
	public volatile LodRegion[][] regions;
	
	/** stores if the region at the given x and z index needs to be saved to disk */
	private volatile boolean[][] isRegionDirty;
	/** stores if the region at the given x and z index needs to be regenerated */
	private volatile boolean[][] regenRegionBuffer;
	/** stores if the buffer size at the given x and z index needs to be changed */
	private volatile boolean[][] recreateRegionBuffer;
	
	/**
	 * if true that means there are regions in this dimension
	 * that need to have their buffers rebuilt.
	 */
	public volatile boolean regenDimensionBuffers = false;
	
	private LodDimensionFileHandler fileHandler;
	
	private volatile RegionPos center;
	
	/** prevents the cutAndExpandThread from expanding at the same location multiple times */
	private volatile ChunkPos lastExpandedChunk;
	/** prevents the cutAndExpandThread from cutting at the same location multiple times */
	private volatile ChunkPos lastCutChunk;
	private ExecutorService cutAndExpandThread = Executors.newSingleThreadExecutor(new LodThreadFactory(this.getClass().getSimpleName() + " - Cut and Expand"));
	
	/**
	 * Creates the dimension centered at (0,0)
	 *
	 * @param newWidth in regions
	 */
	public LodDimension(DimensionType newDimension, LodWorld lodWorld, int newWidth)
	{
		lastCutChunk = null;
		lastExpandedChunk = null;
		dimension = newDimension;
		width = newWidth;
		halfWidth = width / 2;
		MinecraftWrapper mc = MinecraftWrapper.INSTANCE;
		
		if (newDimension != null && lodWorld != null)
		{
			try
			{
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
					
					saveDir = new File(mc.getGameDirectory().getCanonicalFile().getPath() +
							File.separatorChar + "lod server data" + File.separatorChar + mc.getCurrentDimensionId());
				}
				
				fileHandler = new LodDimensionFileHandler(saveDir, this);
			}
			catch (IOException e)
			{
				// the file handler wasn't able to be created
				// we won't be able to read or write any files
			}
		}
		
		
		regions = new LodRegion[width][width];
		isRegionDirty = new boolean[width][width];
		regenRegionBuffer = new boolean[width][width];
		recreateRegionBuffer = new boolean[width][width];
		
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
		// the total width, just delete the current data
		// and update the centerX and/or centerZ
		if (Math.abs(xOffset) >= width || Math.abs(zOffset) >= width)
		{
			for (int x = 0; x < width; x++)
				for (int z = 0; z < width; z++)
					regions[x][z] = null;
			
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
		}
		else
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
		}
		else
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
	 * return the minimum needed memory in bytes
	 */
	public int getMinMemoryNeeded()
	{
		int count = 0;
		LodRegion region;
		
		for (int x = 0; x < regions.length; x++)
		{
			for (int z = 0; z < regions.length; z++)
			{
				region = regions[x][z];
				if (region != null)
					count += region.getMinMemoryNeeded(LodConfig.CLIENT.graphics.lodTemplate.get());
			}
		}
		return count;
	}
	
	
	/**
	 * Gets the region at the given LevelPos
	 * <br>
	 * Returns null if the region doesn't exist
	 * or is outside the loaded area.
	 */
	public LodRegion getRegion(byte detailLevel, int levelPosX, int levelPosZ)
	{
		int xRegion = LevelPosUtil.getRegion(detailLevel, levelPosX);
		int zRegion = LevelPosUtil.getRegion(detailLevel, levelPosZ);
		int xIndex = (xRegion - center.x) + halfWidth;
		int zIndex = (zRegion - center.z) + halfWidth;
		
		if (!regionIsInRange(xRegion, zRegion))
			return null;
			// throw new ArrayIndexOutOfBoundsException("Region for level pos " + LevelPosUtil.toString(detailLevel, posX, posZ) + " out of range");
		else if (regions[xIndex][zIndex] == null)
			return null;
		else if (regions[xIndex][zIndex].getMinDetailLevel() > detailLevel)
			return null;
		//throw new InvalidParameterException("Region for level pos " + LevelPosUtil.toString(detailLevel, posX, posZ) + " currently only reach level " + regions[xIndex][zIndex].getMinDetailLevel());
		
		return regions[xIndex][zIndex];
	}
	
	/**
	 * Gets the region at the given X and Z
	 * <br>
	 * Returns null if the region doesn't exist
	 * or is outside the loaded area.
	 */
	public LodRegion getRegion(int regionPosX, int regionPosZ)
	{
		int xIndex = (regionPosX - center.x) + halfWidth;
		int zIndex = (regionPosZ - center.z) + halfWidth;
		
		if (!regionIsInRange(regionPosX, regionPosZ))
			return null;
		//throw new ArrayIndexOutOfBoundsException("Region " + regionPosX + " " + regionPosZ + " out of range");
		
		return regions[xIndex][zIndex];
	}
	
	/** Useful when iterating over every region. */
	public LodRegion getRegionByArrayIndex(int xIndex, int zIndex)
	{
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
		int zIndex = (newRegion.regionPosZ - center.z) + halfWidth;
		
		if (!regionIsInRange(newRegion.regionPosX, newRegion.regionPosZ))
			// out of range
			throw new ArrayIndexOutOfBoundsException("Region " + newRegion.regionPosX + ", " + newRegion.regionPosZ + " out of range");
		
		regions[xIndex][zIndex] = newRegion;
	}
	
	
	/**
	 * Deletes nodes that are a higher detail then necessary, freeing
	 * up memory.
	 */
	public void cutRegionNodesAsync(int playerPosX, int playerPosZ)
	{
		ChunkPos newPlayerChunk = new ChunkPos(LevelPosUtil.getChunkPos((byte) 0, playerPosX), LevelPosUtil.getChunkPos((byte) 0, playerPosZ));
		
		if (lastCutChunk == null)
			lastCutChunk = new ChunkPos(newPlayerChunk.x + 1, newPlayerChunk.z - 1);
		
		// don't run the tree cutter multiple times
		// for the same location
		if (newPlayerChunk.x != lastCutChunk.x || newPlayerChunk.z != lastCutChunk.z)
		{
			lastCutChunk = newPlayerChunk;
			
			Thread thread = new Thread(() ->
			{
				int regionX;
				int regionZ;
				int minDistance;
				byte detail;
				byte minAllowedDetailLevel;
				
				// go over every region in the dimension
				for (int x = 0; x < regions.length; x++)
				{
					for (int z = 0; z < regions.length; z++)
					{
						regionX = (x + center.x) - halfWidth;
						regionZ = (z + center.z) - halfWidth;
						
						if (regions[x][z] != null)
						{
							// check what detail level this region should be
							// and cut it if it is higher then that
							minDistance = LevelPosUtil.minDistance(LodUtil.REGION_DETAIL_LEVEL, regionX, regionZ, playerPosX, playerPosZ);
							detail = DetailDistanceUtil.getTreeCutDetailFromDistance(minDistance);
							minAllowedDetailLevel = DetailDistanceUtil.getCutLodDetail(detail);
							
							if (regions[x][z].getMinDetailLevel() > minAllowedDetailLevel)
							{
								regions[x][z].cutTree(minAllowedDetailLevel);
								recreateRegionBuffer[x][z] = true;
							}
						}
					}// region z
				}// region z
			});
			
			cutAndExpandThread.execute(thread);
		}
	}
	
	/** Either expands or loads all regions in the rendered LOD area */
	public void expandOrLoadRegionsAsync(int playerPosX, int playerPosZ)
	{
		DistanceGenerationMode generationMode = LodConfig.CLIENT.worldGenerator.distanceGenerationMode.get();
		ChunkPos newPlayerChunk = new ChunkPos(LevelPosUtil.getChunkPos((byte) 0, playerPosX), LevelPosUtil.getChunkPos((byte) 0, playerPosZ));
		VerticalQuality verticalQuality = LodConfig.CLIENT.worldGenerator.lodQualityMode.get();
		
		
		if (lastExpandedChunk == null)
			lastExpandedChunk = new ChunkPos(newPlayerChunk.x + 1, newPlayerChunk.z - 1);
		
		// don't run the expander multiple times
		// for the same location
		if (newPlayerChunk.x != lastExpandedChunk.x || newPlayerChunk.z != lastExpandedChunk.z)
		{
			lastExpandedChunk = newPlayerChunk;
			
			Thread thread = new Thread(() ->
			{
				int regionX;
				int regionZ;
				LodRegion region;
				int minDistance;
				byte detail;
				byte levelToGen;
				
				for (int x = 0; x < regions.length; x++)
				{
					for (int z = 0; z < regions.length; z++)
					{
						regionX = (x + center.x) - halfWidth;
						regionZ = (z + center.z) - halfWidth;
						final RegionPos regionPos = new RegionPos(regionX, regionZ);
						region = regions[x][z];
						
						minDistance = LevelPosUtil.minDistance(LodUtil.REGION_DETAIL_LEVEL, regionX, regionZ, playerPosX, playerPosZ);
						detail = DetailDistanceUtil.getTreeGenDetailFromDistance(minDistance);
						levelToGen = DetailDistanceUtil.getLodGenDetail(detail).detailLevel;
						
						// check that the region isn't null and at least this detail level
						if (region == null || region.getGenerationMode() != generationMode)
						{
							// First case, region has to be created
							
							// try to get the region from file
							regions[x][z] = getRegionFromFile(regionPos, levelToGen, generationMode, verticalQuality);
							
							// if there is no region file create a empty region
							if (regions[x][z] == null)
								regions[x][z] = new LodRegion(levelToGen, regionPos, generationMode, verticalQuality);
							
							regenRegionBuffer[x][z] = true;
							regenDimensionBuffers = true;
							recreateRegionBuffer[x][z] = true;
						}
						else if (region.getMinDetailLevel() > levelToGen)
						{
							// Second case, the region exists at a higher detail level.
							
							// Expand the region by introducing the missing layer
							region.growTree(levelToGen);
							recreateRegionBuffer[x][z] = true;
						}
					}
				}
			});
			
			cutAndExpandThread.execute(thread);
		}
	}
	
	/**
	 * Add the given LOD to this dimension at the coordinate
	 * stored in the LOD. If an LOD already exists at the given
	 * coordinate it will be overwritten.
	 */
	public Boolean addData(byte detailLevel, int posX, int posZ, int verticalIndex, long data, boolean dontSave)
	{
		int regionPosX = LevelPosUtil.getRegion(detailLevel, posX);
		int regionPosZ = LevelPosUtil.getRegion(detailLevel, posZ);
		
		// don't continue if the region can't be saved
		LodRegion region = getRegion(regionPosX, regionPosZ);
		if (region == null)
			return false;
		
		boolean nodeAdded = region.addData(detailLevel, posX, posZ, verticalIndex, data);
		
		// only save valid LODs to disk
		if (!dontSave && fileHandler != null)
		{
			try
			{
				// mark the region as dirty so it will be saved to disk
				int xIndex = (regionPosX - center.x) + halfWidth;
				int zIndex = (regionPosZ - center.z) + halfWidth;
				
				isRegionDirty[xIndex][zIndex] = true;
				regenRegionBuffer[xIndex][zIndex] = true;
				regenDimensionBuffers = true;
			}
			catch (ArrayIndexOutOfBoundsException e)
			{
				e.printStackTrace();
				// If this happens, the method was probably 
				// called when the dimension was changing size.
				// Hopefully this shouldn't be an issue.
			}
		}
		
		return nodeAdded;
	}
	
	/** marks the region at the given region position to have its buffer rebuilt */
	public void markRegionBufferToRegen(int xRegion, int zRegion)
	{
		int xIndex = (xRegion - center.x) + halfWidth;
		int zIndex = (zRegion - center.z) + halfWidth;
		regenRegionBuffer[xIndex][zIndex] = true;
	}
	
	/**
	 * Returns every position that need to be generated based on the position of the player
	 */
	public PosToGenerateContainer getDataToGenerate(int maxDataToGenerate, int playerBlockPosX, int playerBlockPosZ)
	{
		PosToGenerateContainer posToGenerate;
		LodRegion region;
		// TODO what are dx, dz, and t?
		int x, z, dx, dz, t;
		x = 0;
		z = 0;
		dx = 0;
		dz = -1;
		
		// TODO please comment what this code is doing
		switch (LodConfig.CLIENT.worldGenerator.generationPriority.get())
		{
		default:
		case NEAR_FIRST:
			posToGenerate = new PosToGenerateContainer((byte) 10, maxDataToGenerate, playerBlockPosX, playerBlockPosZ);
			
			int playerChunkX = LevelPosUtil.getChunkPos(LodUtil.BLOCK_DETAIL_LEVEL, playerBlockPosX);
			int playerChunkZ = LevelPosUtil.getChunkPos(LodUtil.BLOCK_DETAIL_LEVEL, playerBlockPosZ);
			
			int xChunkToCheck;
			int zChunkToCheck;
			byte detailLevel;
			int posX;
			int posZ;
			long data;
			int numbChunksWide = (width) * 32;
			int circleLimit = Integer.MAX_VALUE;
			
			for (int i = 0; i < numbChunksWide * numbChunksWide; i++)
			{
				// use this for circular generation
				if (maxDataToGenerate < 0)
				{
					if (circleLimit < Math.abs(x) && circleLimit < Math.abs(z))
						break;
				}
				else if (maxDataToGenerate == 0)
				{
					maxDataToGenerate--;
					circleLimit = (int) (Math.abs(x) * 1.41f);
				}
				
				xChunkToCheck = x + playerChunkX;
				zChunkToCheck = z + playerChunkZ;
				
				region = getRegion(LodUtil.CHUNK_DETAIL_LEVEL, xChunkToCheck, zChunkToCheck);
				if (region == null)
					continue;
				
				detailLevel = region.getMinDetailLevel();
				
				posX = LevelPosUtil.convert(LodUtil.CHUNK_DETAIL_LEVEL, xChunkToCheck, detailLevel);
				posZ = LevelPosUtil.convert(LodUtil.CHUNK_DETAIL_LEVEL, zChunkToCheck, detailLevel);
				data = getSingleData(detailLevel, posX, posZ);
				
				if (DataPointUtil.getGenerationMode(data) < LodConfig.CLIENT.worldGenerator.distanceGenerationMode.get().complexity)
				{
					posToGenerate.addPosToGenerate(detailLevel, posX, posZ);
					if (maxDataToGenerate >= 0)
						maxDataToGenerate--;
				}
				
				if ((x == z) || ((x < 0) && (x == -z)) || ((x > 0) && (x == 1 - z)))
				{
					t = dx;
					dx = -dz;
					dz = t;
				}
				x += dx;
				z += dz;
			}
			break;
			
			
		case FAR_FIRST:
			posToGenerate = new PosToGenerateContainer((byte) 8, maxDataToGenerate, playerBlockPosX, playerBlockPosZ);
			
			int xRegion;
			int zRegion;
			
			for (int i = 0; i < width * width; i++)
			{
				xRegion = x + center.x;
				zRegion = z + center.z;
				
				region = getRegion(xRegion, zRegion);
				if (region != null)
					region.getDataToGenerate(posToGenerate, playerBlockPosX, playerBlockPosZ);
				
				
				if ((x == z) || ((x < 0) && (x == -z)) || ((x > 0) && (x == 1 - z)))
				{
					t = dx;
					dx = -dz;
					dz = t;
				}
				x += dx;
				z += dz;
			}
			break;
		}
		return posToGenerate;
	}
	
	/**
	 * Returns every node that should be rendered based on the position of the player.
	 * 
	 * TODO why isn't posToRender returned? it would make it a bit more clear what is happening
	 */
	public void getDataToRender(PosToRenderContainer posToRender, RegionPos regionPos, int playerPosX,
			int playerPosZ)
	{
		LodRegion region = getRegion(regionPos.x, regionPos.z);
		if (region != null)
			region.getDataToRender(posToRender, playerPosX, playerPosZ, LodConfig.CLIENT.worldGenerator.generationPriority.get() == GenerationPriority.NEAR_FIRST);
	}
	
	/**
	 * Determines how many vertical LODs could be used 
	 * for the given region at the given detail level
	 */
	public int getMaxVerticalData(byte detailLevel, int posX, int posZ)
	{
		if (detailLevel > LodUtil.REGION_DETAIL_LEVEL)
			throw new IllegalArgumentException("getMaxVerticalData given a level of [" + detailLevel + "] when [" + LodUtil.REGION_DETAIL_LEVEL + "] is the max.");
		
		LodRegion region = getRegion(detailLevel, posX, posZ);
		if (region == null)
			return 0;
		
		return region.getMaxVerticalData(detailLevel);
	}
	
	/**
	 * Get the data point at the given X and Z coordinates
	 * in this dimension.
	 * <br>
	 * Returns null if the LodChunk doesn't exist or
	 * is outside the loaded area.
	 */
	public long getData(byte detailLevel, int posX, int posZ, int verticalIndex)
	{
		if (detailLevel > LodUtil.REGION_DETAIL_LEVEL)
			throw new IllegalArgumentException("getLodFromCoordinates given a level of \"" + detailLevel + "\" when \"" + LodUtil.REGION_DETAIL_LEVEL + "\" is the max.");
		
		LodRegion region = getRegion(detailLevel, posX, posZ);
		if (region == null)
			return DataPointUtil.EMPTY_DATA;
		
		return region.getData(detailLevel, posX, posZ, verticalIndex);
	}
	
	
	/**
	 * Get the data point at the given X and Z coordinates
	 * in this dimension.
	 * <br>
	 * Returns null if the LodChunk doesn't exist or
	 * is outside the loaded area.
	 */
	public long getSingleData(byte detailLevel, int posX, int posZ)
	{
		if (detailLevel > LodUtil.REGION_DETAIL_LEVEL)
			throw new IllegalArgumentException("getLodFromCoordinates given a level of \"" + detailLevel + "\" when \"" + LodUtil.REGION_DETAIL_LEVEL + "\" is the max.");
		
		LodRegion region = getRegion(detailLevel, posX, posZ);
		if (region == null)
			return DataPointUtil.EMPTY_DATA;
		
		return region.getSingleData(detailLevel, posX, posZ);
	}
	
	/** Clears the given region */
	public void clear(byte detailLevel, int posX, int posZ)
	{
		if (detailLevel > LodUtil.REGION_DETAIL_LEVEL)
			throw new IllegalArgumentException("getLodFromCoordinates given a level of \"" + detailLevel + "\" when \"" + LodUtil.REGION_DETAIL_LEVEL + "\" is the max.");
		
		LodRegion region = getRegion(detailLevel, posX, posZ);
		if (region == null)
			return;
		
		region.clear(detailLevel, posX, posZ);
	}
	
	/**
	 * Returns if the buffer at the given array index needs
	 * to have its buffer regenerated.
	 */
	public boolean doesRegionNeedBufferRegen(int xIndex, int zIndex)
	{
		return regenRegionBuffer[xIndex][zIndex];
	}
	
	/**
	 * TODO we aren't currently using this, is there a reason for that?
	 * is this significantly different than regenRegionBuffer?
	 * 
	 * Returns if the buffer at the given array index needs
	 * to have its buffer resized.
	 */
	public boolean doesRegionNeedBufferResized(int xIndex, int zIndex)
	{
		return recreateRegionBuffer[xIndex][zIndex];
	}
	
	/**
	 * Sets if the buffer at the given array index needs
	 * to have its buffer regenerated.
	 */
	public void setRegenRegionBufferByArrayIndex(int xIndex, int zIndex, boolean newRegen)
	{
		regenRegionBuffer[xIndex][zIndex] = newRegen;
	}
	
	/**
	 * Get the data point at the given LevelPos
	 * in this dimension.
	 * <br>
	 * Returns null if the LodChunk doesn't exist or
	 * is outside the loaded area.
	 */
	public void updateData(byte detailLevel, int posX, int posZ)
	{
		if (detailLevel > LodUtil.REGION_DETAIL_LEVEL)
			throw new IllegalArgumentException("getLodFromCoordinates given a level of \"" + detailLevel + "\" when \"" + LodUtil.REGION_DETAIL_LEVEL + "\" is the max.");
		
		LodRegion region = getRegion(detailLevel, posX, posZ);
		if (region == null)
			return;
		
		region.updateArea(detailLevel, posX, posZ);
	}
	
	/**
	 * Returns true if a region exists at the given LevelPos
	 */
	public boolean doesDataExist(byte detailLevel, int posX, int posZ)
	{
		LodRegion region = getRegion(detailLevel, posX, posZ);
		if (region == null)
			return false;
		
		return region.doesDataExist(detailLevel, posX, posZ);
	}
	
	/**
	 * Loads the region at the given RegionPos from file,
	 * if a file exists for that region.
	 */
	public LodRegion getRegionFromFile(RegionPos regionPos, byte detailLevel, 
			DistanceGenerationMode generationMode, VerticalQuality verticalQuality)
	{
		if (fileHandler != null)
			return fileHandler.loadRegionFromFile(detailLevel, regionPos, generationMode, verticalQuality);
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
	 * Returns whether the region at the given RegionPos
	 * is within the loaded range.
	 */
	public boolean regionIsInRange(int regionX, int regionZ)
	{
		int xIndex = (regionX - center.x) + halfWidth;
		int zIndex = (regionZ - center.z) + halfWidth;
		
		return xIndex >= 0 && xIndex < width && zIndex >= 0 && zIndex < width;
	}
	
	/** Returns the dimension's center region position X value */
	public int getCenterRegionPosX()
	{
		return center.x;
	}
	
	/** Returns the dimension's center region position Z value */
	public int getCenterRegionPosZ()
	{
		return center.z;
	}
	
	/**
	 * returns the width of the dimension in regions
	 */
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
	
	/** Update the width of this dimension, in regions */
	public void setRegionWidth(int newWidth)
	{
		width = newWidth;
		halfWidth = Math.floorDiv(width, 2);
		
		regions = new LodRegion[width][width];
		isRegionDirty = new boolean[width][width];
		regenRegionBuffer = new boolean[width][width];
		recreateRegionBuffer = new boolean[width][width];
		
		// populate isRegionDirty
		for (int i = 0; i < width; i++)
			for (int j = 0; j < width; j++)
				isRegionDirty[i][j] = false;
	}
	
	
	@Override
	public String toString()
	{
		LodRegion region;
		
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Dimension : \n");
		for (int x = 0; x < regions.length; x++)
		{
			for (int z = 0; z < regions.length; z++)
			{
				region = regions[x][z];
				if (region == null)
				{
					stringBuilder.append("n");
					stringBuilder.append("\t");
					
				}
				else
				{
					stringBuilder.append(region.getMinDetailLevel());
					stringBuilder.append("\t");
				}
			}
			stringBuilder.append("\n");
		}
		return stringBuilder.toString();
	}
	
	/** Returns the minimum memory required by the dimension in Bytes */
	public int getMemoryRequired(int x, int z, LodTemplate template)
	{
		/*return regions[x][z].getMinMemoryNeeded(template);*/
		
		/*TODO add memory use calculated with the following cases
		switch (LodConfig.CLIENT.graphics.detailDropOff.get())
		{
			default:
			case BY_BLOCK:
				break;
			case BY_REGION_FANCY:
				break;
			case BY_REGION_FAST:
		}*/
		/*return regions[x][z].getMinMemoryNeeded(template);*/

		/*TODO add memory use calculated with the following cases
		switch (LodConfig.CLIENT.graphics.detailDropOff.get())
		{
			default:
			case BY_BLOCK:
				break;
			case BY_REGION_FANCY:
				break;
			case BY_REGION_FAST:
		}*/
		int minDistance = LevelPosUtil.minDistance(LodUtil.REGION_DETAIL_LEVEL, x, z, halfWidth, halfWidth);
		int detail = DetailDistanceUtil.getTreeCutDetailFromDistance(minDistance);
		int levelToGen = DetailDistanceUtil.getLodDrawDetail(detail);
		int size = 1 << (LodUtil.REGION_DETAIL_LEVEL - levelToGen);
		int maxVerticalData = DetailDistanceUtil.getMaxVerticalData(detail);
		int memoryUse = LodUtil.regionRenderingMemoryUse(x,z,template);
		System.out.println(detail + " " + memoryUse + " " + template.getBufferMemoryForSingleLod(maxVerticalData));
		return memoryUse;
		//return memoryUse;
	}
}
