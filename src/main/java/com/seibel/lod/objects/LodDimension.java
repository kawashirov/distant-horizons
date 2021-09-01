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
import java.security.InvalidParameterException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.mutable.MutableBoolean;

import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.handlers.LodConfig;
import com.seibel.lod.handlers.LodDimensionFileHandler;
import com.seibel.lod.objects.LevelPos.LevelPos;
import com.seibel.lod.util.DetailDistanceUtil;
import com.seibel.lod.util.LodThreadFactory;
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
	public volatile boolean regen[][];
	/** if true that means there are regions in this dimension
	 * that need to have their buffers rebuilt. */
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
		halfWidth = (int) Math.floor(width / 2);
		Minecraft mc = Minecraft.getInstance();
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
		regen = new boolean[width][width];

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
	 * return needed memory in byte
	 */
	public int getMinMemoryNeeded()
	{
		int regionX;
		int regionZ;
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
	public LodRegion getRegion(LevelPos levelPos)
	{

		RegionPos regionPos = levelPos.getRegionPos();
		int xIndex = (regionPos.x - center.x) + halfWidth;
		int zIndex = (regionPos.z - center.z) + halfWidth;

		if (!regionIsInRange(regionPos.x, regionPos.z))
			throw new ArrayIndexOutOfBoundsException("Region for level pos " + levelPos + " out of range");
		else if (regions[xIndex][zIndex] == null)
			throw new InvalidParameterException("Region for level pos " + levelPos + " not currently initialized");
		else if (regions[xIndex][zIndex].getMinDetailLevel() > levelPos.detailLevel)
			throw new InvalidParameterException("Region for level pos " + levelPos + " currently only reach level " + regions[xIndex][zIndex].getMinDetailLevel());
		return regions[xIndex][zIndex];
	}

	/**
	 * Gets the region at the given X and Z
	 * <br>
	 * Returns null if the region doesn't exist
	 * or is outside the loaded area.
	 */
	public LodRegion getRegion(RegionPos regionPos)
	{
		int xIndex = (regionPos.x - center.x) + halfWidth;
		int zIndex = (regionPos.z - center.z) + halfWidth;

		if (!regionIsInRange(regionPos.x, regionPos.z))
			throw new ArrayIndexOutOfBoundsException("Region " + regionPos + " out of range");
		else if (regions[xIndex][zIndex] == null)
			throw new InvalidParameterException("Region " + regionPos + " not currently initialized");
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
			throw new ArrayIndexOutOfBoundsException("Region " + newRegion.regionPosX + ", " + newRegion.regionPosZ + " out of range");

		regions[xIndex][zIndex] = newRegion;
	}


	/**
	 *
	 */
	public void treeCutter(int playerPosX, int playerPosZ)
	{
		ChunkPos newPlayerChunk = (new LevelPos((byte) 0, playerPosX, playerPosZ)).getChunkPos();
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
				LevelPos levelPos = new LevelPos();

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
							levelPos.changeParameters(LodUtil.REGION_DETAIL_LEVEL, regionX, regionZ);
							minDistance = levelPos.minDistance(playerPosX, playerPosZ);
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
		DistanceGenerationMode generationMode = LodConfig.CLIENT.distanceGenerationMode.get();
		ChunkPos newPlayerChunk = (new LevelPos((byte) 0, playerPosX, playerPosZ)).getChunkPos();

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
				LevelPos levelPos = new LevelPos();
				for (int x = 0; x < regions.length; x++)
				{
					for (int z = 0; z < regions.length; z++)
					{
						regionX = (x + center.x) - halfWidth;
						regionZ = (z + center.z) - halfWidth;
						levelPos.changeParameters(LodUtil.REGION_DETAIL_LEVEL, regionX, regionZ);
						final RegionPos regionPos = new RegionPos(regionX, regionZ);
						region = regions[x][z];
						//We require that the region we are checking is loaded with at least this level

						levelPos.changeParameters(LodUtil.REGION_DETAIL_LEVEL, regionX, regionZ);
						minDistance = levelPos.minDistance(playerPosX, playerPosZ);
						detail = DetailDistanceUtil.getDistanceTreeGenInverse(minDistance);
						levelToGen = DetailDistanceUtil.getLodGenDetail(detail).detailLevel;
						if (region == null || region.getGenerationMode() != generationMode)
						{
							//First case, region has to be initialized

							//We check if there is a file at the target level
							regions[x][z] = getRegionFromFile(regionPos, levelToGen, generationMode);

							//if there is no file we initialize the region
							if (regions[x][z] == null)
							{
								regions[x][z] = new LodRegion(levelToGen, regionPos, generationMode);
							}
							regen[x][z] = true;
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
	public synchronized Boolean addData(LevelPos levelPos, short[] lodDataPoint, boolean dontSave, boolean serverQuality)
	{

		// don't continue if the region can't be saved
		RegionPos regionPos = levelPos.getRegionPos();
		if (!regionIsInRange(regionPos.x, regionPos.z))
		{
			return false;
		}

		LodRegion region = getRegion(levelPos);

		boolean nodeAdded = region.addData(levelPos, lodDataPoint, serverQuality);
		// only save valid LODs to disk
		if (!dontSave && fileHandler != null)
		{
			try
			{
				// mark the region as dirty so it will be saved to disk
				int xIndex = (regionPos.x - center.x) + halfWidth;
				int zIndex = (regionPos.z - center.z) + halfWidth;
				isRegionDirty[xIndex][zIndex] = true;
				regen[xIndex][zIndex] = true;
				regenDimension = true;
			} catch (ArrayIndexOutOfBoundsException e)
			{
				// This method was probably called when the dimension was changing size.
				// Hopefully this shouldn't be an issue.
			}
		}
		return nodeAdded;
	}

	public void setToRegen(int xRegion, int zRegion){
		int xIndex = (xRegion - center.x) + halfWidth;
		int zIndex = (zRegion - center.z) + halfWidth;
		regen[xIndex][zIndex] = true;
	}

	/**
	 * method to get all the quadtree level that have to be generated based on the position of the player
	 *
	 * @return list of quadTrees
	 */
	public void getDataToGenerate(ConcurrentMap<LevelPos, MutableBoolean> dataToGenerate, int playerPosX, int playerPosZ)
	{

		int n = regions.length;
		int xIndex;
		int zIndex;
		LodRegion region;
		RegionPos regionPos;
		for (int xRegion = 0; xRegion < n; xRegion++)
		{
			for (int zRegion = 0; zRegion < n; zRegion++)
			{
				try
				{
					xIndex = (xRegion + center.x) - halfWidth;
					zIndex = (zRegion + center.z) - halfWidth;
					regionPos = new RegionPos(xIndex, zIndex);
					region = getRegion(regionPos);
					region.getDataToGenerate(dataToGenerate, playerPosX, playerPosZ);

				} catch (Exception e)
				{
					//e.printStackTrace();
				}
			}
		}
	}

	/**
	 * method to get all the nodes that have to be rendered based on the position of the player
	 *
	 * @return list of nodes
	 */
	public void getDataToRender(ConcurrentMap<LevelPos, MutableBoolean> dataToRender, RegionPos regionPos, int playerPosX, int playerPosZ)
	{
		try
		{
			LodRegion region = getRegion(regionPos);
			region.getDataToRender(dataToRender, playerPosX, playerPosZ);
		} catch (NullPointerException e)
		{
			System.out.println(regionPos);
			e.printStackTrace();
		} catch (Exception e)
		{
			//e.printStackTrace();
		}
	}

	/**
	 * Get the LodNodeData at the given X and Z coordinates
	 * in this dimension.
	 * <br>
	 * Returns null if the LodChunk doesn't exist or
	 * is outside the loaded area.
	 */
	public short[] getData(ChunkPos chunkPos)
	{
		LevelPos levelPos = new LevelPos(LodUtil.CHUNK_DETAIL_LEVEL, chunkPos.x, chunkPos.z);
		return getData(levelPos);
	}

	/**
	 * Get the data point at the given X and Z coordinates
	 * in this dimension.
	 * <br>
	 * Returns null if the LodChunk doesn't exist or
	 * is outside the loaded area.
	 */
	public short[] getData(LevelPos levelPos)
	{
		if (levelPos.detailLevel > LodUtil.REGION_DETAIL_LEVEL)
			throw new IllegalArgumentException("getLodFromCoordinates given a level of \"" + levelPos.detailLevel + "\" when \"" + LodUtil.REGION_DETAIL_LEVEL + "\" is the max.");

		try
		{
			LodRegion region = getRegion(levelPos);

			if (region == null)
			{
				return null;
			}

			return region.getData(levelPos);

		} catch (Exception e)
		{
			return null;
		}
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
	public boolean doesDataExist(LevelPos levelPos)
	{
		try
		{
			LodRegion region = getRegion(levelPos);

			if (region == null)
			{
				return false;
			}

			return region.doesDataExist(levelPos.clone());
		} catch (Exception e)
		{
			return false;
		}
	}

	/**
	 * Get the region at the given X and Z coordinates from the
	 * RegionFileHandler.
	 */
	public LodRegion getRegionFromFile(RegionPos regionPos, byte detailLevel, DistanceGenerationMode generationMode)
	{
		if (fileHandler != null)
			return fileHandler.loadRegionFromFile(detailLevel, regionPos, generationMode);
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
		regen = new boolean[width][width];

		// populate isRegionDirty
		for (int i = 0; i < width; i++)
			for (int j = 0; j < width; j++)
				isRegionDirty[i][j] = false;
	}


	@Override
	public String toString()
	{
		int regionX;
		int regionZ;
		LevelPos levelPos;
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
		System.out.println(stringBuilder);
		return stringBuilder.toString();
	}
}
