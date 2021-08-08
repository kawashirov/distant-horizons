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
package com.seibel.lod.builders.lodNodeTemplates;

import com.seibel.lod.objects.LodQuadTreeDimension;
import com.seibel.lod.objects.LodQuadTreeNode;

import net.minecraft.client.renderer.BufferBuilder;

/**
 * TODO DynamicLodTemplate
 * Chunks smoothly transition between
 * each other, unless a neighboring chunk
 * is at a significantly different height.
 * 
 * @author James Seibel
 * @version 06-16-2021
 */
public class DynamicLodNodeTemplate extends AbstractLodNodeTemplate
{
	@Override
	public void addLodToBuffer(BufferBuilder buffer,
							   LodQuadTreeDimension lodDim, LodQuadTreeNode lod,
							   double xOffset, double yOffset, double zOffset,
							   boolean debugging)
	{
		System.err.println("DynamicLodTemplate not implemented!");
	}

	@Override
	public int getBufferMemoryForSingleNode(int detailLevel)
	{
		// TODO Auto-generated method stub
		return 0;
	}
}
