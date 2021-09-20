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
import com.seibel.lod.enums.LodQualityMode;
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
 * for a given dimension.
 *
 * @author Leonardo Amato
 * @author James Seibel
 * @version 9-18-2021
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
	
	// these three variables are private to force use of the getWidth() method
	// which is a safer way to get the width then directly asking the arrays
	/** stores all the regions in this dimension */
	private volatile LodRegion[][] regions;
	/** stores if the region at the given x and z index needs to be saved to disk */
	private volatile boolean[][] isRegionDirty;
	/** stores if the region at the given x and z index needs to be regenerated */
	private volatile boolean[][] regionNeedsRegen;
	
	/**
	 * if true that means there are regions in this dimension
	 * that need to have their buffers rebuilt.
	 */
	public volatile boolean regenDimension = false;
	
	private volatile RegionPos center;
	private volatile ChunkPos lastGenChunk;
	private volatile ChunkPos lastCutChunk;
	private LodDimensionFileHandler fileHandler;
	private ExecutorService cutAndGenThreads = Executors.newSingleThreadExecutor(new LodThreadFactory(this.getClass().getSimpleName() + " - cutAndGen"));
	
	/**
	 * Creates the dimension centered at (0,0)
	 *
	 * @param newWidth in regions
	 */
	public LodDimension(DimensionType newDimension, LodWorld lodWorld, int newWidth)
	{
		lastCutChunk = null;
		lastGenChunk = null;
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
				} else
				{
					// connected to server
					
					saveDir = new File(mc.getGameDirectory().getCanonicalFile().getPath() +
							File.separatorChar + "lod server data" + File.separatorChar + mc.getCurrentDimensionId());
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
		regionNeedsRegen = new boolean[width][width];
		
		//treeGenerator((int) mc.player.getX(),(int) mc.player.getZ());
		
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
					{
						regions[x][z] = null;
					}
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
	 * return needed memory in bytes
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
				{
					count += region.getMinMemoryNeeded();
				}
			}
		}
		return count;
	}
	
	
	/**
	 * Gets the region at the given X and Z
	 * <br>
	 * Returns null if the region doesn't exist
	 * or is outside the loaded area.
	 */
	public LodRegion getRegion(byte detailLevel, int posX, int posZ)
	{
		int xRegion = LevelPosUtil.getRegion(detailLevel, posX);
		int zRegion = LevelPosUtil.getRegion(detailLevel, posZ);
		int xIndex = (xRegion - center.x) + halfWidth;
		int zIndex = (zRegion - center.z) + halfWidth;
		
		if (!regionIsInRange(xRegion, zRegion))
			return null;
		//throw new ArrayIndexOutOfBoundsException("Region for level pos " + LevelPosUtil.toString(detailLevel, posX, posZ) + " out of range");
		else if (regions[xIndex][zIndex] == null)
			return null;
		//throw new InvalidParameterException("Region for level pos " + LevelPosUtil.toString(detailLevel, posX, posZ) + " not currently initialized");
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
		else if (regions[xIndex][zIndex] == null)
			return null;
		//throw new InvalidParameterException("Region " + regionPosX + " " + regionPosZ + " not currently initialized");
		return regions[xIndex][zIndex];
	}
	
	/** Useful when needing to iterate over every region. */
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
	 *
	 */
	public void treeCutter(int playerPosX, int playerPosZ)
	{
		ChunkPos newPlayerChunk = new ChunkPos(LevelPosUtil.getChunkPos((byte) 0, playerPosX), LevelPosUtil.getChunkPos((byte) 0, playerPosZ));
		if (lastCutChunk == null)
			lastCutChunk = new ChunkPos(newPlayerChunk.x + 1, newPlayerChunk.z - 1);
		if (newPlayerChunk.x != lastCutChunk.x || newPlayerChunk.z != lastCutChunk.z)
		{
			lastCutChunk = newPlayerChunk;
			Thread thread = new Thread(() ->
			{
				int regionX;
				int regionZ;
				int minDistance;
				byte detail;
				byte levelToCut;
				
				for (int x = 0; x < regions.length; x++)
				{
					for (int z = 0; z < regions.length; z++)
					{
						regionX = (x + center.x) - halfWidth;
						regionZ = (z + center.z) - halfWidth;
						//we start checking from the first circle. If the whole region is in the circle
						//we proceed to cut all the level lower than the level of circle 1 and we break
						//if this is not the case w
						if (regions[x][z] != null)
						{
							minDistance = LevelPosUtil.minDistance(LodUtil.REGION_DETAIL_LEVEL, regionX, regionZ, playerPosX, playerPosZ);
							detail = DetailDistanceUtil.getDistanceTreeCutInverse(minDistance);
							levelToCut = DetailDistanceUtil.getCutLodDetail(detail);
							if (regions[x][z].getMinDetailLevel() > levelToCut)
							{
								regions[x][z].cutTree(levelToCut);
							}
						}
						
					}// region z
				}// region z
				
			});
			cutAndGenThreads.execute(thread);
		}
	}
	
	/**
	 *
	 */
	public void treeGenerator(int playerPosX, int playerPosZ)
	{
		DistanceGenerationMode generationMode = LodConfig.CLIENT.worldGenerator.distanceGenerationMode.get();
		ChunkPos newPlayerChunk = new ChunkPos(LevelPosUtil.getChunkPos((byte) 0, playerPosX), LevelPosUtil.getChunkPos((byte) 0, playerPosZ));
		LodQualityMode lodQualityMode = LodConfig.CLIENT.worldGenerator.lodQualityMode.get();
		
		if (lastGenChunk == null)
			lastGenChunk = new ChunkPos(newPlayerChunk.x + 1, newPlayerChunk.z - 1);
		if (newPlayerChunk.x != lastGenChunk.x || newPlayerChunk.z != lastGenChunk.z)
		{
			lastGenChunk = newPlayerChunk;
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
						//We require that the region we are checking is loaded with at least this level
						
						minDistance = LevelPosUtil.minDistance(LodUtil.REGION_DETAIL_LEVEL, regionX, regionZ, playerPosX, playerPosZ);
						detail = DetailDistanceUtil.getDistanceTreeGenInverse(minDistance);
						levelToGen = DetailDistanceUtil.getLodGenDetail(detail).detailLevel;
						if (region == null || region.getGenerationMode() != generationMode)
						{
							//First case, region has to be initialized
							
							//We check if there is a file at the target level
							regions[x][z] = getRegionFromFile(regionPos, levelToGen, generationMode, lodQualityMode);
							
							//if there is no file we initialize the region
							if (regions[x][z] == null)
							{
								regions[x][z] = new LodRegion(levelToGen, regionPos, generationMode, lodQualityMode);
							}
							regionNeedsRegen[x][z] = true;
							regenDimension = true;
							
						} else if (region.getMinDetailLevel() > levelToGen)
						{
							//Second case, region has been initialized but at a higher level
							//We expand the region by introducing the missing layer
							region.expand(levelToGen);
						}
					}
				}
			});
			cutAndGenThreads.execute(thread);
		}
	}

	/**
	 * Add the given LOD to this dimension at the coordinate
	 * stored in the LOD. If an LOD already exists at the given
	 * coordinates it will be overwritten.
	 */
	public Boolean addData(byte detailLevel, int posX, int posZ, int verticalIndex, long data, boolean dontSave, boolean serverQuality)
	{

		// don't continue if the region can't be saved
		int regionPosX = LevelPosUtil.getRegion(detailLevel, posX);
		int regionPosZ = LevelPosUtil.getRegion(detailLevel, posZ);

		LodRegion region = getRegion(regionPosX, regionPosZ);
		if (region == null)
			return false;
		boolean nodeAdded = region.addData(detailLevel, posX, posZ, verticalIndex, data, serverQuality);
		// only save valid LODs to disk
		if (!dontSave && fileHandler != null)
		{
			try
			{
				// mark the region as dirty so it will be saved to disk
				int xIndex = (regionPosX - center.x) + halfWidth;
				int zIndex = (regionPosZ - center.z) + halfWidth;
				isRegionDirty[xIndex][zIndex] = true;
				regionNeedsRegen[xIndex][zIndex] = true;
				regenDimension = true;
			} catch (ArrayIndexOutOfBoundsException e)
			{
				e.printStackTrace();
				// This method was probably called when the dimension was changing size.
				// Hopefully this shouldn't be an issue.
			}
		}
		return nodeAdded;
	}
	
	
	/**
	 * Add the given LOD to this dimension at the coordinate
	 * stored in the LOD. If an LOD already exists at the given
	 * coordinates it will be overwritten.
	 */
	public Boolean addSingleData(byte detailLevel, int posX, int posZ, long dataPoint, boolean dontSave, boolean serverQuality)
	{
		
		// don't continue if the region can't be saved
		int regionPosX = LevelPosUtil.getRegion(detailLevel, posX);
		int regionPosZ = LevelPosUtil.getRegion(detailLevel, posZ);
		
		LodRegion region = getRegion(regionPosX, regionPosZ);
		if (region == null)
			return false;
		boolean nodeAdded = region.addSingleData(detailLevel, posX, posZ, dataPoint, serverQuality);
		// only save valid LODs to disk
		if (!dontSave && fileHandler != null)
		{
			try
			{
				// mark the region as dirty so it will be saved to disk
				int xIndex = (regionPosX - center.x) + halfWidth;
				int zIndex = (regionPosZ - center.z) + halfWidth;
				isRegionDirty[xIndex][zIndex] = true;
				regionNeedsRegen[xIndex][zIndex] = true;
				regenDimension = true;
			} catch (ArrayIndexOutOfBoundsException e)
			{
				e.printStackTrace();
				// This method was probably called when the dimension was changing size.
				// Hopefully this shouldn't be an issue.
			}
		}
		return nodeAdded;
	}
	
	public void setToRegen(int xRegion, int zRegion)
	{
		int xIndex = (xRegion - center.x) + halfWidth;
		int zIndex = (zRegion - center.z) + halfWidth;
		regionNeedsRegen[xIndex][zIndex] = true;
	}
	
	/**
	 * method to get all the quadtree level that have to be generated based on the position of the player
	 *
	 * @return list of quadTrees
	 */
	public PosToGenerateContainer getDataToGenerate(byte farDetail, int maxDataToGenerate, double farRatio, int playerPosX, int playerPosZ)
	{
		PosToGenerateContainer posToGenerate = new PosToGenerateContainer(farDetail, maxDataToGenerate, (int) (maxDataToGenerate * farRatio), playerPosX, playerPosZ);
		int n = regions.length;
		int xIndex;
		int zIndex;
		LodRegion region;
		for (int xRegion = 0; xRegion < n; xRegion++)
		{
			for (int zRegion = 0; zRegion < n; zRegion++)
			{
				xIndex = (xRegion + center.x) - halfWidth;
				zIndex = (zRegion + center.z) - halfWidth;
				region = getRegion(xIndex, zIndex);
				if (region != null)
					region.getDataToGenerate(posToGenerate, playerPosX, playerPosZ);
				
			}
		}
		return posToGenerate;
	}
	
	/**
	 * method to get all the nodes that have to be rendered based on the position of the player
	 *
	 * @return list of nodes
	 */
	public void getDataToRender(PosToRenderContainer posToRender, RegionPos regionPos, int playerPosX, int playerPosZ)
	{
		LodRegion region = getRegion(regionPos.x, regionPos.z);
		if (region != null)
			region.getDataToRender(posToRender, playerPosX, playerPosZ);
	}

	public int getMaxVerticalData(byte detailLevel, int posX, int posZ)
	{
		if (detailLevel > LodUtil.REGION_DETAIL_LEVEL)
			throw new IllegalArgumentException("getLodFromCoordinates given a level of \"" + detailLevel + "\" when \"" + LodUtil.REGION_DETAIL_LEVEL + "\" is the max.");

		LodRegion region = getRegion(detailLevel, posX, posZ);

		if (region == null)
		{
			return 0;
		}

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
		{
			return DataPointUtil.EMPTY_DATA;
		}
		
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
		{
			return DataPointUtil.EMPTY_DATA;
		}
		
		return region.getSingleData(detailLevel, posX, posZ);
	}

	public void clear(byte detailLevel, int posX, int posZ)
	{
		if (detailLevel > LodUtil.REGION_DETAIL_LEVEL)
			throw new IllegalArgumentException("getLodFromCoordinates given a level of \"" + detailLevel + "\" when \"" + LodUtil.REGION_DETAIL_LEVEL + "\" is the max.");

		LodRegion region = getRegion(detailLevel, posX, posZ);

		if (region == null)
		{
			return;
		}

		region.clear(detailLevel, posX, posZ);
	}
	
	public boolean getRegenByArrayIndex(int xIndex, int zIndex)
	{
		return regionNeedsRegen[xIndex][zIndex];
	}
	
	public void setRegenByArrayIndex(int xIndex, int zIndex, boolean newRegen)
	{
		regionNeedsRegen[xIndex][zIndex] = newRegen;
	}
	
	/**
	 * Get the data point at the given X and Z coordinates
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
		{
			return;
		}
		region.updateArea(detailLevel, posX, posZ);
	}
	
	/**
	 * return true if and only if the node at that position exist
	 */
	public boolean doesDataExist(byte detailLevel, int posX, int posZ)
	{
		LodRegion region = getRegion(detailLevel, posX, posZ);
		
		if (region == null)
		{
			return false;
		}
		
		return region.doesDataExist(detailLevel, posX, posZ);
	}
	
	/**
	 * Get the region at the given X and Z coordinates from the
	 * RegionFileHandler.
	 */
	public LodRegion getRegionFromFile(RegionPos regionPos, byte detailLevel, DistanceGenerationMode generationMode, LodQualityMode lodQualityMode)
	{
		if (fileHandler != null)
			return fileHandler.loadRegionFromFile(detailLevel, regionPos, generationMode, lodQualityMode);
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
	
	/** returns the width of the dimension in regions */
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
		halfWidth = Math.floorDiv(width, 2);
		
		regions = new LodRegion[width][width];
		isRegionDirty = new boolean[width][width];
		regionNeedsRegen = new boolean[width][width];
		
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
					
				} else
				{
					stringBuilder.append(region.getMinDetailLevel());
					stringBuilder.append("\t");
				}
			}
			stringBuilder.append("\n");
		}
		return stringBuilder.toString();
	}

}
