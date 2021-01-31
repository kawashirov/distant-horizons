package backsun.lod.objects;

import backsun.lod.util.LodFileHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.DimensionType;

/**
 * This object holds all loaded LOD regions
 * for a given dimension.
 * 
 * @author James Seibel
 * @version 01-30-2021
 */
public class LodDimension
{
	public final DimensionType dimension;
	
	private int width; // if this ever changes make sure to update the halfWidth too
	private int halfWidth;
	
	public LodRegion regions[][];
	public boolean isRegionDirty[][];
	
	private int centerX;
	private int centerZ;
	
	private LodFileHandler rfHandler;
	
	public LodDimension(DimensionType newDimension, int newMaxWidth)
	{
		dimension = newDimension;
		width = newMaxWidth;
		
		// TODO what can be done if connected to a server?
		rfHandler = new LodFileHandler(Minecraft.getMinecraft().getIntegratedServer().getWorld(0).getSaveHandler(), this);
		
		regions = new LodRegion[width][width];
		isRegionDirty = new boolean[width][width];
		
		// populate isRegionDirty
		for(int i = 0; i < width; i++)
			for(int j = 0; j < width; j++)
				isRegionDirty[i][j] = false;
		
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
		int zIndex = (regionZ - centerZ) + halfWidth;
		
		if (!regionIsInRange(regionX, regionZ))
			// out of range
			return null;
		
		if (regions[xIndex][zIndex] == null)
		{
			regions[xIndex][zIndex] = getRegionFromFile(regionX, regionZ);
			if (regions[xIndex][zIndex] == null)
			{
				regions[xIndex][zIndex] = new LodRegion(regionX, regionZ);
			}
		}
		
		return regions[xIndex][zIndex];
	}
	
	/**
	 * Overwrite the LodRegion at the location of newRegion with newRegion.
	 * @throws ArrayIndexOutOfBoundsException if newRegion is outside what can be stored in this LodDimension.
	 */
	public void setRegion(LodRegion newRegion) throws ArrayIndexOutOfBoundsException
	{
		int xIndex = (newRegion.x - centerX) + halfWidth;
		int zIndex = (centerZ - newRegion.z) + halfWidth;
		
		if (!regionIsInRange(newRegion.x, newRegion.z))
			// out of range
			throw new ArrayIndexOutOfBoundsException();
		
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
		
		// don't continue if the region can't be saved
		if (!regionIsInRange(regionX, regionZ))
			return;
		
		LodRegion region = getRegion(regionX, regionZ);
		
		if (region == null)
		{
			// if no region exists, create it
			region = new LodRegion(regionX, regionZ);
			setRegion(region);
		}
		
		region.addLod(lod);
		
		// mark the region as dirty so it will be saved to disk
		int xIndex = (regionX - centerX) + halfWidth;
		int zIndex = (regionZ - centerZ) + halfWidth;
		isRegionDirty[xIndex][zIndex] = true;
		
		
		
		rfHandler.saveDirtyRegionsToFile();
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
	
	
	
	
	public LodRegion getRegionFromFile(int regionX, int regionZ)
	{
		return rfHandler.loadRegionFromFile(regionX, regionZ);
	}
	
	
	/**
	 * Returns whether the region at the given X and Z coordinates
	 * is within the loaded range.
	 */
	private boolean regionIsInRange(int regionX, int regionZ)
	{
		int xIndex = (regionX - centerX) + halfWidth;
		int zIndex = (regionZ - centerZ) + halfWidth;
		
		return xIndex >= 0 && xIndex < width && zIndex >= 0 && zIndex < width;
	}

	
	
	public int getWidth()
	{
		return width;
	}
}





