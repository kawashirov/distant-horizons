package com.seibel.lod.objects;


import com.seibel.lod.config.LodConfig;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.enums.VerticalQuality;
import com.seibel.lod.util.DataPointUtil;
import com.seibel.lod.util.DetailDistanceUtil;
import com.seibel.lod.util.LevelPosUtil;
import com.seibel.lod.util.LodUtil;
import com.seibel.lod.wrappers.MinecraftWrapper;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.storage.RegionFile;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import org.lwjgl.system.CallbackI;

import java.io.File;

/**
 * This object holds all loaded LevelContainers acting as a quad tree
 * for a given region. <Br><Br>
 *
 * <strong>Coordinate Standard: </strong><br>
 * Coordinate called posX or posZ are relative LevelPos coordinates <br>
 * unless stated otherwise. <br>
 * @author Leonardo Amato
 * @version 10-10-2021
 */
public class LodRegion
{
	/**
	 * Number of detail level supported by a region
	 */
	private static final byte POSSIBLE_LOD = 10;
	
	
	/**
	 * Holds the lowest (least detailed) detail level in this region
	 */
	private byte minDetailLevel;
	
	/**
	 * This holds all data for this region
	 */
	private final LevelContainer[] dataContainer;
	
	/**
	 * This chunk Pos has been generated
	 */
	private final boolean[] preGeneratedChunkPos;
	
	/**
	 * the generation mode for this region
	 */
	private DistanceGenerationMode generationMode;
	/**
	 * the vertical quality of this region
	 */
	private VerticalQuality verticalQuality;
	
	/**
	 * this region's x RegionPos
	 */
	public final int regionPosX;
	/**
	 * this region's z RegionPos
	 */
	public final int regionPosZ;
	
	public LodRegion(byte minDetailLevel, RegionPos regionPos, DistanceGenerationMode generationMode, VerticalQuality verticalQuality)
	{
		this.minDetailLevel = minDetailLevel;
		this.regionPosX = regionPos.x;
		this.regionPosZ = regionPos.z;
		this.verticalQuality = verticalQuality;
		this.generationMode = generationMode;
		dataContainer = new LevelContainer[POSSIBLE_LOD];
		
		
		// Initialize all the different matrices
		for (byte lod = minDetailLevel; lod <= LodUtil.REGION_DETAIL_LEVEL; lod++)
		{
			dataContainer[lod] = new VerticalLevelContainer(lod);
		}
		
		boolean fileFound = false;
		
		preGeneratedChunkPos = new boolean[32 * 32];
		/*
		if (MinecraftWrapper.INSTANCE.hasSinglePlayerServer() && LodConfig.CLIENT.worldGenerator.useExperimentalPreGenLoading.get())
		{
			File regionFileDirHead;
			File regionFileDirParent;
			// local world
			
			ServerWorld serverWorld = LodUtil.getServerWorldFromDimension(MinecraftWrapper.INSTANCE.getCurrentDimension());
			
			// provider needs a separate variable to prevent
			// the compiler from complaining
			StringBuilder string = new StringBuilder();
			try
			{
				ServerChunkProvider provider = serverWorld.getChunkSource();
				
				//System.out.println(provider.dataStorage.dataFolder);
				regionFileDirHead = new File(provider.dataStorage.dataFolder.getCanonicalFile().getParentFile().toPath().toAbsolutePath().toString() + File.separatorChar + "region", "r." + regionPosZ + "." + regionPosX + ".mca");
				if (regionFileDirHead.exists())
				{
					regionFileDirParent = regionFileDirHead.getParentFile();
					//string.append(regionFileDirParent.toString());
					string.append(regionFileDirHead);
					RegionFile regionFile = new RegionFile(regionFileDirHead, regionFileDirParent, true);
					for (int x = 0; x < 32; x++)
					{
						for (int z = 0; z < 32; z++)
						{
							preGeneratedChunkPos[x * 32 + z] = regionFile.doesChunkExist(new ChunkPos(regionPosX * 32 + x, regionPosZ * 32 + z));
						}
					}
					
					string.append("region " + regionPosX + " " + regionPosZ + "\n");
					for (int x = 0; x < 32; x++)
					{
						for (int z = 0; z < 32; z++)
						{
							//regionFile.doesChunkExist()
							string.append(preGeneratedChunkPos[x * 32 + z] + "\t");
						}
						string.append("\n");
					}
					regionFile.close();
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			System.out.println(string);
		}*/
		
	}
	
	
	/**
	 * Return true if the chunk has been pregenerated in game
	 */
	public boolean isChunkPreGenerated(int xChunkPos, int zChunkPos)
	{
		xChunkPos = LevelPosUtil.getRegionModule(LodUtil.CHUNK_DETAIL_LEVEL, xChunkPos);
		zChunkPos = LevelPosUtil.getRegionModule(LodUtil.CHUNK_DETAIL_LEVEL, zChunkPos);
		return preGeneratedChunkPos[xChunkPos * 32 + zChunkPos];
	}
	
