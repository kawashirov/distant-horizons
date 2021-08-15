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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.seibel.lod.objects.LodQuadTree;
import com.seibel.lod.objects.LodQuadTreeDimension;
import com.seibel.lod.objects.LodQuadTreeNode;
import com.seibel.lod.objects.RegionPos;
import com.seibel.lod.proxy.ClientProxy;

/**
 * This object handles creating LodRegions
 * from files and saving LodRegion objects
 * to file.
 * 
 * @author James Seibel
 * @version 8-14-2021
 */
public class LodQuadTreeDimensionFileHandler
{
	/** This is what separates each piece of data */
	public static final char DATA_DELIMITER = ',';
	
	
	private LodQuadTreeDimension loadedDimension = null;
	public long regionLastWriteTime[][];
	
	private File dimensionDataSaveFolder;
	
	/** lod */
	private static final String FILE_NAME_PREFIX = "lod";
	/** .txt */
	private static final String FILE_EXTENSION = ".txt";
	/** .tmp <br>
	 * Added to the end of the file path when saving to prevent
	 * nulling a currently existing file. <br>
	 * After the file finishes saving it will end with
	 * FILE_EXTENSION. */
	private static final String TMP_FILE_EXTENSION = ".tmp";
	
	/** This is the file version currently accepted by this
	 * file handler, older versions (smaller numbers) will be deleted and overwritten,
	 * newer versions (larger numbers) will be ignored and won't be read. */
	public static final int LOD_SAVE_FILE_VERSION = 3;
	
	/** This is the string written before the file version */
	private static final String LOD_FILE_VERSION_PREFIX = "lod_save_file_version";
	
	/** Allow saving asynchronously, but never try to save multiple regions
	 * at a time */
	private ExecutorService fileWritingThreadPool = Executors.newSingleThreadExecutor();
	
	
	public LodQuadTreeDimensionFileHandler(File newSaveFolder, LodQuadTreeDimension newLoadedDimension)
	{
		if (newSaveFolder == null)
			throw new IllegalArgumentException("LodDimensionFileHandler requires a valid File location to read and write to.");
		
		dimensionDataSaveFolder = newSaveFolder;
		
		loadedDimension = newLoadedDimension;
		// these two variable are used in sync with the LodDimension
		regionLastWriteTime = new long[loadedDimension.getWidth()][loadedDimension.getWidth()];
		for(int i = 0; i < loadedDimension.getWidth(); i++)
			for(int j = 0; j < loadedDimension.getWidth(); j++)
				regionLastWriteTime[i][j] = -1;
	}
	
	
	
	
	
	//================//
	// read from file //
	//================//
	
	
	
