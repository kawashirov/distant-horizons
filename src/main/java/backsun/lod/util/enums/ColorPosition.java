package backsun.lod.util.enums;

/**
 * TOP, N, S, E, W, BOTTOM
 */
public enum ColorPosition
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
	
	private ColorPosition(int newValue)
	{
		index = newValue;
	}
}
