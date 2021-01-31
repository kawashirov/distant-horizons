package backsun.lod.objects;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * This stores all LODs for a given world.
 * 
 * @author James Seibel
 * @version 01-30-2021
 */
public class LodWorld
{
	public String worldName;
	
	/**
	 * Key = Dimension id (as an int)
	 */
	private Dictionary<Integer, LodDimension> LodDimensions;
	
	
	public LodWorld(String newWorldName)
	{
		worldName = newWorldName;
		LodDimensions = new Hashtable<Integer, LodDimension>();
	}
	
	
	
	public void addLodDimension(LodDimension newStorage)
	{
		LodDimensions.put(newStorage.dimension.getId(), newStorage);
	}
	
	public LodDimension getLodDimension(int dimensionId)
	{
		return LodDimensions.get(dimensionId);
	}
}
