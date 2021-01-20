package backsun.lod.util.enums;

/**
 * 
 * @author James Seibel
 * @version 1-20-2020
 * 
 * NE, SE, SW, NW
 */
public enum LodLocation
{
	// used for position
	
	/** -Z, +X */
	NE(0),
	/** +Z, +X */
	SE(1),
	/** +Z, -X */
	SW(2),
	/** -Z, -X */
	NW(3);
	
	public final int value;
	
	private LodLocation(int newValue)
	{
		value = newValue;
	}
}
