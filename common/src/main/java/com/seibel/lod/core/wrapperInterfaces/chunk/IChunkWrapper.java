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

package com.seibel.lod.core.wrapperInterfaces.chunk;

import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockColorWrapper;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockShapeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;

/**
 * @author James Seibel
 * @version 11-17-2021
 */
public interface IChunkWrapper
{
	public int getHeight();
	
	public boolean isPositionInWater(AbstractBlockPosWrapper blockPos);
	
	public int getHeightMapValue(int xRel, int zRel);
	
	public IBiomeWrapper getBiome(int xRel, int yAbs, int zRel);
	
	public IBlockColorWrapper getBlockColorWrapper(AbstractBlockPosWrapper blockPos);
	
	public IBlockShapeWrapper getBlockShapeWrapper(AbstractBlockPosWrapper blockPos);
	
	public AbstractChunkPosWrapper getPos();
	
	public boolean isLightCorrect();
	
	public boolean isWaterLogged(AbstractBlockPosWrapper blockPos);
	
	public int getEmittedBrightness(AbstractBlockPosWrapper blockPos);
}
