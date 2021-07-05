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
package com.seibel.lod.enums;

/**
 * TOP, NORTH, SOUTH, EAST, WEST, BOTTOM
 * 
 * @author James Seibel
 * @version 10-17-2020
 */
public enum ColorDirection
{
	// used for colors
	/** +Y */
	TOP(0),
	
	/** -Z */
	NORTH(1),
	/** +Z */
	SOUTH(2),
	
	/** +X */
	EAST(3),
	/** -X */
	WEST(4),
	
	/** -Y */
	BOTTOM(5);
	
	public final int value;
	
	private ColorDirection(int newValue)
	{
		value = newValue;
	}
}
