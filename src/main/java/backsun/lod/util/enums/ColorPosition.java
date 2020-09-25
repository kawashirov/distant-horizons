package backsun.lod.util.enums;

/**
 * TOP, N, S, E, W, BOTTOM
 */
public enum ColorPosition
{
	// used for colors
	TOP(0),		// +Y
	
	N(1),		// -Z
	S(2),		// +Z
	
	E(3),		// +X
	W(4), 		// -X
	
	BOTTOM(5);	// -Y
	
	public final int index;
	
	private ColorPosition(int newValue)
	{
		index = newValue;
	}
}