	/**
	 * Inserts the data point into the region.
	 * <p>
	 * TODO this will always return true unless it has
	 * @return true if the data was added successfully
	 */
	public boolean addData(byte detailLevel, int posX, int posZ, int verticalIndex, long data)
	{
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		
		// The dataContainer could have null entries if the
		// detailLevel changes.
		if (this.dataContainer[detailLevel] == null)
		{
			this.dataContainer[detailLevel] = new VerticalLevelContainer(detailLevel);
		}
		
		this.dataContainer[detailLevel].addData(data, posX, posZ, verticalIndex);
		
		return true;
	}
	
	/**
	 * Inserts the vertical data into the region.
	 * <p>
	 * TODO this will always return true unless it has
	 * @return true if the data was added successfully
	 */
	public boolean addVerticalData(byte detailLevel, int posX, int posZ, long[] data)
	{
		//position is already relative
		//posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		//posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		
		// The dataContainer could have null entries if the
		// detailLevel changes.
		if (this.dataContainer[detailLevel] == null)
			this.dataContainer[detailLevel] = new VerticalLevelContainer(detailLevel);
		
		return this.dataContainer[detailLevel].addVerticalData(data, posX, posZ);
	}
	
	/**
	 * Get the dataPoint at the given relative position.
	 * @return the data at the relative pos and detail level,
	 * 0 if the data doesn't exist.
	 */
	public long getData(byte detailLevel, int posX, int posZ, int verticalIndex)
	{
		return dataContainer[detailLevel].getData(posX, posZ, verticalIndex);
	}
	
	/**
	 * Get the dataPoint at the given relative position.
	 * @return the data at the relative pos and detail level,
	 * 0 if the data doesn't exist.
	 */
	public long getSingleData(byte detailLevel, int posX, int posZ)
	{
		return dataContainer[detailLevel].getSingleData(posX, posZ);
	}
	
	/**
	 * Clears the datapoint at the given relative position
	 */
	public void clear(byte detailLevel, int posX, int posZ)
	{
		dataContainer[detailLevel].clear(posX, posZ);
	}
	
	/**
	 * This method will fill the posToGenerate array with all levelPos that
	 * are render-able.
	 * <p>
	 * TODO why don't we return the posToGenerate, it would make this easier to understand
	 */
	public void getPosToGenerate(PosToGenerateContainer posToGenerate,
			int playerBlockPosX, int playerBlockPosZ)
	{
		getPosToGenerate(posToGenerate, LodUtil.REGION_DETAIL_LEVEL, 0, 0, playerBlockPosX, playerBlockPosZ);
		
	}
	
