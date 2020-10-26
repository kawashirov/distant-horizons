package backsun.lod.objects;

/**
 * A LodRegion is the a 32x32
 * 2D array of LodChunk objects.
 * Each LodRegion corresponds to
 * one file in the file system.
 * 
 * @author James Seibel
 * @version 10-22-2020
 */
public class LodRegion
{
	public static final int SIZE = 32;
	
	/**	X coordinate of this region */
	public final int x;
	/** Z coordinate of this region */
	public final int z;
	
	public LodChunk chunks[][];
	
	
	public LodRegion(int regionX, int regionZ)
	{
		x = regionX;
		z = regionZ;
		
		chunks = new LodChunk[SIZE][SIZE];
	}
}
