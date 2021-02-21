package com.backsun.lod.renderer;

import net.minecraft.client.Minecraft;

/**
 * This holds miscellaneous helper code
 * to be used in the rendering process.
 * 
 * @author James Seibel
 * @version 2-13-2021
 */
public class RenderUtil
{
	/**
	 * Returns if the given coordinate is in the loaded area of the world.
	 * @param centerCoordinate the center of the loaded world
	 */
	public static boolean isCoordinateInLoadedArea(int i, int j, int centerCoordinate)
	{
		Minecraft mc = Minecraft.getInstance();
		
		return (i >= centerCoordinate - mc.gameSettings.renderDistanceChunks 
				&& i <= centerCoordinate + mc.gameSettings.renderDistanceChunks) 
				&& 
				(j >= centerCoordinate - mc.gameSettings.renderDistanceChunks 
				&& j <= centerCoordinate + mc.gameSettings.renderDistanceChunks);
	}
	
	
	/**
	 * Find the coordinates that are in the center half of the given
	 * 2D matrix, starting at (0,0) and going to (2 * lodRadius, 2 * lodRadius).
	 */
	public static boolean isCoordinateInNearFogArea(int i, int j, int lodRadius)
	{
		int halfRadius = lodRadius / 2;
		
		return (i >= lodRadius - halfRadius 
				&& i <= lodRadius + halfRadius) 
				&& 
				(j >= lodRadius - halfRadius
				&& j <= lodRadius + halfRadius);
	}
	
}
