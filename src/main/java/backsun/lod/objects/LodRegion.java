package backsun.lod.objects;

/**
 * A LodRegion is the a 32x32
 * 2D array of LodChunk objects.
 * Each LodRegion corresponds to
 * one file in the file system.
 * 
 * @author James Seibel
 *
 */
public class LodRegion
{
	/**	X coordinate of this region */
	private int x;
	/** Z coordinate of this region */
	private int z;
	
	public LodChunk data[][] = new LodChunk[32][32];
	
	
	public LodRegion(int regionX, int regionZ)
	{
		x = regionX;
		z = regionZ;
	}


	
	public int getX()
	{
		return x;
	}
	
	public int getZ()
	{
		return z;
	}
}
