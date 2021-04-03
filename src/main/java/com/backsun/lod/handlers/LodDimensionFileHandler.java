package com.backsun.lod.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.backsun.lod.objects.LodChunk;
import com.backsun.lod.objects.LodDimension;
import com.backsun.lod.objects.LodRegion;

/**
 * This object handles creating LodRegions
 * from files and saving LodRegion objects
 * to file.
 * 
 * @author James Seibel
 * @version 03-31-2021
 */
public class LodDimensionFileHandler
{
	private LodDimension loadedDimension = null;
	public long regionLastWriteTime[][];
	
	private File dimensionDataSaveFolder;
	
	private final String FILE_NAME_PREFIX = "lod";
	private final String FILE_EXTENSION = ".txt";
	
	private ExecutorService fileWritingThreadPool = Executors.newFixedThreadPool(1);
	
	
	public LodDimensionFileHandler(File newSaveFolder, LodDimension newLoadedDimension)
	{
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
	 * Return the LodRegion at the given coordinates.
	 * (null if the file doesn't exist)
	 */
	public LodRegion loadRegionFromFile(int regionX, int regionZ)
	{
		if (!readyToReadAndWrite())
			return null;
		
		String fileName = getFileNameForRegion(regionX, regionZ);
		
		File f = new File(fileName);
		
		if (!f.exists())
		{
			// there wasn't a file, don't
			// return anything
			return null;
		}
		
		LodRegion region = new LodRegion(regionX, regionZ);
		
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(f));
			String s = br.readLine();
			
			while(s != null && !s.isEmpty())
			{
				try
				{
					// convert each line into an LOD object and add it to the region
					LodChunk lod = new LodChunk(s);
					
					region.addLod(lod);
				}
				catch(IllegalArgumentException e)
				{
					// we were unable to create this chunk
					// for whatever reason.
					// skip to the next chunk
				}
				
				s = br.readLine();
			}
			
			br.close();
		}
		catch (IOException e)
		{
			// File not found
			
			// or the buffered reader encountered a 
			// problem reading the file
			return null;
		}
		
		return region;
	}
	
	
	
	
	
	
	
	
	//==============//
	// Save to File //
	//==============//
	
	/**
	 * Save all dirty regions in this LodDimension to file.
	 */
	public synchronized void saveDirtyRegionsToFileAsync()
	{
		if (!readyToReadAndWrite())
			// we aren't ready to read and write yet
			return;
		
		fileWritingThreadPool.execute(saveDirtyRegionsThread);
	}
	private Thread saveDirtyRegionsThread = new Thread(() -> 
	{
		for(int i = 0; i < loadedDimension.getWidth(); i++)
		{
			for(int j = 0; j < loadedDimension.getWidth(); j++)
			{
				if(loadedDimension.isRegionDirty[i][j])
				{
					saveRegionToDisk(loadedDimension.regions[i][j]);
					loadedDimension.isRegionDirty[i][j] = false;
				}
			}
		}
	});
 	
	/**
	 * Save a specific region to disk.<br>
	 * Note: it will save to the LodDimension that this
	 * handler is associated with.
	 */
	private void saveRegionToDisk(LodRegion region)
	{
		if (!readyToReadAndWrite() || region == null)
			return;
		
		// convert chunk coordinates to region
		// coordinates
		int x = region.x;
		int z = region.z;
		
		File f = new File(getFileNameForRegion(x, z));
		
		try
		{
			// make sure the file and folder exists
			if (!f.exists())
				if(!f.getParentFile().exists())
					f.getParentFile().mkdirs();
				f.createNewFile();
			
			FileWriter fw = new FileWriter(f);
			
			for(LodChunk[] chunkArray : region.getAllLods())
				for(LodChunk chunk : chunkArray)
					if(chunk != null && !chunk.isPlaceholder())
						fw.write(chunk.toData() + "\n");
			
			fw.close();
		}
		catch(Exception e)
		{
			System.err.println("LOD file write error: " + e.getMessage());
		}
	}
	
	
 	
	
	
	
	
	//================//
	// helper methods //
	//================//
	
	
	/**
	 * Return the name of the file that should contain the 
	 * region at the given x and z. <br>
	 * Returns null if this object isn't ready to read and write.
	 */
	private String getFileNameForRegion(int regionX, int regionZ)
	{
		if (!readyToReadAndWrite())
			return null;
		
		try
		{
			// saveFolder is something like
			// ".\Super Flat\DIM-1\data"
			// or
			// ".\Super Flat\data"
			return dimensionDataSaveFolder.getCanonicalPath() + "\\" +
					FILE_NAME_PREFIX + "." + regionX + "." + regionZ + FILE_EXTENSION;
		}
		catch(IOException e)
		{
			return null;
		}
	}
	
	
	/**
	 * Returns if this FileHandler is ready to read
	 * and write files.
	 * <br>
	 * This returns true when the world save directory is known.
	 */
	public boolean readyToReadAndWrite()
	{
		return dimensionDataSaveFolder != null;
	}
	
	
	
}
