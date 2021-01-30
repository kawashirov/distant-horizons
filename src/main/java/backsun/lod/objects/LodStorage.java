package backsun.lod.objects;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * This stores all LODs for a given world.
 * 
 * @author James Seibel
 * @version 01-30-2021
 */
public class LodStorage
{
	/**
	 * Key = Dimension id (as an int)
	 */
	private Dictionary<Integer, LodDimensionalStorage> LodDimensions;
	
	public String worldName;
	
	
	public LodStorage()
	{
		LodDimensions = new Hashtable<Integer, LodDimensionalStorage>();
	}
	
	
	
	public void addLodDimensionalStorage(LodDimensionalStorage newStorage)
	{
		LodDimensions.put(newStorage.dimension.getId(), newStorage);
	}
	
	public LodDimensionalStorage getLodDimensionalStorage(int dimensionId)
	{
		return LodDimensions.get(dimensionId);
	}
}
