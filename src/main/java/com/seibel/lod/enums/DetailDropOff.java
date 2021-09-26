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
 * By_Region_Fast, <br>
 * By_Region_Fancy, <br>
 * By_Chunk
 * 
 * @author Leonardo Amato
 * @version 9-25-2021
 */
public enum DetailDropOff
{
	/** quality is determined per-region, using the lowest quality that would be used in BY_CHUNK */
	BY_REGION_FAST,
	
	/** quality is determined per-region, using the highest quality that would be used in BY_CHUNK */
	BY_REGION_FANCY,
	
	/** quality is determined per-chunk (best quality option, may cause stuttering when moving) */
	BY_CHUNK,
}
