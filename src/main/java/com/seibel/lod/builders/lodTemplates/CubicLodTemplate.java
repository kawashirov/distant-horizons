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
package com.seibel.lod.builders.lodTemplates;

import java.util.Map;

import com.seibel.lod.enums.DebugMode;
import com.seibel.lod.util.DataPointUtil;
import com.seibel.lod.util.LodUtil;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

/**
 * Builds LODs as rectangular prisms.
 *
 * @author James Seibel
 * @version 8-10-2021
 */
public class CubicLodTemplate extends AbstractLodTemplate
{

	public CubicLodTemplate()
	{

	}

	@Override
	public void addLodToBuffer(BufferBuilder buffer, BlockPos bufferCenterBlockPos, long data, Map<Direction, long[]> adjData,
	                           byte detailLevel, int posX, int posZ, Box box, DebugMode debugging, NativeImage lightMap)
	{
		int width = 1 << detailLevel;

		int color = DataPointUtil.getLightColor(data, lightMap);
		// add each LOD for the detail level

		if (debugging != DebugMode.OFF)

		{
			color = LodUtil.DEBUG_DETAIL_LEVEL_COLORS[detailLevel].getRGB();
		}

		generateBoundingBox(
				box,
				DataPointUtil.getHeight(data),
				DataPointUtil.getDepth(data),
				width,
				posX * width,
				0,
				posZ * width,
				bufferCenterBlockPos,
				adjData,
				color);

		if (box != null)

		{
			addBoundingBoxToBuffer(buffer, box);
		}

	}

	private void generateBoundingBox(Box box, int height, int depth, int width, double xOffset, double yOffset, double zOffset, BlockPos bufferCenterBlockPos, Map<Direction, long[]> adjData, int color)
	{
		// don't add an LOD if it is empty
		if (height == -1 && depth == -1)
			return;

		if (depth == height)
		{
			// if the top and bottom points are at the same height
			// render this LOD as 1 block thick
			height++;
		}

		// offset the AABB by it's x/z position in the world since
		// it uses doubles to specify its location, unlike the model view matrix
		// which only uses floats
		double x = -bufferCenterBlockPos.getX();
		double z = -bufferCenterBlockPos.getZ();
		box.reset();
		box.setColor(color);
		box.set(width, height - depth, width);
		box.move((int) (xOffset + x), (int) (depth + yOffset), (int) (zOffset + z));
		box.setUpCulling(32, bufferCenterBlockPos);
		box.setAdjData(adjData);
	}

	private void addBoundingBoxToBuffer(BufferBuilder buffer, Box box)
	{
		for (Direction direction : Box.DIRECTIONS)
		{
			//if(box.isCulled(direction))
			//	continue;
			int adjIndex = 0;
			while (box.shouldContinue(direction, adjIndex))
			{
				for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++)
				{
					addPosAndColor(buffer,
							box.getX(direction, vertexIndex),
							box.getY(direction, vertexIndex, adjIndex),
							box.getZ(direction, vertexIndex),
							box.getColor(direction));
				}
				adjIndex++;
			}
		}
	}

	@Override
	public int getBufferMemoryForSingleNode(int maxVerticalData)
	{
		// (sidesOnACube * pointsInASquare * (positionPoints + colorPoints)))
		return (2 * 4 * (3 + 4)) + (4 * 4 * (maxVerticalData + 1) * (3 + 4));
	}

}
