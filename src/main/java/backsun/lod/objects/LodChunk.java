package backsun.lod.objects;

import java.awt.Color;

/**
 * This object contains position
 * and color data for an LOD object.
 * 
 * @author James Seibel
 * @version 09-25-2020
 */
public class LodChunk
{
	/** The x coordinate of the chunk. */
	public int x;
	/** The z coordinate of the chunk. */
	public int z;
	
	// each Vec3 is the average location of
	// 8th of the chunk.
	public Vec3 top[];
	public Vec3 bottom[];
	
	/** The average color of each 6 cardinal directions */
	public Color colors[];
	
	
	
	
	public LodChunk()
	{
		top = new Vec3[4];
		bottom = new Vec3[4];
		colors = new Color[6];
	}
	
	public LodChunk(Vec3[] newTop, Vec3[] newBottom, Color newColors[])
	{
		top = newTop;
		bottom = newBottom;
		colors = newColors;
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
			s += top[i].toData(delimiter,delimiter);
			
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
			s += bottom[i].toData(delimiter,delimiter);
			
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
			s += top[i].toString();
			
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
			s += bottom[i].toString();
			
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