	/**
	 * A recursive method that fills the posToGenerate array with all levelPos that
	 * need to be generated.
	 * <p>
	 * TODO why don't we return the posToGenerate, it would make this easier to understand
	 */
	private void getPosToGenerate(PosToGenerateContainer posToGenerate, byte detailLevel,
			int childOffsetPosX, int childOffsetPosZ, int playerPosX, int playerPosZ)
	{
		// equivalent to 2^(...)
		int size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		
		// calculate what LevelPos are in range to generate
		int maxDistance = LevelPosUtil.maxDistance(detailLevel, childOffsetPosX, childOffsetPosZ, playerPosX, playerPosZ, regionPosX, regionPosZ);
		
		// determine this child's levelPos
		byte childDetailLevel = (byte) (detailLevel - 1);
		int childPosX = childOffsetPosX * 2;
		int childPosZ = childOffsetPosZ * 2;
		
		int childSize = 1 << (LodUtil.REGION_DETAIL_LEVEL - childDetailLevel);
		
		byte targetDetailLevel = DetailDistanceUtil.getLodGenDetail(DetailDistanceUtil.getGenerationDetailFromDistance(maxDistance)).detailLevel;
		if (targetDetailLevel <= detailLevel)
		{
			if (targetDetailLevel == detailLevel)
			{
				if (!doesDataExist(detailLevel, childOffsetPosX, childOffsetPosZ))
					posToGenerate.addPosToGenerate(detailLevel, childOffsetPosX + regionPosX * size, childOffsetPosZ + regionPosZ * size);
			}
			else
			{
				// we want at max one request per chunk (since the world generator creates chunks).
				// So for lod smaller than a chunk, only recurse down
				// the top right child
				
				if (detailLevel > LodUtil.CHUNK_DETAIL_LEVEL)
				{
					int ungeneratedChildren = 0;
					
					// make sure all children are generated to this detailLevel
					for (int x = 0; x <= 1; x++)
					{
						for (int z = 0; z <= 1; z++)
						{
							if (!doesDataExist(childDetailLevel, childPosX + x, childPosZ + z))
							{
								ungeneratedChildren++;
								posToGenerate.addPosToGenerate(childDetailLevel, childPosX + x + regionPosX * childSize, childPosZ + z + regionPosZ * childSize);
							}
						}
					}
					
					// only if all the children are correctly generated
					// should we go deeper
					if (ungeneratedChildren == 0)
						for (int x = 0; x <= 1; x++)
							for (int z = 0; z <= 1; z++)
								getPosToGenerate(posToGenerate, childDetailLevel, childPosX + x, childPosZ + z, playerPosX, playerPosZ);
				}
				else
				{
					// The detail Level is smaller than a chunk.
					// Only recurse down the top right child.
					
					if (DetailDistanceUtil.getLodGenDetail(childDetailLevel).detailLevel <= (childDetailLevel))
					{
						if (!doesDataExist(childDetailLevel, childPosX, childPosZ))
							posToGenerate.addPosToGenerate(childDetailLevel, childPosX + regionPosX * childSize, childPosZ + regionPosZ * childSize);
						else
							getPosToGenerate(posToGenerate, childDetailLevel, childPosX, childPosZ, playerPosX, playerPosZ);
					}
				}
			}
		}
		// we have gone beyond the target Detail level
		// we can stop generating
		
	}
	
	
	/**
	 * This method will fill the posToRender array with all levelPos that
	 * are render-able.
	 * <p>
	 * TODO why don't we return the posToRender, it would make this easier to understand
	 */
	public void getPosToRender(PosToRenderContainer posToRender,
			int playerPosX, int playerPosZ, boolean requireCorrectDetailLevel)
	{
		getPosToRender(posToRender, LodUtil.REGION_DETAIL_LEVEL, 0, 0, playerPosX, playerPosZ, requireCorrectDetailLevel);
	}
	
