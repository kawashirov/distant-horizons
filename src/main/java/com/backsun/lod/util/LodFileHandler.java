package com.backsun.lod.util;

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

import net.minecraft.client.Minecraft;

/**
 * This object handles creating LodRegions
 * from files and saving LodRegion objects
 * to file.
 * 
 * @author James Seibel
 * @version 01-30-2021
 */
public class LodFileHandler
{
	// TODO this object needs to be changed to use NBT data instead of writing to files
	private static final boolean IMPLEMENTED = false;
	
	private Minecraft mc = Minecraft.getInstance();
	
	private LodDimension loadedRegion = null;
	public long regionLastWriteTime[][];
	
	private ExecutorService fileWritingThreadPool = Executors.newSingleThreadExecutor();
	
	
	public LodFileHandler(LodDimension newLoadedRegion)
	{
		loadedRegion = newLoadedRegion;
		// these two variable are used in sync with the LodDimension
		regionLastWriteTime = new long[loadedRegion.getWidth()][loadedRegion.getWidth()];
		for(int i = 0; i < loadedRegion.getWidth(); i++)
			for(int j = 0; j < loadedRegion.getWidth(); j++)
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
		if (!IMPLEMENTED)
			return null;
		
		// we don't currently support reading or writing
		// files when connected to a server
		if (!mc.isIntegratedServerRunning())
			return null;
		
		String fileName = "";
		
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
	
	
	public synchronized void saveDirtyRegionsToFile()
	{
		if (!IMPLEMENTED)
			return;
		
		// we don't currently support reading or writing
		// files when connected to a server
		if (!mc.isIntegratedServerRunning())
			return;
		
		fileWritingThreadPool.execute(saveDirtyRegionsThread);
	}
	private Thread saveDirtyRegionsThread = new Thread(() -> 
	{
		for(int i = 0; i < loadedRegion.getWidth(); i++)
		{
			for(int j = 0; j < loadedRegion.getWidth(); j++)
			{
				if(loadedRegion.isRegionDirty[i][j])
				{
					saveRegionToDisk(loadedRegion.regions[i][j]);
					loadedRegion.isRegionDirty[i][j] = false;
				}
			}
		}
	});
 	
	
	private void saveRegionToDisk(LodRegion region)
	{
		if (!IMPLEMENTED)
			return;
		
		if (region == null)
			return;
		
//		int x = region.x;
//		int z = region.z;
		
		File f = new File("");
		
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
					if(chunk != null)
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
	 * If on single player this will return the name of the user's
	 * world, if in multiplayer it will return the server name
	 * and game version.
	 */
	public static String getWorldName()
	{
		Minecraft mc = Minecraft.getInstance();
		
		if(mc.isIntegratedServerRunning())
		{
			return mc.getIntegratedServer().getName();
		}
		else
		{
			return mc.getCurrentServerData().serverName + "_version_" + mc.getCurrentServerData().gameVersion;
		}
	}
	
	
}
