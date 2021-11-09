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

package com.seibel.lod.builders.bufferBuilding.lodTemplates;

import java.util.Map;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.seibel.lod.enums.DebugMode;
import com.seibel.lod.util.ColorUtil;
import com.seibel.lod.util.DataPointUtil;
import com.seibel.lod.util.LodUtil;
import com.seibel.lod.wrappers.Block.BlockPosWrapper;

import net.minecraft.core.Direction;

/**
 * Builds LODs as rectangular prisms.
 * @author James Seibel
 * @version 11-8-2021
 */
public class CubicLodTemplate extends AbstractLodTemplate
{
	
	public CubicLodTemplate()
	{
	
	}
	
	@Override
	public void addLodToBuffer(BufferBuilder buffer, BlockPosWrapper bufferCenterBlockPos, long data, Map<Direction, long[]> adjData,
			byte detailLevel, int posX, int posZ, Box box, DebugMode debugging, boolean[] adjShadeDisabled)
	{
		if (box == null)
			return;
		
		// equivalent to 2^detailLevel
		int blockWidth = 1 << detailLevel;
		
		int color;
		if (debugging != DebugMode.OFF)
			color = LodUtil.DEBUG_DETAIL_LEVEL_COLORS[detailLevel].getRGB();
		else
			color = DataPointUtil.getColor(data);
		
		
		generateBoundingBox(
				box,
				DataPointUtil.getHeight(data),
				DataPointUtil.getDepth(data),
				blockWidth,
				posX * blockWidth, 0, posZ * blockWidth, // x, y, z offset
				bufferCenterBlockPos,
				adjData,
				color,
				DataPointUtil.getLightSkyAlt(data),
				DataPointUtil.getLightBlock(data),
				adjShadeDisabled);
		
		addBoundingBoxToBuffer(buffer, box);
	}
	
	private void generateBoundingBox(Box box,
			int height, int depth, int width,
			double xOffset, double yOffset, double zOffset,
			BlockPosWrapper bufferCenterBlockPos,
			Map<Direction, long[]> adjData,
			int color,
			int skyLight,
			int blockLight,
			boolean[] adjShadeDisabled)
	{
		// don't add an LOD if it is empty
		if (height == -1 && depth == -1)
			return;
		
		if (depth == height)
			// if the top and bottom points are at the same height
			// render this LOD as 1 block thick
			height++;
		
		// offset the AABB by its x/z position in the world since
		// it uses doubles to specify its location, unlike the model view matrix
		// which only uses floats
		double x = -bufferCenterBlockPos.getX();
		double z = -bufferCenterBlockPos.getZ();
		box.reset();
		box.setColor(color, adjShadeDisabled);
		box.setLights(skyLight, blockLight);
		box.setWidth(width, height - depth, width);
		box.setOffset((int) (xOffset + x), (int) (depth + yOffset), (int) (zOffset + z));
		box.setUpCulling(32, bufferCenterBlockPos);
		box.setAdjData(adjData);
	}
	
	private void addBoundingBoxToBuffer(BufferBuilder buffer, Box box)
	{
		int color;
		int skyLight;
		int blockLight;
		for (Direction direction : Box.DIRECTIONS)
		{
			if(box.isCulled(direction))
				continue;
			
			int verticalFaceIndex = 0;
			while (box.shouldRenderFace(direction, verticalFaceIndex))
			{
				for (int vertexIndex = 0; vertexIndex < 6; vertexIndex++)
				{
					color = box.getColor(direction);
					skyLight = box.getSkyLight(direction, verticalFaceIndex);
					blockLight = box.getBlockLight();
					color = ColorUtil.applyLightValue(color, skyLight, blockLight);
					addPosAndColor(buffer,
							box.getX(direction, vertexIndex),
							box.getY(direction, vertexIndex, verticalFaceIndex),
							box.getZ(direction, vertexIndex),
							color);
				}
				verticalFaceIndex++;
			}
		}
	}
	
}