	/**
	 * This method will fill the posToRender array with all levelPos that
	 * are render-able.
	 * <p>
	 * TODO why don't we return the posToRender, it would make this easier to understand
	 * TODO this needs some more comments, James was only able to figure out part of it
	 */
	private void getPosToRender(PosToRenderContainer posToRender,
			byte detailLevel, int posX, int posZ,
			int playerPosX, int playerPosZ, boolean requireCorrectDetailLevel)
	{
		// equivalent to 2^(...)
		int size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		
		byte desiredLevel;
		int maxDistance;
		boolean stopNow;
		int minDistance;
		int childLevel;
		
		
		// calculate the LevelPos that are in range
		switch (LodConfig.CLIENT.graphics.detailDropOff.get())
		{
		
		case FAST:
			int playerRegionX = LevelPosUtil.getRegion(LodUtil.BLOCK_DETAIL_LEVEL, playerPosX);
			int playerRegionZ = LevelPosUtil.getRegion(LodUtil.BLOCK_DETAIL_LEVEL, playerPosZ);
			if (playerRegionX == regionPosX && playerRegionZ == regionPosZ)
			{
				maxDistance = LevelPosUtil.maxDistance(detailLevel, posX, posZ, playerPosX, playerPosZ, regionPosX, regionPosZ);
				desiredLevel = DetailDistanceUtil.getLodDrawDetail(DetailDistanceUtil.getDrawDetailFromDistance(maxDistance));
				minDistance = LevelPosUtil.minDistance(detailLevel, posX, posZ, playerPosX, playerPosZ, regionPosX, regionPosZ);
				childLevel = DetailDistanceUtil.getLodDrawDetail(DetailDistanceUtil.getDrawDetailFromDistance(minDistance));
				stopNow = detailLevel == childLevel - 1;
				break;
			}
		default:
		case FANCY:
			maxDistance = LevelPosUtil.maxDistance(detailLevel, posX, posZ, playerPosX, playerPosZ, regionPosX, regionPosZ);
			desiredLevel = DetailDistanceUtil.getLodDrawDetail(DetailDistanceUtil.getDrawDetailFromDistance(maxDistance));
			minDistance = LevelPosUtil.minDistance(detailLevel, posX, posZ, playerPosX, playerPosZ, regionPosX, regionPosZ);
			childLevel = DetailDistanceUtil.getLodDrawDetail(DetailDistanceUtil.getDrawDetailFromDistance(minDistance));
			stopNow = detailLevel == childLevel - 1;
			break;
		}
		
		if (stopNow)
		{
			posToRender.addPosToRender(detailLevel,
					posX + regionPosX * size,
					posZ + regionPosZ * size);
		}
		else
			//if (desiredLevel > detailLevel)
			//{
			// we have gone beyond the target Detail level
			// we can stop generating
			//} else
			if (desiredLevel == detailLevel)
			{
				posToRender.addPosToRender(detailLevel,
						posX + regionPosX * size,
						posZ + regionPosZ * size);
			}
			else //case where (detailLevel > desiredLevel)
			{
				int childPosX = posX * 2;
				int childPosZ = posZ * 2;
				byte childDetailLevel = (byte) (detailLevel - 1);
				int childrenCount = 0;
				
				for (int x = 0; x <= 1; x++)
				{
					for (int z = 0; z <= 1; z++)
					{
						if (doesDataExist(childDetailLevel, childPosX + x, childPosZ + z))
						{
							if (!requireCorrectDetailLevel)
								childrenCount++;
							else
								getPosToRender(posToRender, childDetailLevel, childPosX + x, childPosZ + z, playerPosX, playerPosZ, requireCorrectDetailLevel);
						}
					}
				}
				
				
				if (!requireCorrectDetailLevel)
				{
					// If all the four children exist go deeper
					if (childrenCount == 4)
					{
						for (int x = 0; x <= 1; x++)
							for (int z = 0; z <= 1; z++)
								getPosToRender(posToRender, childDetailLevel, childPosX + x, childPosZ + z, playerPosX, playerPosZ, requireCorrectDetailLevel);
					}
					else
					{
						posToRender.addPosToRender(detailLevel,
								posX + regionPosX * size,
								posZ + regionPosZ * size);
					}
				}
			}
	}
	
	
	/**
	 * Updates all children.
	 * <p>
	 * TODO could this be renamed mergeArea?
	 */
	public void updateArea(byte detailLevel, int posX, int posZ)
	{
		int width;
		int startX;
		int startZ;
		
		// TODO what are each of these loops updating?
		for (byte down = (byte) (minDetailLevel + 1); down <= detailLevel; down++)
		{
			startX = LevelPosUtil.convert(detailLevel, posX, down);
			startZ = LevelPosUtil.convert(detailLevel, posZ, down);
			width = 1 << (detailLevel - down);
			
			for (int x = 0; x < width; x++)
				for (int z = 0; z < width; z++)
					update(down, startX + x, startZ + z);
		}
		
		
		for (byte up = (byte) (detailLevel + 1); up <= LodUtil.REGION_DETAIL_LEVEL; up++)
		{
			update(up,
					LevelPosUtil.convert(detailLevel, posX, up),
					LevelPosUtil.convert(detailLevel, posZ, up));
		}
	}
	
	/**
	 * Update the child at the given relative Pos
	 * <p>
	 * TODO could this be renamed mergeChildData?
	 */
	private void update(byte detailLevel, int posX, int posZ)
	{
		dataContainer[detailLevel].updateData(dataContainer[detailLevel - 1], posX, posZ);
	}
	
	
	/**
	 * Returns if data exists at the given relative Pos.
	 */
	public boolean doesDataExist(byte detailLevel, int posX, int posZ)
	{
		if (detailLevel < minDetailLevel)
			return false;
		
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		
		if (dataContainer == null || dataContainer[detailLevel] == null)
			return false;
		
		return dataContainer[detailLevel].doesItExist(posX, posZ);
	}
	
