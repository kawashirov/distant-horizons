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

package com.seibel.lod.core.wrapperInterfaces.block;

import com.seibel.lod.core.enums.LodDirection;

/**
 * BlockPos needs to be abstract instead of a interfaces
 * so that we can define its constructors.
 * 
 * @author James Seibel
 * @version 11-20-2021
 */
public abstract class AbstractBlockPosWrapper
{
	public AbstractBlockPosWrapper() { }
	public AbstractBlockPosWrapper(int x, int y, int z) { }
	
	
	
	public abstract void set(int x, int y, int z);
	
	public abstract int getX();
	public abstract int getY();
	public abstract int getZ();
	
	public abstract int get(LodDirection.Axis axis);
	
	/** returns itself */
	public abstract AbstractBlockPosWrapper offset(int x, int y, int z);
}
