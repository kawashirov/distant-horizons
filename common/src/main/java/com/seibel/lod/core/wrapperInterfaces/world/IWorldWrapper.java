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

package com.seibel.lod.core.wrapperInterfaces.world;

import java.io.File;

import com.seibel.lod.core.enums.WorldType;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;

/**
 * Can be either a Server world or a Client world.
 * 
 * @author James Seibel
 * @version 11-20-2021
 */
public interface IWorldWrapper
{
	public IDimensionTypeWrapper getDimensionType();
	
	public WorldType getWorldType();
	
	public int getBlockLight(AbstractBlockPosWrapper blockPos);
	
	public int getSkyLight(AbstractBlockPosWrapper blockPos);
	
	public IBiomeWrapper getBiome(AbstractBlockPosWrapper blockPos);
	
	public boolean hasCeiling();
	
	public boolean hasSkyLight();
	
	public boolean isEmpty();
	
	public int getHeight();
	
	public int getSeaLevel();
	
	/** @throws UnsupportedOperationException if the WorldWrapper isn't for a ServerWorld */
	public File getSaveFolder() throws UnsupportedOperationException;

	
}
