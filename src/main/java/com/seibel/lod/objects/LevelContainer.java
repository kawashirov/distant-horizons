package com.seibel.lod.objects;

public interface LevelContainer
{
	/**
	 * With this you can add data to the level container
	 * @param data actual data to add in an array of long format.
	 * @param posX x position in the detail level
	 * @param posZ z position in the detail level
	 * @param index z position in the detail level
	 * @return true if correctly added, false otherwise
	 */
	boolean addData(long data, int posX, int posZ, int index);
	
	/**
	 * With this you can add data to the level container
	 * @param data actual data to add in an array of long format.
	 * @param posX x position in the detail level
	 * @param posZ z position in the detail level
	 * @return true if correctly added, false otherwise
	 */
	boolean addSingleData(long data, int posX, int posZ);
	
	/**
	 * With this you can get data from the level container
	 * @param posX x position in the detail level
	 * @param posZ z position in the detail level
	 * @return the data in long array format
	 */
	long getData(int posX, int posZ, int index);
	
	/**
	 * With this you can get data from the level container
	 * @param posX x position in the detail level
	 * @param posZ z position in the detail level
	 * @return the data in long array format
	 */
	long getSingleData(int posX, int posZ);
	
	/**
	 * @param posX x position in the detail level
	 * @param posZ z position in the detail level
	 * @return true only if the data exist
	 */
	boolean doesItExist(int posX, int posZ);
	
	/**
	 * @return return the detailLevel of this level container
	 */
	byte getDetailLevel();
	
	
	int getMaxVerticalData();
	
	/** Clears the dataPoint at the given array index */
	void clear(int posX, int posZ);
	
	/**
	 * This return a level container with detail level lower than the current level.
	 * The new level container may use information of this level.
	 * @return the new level container
	 */
	LevelContainer expand();
	
	/**
	 * @param lowerLevelContainer lower level where we extract the data
	 * @param posX x position in the detail level to update
	 * @param posZ z position in the detail level to update
	 */
	void updateData(LevelContainer lowerLevelContainer, int posX, int posZ);
	
	/**
	 * This will give the data to save in the file
	 * @return data as a String
	 */
	byte[] toDataString();
	
	
	/**
	 * This will give the data to save in the file
	 * @return data as a String
	 */
	int getMaxNumberOfLods();
	
	/**
	 * This will give the data to save in the file
	 * @return data as a String
	 */
	int getMaxMemoryUse();
}
