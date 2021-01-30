package backsun.lod.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import backsun.lod.objects.LodDimension;
import backsun.lod.objects.LodChunk;
import backsun.lod.objects.LodRegion;
import net.minecraft.client.Minecraft;
import net.minecraft.world.storage.ISaveHandler;

/**
 * This object handles creating LodRegions
 * from files and saving LodRegion objects
 * to file.
 * 
 * @author James Seibel
 * @version 01-30-2021
 */
public class LodRegionFileHandler
{
	private LodDimension loadedRegion = null;
	public long regionLastWriteTime[][];
	
	// String s = Minecraft.getMinecraftDir().getCanonicalPath() + "/saves/" + world.getSaveHandler().getSaveDirectoryName() + "/data/AA/World" + world.provider.dimensionId + ".dat";
	private String save_dir;
	public ISaveHandler saveHandler;
	
	private final String FILE_NAME_PREFIX = "lod";
	private final String FILE_EXTENSION = ".txt";
	
	private ExecutorService fileWritingThreadPool = Executors.newFixedThreadPool(1);
	/** Is true if the readyToReadAndWrite is false */
	private boolean waitingToSaveRegions = false;
	
	
	public LodRegionFileHandler(ISaveHandler newSaveHandler, LodDimension newLoadedRegion)
	{
		saveHandler = newSaveHandler;
		
		if (saveHandler == null)
		{
			throw new NullPointerException("LodRegionFileHandler requires a world SaveHandler.");
		}
		
		loadedRegion = newLoadedRegion;
		// these two variable are used in sync with the LodDimension
		regionLastWriteTime = new long[loadedRegion.getWidth()][loadedRegion.getWidth()];
		for(int i = 0; i < loadedRegion.getWidth(); i++)
			for(int j = 0; j < loadedRegion.getWidth(); j++)
				regionLastWriteTime[i][j] = -1;
		
		
		if (saveHandler != null && saveHandler.getWorldDirectory() != null)
			try {
				save_dir = saveHandler.getWorldDirectory().getCanonicalPath();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	
	
	
	
	
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
	
	
	public synchronized void saveDirtyRegionsToFile()
	{
		if (!readyToReadAndWrite())
		{	
			// we aren't ready to read and write yet
			if(!waitingToSaveRegions)
			{
				waitingToSaveRegions = true;
				
				// retry until we are able to read and write
				// then wake up the fileWritingThreadPool
				Thread retryReady = new Thread(() -> 
				{
					try
					{
						// check once every so often so see
						// if anything has changed so we can
						// start reading and writing files
						while(!readyToReadAndWrite())
						{
							// TODO what can be done if connected to a server?
							saveHandler = Minecraft.getMinecraft().getIntegratedServer().getWorld(0).getSaveHandler();
							this.wait(1000);
						}
						
						// we can start writing files now
						if (waitingToSaveRegions)
						{
							fileWritingThreadPool.execute(saveDirtyRegionsAsync);
							waitingToSaveRegions = false;
						}
					}
					catch (InterruptedException e) 
					{ /* should never be called */}
				});
				
				retryReady.run();
			}
			
			return;
		}
		
		fileWritingThreadPool.execute(saveDirtyRegionsAsync);
	}
	
	public void saveRegionToDisk(LodRegion region)
	{
		// convert chunk coordinates to region
		// coordinates
		int x = region.x;
		int z = region.z;
		
		File f = new File(getFileNameForRegion(x, z));
		
		try
		{
			if (!f.exists())
			{
				// make the LOD folder if it doesn't exist
				if(!f.getParentFile().exists())
				{
					f.getParentFile().mkdirs();
				}
				
				f.createNewFile();
			}
			
			FileWriter fw = new FileWriter(f);
			
			for(LodChunk[] chunkArray : region.getAllLods())
			{
				for(LodChunk chunk : chunkArray)
				{
					if(chunk != null)
					{
						fw.write(chunk.toData() + "\n");
					}
				}
			}
			
			fw.close();
		}
		catch(Exception e)
		{
			System.err.println("LOD ERROR: " + e.getMessage());
			//e.printStackTrace();
		}
	}
	
	
	private Thread saveDirtyRegionsAsync = new Thread(() -> 
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
		
		waitingToSaveRegions = false;
	});
 	
 	
	
	/**
	 * Return the name of the file that should contain the 
	 * region at the given x and z. <br>
	 * Returns null if this object isn't ready to read and write.
	 * @param regionX
	 * @param regionZ
	 */
	private String getFileNameForRegion(int regionX, int regionZ)
	{
		if (!readyToReadAndWrite())
			return null;
		
		return save_dir + "\\lod_data\\DIM" + loadedRegion.dimension.getId() + "\\" +
				FILE_NAME_PREFIX + "." + regionX + "." + regionZ + FILE_EXTENSION;
	}
	
	
	/**
	 * Returns if this FileHandler is ready to read
	 * and write files.
	 */
	public boolean readyToReadAndWrite()
	{
		return saveHandler != null && saveHandler.getWorldDirectory() != null && 
				save_dir != null && !save_dir.isEmpty();
	}
	
//	/**
//	 * Used to wake up the fileWritingThreadPool once
//	 * we are able to read and write files.
//	 */
//	private Thread retryReady = new Thread(() -> 
//	{
//		try
//		{
//			// check once every so often so see
//			// if anything has changed so we can
//			// start reading and writing files
//			while(!readyToReadAndWrite())
//			{
//				this.wait(1000);
//			}
//			
//			// we can start writing files now
//			if (waitingToSaveRegions)
//			{
//				fileWritingThreadPool.execute(saveDirtyRegionsAsync);
//				waitingToSaveRegions = false;
//			}
//		}
//		catch (InterruptedException e) 
//		{ /* should never be called */}
//	});
}
