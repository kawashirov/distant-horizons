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
 
package com.seibel.lod.fabric.wrappers.modAccessor;

import java.util.HashSet;

import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.IOptifineAccessor;

public class OptifineAccessor implements IOptifineAccessor
{

	@Override
	public String getModName()
	{
		return "Optifine-Fabric-1.18.X";
	}

	@Override
	public HashSet<AbstractChunkPosWrapper> getNormalRenderedChunks()
	{
		// TODO: Impl proper methods here
		return null;
	}
	
}
