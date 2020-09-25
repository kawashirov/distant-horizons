package backsun.lod.objects;

/**
 * @author James Seibel
 * @version 09-25-2020
 */
public class Vec3
{
	public short x;
	public short y;
	public short z;
	
	
	
	public Vec3()
	{
		x = 0;
		y = 0;
		z = 0;
	}
	
	public Vec3(int newX, int newY, int newZ)
	{
		x = (short) newX;
		y = (short) newY;
		z = (short) newZ;
	}
	
	
	
	/**
	 * Exports data in the form:
	 * <br>
	 * x, y, z
	 */
	public String toData(String delimiter, String endDelimiter)
	{
		return x + delimiter + y + delimiter + z + endDelimiter;
	}
	
	@Override
	public String toString()
	{
		return "x: " + x + " y: " + y + " z: " + z;
	}
}
