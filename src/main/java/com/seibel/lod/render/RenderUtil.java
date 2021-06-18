package com.seibel.lod.render;

import com.seibel.lod.enums.LodDetail;
import com.seibel.lod.enums.LodTemplate;
import com.seibel.lod.handlers.LodConfigHandler;

import net.minecraft.client.Minecraft;

/**
 * This holds miscellaneous helper code
 * to be used in the rendering process.
 * 
 * @author James Seibel
 * @version 6-17-2021
 */
public class RenderUtil
{
	private static final Minecraft mc = Minecraft.getInstance();
	
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
	
	
	/**
	 * Get how much buffer memory would be required for the given radius multiplier
	 */
	public static int getBufferMemoryForRadiusMultiplier(int radiusMultiplier)
	{ 
		int numbChunksWide = mc.gameSettings.renderDistanceChunks * 
							radiusMultiplier * 2;
		
		// calculate the max amount of buffer memory needed (in bytes)
		return numbChunksWide * numbChunksWide *
				LodConfigHandler.CLIENT.lodTemplate.get().
				getBufferMemoryForSingleLod(LodConfigHandler.CLIENT.lodDetail.get());
	}
	
	/**
	 * Returns the maxViewDistanceMultiplier for the given LodTemplate
	 * at the given LodDetail level.
	 */
	public static int getMaxRadiusMultiplierWithAvaliableMemory(LodTemplate lodTemplate, LodDetail lodDetail)
	{
		int maxNumberOfLods = LodRender.MAX_ALOCATEABLE_DIRECT_MEMORY / lodTemplate.getBufferMemoryForSingleLod(lodDetail); 
		int numbLodsWide = (int) Math.sqrt(maxNumberOfLods);
		
		return numbLodsWide / (2 * mc.gameSettings.renderDistanceChunks);
	}
}
