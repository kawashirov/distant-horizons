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

import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.enums.rendering.DebugMode;
import com.seibel.lod.core.objects.VertexOptimizer;
import com.seibel.lod.core.objects.opengl.LodBufferBuilder;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;

/**
 * TODO #21 TriangularLodTemplate
 * Builds each LOD chunk as a singular rectangular prism.
 * @author James Seibel
 * @version 06-16-2021
 */
public class TriangularLodTemplate extends AbstractLodTemplate
{
	@Override
	public void addLodToBuffer(LodBufferBuilder buffer, AbstractBlockPosWrapper bufferCenterBlockPos, long data, Map<LodDirection, long[]> adjData,
			byte detailLevel, int posX, int posZ, VertexOptimizer vertexOptimizer, DebugMode debugging, boolean[] adjShadeDisabled)
	{
		ClientApi.LOGGER.error(DynamicLodTemplate.class.getSimpleName() + " is not implemented!");
	}
	
}
