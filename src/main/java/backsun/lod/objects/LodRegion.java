package backsun.lod.objects;

/**
 * A LodRegion is the a 32x32
 * 2D array of LodChunk objects.
 * Each LodRegion corresponds to
 * one file in the file system.
 * 
 * @author James Seibel
 * @version 1-20-2021
 */
public class LodRegion
{
	/** number of chunks wide */
	public static final int SIZE = 32;
	
	/**	X coordinate of this region */
	public final int x;
	/** Z coordinate of this region */
	public final int z;
	
	private LodChunk chunks[][];
	
	
	public LodRegion(int regionX, int regionZ)
	{
		x = regionX;
		z = regionZ;
		
		chunks = new LodChunk[SIZE][SIZE];
	}
	
	
	public void addLod(LodChunk lod)
	{
		// we use ABS since LODs can be negative, but if they are
		// the region will negative first, therefore we don't have to
		// store the LOD chunks at negative indexes since we search 
		// LOD the region first
		int xIndex = Math.abs(lod.x % SIZE);
		int zIndex = Math.abs(lod.z % SIZE);
		
		chunks[xIndex][zIndex] = lod;
	}
	
	public LodChunk getLod(int chunkX, int chunkZ)
	{
		// since we add LOD's with ABS, we get them the same way
		int arrayX = Math.abs(chunkX % SIZE);
		int arrayZ = Math.abs(chunkZ % SIZE);
		
		if(arrayX >= SIZE || arrayZ >= SIZE)
			return null;
		
		return chunks[arrayX][arrayZ];
	}
	
	
	
	public LodChunk[][] getAllLods()
	{
		return chunks;
	}
	
	
	
	@Override
	public String toString()
	{
		String s = "";
		
		s += "x: " + x + " z: " + z + "\t";
		
		return s;
	}
}
