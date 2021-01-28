package backsun.lod.util.enums;

/**
 * @author James Seibel
 * @version 1-23-2021
 */
public enum DrawMode
{
	/** Draw the LOD objects in groups.
	 * <br>
	 * <br>
	 * Fancy fog: render the center and outside LOD
	 * objects in 2 different groups.
	 * <br>
	 * Fast fog: render all LOD objects at one time. 
	 */
	BATCH(0),
	
	/** Draw each LOD objects separately.
	 * <br>
	 * <br>
	 * Not suggested normally since draw calls are GPU expensive.
	 */
	INDIVIDUAL(5);
	
	public final int value;
	
	private DrawMode(int newValue)
	{
		value = newValue;
	}
}
