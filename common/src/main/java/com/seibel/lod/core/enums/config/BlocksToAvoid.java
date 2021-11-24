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

package com.seibel.lod.core.enums.config;

/**
 * heightmap <br>
 * multi_lod <br>
 * 
 * @author Leonardo Amato
 * @version 11-16-2021
 */
public enum BlocksToAvoid
{
	NONE(false, false),
	
	NON_FULL(true, false),
	
	NO_COLLISION(false, true),
	
	BOTH(true, true);
	
	public final boolean nonFull;
	public final boolean noCollision;
	
	BlocksToAvoid(boolean nonFull, boolean noCollision)
	{
		this.nonFull = nonFull;
		this.noCollision = noCollision;
	}
}