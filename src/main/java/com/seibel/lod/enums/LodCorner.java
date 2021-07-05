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
 * NE, SE, SW, NW
 * 
 * @author James Seibel
 * @version 1-20-2020
 */
public enum LodCorner
{
	/** -Z, +X */
	NE(0),
	/** +Z, +X */
	SE(1),
	/** +Z, -X */
	SW(2),
	/** -Z, -X */
	NW(3);
	
	public final int value;
	
	private LodCorner(int newValue)
	{
		value = newValue;
	}
}
