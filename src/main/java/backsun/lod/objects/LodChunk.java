package backsun.lod.objects;

import java.awt.Color;

import backsun.lod.util.enums.LodPosition;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

/**
 * This object contains position
 * and color data for an LOD object.
 * 
 * @author James Seibel
 * @version 09-27-2020
 */
public class LodChunk
{
	private final int CHUNK_DATA_WIDTH = 16;
	
	/**
	 * This is how many blocks are
	 * required at a specific y-value
	 * to constitute a LOD point
	 */
	private final int LOD_BLOCK_REQ = 16;
	// the max number of blocks per layer = 64 (8*8)
	// since each layer is 1/4 the chunk
	
	
	
	/** The x coordinate of the chunk. */
	public int x;
	/** The z coordinate of the chunk. */
	public int z;
	
	// each short is the height of
	// that 8th of the chunk.
	public short top[];
	public short bottom[];
	
	/** The average color of each 6 cardinal directions */
	public Color colors[];
	
	
	
	
	public LodChunk()
	{
		top = new short[4];
		bottom = new short[4];
		colors = new Color[6];
	}
	
	public LodChunk(short[] newTop, short[] newBottom, Color newColors[])
	{
		top = newTop;
		bottom = newBottom;
		colors = newColors;
	}
	
	public LodChunk(Chunk chunk)
	{
		top = new short[4];
		bottom = new short[4];
		colors = new Color[6];
		
		for(LodPosition p : LodPosition.values())
		{
			top[p.index] = generateLodSection(chunk, true, p);
			bottom[p.index] = generateLodSection(chunk, false, p);
		}
		
		// TODO determine the average color of each direction
//		for(ColorPosition p : ColorPosition.values())
//		{
//			
//		}
	}
	
	
	
	/**
	 * If invalid/null/empty chunks are given 
	 * crashes may occur.
	 */
	public short generateLodSection(Chunk chunk, boolean getTopSection, LodPosition pos)
	{
		// should have a length of 16
		// (each storage is 16x16x16 and the
		// world height is 256)
		ExtendedBlockStorage[] data = chunk.getBlockStorageArray();
		
		
		
		int startX = 0;
		int endX = 0;
		
		int startZ = 0;
		int endZ = 0;
		
		// determine where we should look in this
		// chunk
		switch(pos)
		{
			case NE:
				// -N
				startZ = 0;
				endZ = (CHUNK_DATA_WIDTH / 2) - 1;
				// +E
				startX = CHUNK_DATA_WIDTH / 2;
				endX = CHUNK_DATA_WIDTH - 1;
				break;
				
			case SE:
				// +S
				startZ = CHUNK_DATA_WIDTH / 2;
				endZ = CHUNK_DATA_WIDTH;
				// +E
				startX = CHUNK_DATA_WIDTH / 2;
				endX = CHUNK_DATA_WIDTH;
				break;
				
			case SW:
				// +S
				startZ = CHUNK_DATA_WIDTH / 2;
				endZ = CHUNK_DATA_WIDTH;
				// -W
				startX = 0;
				endX = (CHUNK_DATA_WIDTH / 2) - 1;
				break;
				
			case NW:
				// -N
				startZ = 0;
				endZ = CHUNK_DATA_WIDTH / 2;
				// -W
				startX = 0;
				endX = CHUNK_DATA_WIDTH / 2;
				break;
		}
		
		
		if(getTopSection)
			return determineTopPoint(data, startX, endX, startZ, endZ);
		else
			return determineBottomPoint(data, startX, endX, startZ, endZ);
	}
	
	private short determineBottomPoint(ExtendedBlockStorage[] data, int startX, int endX, int startZ, int endZ)
	{
		// search from the bottom up
		for(int i = 0; i < data.length; i++)
		{
			for(int y = 0; y < 16; y++)
			{
				
				if(isLayerValidLodPoint(data, startX, endX, startZ, endZ, i, y))
				{
					// we found
					// enough blocks in this
					// layer to count as an
					// LOD point
					return (short) (y + (i * 16));
				}
				
			} // y
		} // data
		
		
		// we never found a valid LOD point
		return -1;
	}
	
	private short determineTopPoint(ExtendedBlockStorage[] data, int startX, int endX, int startZ, int endZ)
	{
		// search from the top down
		for(int i = data.length - 1; i >= 0; i--)
		{
			for(int y = 15; y >= 0; y--)
			{
				
				if(isLayerValidLodPoint(data, startX, endX, startZ, endZ, i, y))
				{
					// we found
					// enough blocks in this
					// layer to count as an
					// LOD point
					return (short) (y + (i * 16));
				}
				
			} // y
		} // data
		
		
		// we never found a valid LOD point
		return -1;
	}
	
	/**
	 * Is the layer between the given X, Z, and dataIndex
	 * values a valid LOD point?
	 */
	private boolean isLayerValidLodPoint(
			ExtendedBlockStorage[] data, 
			int startX, int endX, 
			int startZ, int endZ, 
			int dataIndex, int y)
	{
		// search through this layer
		int layerBlocks = 0;
		
		for(int x = startX; x < endX; x++)
		{
			for(int z = startZ; z < endZ; z++)
			{
				if(data[dataIndex] == null)
				{
					// this section doesn't have any blocks
					// skip to the next data section
					return false;
				}
				else
				{
					if(data[dataIndex].get(x, y, z) != null && data[dataIndex].get(x, y, z).isOpaqueCube())
					{
						// we found a valid block in
						// in this layer
						layerBlocks++;
						
						if(layerBlocks >= LOD_BLOCK_REQ)
						{
							return true;
						}
					}
				}
				
			} // z
		} // x
		
		return false;
	}
	
	
	
	
	
	
	/**
	 * Similar to toString, but supports different delimiters,
	 * and has fewer overall characters ("top " is written as "t").
	 * <br>
	 * Exports data in the form "t", (top data), "b", (bottom data), "c", (rgb color data)
	 */
	public String toData(String delimiter, String endDelimiter)
	{
		String s = "";
		
		s += "t";
		for(int i = 0; i < top.length; i++)
		{
			s += top[i];
			
			if (i != top.length - 1)
			{
				// separate each item, except the
				// last item
				s += delimiter;
			}
		}
		
		
		s += "b";
		for(int i = 0; i < bottom.length; i++)
		{
			s += bottom[i];
			
			if (i != bottom.length - 1)
			{
				s += delimiter;
			}
		}
		
		
		s += "c";
		for(int i = 0; i < colors.length; i++)
		{
			s += colors[i].getRed() + delimiter + colors[i].getGreen() + delimiter + colors[i].getBlue();
			
			if (i != colors.length - 1)
			{
				s += delimiter;
			}
		}
		
		s += endDelimiter;
		
		return s;
	}
	
	
	@Override
	public String toString()
	{
		String s = "";
		
		s += "top: ";
		for(int i = 0; i < top.length; i++)
		{
			s += top[i];
			
			if (i != top.length - 1)
			{
				// separate each item, except the
				// last item
				s += " ";
			}
		}
		s += "\n";
		
		
		s += "bottom: ";
		for(int i = 0; i < bottom.length; i++)
		{
			s += bottom[i];
			
			if (i != bottom.length - 1)
			{
				s += " ";
			}
		}
		s += "\n";
		
		
		s += "colors ";
		for(int i = 0; i < colors.length; i++)
		{
			s += colors[i].getRed() + ", " + colors[i].getGreen() + ", " + colors[i].getBlue();
			
			if (i != colors.length - 1)
			{
				s += " ";
			}
		}
		
		return s;
	}
}
