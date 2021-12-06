/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
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

package com.seibel.lod.core.render;

import com.seibel.lod.core.objects.math.Vec3f;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;

/**
 * This holds miscellaneous helper code
 * to be used in the rendering process.
 * 
 * @author James Seibel
 * @version 10-19-2021
 */
public class RenderUtil
{
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonHandler.get(IMinecraftRenderWrapper.class);
	
	
	/**
	 * Returns if the given ChunkPos is in the loaded area of the world.
	 * @param center the center of the loaded world (probably the player's ChunkPos)
	 */
	public static boolean isChunkPosInLoadedArea(AbstractChunkPosWrapper pos, AbstractChunkPosWrapper center)
	{
		return (pos.getX() >= center.getX() - MC_RENDER.getRenderDistance()
				&& pos.getX() <= center.getX() + MC_RENDER.getRenderDistance())
				&&
				(pos.getZ() >= center.getZ() - MC_RENDER.getRenderDistance()
				&& pos.getZ() <= center.getZ() + MC_RENDER.getRenderDistance());
	}
	
	/**
	 * Returns if the given coordinate is in the loaded area of the world.
	 * @param centerCoordinate the center of the loaded world
	 */
	public static boolean isCoordinateInLoadedArea(int x, int z, int centerCoordinate)
	{
		return (x >= centerCoordinate - MC_RENDER.getRenderDistance()
				&& x <= centerCoordinate + MC_RENDER.getRenderDistance())
				&&
				(z >= centerCoordinate - MC_RENDER.getRenderDistance()
						&& z <= centerCoordinate + MC_RENDER.getRenderDistance());
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
	 * Returns true if one of the region's 4 corners is in front
	 * of the camera.
	 */
	public static boolean isRegionInViewFrustum(AbstractBlockPosWrapper playerBlockPos, Vec3f cameraDir, AbstractBlockPosWrapper vboCenterPos)
	{
		// convert the vbo position into a direction vector
		// starting from the player's position
		Vec3f vboVec = new Vec3f(vboCenterPos.getX(), 0, vboCenterPos.getZ());
		Vec3f playerVec = new Vec3f(playerBlockPos.getX(), playerBlockPos.getY(), playerBlockPos.getZ());
		
		vboVec.subtract(playerVec);
		
		
		int halfRegionWidth = LodUtil.REGION_WIDTH / 2;
		
		// calculate the 4 corners
		Vec3f vboSeVec = new Vec3f(vboVec.x + halfRegionWidth, vboVec.y, vboVec.z + halfRegionWidth);
		Vec3f vboSwVec = new Vec3f(vboVec.x - halfRegionWidth, vboVec.y, vboVec.z + halfRegionWidth);
		Vec3f vboNwVec = new Vec3f(vboVec.x - halfRegionWidth, vboVec.y, vboVec.z - halfRegionWidth);
		Vec3f vboNeVec = new Vec3f(vboVec.x + halfRegionWidth, vboVec.y, vboVec.z - halfRegionWidth);
		
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
	private static boolean isNormalizedVectorInViewFrustum(Vec3f objectVector, Vec3f cameraDir)
	{
		// the -0.1 is to offer a slight buffer, so we are
		// more likely to render LODs and thus, hopefully prevent
		// flickering or odd disappearances
		return objectVector.dotProduct(cameraDir) > -0.1;
	}
}
