package backsun.lod.proxy;

import net.minecraft.item.Item;

public class CommonProxy
{
	/**
	 * 
	 * @param item
	 * @param meta
	 * @param id
	 */
	public void registerItemRender(Item item, int meta, String id)
	{
		// nothing is needed here, only the clientProxy needs to have an implementation of this
	}
	
}
