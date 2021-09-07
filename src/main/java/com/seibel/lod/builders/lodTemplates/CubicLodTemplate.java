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

import com.seibel.lod.config.LodConfig;
import com.seibel.lod.enums.DebugMode;
import com.seibel.lod.enums.ShadingMode;
import com.seibel.lod.objects.DataPoint;
import com.seibel.lod.util.ColorUtil;
import com.seibel.lod.util.LodUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
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
	private final int CULL_OFFSET = 16;

	public CubicLodTemplate()
	{

	}

	@Override
	public void addLodToBuffer(BufferBuilder buffer, BlockPos bufferCenterBlockPos, long data, long[] adjData,
	                           byte detailLevel, int posX, int posZ, Box box, DebugMode debugging)
	{
		int width = 1 << detailLevel;

		// add each LOD for the detail level
		generateBoundingBox(
				box,
				DataPoint.getHeight(data),
				DataPoint.getDepth(data),
				width,
				posX * width,
				0,
				posZ * width,
				bufferCenterBlockPos);

		int color = DataPoint.getColor(data);
		if (debugging != DebugMode.OFF)
		{
			color = LodUtil.DEBUG_DETAIL_LEVEL_COLORS[detailLevel].getRGB();
		}

		if (box != null)
		{
			addBoundingBoxToBuffer(buffer, box, color, bufferCenterBlockPos, adjData);
		}

	}

	private void generateBoundingBox(Box box, int height, int depth, int width, double xOffset, double yOffset, double zOffset, BlockPos bufferCenterBlockPos)
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
		double z = -bufferCenterBlockPos.getZ();;
		box.set(width, height - depth, width);
		box.move((int) (xOffset + x), (int) (yOffset + depth), (int) (zOffset + z));
	}

	private void addBoundingBoxToBuffer(BufferBuilder buffer, Box box, int c, BlockPos playerBlockPos, long[] adjData)
	{
		int topColor = c;
		int bottomColor = c;
		int northColor = c;
		int southColor = c;
		int westColor = c;
		int eastColor = c;

		// darken the bottom and side colors if requested
		if (LodConfig.CLIENT.graphics.shadingMode.get() == ShadingMode.DARKEN_SIDES)
		{
			// the side colors are different because
			// when using fast lighting in Minecraft the north/south
			// and east/west sides are different in a similar way
			/**TODO OPTIMIZE THIS STEP*/
			Minecraft mc = Minecraft.getInstance();
			topColor = ColorUtil.applyShade(c, mc.level.getShade(Direction.UP, true));
			bottomColor = ColorUtil.applyShade(c, mc.level.getShade(Direction.DOWN, true));
			northColor = ColorUtil.applyShade(c, mc.level.getShade(Direction.NORTH, true));
			southColor = ColorUtil.applyShade(c, mc.level.getShade(Direction.SOUTH, true));
			westColor = ColorUtil.applyShade(c, mc.level.getShade(Direction.WEST, true));
			eastColor = ColorUtil.applyShade(c, mc.level.getShade(Direction.EAST, true));
		}

		// apply the user specified saturation and brightness
		float saturationMultiplier = LodConfig.CLIENT.graphics.saturationMultiplier.get().floatValue();
		float brightnessMultiplier = LodConfig.CLIENT.graphics.brightnessMultiplier.get().floatValue();

		if (saturationMultiplier != 1 || brightnessMultiplier != 1)
		{
			topColor = ColorUtil.applySaturationAndBrightnessMultipliers(topColor, saturationMultiplier, brightnessMultiplier);
			bottomColor = ColorUtil.applySaturationAndBrightnessMultipliers(bottomColor, saturationMultiplier, brightnessMultiplier);
			northColor = ColorUtil.applySaturationAndBrightnessMultipliers(northColor, saturationMultiplier, brightnessMultiplier);
			southColor = ColorUtil.applySaturationAndBrightnessMultipliers(southColor, saturationMultiplier, brightnessMultiplier);
			westColor = ColorUtil.applySaturationAndBrightnessMultipliers(westColor, saturationMultiplier, brightnessMultiplier);
			eastColor = ColorUtil.applySaturationAndBrightnessMultipliers(eastColor, saturationMultiplier, brightnessMultiplier);
		}
		int minX;
		int maxX;
		int minY;
		int maxY;
		int minZ;
		int maxZ;
		long data;
		int tempMinY;
		int tempMaxY;

		int red;
		int green;
		int blue;
		int alpha;
		boolean disableCulling = true;
		/**TODO make all of this more automatic if possible*/
		if (playerBlockPos.getY() > box.getMaxY() - CULL_OFFSET || disableCulling)
		{
			red = ColorUtil.getRed(topColor);
			green = ColorUtil.getGreen(topColor);
			blue = ColorUtil.getBlue(topColor);
			alpha = ColorUtil.getAlpha(topColor);
			// top (facing up)
			minX = box.getMinX();
			maxX = box.getMaxX();
			minY = box.getMinY();
			maxY = box.getMaxY();
			minZ = box.getMinZ();
			maxZ = box.getMaxZ();
			addPosAndColor(buffer, minX, maxY, minZ, red, green, blue, alpha);
			addPosAndColor(buffer, minX, maxY, maxZ, red, green, blue, alpha);
			addPosAndColor(buffer, maxX, maxY, maxZ, red, green, blue, alpha);
			addPosAndColor(buffer, maxX, maxY, minZ, red, green, blue, alpha);
		}
		if (playerBlockPos.getY() < box.getMinY() + CULL_OFFSET || disableCulling)
		{
			red = ColorUtil.getRed(bottomColor);
			green = ColorUtil.getGreen(bottomColor);
			blue = ColorUtil.getBlue(bottomColor);
			alpha = ColorUtil.getAlpha(bottomColor);
			// bottom (facing down)
			minX = box.getMinX();
			maxX = box.getMaxX();
			minY = box.getMinY();
			maxY = box.getMaxY();
			minZ = box.getMinZ();
			maxZ = box.getMaxZ();
			addPosAndColor(buffer, maxX, minY, minZ, red, green, blue, alpha);
			addPosAndColor(buffer, maxX, minY, maxZ, red, green, blue, alpha);
			addPosAndColor(buffer, minX, minY, maxZ, red, green, blue, alpha);
			addPosAndColor(buffer, minX, minY, minZ, red, green, blue, alpha);
		}

		if (playerBlockPos.getX() < box.getMaxX() + CULL_OFFSET || disableCulling)
		{
			red = ColorUtil.getRed(westColor);
			green = ColorUtil.getGreen(westColor);
			blue = ColorUtil.getBlue(westColor);
			alpha = ColorUtil.getAlpha(westColor);
			// west (facing -X)
			data = adjData[0];

			minX = box.getMinX();
			maxX = box.getMaxX();
			minY = box.getMinY();
			maxY = box.getMaxY();
			minZ = box.getMinZ();
			maxZ = box.getMaxZ();
			if (data == 0)
			{
				addPosAndColor(buffer, minX, minY, minZ, red, green, blue, alpha);
				addPosAndColor(buffer, minX, minY, maxZ, red, green, blue, alpha);
				addPosAndColor(buffer, minX, maxY, maxZ, red, green, blue, alpha);
				addPosAndColor(buffer, minX, maxY, minZ, red, green, blue, alpha);
			}else
			{
				maxY = box.getMaxY();
				tempMaxY = DataPoint.getHeight(data);
				if (tempMaxY < maxY)
				{
					minY = Math.max(tempMaxY, minY);
					addPosAndColor(buffer, minX, minY, minZ, red, green, blue, alpha);
					addPosAndColor(buffer, minX, minY, maxZ, red, green, blue, alpha);
					addPosAndColor(buffer, minX, maxY, maxZ, red, green, blue, alpha);
					addPosAndColor(buffer, minX, maxY, minZ, red, green, blue, alpha);
				}
				tempMinY = DataPoint.getDepth(data);
				minY = box.getMinY();
				if (tempMinY > minY)
				{
					maxY = Math.min(tempMinY, maxX);
					addPosAndColor(buffer, minX, minY, minZ, red, green, blue, alpha);
					addPosAndColor(buffer, minX, minY, maxZ, red, green, blue, alpha);
					addPosAndColor(buffer, minX, maxY, maxZ, red, green, blue, alpha);
					addPosAndColor(buffer, minX, maxY, minZ, red, green, blue, alpha);
				}
			}
		}

		if (playerBlockPos.getX() > box.getMinX() - CULL_OFFSET || disableCulling)
		{
			red = ColorUtil.getRed(eastColor);
			green = ColorUtil.getGreen(eastColor);
			blue = ColorUtil.getBlue(eastColor);
			alpha = ColorUtil.getAlpha(eastColor);
			// east (facing +X)
			data = adjData[1];
			minX = box.getMinX();
			maxX = box.getMaxX();
			minY = box.getMinY();
			maxY = box.getMaxY();
			minZ = box.getMinZ();
			maxZ = box.getMaxZ();
			if (data == 0)
			{
				addPosAndColor(buffer, maxX, maxY, minZ, red, green, blue, alpha);
				addPosAndColor(buffer, maxX, maxY, maxZ, red, green, blue, alpha);
				addPosAndColor(buffer, maxX, minY, maxZ, red, green, blue, alpha);
				addPosAndColor(buffer, maxX, minY, minZ, red, green, blue, alpha);
			}
			else
			{
				maxY = box.getMaxY();
				tempMaxY = DataPoint.getHeight(data);
				if (tempMaxY < maxY)
				{
					minY = Math.max(tempMaxY, minY);
					addPosAndColor(buffer, maxX, maxY, minZ, red, green, blue, alpha);
					addPosAndColor(buffer, maxX, maxY, maxZ, red, green, blue, alpha);
					addPosAndColor(buffer, maxX, minY, maxZ, red, green, blue, alpha);
					addPosAndColor(buffer, maxX, minY, minZ, red, green, blue, alpha);
				}
				tempMinY = DataPoint.getDepth(data);
				minY = box.getMinY();
				if (tempMinY > minY)
				{
					maxY = Math.min(tempMinY, maxX);
					addPosAndColor(buffer, maxX, maxY, minZ, red, green, blue, alpha);
					addPosAndColor(buffer, maxX, maxY, maxZ, red, green, blue, alpha);
					addPosAndColor(buffer, maxX, minY, maxZ, red, green, blue, alpha);
					addPosAndColor(buffer, maxX, minY, minZ, red, green, blue, alpha);
				}
			}
		}

		if (playerBlockPos.getZ() > box.getMinZ() - CULL_OFFSET || disableCulling)
		{
			red = ColorUtil.getRed(northColor);
			green = ColorUtil.getGreen(northColor);
			blue = ColorUtil.getBlue(northColor);
			alpha = ColorUtil.getAlpha(northColor);
			data = adjData[3];
			minX = box.getMinX();
			maxX = box.getMaxX();
			minY = box.getMinY();
			maxY = box.getMaxY();
			minZ = box.getMinZ();
			maxZ = box.getMaxZ();
			// north (facing +Z)
			if (data == 0)
			{
				addPosAndColor(buffer, maxX, minY, maxZ, red, green, blue, alpha);
				addPosAndColor(buffer, maxX, maxY, maxZ, red, green, blue, alpha);
				addPosAndColor(buffer, minX, maxY, maxZ, red, green, blue, alpha);
				addPosAndColor(buffer, minX, minY, maxZ, red, green, blue, alpha);
			}
			else
			{
				maxY = box.getMaxY();
				tempMaxY = DataPoint.getHeight(data);
				if (tempMaxY < maxY)
				{
					minY = Math.max(tempMaxY, minY);
					addPosAndColor(buffer, maxX, minY, maxZ, red, green, blue, alpha);
					addPosAndColor(buffer, maxX, maxY, maxZ, red, green, blue, alpha);
					addPosAndColor(buffer, minX, maxY, maxZ, red, green, blue, alpha);
					addPosAndColor(buffer, minX, minY, maxZ, red, green, blue, alpha);
				}
				tempMinY = DataPoint.getDepth(data);
				minY = box.getMinY();
				if (tempMinY > minY)
				{
					maxY = Math.min(tempMinY, maxX);
					addPosAndColor(buffer, maxX, minY, maxZ, red, green, blue, alpha);
					addPosAndColor(buffer, maxX, maxY, maxZ, red, green, blue, alpha);
					addPosAndColor(buffer, minX, maxY, maxZ, red, green, blue, alpha);
					addPosAndColor(buffer, minX, minY, maxZ, red, green, blue, alpha);
				}
			}
		}

		if (playerBlockPos.getZ() < box.getMaxZ() + CULL_OFFSET || disableCulling)
		{
			red = ColorUtil.getRed(southColor);
			green = ColorUtil.getGreen(southColor);
			blue = ColorUtil.getBlue(southColor);
			alpha = ColorUtil.getAlpha(southColor);
			data = adjData[2];
			minX = box.getMinX();
			maxX = box.getMaxX();
			minY = box.getMinY();
			maxY = box.getMaxY();
			minZ = box.getMinZ();
			maxZ = box.getMaxZ();
			// south (facing -Z)
			if (data == 0)
			{
				addPosAndColor(buffer, minX, minY, minZ, red, green, blue, alpha);
				addPosAndColor(buffer, minX, maxY, minZ, red, green, blue, alpha);
				addPosAndColor(buffer, maxX, maxY, minZ, red, green, blue, alpha);
				addPosAndColor(buffer, maxX, minY, minZ, red, green, blue, alpha);
			}
			else
			{
				maxY = box.getMaxY();
				tempMaxY = DataPoint.getHeight(data);
				if (tempMaxY < maxY)
				{
					minY = Math.max(tempMaxY, minY);
					addPosAndColor(buffer, minX, minY, minZ, red, green, blue, alpha);
					addPosAndColor(buffer, minX, maxY, minZ, red, green, blue, alpha);
					addPosAndColor(buffer, maxX, maxY, minZ, red, green, blue, alpha);
					addPosAndColor(buffer, maxX, minY, minZ, red, green, blue, alpha);
				}
				tempMinY = DataPoint.getDepth(data);
				minY = box.getMinY();
				if (tempMinY > minY)
				{
					maxY = Math.min(tempMinY, maxX);
					addPosAndColor(buffer, minX, minY, minZ, red, green, blue, alpha);
					addPosAndColor(buffer, minX, maxY, minZ, red, green, blue, alpha);
					addPosAndColor(buffer, maxX, maxY, minZ, red, green, blue, alpha);
					addPosAndColor(buffer, maxX, minY, minZ, red, green, blue, alpha);
				}
			}
		}
	}

	@Override
	public int getBufferMemoryForSingleNode()
	{
		// (sidesOnACube * pointsInASquare * (positionPoints + colorPoints)))
		return (6 * 4 * (3 + 4));
	}

}
