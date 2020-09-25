package backsun.lod.util.enums;

public enum LodPosition
{
	// used for position
	NE(0), // -Z, +X
	SE(1), // +Z, +X
	SW(2), // +Z, -X
	NW(3); // -Z, -X
	
	public final int index;
	
	private LodPosition(int newValue)
	{
		index = newValue;
	}
}
