package backsun.lod.util.enums;

public enum LodPosition
{
	// used for position
	NE(0),
	SE(1),
	SW(2),
	NW(3);
	
	public final int index;
	
	private LodPosition(int newValue)
	{
		index = newValue;
	}
}