	/**
	 * Return the LodQuadTree region at the given coordinates.
	 * (null if the file doesn't exist)
	 */
	public LodQuadTree loadRegionFromFile(RegionPos regionPos)
	{
		int regionX = regionPos.x;
		int regionZ = regionPos.z;
		
		String fileName = getFileNameAndPathForRegion(regionX, regionZ);
		if(FILE_EXTENSION == ".bin"){
			try {
				ObjectInputStream is = new ObjectInputStream(new FileInputStream(fileName));
				List<LodQuadTreeNode> dataList = (List<LodQuadTreeNode>) is.readObject();
				//LodQuadTree region = (LodQuadTree) is.readObject();
				is.close();
				return new LodQuadTree(dataList, regionX, regionZ);
				//return region;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			return new LodQuadTree(new ArrayList<>(), regionX, regionZ);
			//return null;
		}

		if(FILE_EXTENSION == ".txt") {
			File f = new File(fileName);

			if (!f.exists()) {
				// there wasn't a file, don't
				// return anything
				return null;
			}

			List<LodQuadTreeNode> dataList = new ArrayList<>();
			try {
				BufferedReader br = new BufferedReader(new FileReader(f));
				String s = br.readLine();
				int fileVersion = -1;

				if (s != null && !s.isEmpty()) {
					// try to get the file version
					try {
						fileVersion = Integer.parseInt(s.substring(s.indexOf(' ')).trim());
					} catch (NumberFormatException | StringIndexOutOfBoundsException e) {
						// this file doesn't have a version
						// keep the version as -1
						fileVersion = -1;
					}

					// check if this file can be read by this file handler
					if (fileVersion < LOD_SAVE_FILE_VERSION) {
						// the file we are reading is an older version,
						// close the reader and delete the file.
						br.close();
						f.delete();
						ClientProxy.LOGGER.info("Outdated LOD region file for region: (" + regionX + "," + regionZ + ") version: " + fileVersion +
								", version requested: " + LOD_SAVE_FILE_VERSION +
								" File was been deleted.");

						return null;
					} else if (fileVersion > LOD_SAVE_FILE_VERSION) {
						// the file we are reading is a newer version,
						// close the reader and ignore the file, we don't
						// want to accidently delete anything the user may want.
						br.close();
						ClientProxy.LOGGER.info("Newer LOD region file for region: (" + regionX + "," + regionZ + ") version: " + fileVersion +
								", version requested: " + LOD_SAVE_FILE_VERSION +
								" this region will not be written to in order to protect the newer file.");

						return null;
					}
				} else {
					// there is no data in this file
					br.close();
					return null;
				}


				// this file is a readable version, begin reading the file
				s = br.readLine();

				while (s != null && !s.isEmpty()) {
					try {
						dataList.add(new LodQuadTreeNode(s));
					} catch (IllegalArgumentException e) {
						// we were unable to create this chunk
						// for whatever reason.
						// skip to the next chunk
						ClientProxy.LOGGER.warn(e.getMessage());
					}

					s = br.readLine();
				}

				br.close();
			} catch (IOException e) {
				// the buffered reader encountered a
				// problem reading the file
				return null;
			}
			return new LodQuadTree(dataList, regionX, regionZ);
		}

		return null;
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
		for(int i = 0; i < loadedDimension.getWidth(); i++)
		{
			for(int j = 0; j < loadedDimension.getWidth(); j++)
			{
				if(loadedDimension.isRegionDirty[i][j] && loadedDimension.regions[i][j] != null)
				{
					saveRegionToFile(loadedDimension.regions[i][j]);
					loadedDimension.isRegionDirty[i][j] = false;
				}
			}
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
	private void saveRegionToFile(LodQuadTree region)
	{
		// convert to region coordinates
		int x = region.getLodNodeData().posX;
		int z = region.getLodNodeData().posZ;


		File oldFile = new File(getFileNameAndPathForRegion(x, z));

		if(FILE_EXTENSION == ".bin"){
			try {
				ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(getFileNameAndPathForRegion(x, z)));
				os.writeObject(region.getNodeListWithMask(LodQuadTreeDimension.FULL_COMPLEXITY_MASK, false, true));
				//os.writeObject(region);
				os.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(FILE_EXTENSION == ".txt") {
			try {
				// make sure the file and folder exists
				if (!oldFile.exists()) {
					// the file doesn't exist,
					// create it and the folder if need be
					if (!oldFile.getParentFile().exists())
						oldFile.getParentFile().mkdirs();
					oldFile.createNewFile();
				} else {
					// the file exists, make sure it
					// is the correct version.
					// (to make sure we don't overwrite a newer
					// version file if it exists)

					BufferedReader br = new BufferedReader(new FileReader(oldFile));
					String s = br.readLine();
					int fileVersion = LOD_SAVE_FILE_VERSION;

					if (s != null && !s.isEmpty()) {
						// try to get the file version
						try {
							fileVersion = Integer.parseInt(s.substring(s.indexOf(' ')).trim());
						} catch (NumberFormatException | StringIndexOutOfBoundsException e) {
							// this file doesn't have a correctly formated version
							// just overwrite the file
						}
					}
					br.close();

					// check if this file can be written to by the file handler
					if (fileVersion <= LOD_SAVE_FILE_VERSION) {
						// we are good to continue and overwrite the old file
					} else //if(fileVersion > LOD_SAVE_FILE_VERSION)
					{
						// the file we are reading is a newer version,
						// don't write anything, we don't want to accidently
						// delete anything the user may want.
						return;
					}
				}

				// the old file is good, now create a new save file
				File newFile = new File(getFileNameAndPathForRegion(x, z) + TMP_FILE_EXTENSION);

				FileWriter fw = new FileWriter(newFile);

				// add the version of this file
				fw.write(LOD_FILE_VERSION_PREFIX + " " + LOD_SAVE_FILE_VERSION + "\n");

				// add each LodChunk to the file
				List<LodQuadTreeNode> nodesToSave = Collections.unmodifiableList(region.getNodeListWithMask(LodQuadTreeDimension.FULL_COMPLEXITY_MASK, false, true));
				for (LodQuadTreeNode lodQuadTreeNode : nodesToSave) {
					fw.write(lodQuadTreeNode.toData() + "\n");
					lodQuadTreeNode.dirty = false;
				}
				fw.close();

				// overwrite the old file with the new one
				Files.move(newFile.toPath(), oldFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (Exception e) {
				ClientProxy.LOGGER.error("LOD file write error: ");
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
	 *
	 * example: "lod.0.0.txt"
	 */
	private String getFileNameAndPathForRegion(int regionX, int regionZ)
	{
		try
		{
			// saveFolder is something like
			// ".\Super Flat\DIM-1\data"
			// or
			// ".\Super Flat\data"
			return dimensionDataSaveFolder.getCanonicalPath() + File.separatorChar +
					FILE_NAME_PREFIX + "." + regionX + "." + regionZ + FILE_EXTENSION;
		}
		catch(IOException e)
		{
			return null;
		}
	}
	
}
