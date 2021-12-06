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

package com.seibel.lod.core.wrapperInterfaces.worldGeneration;

import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;

/**
 * This is used for generating chunks
 * in a variety of detail and threading levels.
 * <p>
 * Abstract instead of an interface, so
 * we can define its constructors.
 * 
 * @author James Seibel
 * @version 11-20-2021
 */
public abstract class AbstractWorldGeneratorWrapper
{
	public AbstractWorldGeneratorWrapper(LodBuilder newLodBuilder, LodDimension newLodDimension, IWorldWrapper worldWrapper) { }
	
	
	public abstract void generateBiomesOnly(AbstractChunkPosWrapper pos, DistanceGenerationMode generationMode);
	
	public abstract void generateSurface(AbstractChunkPosWrapper pos);
	
	public abstract void generateFeatures(AbstractChunkPosWrapper pos);
	
	public abstract void generateFull(AbstractChunkPosWrapper pos);
}
