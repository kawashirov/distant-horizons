package com.seibel.lod.objects;

import com.seibel.lod.util.LevelPosUtil;
import com.seibel.lod.util.LodUtil;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public interface LevelContainer
{
	public static final char VERTICAL_DATA_DELIMITER = '\t';
	public static final char DATA_DELIMITER = '\n';
	/**With this you can add data to the level container
	 *
	 * @param data actual data to add in a array of long format.
	 * @param posX x position in the detail level
	 * @param posZ z position in the detail level
	 * @return true if correctly added, false otherwise
	 */
	public boolean addData(long[] data, int posX, int posZ);

	/**With this you can get data from the level container
	 *
	 * @param posX x position in the detail level
	 * @param posZ z position in the detail level
	 * @return the data in long array format
	 */
	public long[] getData(int posX, int posZ);

	/**
	 * @param posX x position in the detail level
	 * @param posZ z position in the detail level
	 * @return true only if the data exist
	 */
	public boolean doesItExist(int posX, int posZ);

	/**
	 * @return return the deatilLevel of this level container
	 */
	public byte getDetailLevel();

	/**This return a level container with detail level lower than the current level.
	 * The new level container may use information of this level.
	 * @return the new level container
	 */
	public LevelContainer expand();

	/**
	 *
	 * @param lowerLevelContainer lower level where we extract the data
	 * @param posX x position in the detail level to update
	 * @param posZ z position in the detail level to update
	 */
	public void updateData(LevelContainer lowerLevelContainer, int posX, int posZ);

	/**
	 * This will give the data to save in the file
	 * @return data as a String
	 */
	public String toDataString();
}
