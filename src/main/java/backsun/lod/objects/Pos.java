package backsun.lod.objects;

/**
 * Position: NE, SE, SW, NW
 * <br>
 * Color: TOP, N, S, E, W, BOTTOM
 */
public enum Pos
{
	// used for position
	NE(0),
	SE(1),
	SW(2),
	NW(3),
	
	// used for colors
	TOP(0),
	N(1),
	S(2),
	E(3),
	W(4),
	BOTTOM(5);
	
	public final int index;
	
	private Pos(int newValue)
	{
		index = newValue;
	}
}
