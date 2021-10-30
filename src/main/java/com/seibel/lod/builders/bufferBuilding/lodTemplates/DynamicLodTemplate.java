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
import com.seibel.lod.proxy.ClientProxy;
import com.seibel.lod.wrappers.Block.BlockPosWrapper;

import net.minecraft.core.Direction;

/**
 * TODO DynamicLodTemplate
 * Chunks smoothly transition between
 * each other, unless a neighboring chunk
 * is at a significantly different height.
 * @author James Seibel
 * @version 06-16-2021
 */
public class DynamicLodTemplate extends AbstractLodTemplate
{
	@Override
	public void addLodToBuffer(BufferBuilder buffer, BlockPosWrapper bufferCenterBlockPos, long data, Map<Direction, long[]> adjData,
			byte detailLevel, int posX, int posZ, Box box, DebugMode debugging, boolean[] adjShadeDisabled)
	{
		ClientProxy.LOGGER.error(DynamicLodTemplate.class.getSimpleName() + " is not implemented!");
	}
	
}
