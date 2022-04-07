/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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
 
package com.seibel.lod.common.wrappers;

import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.wrapperInterfaces.IVersionConstants;

/**
 * @author James Seibel
 * @version 12-11-2021
 */
public class VersionConstants implements IVersionConstants
{
	public static final VersionConstants INSTANCE = new VersionConstants();
	
	
	private VersionConstants()
	{
		
	}
	
	
	@Override
	public int getMinimumWorldHeight()
	{
		return 0;
	}

	@Override
	public int getWorldGenerationCountPerThread()
	{
		return 1;
	}

	@Override
	public boolean isVanillaRenderedChunkSquare()
	{
		return false;
	}
}