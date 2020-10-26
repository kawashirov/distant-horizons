package backsun.lod.objects;

import net.minecraft.world.DimensionType;

/**
 * This object holds the regions
 * currently needed by the LodRenderer.
 * 
 * @author James Seibel
 * @version 10-25-2020
 */
public class LoadedRegions
{
	public final DimensionType dimension;
	
	private int maxWidth;
	
	public LodRegion regions[][];
	
	private int centerX;
	private int centerZ;
	
	public LoadedRegions(DimensionType newDimension, int newMaxWidth)
	{
		dimension = newDimension;
		maxWidth = newMaxWidth;
	}
	
	
	
	/**
	 * Move over all data currently stored and update the centerX and Z
	 */
	public void move(int xOffset, int zOffset)
	{		
		// if the x or z offset is equal to or greater than
		// the total size, just delete the current data
		// and update the centerX and/or centerZ
		if (Math.abs(xOffset) >= maxWidth || Math.abs(zOffset) >= maxWidth)
		{
			for(int x = 0; x < maxWidth; x++)
			{
				for(int z = 0; z < maxWidth; z++)
				{
					regions[x][z] = null;
				}
			}
			// update the new center
			centerX += xOffset;
			centerZ += zOffset;
			
			return;
		}
		
		
		// X
		
		// if xOffset is positive cut off the left side 
		// (move the center to the right)
		int start = (xOffset > 0)? 0 : maxWidth - 1;
		int min = (xOffset > 0)? 0 : maxWidth - Math.abs(xOffset) + centerX;
		int max = (xOffset > 0)? xOffset : maxWidth - 1 - xOffset;
		int increment = (xOffset > 0)? 1 : -1;
		
		for(int x = start; x >= min && x < max; x += increment)
		{
			for(int z = 0; z < maxWidth; z++)
			{
				regions[Math.abs((x + centerX) % maxWidth)][Math.abs((z + centerZ) % maxWidth)] = null;
			}
		}
		
		
		// Z
		start = (zOffset > 0)? 0 : maxWidth - 1;
		min = (zOffset > 0)? 0 : maxWidth - Math.abs(zOffset) + centerZ;
		max = (zOffset > 0)? zOffset : maxWidth - 1 - zOffset;
		increment = (zOffset > 0)? 1 : -1;
		
		for(int x = 0; x < maxWidth; x++)
		{
			for(int z = start; z >= min && z < max; z += increment)
			{
				regions[Math.abs((x + centerX) % maxWidth)][Math.abs((z + centerZ) % maxWidth)] = null;
			}
		}
		
		// update the new center
		centerX += xOffset;
		centerZ += zOffset;
	}
	
	
	public LodChunk getChunkFromCoordinates(int chunkX, int chunkZ)
	{
		int xIndex = (chunkX + centerX) % maxWidth;
		int zIndex = (chunkZ + centerZ) % maxWidth;
		
		return regions[xIndex / maxWidth][zIndex / maxWidth].chunks[xIndex % LodRegion.SIZE][xIndex % LodRegion.SIZE];
	}
}





