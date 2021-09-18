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

import com.seibel.lod.enums.DebugMode;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;

/**
 * This is the abstract class used to create different
 * BufferBuilders.
 *
 * @author James Seibel
 * @version 8-8-2021
 */
public abstract class AbstractLodTemplate
{


	public abstract void addLodToBuffer(BufferBuilder buffer, BlockPos bufferCenterBlockPos, long data, long[] adjData,
	                                    byte detailLevel, int posX, int posZ, Box box, DebugMode debugging, NativeImage lightMap);

	/**
	 * add the given position and color to the buffer
	 */
	protected void addPosAndColor(BufferBuilder buffer,
	                              double x, double y, double z,
	                              int red, int green, int blue, int alpha)
	{

		buffer.vertex(x, y, z).color(red, green, blue, alpha).endVertex();
	}

	/**
	 * Returns in bytes how much buffer memory is required
	 * for one LOD object
	 */
	public abstract int getBufferMemoryForSingleNode();
}
