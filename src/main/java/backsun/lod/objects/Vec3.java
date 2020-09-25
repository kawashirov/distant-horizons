package backsun.lod.objects;

/**
 * @author James Seibel
 * @version 09-25-2020
 */
public class Vec3
{
	public short x = 0;
	public short y = 0;
	public short z = 0;
	
	
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
