package com.seibel.lod.objects;


import com.seibel.lod.builders.LodBuilder;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.util.DataPointUtil;
import com.seibel.lod.util.DetailDistanceUtil;
import com.seibel.lod.util.LevelPosUtil;
import com.seibel.lod.util.LodUtil;

/**
 * STANDARD TO FOLLOW
 * every coordinate called posX or posZ is a relative coordinate and not and absolute coordinate
 * if an array contain coordinate the order is the following
 * 0 for x, 1 for z in 2D
 * 0 for x, 1 for y, 2 for z in 3D
 */

public class LodRegion
{
	//x coord,
	private byte minDetailLevel;
	private static final byte POSSIBLE_LOD = 10;
	//private int numberOfPoints;
	//For each of the following field the first slot is for the level of detail
	//Important: byte have a [-128, 127] range. When converting from or to int a 128 should be added or removed
	//If there is a bug with color then it's probably caused by this.
	//in the future other fields like transparency and light level could be added

	private LevelContainer[] dataContainer;


	private DistanceGenerationMode generationMode;

	public final int regionPosX;
	public final int regionPosZ;

	public LodRegion(RegionPos regionPos)
	{
		this.minDetailLevel = LodUtil.REGION_DETAIL_LEVEL;
		this.regionPosX = regionPos.x;
		this.regionPosZ = regionPos.z;
		dataContainer = new LevelContainer[POSSIBLE_LOD];
	}

	public LodRegion(byte minDetailLevel, RegionPos regionPos, DistanceGenerationMode generationMode)
	{
		this.minDetailLevel = minDetailLevel;
		this.regionPosX = regionPos.x;
		this.regionPosZ = regionPos.z;
		this.generationMode = generationMode;
		dataContainer = new LevelContainer[POSSIBLE_LOD];


		//Initialize all the different matrices
		for (byte lod = minDetailLevel; lod <= LodUtil.REGION_DETAIL_LEVEL; lod++)
		{
			dataContainer[lod] = new SingleLevelContainer(lod);
			/*if(twoDimension){
				dataContainer[lod] = new SingleLevelContainer(lod);
			}else{
				dataContainer[lod] = new VerticalLevelContainer.java(lod);
			}*/
		}
	}

	public DistanceGenerationMode getGenerationMode()
	{
		return generationMode;
	}

	/**
	 * This method can be used to insert data into the LodRegion
	 *
	 * @param dataPoint
	 * @return
	 */
	public boolean addData(byte detailLevel, int posX, int posZ, long[] dataPoint, boolean serverQuality)
	{
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		if (!doesDataExist(detailLevel, posX, posZ) || serverQuality)
		{

			//update the number of node present
			//if (!doesDataExist(detailLevel, posX, posZ)) numberOfPoints++;

			//add the node data
			this.dataContainer[detailLevel].addData(dataPoint, posX, posZ);
			return true;
		} else
		{
			return false;
		}
	}

	/**
	 * This method will return the data in the position relative to the level of detail
	 *
	 * @return the data at the relative pos and level
	 */
	public long[] getData(byte detailLevel, int posX, int posZ)
	{
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		return dataContainer[detailLevel].getData(posX, posZ);
	}

	/**
	 * This method will return all the levelPos that are renderable according to the requisite given in input
	 *
	 * @return
	 */
	public void getDataToGenerate(PosToGenerateContainer posToGenerate, int playerPosX, int playerPosZ)
	{
		getDataToGenerate(posToGenerate, LodUtil.REGION_DETAIL_LEVEL, 0, 0, playerPosX, playerPosZ);

	}

	private void getDataToGenerate(PosToGenerateContainer posToGenerate, byte detailLevel, int posX, int posZ, int playerPosX, int playerPosZ)
	{
		int size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);

		//here i calculate the the LevelPos is in range
		//This is important to avoid any kind of hole in the generation
		//nt minDistance = LevelPosUtil.minDistance(detailLevel, posX + regionPosX * size, posZ + regionPosZ * size, playerPosX, playerPosZ);
		int maxDistance = LevelPosUtil.maxDistance(detailLevel, posX, posZ, playerPosX, playerPosZ, regionPosX, regionPosZ);

		byte childDetailLevel = (byte) (detailLevel - 1);
		int childPosX = posX * 2;
		int childPosZ = posZ * 2;

