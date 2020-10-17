package backsun.lod.util.enums;

/**
 * @author James Seibel
 * @version 10-17-2020
 * 
 * TOP, N, S, E, W, BOTTOM
 */
public enum ColorDirection
{
	// used for colors
	/** +Y */
	TOP(0),
	
	/** -Z */
	N(1),
	/** +Z */
	S(2),
	
	/** +X */
	E(3),
	/** -X */
	W(4),
	
	/** -Y */
	BOTTOM(5);
	
	public final int index;
	
	private ColorDirection(int newValue)
	{
		index = newValue;
	}
}
