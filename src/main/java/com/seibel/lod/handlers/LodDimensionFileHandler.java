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
package com.seibel.lod.handlers;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.enums.LodQualityMode;
import com.seibel.lod.objects.*;
import com.seibel.lod.proxy.ClientProxy;
import com.seibel.lod.util.LodThreadFactory;
import com.seibel.lod.util.LodUtil;

/**
 * This object handles creating LodRegions
 * from files and saving LodRegion objects
 * to file.
 *
 * @author James Seibel
 * @version 9-7-2021
 */
public class LodDimensionFileHandler
{

	private LodDimension loadedDimension = null;
	public long regionLastWriteTime[][];

	private File dimensionDataSaveFolder;

	/**
	 * lod
	 */
	private static final String FILE_NAME_PREFIX = "lod";
	/**
	 * .txt
	 */
	private static final String FILE_EXTENSION = ".dat";
	/**
	 * detail-#
	 */
	private static final String DETAIL_FOLDER_NAME_PREFIX = "detail-";

	/**
	 * .tmp <br>
	 * Added to the end of the file path when saving to prevent
	 * nulling a currently existing file. <br>
	 * After the file finishes saving it will end with
	 * FILE_EXTENSION.
	 */
	private static final String TMP_FILE_EXTENSION = ".tmp";

	/**
	 * This is the file version currently accepted by this
	 * file handler, older versions (smaller numbers) will be deleted and overwritten,
	 * newer versions (larger numbers) will be ignored and won't be read.
	 */
	public static final int LOD_SAVE_FILE_VERSION = 6;

	/**
	 * Allow saving asynchronously, but never try to save multiple regions
	 * at a time
	 */
	private ExecutorService fileWritingThreadPool = Executors.newSingleThreadExecutor(new LodThreadFactory(this.getClass().getSimpleName()));


	public LodDimensionFileHandler(File newSaveFolder, LodDimension newLoadedDimension)
	{
		if (newSaveFolder == null)
			throw new IllegalArgumentException("LodDimensionFileHandler requires a valid File location to read and write to.");

		dimensionDataSaveFolder = newSaveFolder;

		loadedDimension = newLoadedDimension;
		// these two variable are used in sync with the LodDimension
		regionLastWriteTime = new long[loadedDimension.getWidth()][loadedDimension.getWidth()];
		for (int i = 0; i < loadedDimension.getWidth(); i++)
			for (int j = 0; j < loadedDimension.getWidth(); j++)
				regionLastWriteTime[i][j] = -1;
	}


	//================//
	// read from file //
	//================//

