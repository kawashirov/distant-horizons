/*
 *    This file is part of the LOD Mod, licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.seibel.lod.render;

import com.seibel.lod.enums.LodDetail;
import com.seibel.lod.enums.LodTemplate;
import com.seibel.lod.handlers.LodConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.ChunkPos;

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
	 * Returns if the given ChunkPos is in the loaded area of the world.
	 * @param centerCoordinate the center of the loaded world (probably the player's ChunkPos)
	 */
	public static boolean isChunkPosInLoadedArea(ChunkPos pos, ChunkPos center)
	{
		Minecraft mc = Minecraft.getInstance();
		
		return (pos.x >= center.x - mc.options.renderDistance 
				&& pos.x <= center.x + mc.options.renderDistance) 
				&& 
				(pos.z >= center.z - mc.options.renderDistance 
				&& pos.z <= center.z + mc.options.renderDistance);
	}
	
	/**
	 * Returns if the given coordinate is in the loaded area of the world.
	 * @param centerCoordinate the center of the loaded world
	 */
	public static boolean isCoordinateInLoadedArea(int i, int j, int centerCoordinate)
	{
		Minecraft mc = Minecraft.getInstance();
		
		return (i >= centerCoordinate - mc.options.renderDistance 
				&& i <= centerCoordinate + mc.options.renderDistance) 
				&& 
				(j >= centerCoordinate - mc.options.renderDistance 
				&& j <= centerCoordinate + mc.options.renderDistance);
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
		int numbChunksWide = mc.options.renderDistance * 
							radiusMultiplier * 2;
		
		// calculate the max amount of buffer memory needed (in bytes)
		return numbChunksWide * numbChunksWide *
				LodConfig.CLIENT.lodTemplate.get().
				getBufferMemoryForSingleLod(LodConfig.CLIENT.lodDetail.get());
	}
	
	/**
	 * Returns the maxViewDistanceMultiplier for the given LodTemplate
	 * at the given LodDetail level.
	 */
	public static int getMaxRadiusMultiplierWithAvaliableMemory(LodTemplate lodTemplate, LodDetail lodDetail)
	{
		int maxNumberOfLods = LodRenderer.MAX_ALOCATEABLE_DIRECT_MEMORY / lodTemplate.getBufferMemoryForSingleLod(lodDetail); 
		int numbLodsWide = (int) Math.sqrt(maxNumberOfLods);
		
		return numbLodsWide / (2 * mc.options.renderDistance);
	}
}