		int childSize = 1 << (LodUtil.REGION_DETAIL_LEVEL - childDetailLevel);
		//we have reached the target detail level
		byte targetDetailLevel = DetailDistanceUtil.getLodGenDetail(DetailDistanceUtil.getDistanceGenerationInverse(maxDistance)).detailLevel;
		if (targetDetailLevel > detailLevel)
		{
			return;
		} else if (targetDetailLevel == detailLevel)
		{
			if (!doesDataExist(detailLevel, posX, posZ))
			{
				posToGenerate.addPosToGenerate(detailLevel, posX + regionPosX * size, posZ + regionPosZ * size);
			}
		} else
		{
			//we want max a request per chunk. So for lod smaller than chunk we explore only the top rigth child
			if (detailLevel > LodUtil.CHUNK_DETAIL_LEVEL)
			{
				int num = 0;
				//We take all the children that are not generated to at least the generation level taken in input
				for (int x = 0; x <= 1; x++)
				{
					for (int z = 0; z <= 1; z++)
					{

						if (!doesDataExist(childDetailLevel, childPosX + x, childPosZ + z))
						{
							num++;
							posToGenerate.addPosToGenerate(childDetailLevel, childPosX + x + regionPosX * childSize, childPosZ + z + regionPosZ * childSize);
						}
					}
				}

				//only if all the children are correctly generated we go deeper
				if (num == 0)
				{
					for (int x = 0; x <= 1; x++)
					{
						for (int z = 0; z <= 1; z++)
						{
							getDataToGenerate(posToGenerate, childDetailLevel, childPosX + x, childPosZ + z, playerPosX, playerPosZ);
						}
					}
				}
			} else
			//now we keep exploring the top right child
			{
				if (DetailDistanceUtil.getLodGenDetail(childDetailLevel).detailLevel <= (childDetailLevel))
				{
					if (!doesDataExist(childDetailLevel, childPosX, childPosZ))
					{
						posToGenerate.addPosToGenerate(childDetailLevel, childPosX + regionPosX * childSize, childPosZ + regionPosZ * childSize);
					} else
					{
						getDataToGenerate(posToGenerate, childDetailLevel, childPosX, childPosZ, playerPosX, playerPosZ);
					}
				}
			}
		}
	}


	/**
	 * @return
	 */
	public void getDataToRender(PosToRenderContainer posToRender, int playerPosX, int playerPosZ)
	{
		getDataToRender(posToRender, LodUtil.REGION_DETAIL_LEVEL, 0, 0, playerPosX, playerPosZ);
	}

	/**
	 * @return
	 */
	private void getDataToRender(PosToRenderContainer posToRender, byte detailLevel, int posX, int posZ, int playerPosX, int playerPosZ)
	{
		int size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);

		//here i calculate the the LevelPos is in range
		//This is important to avoid any kind of hole in the rendering
		int maxDistance = LevelPosUtil.maxDistance(detailLevel, posX, posZ, playerPosX, playerPosZ, regionPosX, regionPosZ);

		byte supposedLevel = DetailDistanceUtil.getLodDrawDetail(DetailDistanceUtil.getDistanceRenderingInverse(maxDistance));
		if (supposedLevel > detailLevel)
			return;
		else if (supposedLevel == detailLevel)
		{
			posToRender.addPosToRender(detailLevel,
					posX + regionPosX * size,
					posZ + regionPosZ * size);
		} else //case where (detailLevel > supposedLevel)
		{
			int childPosX = posX * 2;
			int childPosZ = posZ * 2;
			byte childDetailLevel = (byte) (detailLevel - 1);
			int childrenCount = 0;
			for (int x = 0; x <= 1; x++)
			{
				for (int z = 0; z <= 1; z++)
				{
					if (doesDataExist(childDetailLevel, childPosX + x, childPosZ + z)) childrenCount++;
				}
			}

			//If all the four children exist we go deeper
			if (childrenCount == 4)
			{
				for (int x = 0; x <= 1; x++)
				{
					for (int z = 0; z <= 1; z++)
					{
						getDataToRender(posToRender, childDetailLevel, childPosX + x, childPosZ + z, playerPosX, playerPosZ);
					}
				}
			} else
			{
				posToRender.addPosToRender(detailLevel,
						posX + regionPosX * size,
						posZ + regionPosZ * size);
			}
		}
	}

	/**
	 */
	public void updateArea(byte detailLevel, int posX, int posZ)
	{
		int width;
		int startX;
		int startZ;
		for (byte bottom = (byte) (minDetailLevel + 1); bottom <= detailLevel; bottom++)
		{
			startX = LevelPosUtil.convert(detailLevel, posX, bottom);
			startZ = LevelPosUtil.convert(detailLevel, posZ, bottom);
			width = 1 << (detailLevel - bottom);
			for (int x = 0; x < width; x++)
			{
				for (int z = 0; z < width; z++)
				{
					update(bottom, startX + x, startZ + z);
				}
			}
		}
		for (byte up = (byte) (detailLevel + 1); up <= LodUtil.REGION_DETAIL_LEVEL; up++)
		{
			update(up,
					LevelPosUtil.convert(detailLevel, posX, up),
					LevelPosUtil.convert(detailLevel, posZ, up));
		}
	}

	/**
	 */
	private void update(byte detailLevel, int posX, int posZ)
	{
		dataContainer[detailLevel].updateData(dataContainer[detailLevel - 1], posX, posZ);
	}


	/**
	 * @return
	 */
	public boolean doesDataExist(byte detailLevel, int posX, int posZ)
	{
		if(detailLevel < minDetailLevel) return false;
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		if(dataContainer == null || dataContainer[detailLevel] == null)
			return false;
		return dataContainer[detailLevel].doesItExist(posX, posZ);
	}

	/**
	 * @return
	 */
	public byte getGenerationMode(byte detailLevel, int posX, int posZ)
	{
		if(dataContainer[detailLevel].doesItExist(posX,posZ))
		{
			//We take the bottom information always
			return DataPointUtil.getGenerationMode(dataContainer[detailLevel].getData(posX,posZ)[0]);
		}else
		{
			return DistanceGenerationMode.NONE.complexity;
		}
	}

	public byte getMinDetailLevel()
	{
		return minDetailLevel;
	}

	/**
	 * This will be used to save a level
	 *
	 * @param detailLevel
	 * @return
	 */
	public LevelContainer getLevel(byte detailLevel)
	{
		if (detailLevel < minDetailLevel)
		{
			throw new IllegalArgumentException("getLevel asked for a level that does not exist: minimum " + minDetailLevel + " level requested " + detailLevel);
		}
		return dataContainer[detailLevel];
	}

	/**
	 * @param levelContainer
	 */
	public void addLevel(LevelContainer levelContainer)
	{
		if (levelContainer.getDetailLevel() < minDetailLevel - 1)
		{
			throw new IllegalArgumentException("addLevel requires a level that is at least the minimum level of the region -1 ");
		}
		if (levelContainer.getDetailLevel() == minDetailLevel - 1) minDetailLevel = levelContainer.getDetailLevel();
		dataContainer[levelContainer.getDetailLevel()] = levelContainer;

	}

	/**
	 * @param detailLevel
	 */
	public void cutTree(byte detailLevel)
	{
		if (minDetailLevel < detailLevel)
		{
			for (byte tempLod = 0; tempLod < detailLevel; tempLod++)
			{
				dataContainer[tempLod] = null;
			}
			minDetailLevel = detailLevel;
		}
	}

	/**
	 * @param detailLevel
	 */
	public void expand(byte detailLevel)
	{
		if (detailLevel < minDetailLevel)
		{
			for (byte tempLod = (byte) (minDetailLevel - 1); tempLod >= detailLevel ; tempLod--)
			{
				if(dataContainer[tempLod + 1] == null)
				{
					dataContainer[tempLod + 1] = new SingleLevelContainer((byte) (tempLod + 1));
				}
				dataContainer[tempLod] = dataContainer[tempLod+1].expand();
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
	 * return needed memory in byte
	 */
	public int getMinMemoryNeeded()
	{
		int count = 0;
		for (byte tempLod = LodUtil.REGION_DETAIL_LEVEL; tempLod > minDetailLevel; tempLod--)
		{
			//i'm doing a upper limit of the minimum
			//Color should be just 3 byte but i'm gonna calculate as 12 byte
			//Height and depth should be just 4 byte but i'm gonna calculate as 8 byte
			count += Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - tempLod) * (8 + 3 + 2 + 2 + 1);
			//count += Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - tempLod) * (24 + 8 + 8 + 8 + 8);
		}
		return count;
	}

	@Override
	public String toString()
	{
		return getLevel(LodUtil.REGION_DETAIL_LEVEL).toString();
	}
}