	/**
	 * Return the LodRegion region at the given coordinates.
	 * (null if the file doesn't exist)
	 */
	public LodRegion loadRegionFromFile(byte detailLevel, RegionPos regionPos, DistanceGenerationMode generationMode, LodQualityMode lodQualityMode)
	{
		int regionX = regionPos.x;
		int regionZ = regionPos.z;
		LodRegion region = new LodRegion(LodUtil.REGION_DETAIL_LEVEL,regionPos, generationMode, lodQualityMode);
		for (byte tempDetailLevel = LodUtil.REGION_DETAIL_LEVEL; tempDetailLevel >= detailLevel; tempDetailLevel--)
		{
			String fileName = getFileNameAndPathForRegion(regionX, regionZ, generationMode, tempDetailLevel, lodQualityMode);

			try
			{
				// if the fileName was null that means the folder is inaccessible
				// for some reason
				if (fileName == null)
					throw new IllegalArgumentException("Unable to read region [" + regionX + ", " + regionZ + "] file, no fileName.");


				File f = new File(fileName);

				if (!f.exists())
				{
					// there wasn't a file, don't
					// return anything
					continue;
				}
				byte data[] = {0};
				long dataSize = f.length();
				dataSize -= 1;
				if (dataSize > 0) {
					try (InputStream inputStream = new BufferedInputStream(new FileInputStream(f))) {
						int fileVersion = -1;
						fileVersion = inputStream.read();
						// check if this file can be read by this file handler
						if (fileVersion < LOD_SAVE_FILE_VERSION) {
							// the file we are reading is an older version,
							// close the reader and delete the file.
							inputStream.close();
							f.delete();
							ClientProxy.LOGGER.info("Outdated LOD region file for region: (" + regionX + "," + regionZ + ") version found: " + fileVersion +
									", version requested: " + LOD_SAVE_FILE_VERSION +
									" File was been deleted.");

							break;
						} else if (fileVersion > LOD_SAVE_FILE_VERSION) {
							// the file we are reading is a newer version,
							// close the reader and ignore the file, we don't
							// want to accidently delete anything the user may want.
							inputStream.close();
							ClientProxy.LOGGER.info("Newer LOD region file for region: (" + regionX + "," + regionZ + ") version found: " + fileVersion +
									", version requested: " + LOD_SAVE_FILE_VERSION +
									" this region will not be written to in order to protect the newer file.");

							break;
						}
						// this file is a readable version, begin reading the file
						data = new byte[(int) dataSize];
						inputStream.read(data);
						inputStream.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
				switch (region.getLodQualityMode()){
					default:
					case HEIGHTMAP:
						region.addLevel(new SingleLevelContainer(data));
						break;
					case MULTI_LOD:
						//region.addLevel(new VerticalLevelContainer(data));
						break;
				}
				//region.addLevel(new SingleLevelContainer(data));
			} catch (Exception e)
			{
				// the buffered reader encountered a
				// problem reading the file
				ClientProxy.LOGGER.error("LOD file read error. Unable to read to [" + fileName + "] error [" + e.getMessage() + "]: ");
				e.printStackTrace();
			}
		}
		if (region.getMinDetailLevel() >= detailLevel)
			region.expand(detailLevel);
		return region;
	}


	//==============//
	// Save to File //
	//==============//

	/**
	 * Save all dirty regions in this LodDimension to file.
	 */
	public void saveDirtyRegionsToFileAsync()
	{
		fileWritingThreadPool.execute(saveDirtyRegionsThread);
	}

	private Thread saveDirtyRegionsThread = new Thread(() ->
	{
		try
		{
			for (int i = 0; i < loadedDimension.getWidth(); i++)
			{
				for (int j = 0; j < loadedDimension.getWidth(); j++)
				{
					if (loadedDimension.isRegionDirty[i][j] && loadedDimension.regions[i][j] != null)
					{
						saveRegionToFile(loadedDimension.regions[i][j]);
						loadedDimension.isRegionDirty[i][j] = false;
					}
				}
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	});

	/**
	 * Save a specific region to disk.<br>
	 * Note: <br>
	 * 1. If a file already exists for a newer version
	 * the file won't be written.<br>
	 * 2. This will save to the LodDimension that this
	 * handler is associated with.
	 */
	private void saveRegionToFile(LodRegion region)
	{
		// convert to region coordinates
		int x = region.regionPosX;
		int z = region.regionPosZ;
		for (byte detailLevel = region.getMinDetailLevel(); detailLevel <= LodUtil.REGION_DETAIL_LEVEL; detailLevel++)
		{
			String fileName = getFileNameAndPathForRegion(x, z, region.getGenerationMode(), detailLevel, region.getLodQualityMode());
			File oldFile = new File(fileName);

			// if the fileName was null that means the folder is inaccessible
			// for some reason
			if (fileName == null)
			{
				ClientProxy.LOGGER.warn("Unable to save region [" + x + ", " + z + "] to file, no fileName.");
				return;
			}


			try
			{
				// make sure the file and folder exists
				if (!oldFile.exists())
				{
					// the file doesn't exist,
					// create it and the folder if need be
					if (!oldFile.getParentFile().exists())
						oldFile.getParentFile().mkdirs();
					oldFile.createNewFile();
				} else
				{
					// the file exists, make sure it
					// is the correct version.
					// (to make sure we don't overwrite a newer
					// version file if it exists)
					int fileVersion = LOD_SAVE_FILE_VERSION;
					try (InputStream inputStream = new BufferedInputStream(new FileInputStream(oldFile))) {
						fileVersion = inputStream.read();
						inputStream.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
					// check if this file can be written to by the file handler
					if (fileVersion <= LOD_SAVE_FILE_VERSION)
					{
						// we are good to continue and overwrite the old file
					} else // if(fileVersion > LOD_SAVE_FILE_VERSION)
					{
						// the file we are reading is a newer version,
						// don't write anything, we don't want to accidently
						// delete anything the user may want.
						return;
					}
				}

				// the old file is good, now create a new save file
				File newFile = new File(fileName + TMP_FILE_EXTENSION);
				try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(newFile));) {

					// add the version of this file
					outputStream.write(LOD_SAVE_FILE_VERSION);

					// add each LodChunk to the file
					outputStream.write(region.getLevel(detailLevel).toDataString());
					outputStream.close();

					// overwrite the old file with the new one
					Files.move(newFile.toPath(), oldFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
				}catch (IOException ex) {
					ex.printStackTrace();
				}
			} catch (Exception e)
			{
				ClientProxy.LOGGER.error("LOD file write error. Unable to write to [" + fileName + "] error [" + e.getMessage() + "]: ");
				e.printStackTrace();
			}
		}
	}


	//================//
	// helper methods //
	//================//


	/**
	 * Return the name of the file that should contain the
	 * region at the given x and z. <br>
	 * Returns null if this object isn't ready to read and write. <br><br>
	 * <p>
	 * example: "lod.0.0.txt" <br><br>
	 * <p>
	 * Returns null if there is an IO Exception.
	 */
	private String getFileNameAndPathForRegion(int regionX, int regionZ, DistanceGenerationMode generationMode, byte detailLevel, LodQualityMode lodQualityMode)
	{
		try
		{
			// saveFolder is something like
			// ".\Super Flat\DIM-1\data"
			// or
			// ".\Super Flat\data"
			return dimensionDataSaveFolder.getCanonicalPath() + File.separatorChar +
					lodQualityMode + File.separatorChar +
					generationMode.toString() + File.separatorChar +
					DETAIL_FOLDER_NAME_PREFIX + detailLevel + File.separatorChar +
					FILE_NAME_PREFIX + "." + regionX + "." + regionZ + FILE_EXTENSION;
		} catch (IOException | SecurityException e)
		{
			ClientProxy.LOGGER.warn("Unable to get the filename for the region [" + regionX + ", " + regionZ + "], error: [" + e.getMessage() + "], stacktrace: ");
			e.printStackTrace();
			return null;
		}
	}

}
