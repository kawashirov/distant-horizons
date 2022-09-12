/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package com.seibel.lod.wrappers.modAccessor;

import java.util.HashSet;

import com.seibel.lod.core.pos.DhChunkPos;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.IOptifineAccessor;

public class OptifineAccessor implements IOptifineAccessor
{

	@Override
	public String getModName()
	{
		return "Optifine-Fabric-1.18.X";
	}

	@Override
	public HashSet<DhChunkPos> getNormalRenderedChunks()
	{
		// TODO: Impl proper methods here
		return null;
	}
	
}
