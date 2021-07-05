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
package com.seibel.lod.objects;

/**
 * This object is similar to ChunkPos or BlockPos.
 * 
 * @author James Seibel
 * @version 03-19-2021
 */
public class RegionPos
{
	public int x;
	public int z;
	
	
	/**
	 * Default Constructor <br>
	 * 
	 * Sets x and z to 0
	 */
	public RegionPos()
	{
		x = 0;
		z = 0;
	}
	
	public RegionPos(int newX, int newZ)
	{
		x = newX;
		z = newZ;
	}
}
