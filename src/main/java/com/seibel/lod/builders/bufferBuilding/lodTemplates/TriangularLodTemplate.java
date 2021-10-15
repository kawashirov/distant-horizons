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

import com.seibel.lod.enums.DebugMode;
import com.seibel.lod.proxy.ClientProxy;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

/**
 * TODO #21 TriangularLodTemplate
 * Builds each LOD chunk as a singular rectangular prism.
 * @author James Seibel
 * @version 06-16-2021
 */
public class TriangularLodTemplate extends AbstractLodTemplate
{
	@Override
	public void addLodToBuffer(BufferBuilder buffer, BlockPos bufferCenterBlockPos, long data, Map<Direction, long[]> adjData,
			byte detailLevel, int posX, int posZ, Box box, DebugMode debugging, NativeImage lightMap, boolean[] adjShadeDisabled, boolean caveLight)
	{
		ClientProxy.LOGGER.error(DynamicLodTemplate.class.getSimpleName() + " is not implemented!");
	}
	
}
