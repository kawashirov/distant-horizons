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
package com.seibel.lod.builders.bufferBuilding.lodTemplates;

import java.util.Map;

import com.seibel.lod.enums.DebugMode;
import com.seibel.lod.util.ColorUtil;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

/**
 * This is the abstract class used to create different
 * BufferBuilders.
 *
 * @author James Seibel
 * @version 8-8-2021
 */
public abstract class AbstractLodTemplate
{
	
	/**
	 * Uploads the given LOD to the buffer.
	 */
	public abstract void addLodToBuffer(BufferBuilder buffer, BlockPos bufferCenterBlockPos, long data, Map<Direction, long[]> adjData,
			byte detailLevel, int posX, int posZ, Box box, DebugMode debugging, NativeImage lightMap);
	
	/**
	 * add the given position and color to the buffer
	 */
	protected void addPosAndColor(BufferBuilder buffer,
			double x, double y, double z,
			int color)
	{
		buffer.vertex(x, y, z).color(ColorUtil.getRed(color), ColorUtil.getGreen(color), ColorUtil.getBlue(color), ColorUtil.getAlpha(color)).endVertex();
	}
	
	/**
	 * Returns in bytes how much buffer memory is required
	 * for one LOD object
	 */
	public abstract int getBufferMemoryForSingleNode(int maxVerticalData);
}