	/**
	 * Gets the generation mode for the data point at the given relative pos.
	 */
	public byte getGenerationMode(byte detailLevel, int posX, int posZ)
	{
		if (dataContainer[detailLevel].doesItExist(posX, posZ))
			// We take the bottom information always
			// TODO what does that mean? bottom of what?
			return DataPointUtil.getGenerationMode(dataContainer[detailLevel].getSingleData(posX, posZ));
		else
			return DistanceGenerationMode.NONE.complexity;
	}
	
	/**
	 * Returns the lowest (least detailed) detail level in this region
	 * TODO is that right?
	 */
	public byte getMinDetailLevel()
	{
		return minDetailLevel;
	}
	
	/**
	 * Returns the LevelContainer for the detailLevel
	 * @throws IllegalArgumentException if the detailLevel is less than minDetailLevel
	 */
	public LevelContainer getLevel(byte detailLevel)
	{
		if (detailLevel < minDetailLevel)
			throw new IllegalArgumentException("getLevel asked for a detail level that does not exist: minimum: [" + minDetailLevel + "] level requested: [" + detailLevel + "]");
		
		return dataContainer[detailLevel];
	}
	
	/**
	 * Add the levelContainer to this Region, updating the minDetailLevel
	 * if necessary.
	 * @throws IllegalArgumentException if the LevelContainer's detailLevel
	 * is 2 or more detail levels lower than the
	 * minDetailLevel of this region.
	 */
	public void addLevelContainer(LevelContainer levelContainer)
	{
		if (levelContainer.getDetailLevel() < minDetailLevel - 1)
		{
			throw new IllegalArgumentException(
					"the LevelContainer's detailLevel was "
							+ "[" + levelContainer.getDetailLevel() + "] but this region "
							+ "only allows adding LevelContainers with a "
							+ "detail level of [" + (minDetailLevel - 1) + "]");
		}
		
		if (levelContainer.getDetailLevel() == minDetailLevel - 1)
			minDetailLevel = levelContainer.getDetailLevel();
		
		dataContainer[levelContainer.getDetailLevel()] = levelContainer;
	}
	
	// TODO James thinks cutTree and growTree (which he renamed to match cutTree)
	// should have more descriptive names, to make sure the "Tree" portion isn't
	// confused with Minecraft trees (the plant).
	
	/**
	 * Removes any dataContainers that are higher than
	 * the given detailLevel
	 */
	public void cutTree(byte detailLevel)
	{
		if (detailLevel > minDetailLevel)
		{
			for (byte detailLevelIndex = 0; detailLevelIndex < detailLevel; detailLevelIndex++)
				dataContainer[detailLevelIndex] = null;
			
			minDetailLevel = detailLevel;
		}
	}
	
	/**
	 * Make this region more detailed to the detailLevel given.
	 * TODO is that correct?
	 */
	public void growTree(byte detailLevel)
	{
		if (detailLevel < minDetailLevel)
		{
			for (byte detailLevelIndex = (byte) (minDetailLevel - 1); detailLevelIndex >= detailLevel; detailLevelIndex--)
			{
				if (dataContainer[detailLevelIndex + 1] == null)
					dataContainer[detailLevelIndex + 1] = new VerticalLevelContainer((byte) (detailLevelIndex + 1));
				
				dataContainer[detailLevelIndex] = dataContainer[detailLevelIndex + 1].expand();
			}
			minDetailLevel = detailLevel;
		}
	}
	
	/**
	 * return RegionPos of this lod region
	 */
	public RegionPos getRegionPos()
	{
		return new RegionPos(regionPosX, regionPosZ);
	}
	
	/**
	 * Returns how many LODs are in this region
	 */
	public int getNumberOfLods()
	{
		int count = 0;
		for (LevelContainer container : dataContainer)
			count += container.getMaxNumberOfLods();
		
		return count;
	}
	
	public VerticalQuality getVerticalQuality()
	{
		return verticalQuality;
	}
	
	public DistanceGenerationMode getGenerationMode()
	{
		return generationMode;
	}
	
	public int getMaxVerticalData(byte detailLevel)
	{
		return dataContainer[detailLevel].getMaxVerticalData();
	}
	
	
	@Override
	public String toString()
	{
		return getLevel(LodUtil.REGION_DETAIL_LEVEL).toString();
	}
}
