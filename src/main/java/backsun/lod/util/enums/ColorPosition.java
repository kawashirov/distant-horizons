package backsun.lod.util.enums;

/**
 * TOP, N, S, E, W, BOTTOM
 */
public enum ColorPosition
{
	// used for colors
	TOP(0),
	N(1),
	S(2),
	E(3),
	W(4),
	BOTTOM(5);
	
	public final int index;
	
	private ColorPosition(int newValue)
	{
		index = newValue;
	}
}
