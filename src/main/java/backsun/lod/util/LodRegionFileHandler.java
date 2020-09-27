package backsun.lod.util;

import java.io.File;
import java.io.FileWriter;

import backsun.lod.objects.LodChunk;
import backsun.lod.objects.LodRegion;

/**
 * This object handles creating LodRegions
 * from files and saving LodRegion objects
 * to file.
 * 
 * @author James Seibel
 * @version 09-27-2020
 */
public class LodRegionFileHandler
{
	private final String SAVE_DIR = "C:/Users/James Seibel/Desktop/lod_save_folder/";
	
	private final String FILE_NAME_PREFIX = "lod";
	private final String FILE_NAME_DELIMITER = "-";
	private final String FILE_EXTENSION = ".txt";
	
	
	public LodRegionFileHandler()
	{
		
	}
	
	
	
	
	
	
	/**
	 * Return the 
	 */
	public LodRegion loadRegionFromFile(int regionX, int regionZ)
	{
		return null;
	}
	
	
	
	
	
	public void saveLodToDisk(LodChunk chunk)
	{
		// convert chunk coordinates to region
		// coordinates
		int x = (int) (chunk.x / 32.0);
		int z = (int) (chunk.z / 32.0);
		
		File f = new File(getFileNameForRegion(x, z));
		
		
		try
		{
			if (!f.exists())
			{
				System.out.println("LOD\tcreating file");
				f.createNewFile();
			}
			
			FileWriter fw = new FileWriter(f,true); // true means append to file
			String data = chunk.x + "\t" + chunk.z;
			
			fw.write(data + "\n");
			fw.close();
			
			System.out.println("LOD \t" + data);
		}
		catch(Exception e)
		{
			System.err.println("LOD ERROR \t" + e);
		}
	}
	
	
	
	
	

	/**
	 * Returns true if a file exists for the region
	 * containing the given chunk.
	 */
	public boolean regionFileExistForChunk(int chunkX, int chunkZ)
	{
		// convert chunk coordinates to region
				// coordinates
		int regionX = (int) (chunkX / 32.0);
		int regionZ = (int) (chunkZ / 32.0);
		
		return new File(getFileNameForRegion(regionX, regionZ)).exists();
	}
	
	/**
	 * Returns true if a file exists
	 * for the given region coordinates.
	 */
	public boolean regionFileExistForRegion(int regionX, int regionZ)
	{
		return new File(getFileNameForRegion(regionX, regionZ)).exists();
	}
	
	
	
	private String getFileNameForRegion(int x, int z)
	{
		return SAVE_DIR + 
				FILE_NAME_PREFIX + FILE_NAME_DELIMITER + 
				x + FILE_NAME_DELIMITER + z + FILE_EXTENSION;
	}
}
