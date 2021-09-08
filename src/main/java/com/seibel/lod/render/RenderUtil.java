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

import com.seibel.lod.config.LodConfig;
import com.seibel.lod.enums.LodTemplate;
import com.seibel.lod.util.LodUtil;
import com.seibel.lod.wrappers.MinecraftWrapper;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.vector.Vector3d;

/**
 * This holds miscellaneous helper code
 * to be used in the rendering process.
 * 
 * @author James Seibel
 * @version 8-21-2021
 */
public class RenderUtil
{
	private static final MinecraftWrapper mc = MinecraftWrapper.INSTANCE;
	
	
	/**
	 * Returns if the given ChunkPos is in the loaded area of the world.
	 * @param centerCoordinate the center of the loaded world (probably the player's ChunkPos)
	 */
	public static boolean isChunkPosInLoadedArea(ChunkPos pos, ChunkPos center)
	{
		return (pos.x >= center.x - mc.getRenderDistance() 
				&& pos.x <= center.x + mc.getRenderDistance()) 
				&& 
				(pos.z >= center.z - mc.getRenderDistance() 
				&& pos.z <= center.z + mc.getRenderDistance());
	}
	
	/**
	 * Returns if the given coordinate is in the loaded area of the world.
	 * @param centerCoordinate the center of the loaded world
	 */
	public static boolean isCoordinateInLoadedArea(int x, int z, int centerCoordinate)
	{
		return (x >= centerCoordinate - mc.getRenderDistance() 
				&& x <= centerCoordinate + mc.getRenderDistance()) 
				&& 
				(z >= centerCoordinate - mc.getRenderDistance() 
				&& z <= centerCoordinate + mc.getRenderDistance());
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
	 * 
	 * issue #62
	 * TODO check if this is actually returning the correct memory needed
	 * it is possible (albeit unlikely) to have a buffer indexOutOfBounds exception
	 * which is caused by the buffer not being big enough.
	 */
	public static int getBufferMemoryForRegion()
	{
		// calculate the max amount of buffer memory needed (in bytes)
		return LodUtil.REGION_WIDTH_IN_CHUNKS * LodUtil.REGION_WIDTH_IN_CHUNKS *
				LodConfig.CLIENT.graphics.lodTemplate.get().getBufferMemoryForSingleLod();
	}
	
	/**
	 * Returns the maxViewDistanceMultiplier for the given LodTemplate
	 * at the given LodDetail level.
	 */
	public static int getMaxRadiusMultiplierWithAvaliableMemory(LodTemplate lodTemplate, int detailLevel)
	{
		int maxNumberOfLods = LodRenderer.MAX_ALOCATEABLE_DIRECT_MEMORY / lodTemplate.getBufferMemoryForSingleLod();
		int numbLodsWide = (int) Math.sqrt(maxNumberOfLods);
		
		return numbLodsWide / (2 * mc.getRenderDistance());
	}
	
	
	/**
	 * Returns true if one of the region's 4 corners is in front
	 * of the camera.
	 */
	public static boolean isRegionInViewFrustum(BlockPos playerBlockPos, Vector3d cameraDir, BlockPos vboCenterPos)
	{
    	// convert the vbo position into a direction vector
    	// starting from the player's position
		Vector3d vboVec = new Vector3d(vboCenterPos.getX(), 0, vboCenterPos.getZ());
		Vector3d playerVec = new Vector3d(playerBlockPos.getX(), playerBlockPos.getY(), playerBlockPos.getZ());
		Vector3d vboCenterVec = vboVec.subtract(playerVec);
		
		
    	int halfRegionWidth = LodUtil.REGION_WIDTH / 2;
    	
    	// calculate the 4 corners
    	Vector3d vboSeVec = new Vector3d(vboCenterVec.x + halfRegionWidth, vboCenterVec.y, vboCenterVec.z + halfRegionWidth);//.normalize();
    	Vector3d vboSwVec = new Vector3d(vboCenterVec.x - halfRegionWidth, vboCenterVec.y, vboCenterVec.z + halfRegionWidth);//.normalize();
    	Vector3d vboNwVec = new Vector3d(vboCenterVec.x - halfRegionWidth, vboCenterVec.y, vboCenterVec.z - halfRegionWidth);//.normalize();
    	Vector3d vboNeVec = new Vector3d(vboCenterVec.x + halfRegionWidth, vboCenterVec.y, vboCenterVec.z - halfRegionWidth);//.normalize();
    	
    	// if any corner is visible, this region should be rendered
    	return isNormalizedVectorInViewFrustum(vboSeVec, cameraDir) ||
    			isNormalizedVectorInViewFrustum(vboSwVec, cameraDir) ||
    			isNormalizedVectorInViewFrustum(vboNwVec, cameraDir) ||
    			isNormalizedVectorInViewFrustum(vboNeVec, cameraDir);
	}
    
	/**
	 * Currently takes the dot product of the two vectors,
	 * but in the future could do more complicated frustum culling tests.
	 */
    private static boolean isNormalizedVectorInViewFrustum(Vector3d objectVector, Vector3d cameraDir)
	{
    	// the -0.1 is to offer a slight buffer so we are
    	// more likely to render LODs and thus, hopefully prevent
    	// flickering or odd disappearences
    	return objectVector.dot(cameraDir) > -0.1;
	}
}
