package backsun.lod.objects;

import java.awt.Color;

import backsun.lod.util.enums.ColorPosition;
import backsun.lod.util.enums.LodPosition;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

/**
 * This object contains position
 * and color data for an LOD object.
 * 
 * @author James Seibel
 * @version 09-28-2020
 */
public class LodChunk
{
	/** This is what separates each piece of data in the toData method */
	public static final char DATA_DELIMITER = ',';
	
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
		x = 0;
		z = 0;
		
		top = new short[4];
		bottom = new short[4];
		colors = new Color[6];
		
		// by default have the colors invisible
		for(ColorPosition p : ColorPosition.values())
		{
			colors[p.index] = new Color(0, 0, 0, 0);
		}
	}
	
	public LodChunk(int newX, int newZ, short[] newTop, short[] newBottom, Color newColors[])
	{
		x = newX;
		z = newZ;
		
		top = newTop;
		bottom = newBottom;
		colors = newColors;
	}
	
	public LodChunk(Chunk chunk)
	{
		x = chunk.x;
		z = chunk.z;
		
		top = new short[4];
		bottom = new short[4];
		colors = new Color[6];
		
		for(LodPosition p : LodPosition.values())
		{
			top[p.index] = generateLodSection(chunk, true, p);
			bottom[p.index] = generateLodSection(chunk, false, p);
		}
		
		// TODO determine the average color of each direction
		for(ColorPosition p : ColorPosition.values())
		{
			colors[p.index] = new Color(0, 0, 0, 0);
		}
	}
	
	
	
	/**
	 * Creates an LodChunk from the string
	 * created by the toData method.
	 * 
	 * @throws IllegalArgumentException if the data isn't valid to create a LodChunk
	 * @throws NumberFormatException if the data can't be converted into an int at any point
	 */
	public LodChunk(String data) throws IllegalArgumentException, NumberFormatException
	{
		/*
		 * data format:
		 * x, z, top data, bottom data, rgb color data
		 * 
		 * example:
		 * 5,8, 4,4,4,4, 0,0,0,0, 255,255,255, 255,255,255, 255,255,255, 255,255,255, 255,255,255, 255,255,255,
		 */
		
		
		
		// make sure there are the correct number of entries
		// in the data string (28)
		int count = 0;
		
		for(int i = 0; i < data.length(); i++)
		{
			if(data.charAt(i) == DATA_DELIMITER)
			{
				count++;
			}
		}
		
		if(count != 28)
		{
			throw new IllegalArgumentException("LodChunk constructor givin an invalid string. The data given had " + count + " delimiters when it should have had 28.");
		}
		
		
		
		// index we will use when going through the String
		int index = 0;
		int lastIndex = 0;
		
		
		
		// x and z position
		index = data.indexOf(DATA_DELIMITER, 0);
		x = Integer.parseInt(data.substring(0,index - 1));
		
		lastIndex = index;
		index = data.indexOf(DATA_DELIMITER, lastIndex);
		z = Integer.parseInt(data.substring(lastIndex,index - 1));
		
		
		
		// top
		for(LodPosition p : LodPosition.values())
		{
			lastIndex = index;
			index = data.indexOf(DATA_DELIMITER, lastIndex);
			
			top[p.index] = Short.parseShort(data.substring(lastIndex,index - 1));
		}
		
		
		// bottom
		for(LodPosition p : LodPosition.values())
		{
			lastIndex = index;
			index = data.indexOf(DATA_DELIMITER, lastIndex);
			
			bottom[p.index] = Short.parseShort(data.substring(lastIndex,index - 1));
		}
		
		
		// color
		for(ColorPosition p : ColorPosition.values())
		{
			int red = 0;
			int green = 0;
			int blue = 0;
			
			// get r,g,b
			for(int i = 0; i < 4; i++)
			{
				lastIndex = index;
				index = data.indexOf(DATA_DELIMITER, lastIndex);
				
				
				switch(i)
				{
				case 0:
					red = Short.parseShort(data.substring(lastIndex,index - 1));
				case 1:
					green = Short.parseShort(data.substring(lastIndex,index - 1));
				case 2:
					blue = Short.parseShort(data.substring(lastIndex,index - 1));
				}
			}
			
			colors[p.index] = new Color(red, green, blue);
		}
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
		// search through this layerfjfgfghj
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
	 * Outputs all data in csv format
	 * with the given delimiter.
	 * <br>
	 * Exports data in the form:
	 * <br>
	 * x, z, top data, bottom data, rgb color data
	 * 
	 * <br>
	 * example output:
	 * <br>
	 * 5,8, 4,4,4,4, 0,0,0,0 255,255,255, 255,255,255, 255,255,255, 255,255,255, 255,255,255, 255,255,255,
	 */
	public String toData()
	{
		String s = "";
		
		s += x + DATA_DELIMITER + z + DATA_DELIMITER;
		
		for(int i = 0; i < top.length; i++)
		{
			s += top[i] + DATA_DELIMITER;
		}
		
		for(int i = 0; i < bottom.length; i++)
		{
			s += bottom[i] + DATA_DELIMITER;
		}
		
		for(int i = 0; i < colors.length; i++)
		{
			s += colors[i].getRed() + DATA_DELIMITER + colors[i].getGreen() + DATA_DELIMITER + colors[i].getBlue() + DATA_DELIMITER;
		}
		
		return s;
	}
	
	
	@Override
	public String toString()
	{
		String s = "";
		
		s += "x: " + x + " z: " + z + "\t";
		
		s += "top: ";
		for(int i = 0; i < top.length; i++)
		{
			s += top[i] + " ";
		}
		s += "\t";
		
		s += "bottom: ";
		for(int i = 0; i < bottom.length; i++)
		{
			s += bottom[i] + " ";
		}
		s += "\t";
		
		s += "colors ";
		for(int i = 0; i < colors.length; i++)
		{
			s += "(" + colors[i].getRed() + ", " + colors[i].getGreen() + ", " + colors[i].getBlue() + "), ";
		}
		
		return s;
	}
}
