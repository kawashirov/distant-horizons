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

package com.seibel.lod.core.builders.bufferBuilding.lodTemplates;

import java.util.Map;

import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.enums.rendering.DebugMode;
import com.seibel.lod.core.objects.VertexOptimizer;
import com.seibel.lod.core.objects.opengl.LodBufferBuilder;
import com.seibel.lod.core.util.ColorUtil;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;

/**
 * This is the abstract class used to create different
 * BufferBuilders.
 * @author James Seibel
 * @version 11-13-2021
 */
public abstract class AbstractLodTemplate
{
	/** Uploads the given LOD to the buffer. */
	public abstract void addLodToBuffer(LodBufferBuilder buffer, AbstractBlockPosWrapper bufferCenterBlockPos, long data, Map<LodDirection, long[]> adjData,
			byte detailLevel, int posX, int posZ, VertexOptimizer vertexOptimizer, DebugMode debugging, boolean[] adjShadeDisabled);
	
	/** add the given position and color to the buffer */
	protected void addPosAndColor(LodBufferBuilder buffer,
			float x, float y, float z,
			int color)
	{
		// TODO re-add transparency by replacing the 255 with "ColorUtil.getAlpha(color)"
		buffer.vertex(x, y, z).color(ColorUtil.getRed(color), ColorUtil.getGreen(color), ColorUtil.getBlue(color), 255).endVertex();
	}
	
}
