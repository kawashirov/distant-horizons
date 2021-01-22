package backsun.lod.objects;

import net.minecraft.world.DimensionType;

/**
 * This object holds the regions
 * currently needed by the LodRenderer.
 * 
 * @author James Seibel
 * @version 1-20-2021
 */
public class LoadedRegions
{
	public final DimensionType dimension;
	
	private int width; // if this ever changes make sure to update the halfWidth too
	private int halfWidth;
	
	public LodRegion regions[][];
	
	private int centerX;
	private int centerZ;
	
	public LoadedRegions(DimensionType newDimension, int newMaxWidth)
	{
		dimension = newDimension;
		width = newMaxWidth;
		
		regions = new LodRegion[width][width];
		
		centerX = 0;
		centerZ = 0;
		
		halfWidth = (int)Math.floor(width / 2);
	}
	
	
	
	public void move(int xOffset, int zOffset)
	{		
		// if the x or z offset is equal to or greater than
		// the total size, just delete the current data
		// and update the centerX and/or centerZ
		if (Math.abs(xOffset) >= width || Math.abs(zOffset) >= width)
		{
			for(int x = 0; x < width; x++)
			{
				for(int z = 0; z < width; z++)
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
		if(xOffset > 0)
		{
			// move everything over to the left (as the center moves to the right)
			for(int x = 0; x < width; x++)
			{
				for(int z = 0; z < width; z++)
				{
					if(x + xOffset < width)
						regions[x][z] = regions[x + xOffset][z];
					else
						regions[x][z] = null;
				}
			}
		}
		else
		{
			// move everything over to the right (as the center moves to the left)
			for(int x = width - 1; x >= 0; x--)
			{
				for(int z = 0; z < width; z++)
				{
					if(x + xOffset >= 0)
						regions[x][z] = regions[x + xOffset][z];
					else
						regions[x][z] = null;
				}
			}
		}
		
		
		
		// Z
		if(zOffset > 0)
		{
			// move everything up (as the center moves down)
			for(int x = 0; x < width; x++)
			{
				for(int z = 0; z < width; z++)
				{
					if(z + zOffset < width)
						regions[x][z] = regions[x][z + zOffset];
					else
						regions[x][z] = null;
				}
			}
		}
		else
		{
			// move everything down (as the center moves up)
			for(int x = 0; x < width; x++)
			{
				for(int z = width - 1; z >= 0; z--)
				{
					if(z + zOffset >= 0)
						regions[x][z] = regions[x][z + zOffset];
					else
						regions[x][z] = null;
				}
			}
		}
		
		
		
		// update the new center
		centerX += xOffset;
		centerZ += zOffset;
	}
	
	
	public int getCenterX()
	{
		return centerX;
	}
	
	public int getCenterZ()
	{
		return centerZ;
	}
	
	
	
	
	
	
	
	public LodRegion getRegion(int regionX, int regionZ)
	{
		int xIndex = (regionX - centerX) + halfWidth;
		int zIndex = (centerZ - regionZ) + halfWidth;
		
		if (xIndex < 0 || xIndex >= width || zIndex < 0 || zIndex >= width)
			// out of range
			return null;
		
		return regions[xIndex][zIndex];
	}
	
	/**
	 * Overwrite the LodRegion at the location of newRegion with newRegion.
	 */
	public void setRegion(LodRegion newRegion)
	{
		int xIndex = (newRegion.x - centerX) + halfWidth;
		int zIndex = (centerZ - newRegion.z) + halfWidth;
		
		if (xIndex < 0 || xIndex >= width || zIndex < 0 || zIndex >= width)
			// out of range
			// TODO, should this throw an exception?
			return;
		
		regions[xIndex][zIndex] = newRegion;
	}
	
	
	
	
	
	
	public void addLod(LodChunk lod)
	{
		int regionX = (lod.x + centerX) / LodRegion.SIZE;
		int regionZ = (lod.z + centerZ) / LodRegion.SIZE;
		
		// prevent issues if X/Z is negative and less than 16
		if (lod.x < 0)
		{
			regionX = (Math.abs(regionX) * -1) - 1; 
		}
		if (lod.z < 0)
		{
			regionZ = (Math.abs(regionZ) * -1) - 1; 
		}
		
		LodRegion region = getRegion(regionX, regionZ);
		
		if (region == null)
		{
			// if no region exists, create it
			region = new LodRegion(regionX, regionZ);
			setRegion(region);
		}
		
		region.addLod(lod);
	}
	
	
	/**
	 * Returns null if the LodChunk isn't loaded
	 */
	public LodChunk getLodFromCoordinates(int chunkX, int chunkZ)
	{
		// (chunkX + centerX) % width
		int regionX = (chunkX + centerX) / LodRegion.SIZE;
		int regionZ = (chunkZ + centerZ) / LodRegion.SIZE;
		
		// prevent issues if chunkX/Z is negative and less than width
		if (chunkX < 0)
		{
			regionX = (Math.abs(regionX) * -1) - 1; 
		}
		if (chunkZ < 0)
		{
			regionZ = (Math.abs(regionZ) * -1) - 1; 
		}
		
		LodRegion region = getRegion(regionX, regionZ);
		
		if(region == null)
			return null;
		
		return region.getLod(chunkX, chunkZ);
	}
}





