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
 * NONE <br>
 * BIOME_ONLY <br>
 * BIOME_ONLY_SIMULATE_HEIGHT <br>
 * SURFACE <br>
 * FEATURES <br>
 * SERVER <br><br>
 * 
 * In order of fastest to slowest.
 * 
 * @author James Seibel
 * @author Leonardo Amato
 * @version 8-7-2021
 */
public enum LodRenderDistance
{
	SHORT(32),

	MEDIUM(64),

	FAR(128);


	/** The higher the number the more complete the generation is. */
	public final int renderDistance;

	LodRenderDistance(int complexity)
	{
		this.renderDistance = complexity;
	}
}
