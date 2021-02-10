package com.backsun.lod.renderer;

import net.minecraft.client.Minecraft;

/**
 * This holds miscellaneous helper code
 * to be used in the rendering process.
 * 
 * @author James Seibel
 * @version 2-10-2021
 */
public class RenderUtil
{
	/**
	 * Returns if the given coordinate is in the loaded area of the world.
	 * @param middle the center of the loaded world
	 */
	public static boolean isCoordinateInLoadedArea(int x, int z, int middle)
	{
		Minecraft mc = Minecraft.getMinecraft();
		
		return (x >= middle - mc.gameSettings.renderDistanceChunks 
				&& x <= middle + mc.gameSettings.renderDistanceChunks) 
				&& 
				(z >= middle - mc.gameSettings.renderDistanceChunks 
				&& z <= middle + mc.gameSettings.renderDistanceChunks);
	}
}
