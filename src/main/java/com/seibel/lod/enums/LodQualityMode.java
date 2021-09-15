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
 * USE_OPTIFINE_FOG_SETTING, <br>
 * NEVER_DRAW_FOG, <br>
 * ALWAYS_DRAW_FOG_FAST, <br>
 * ALWAYS_DRAW_FOG_FANCY <br>
 *
 * @author James Seibel
 * @version 7-03-2021
 */
public enum LodQualityMode
{
	/** Lods are 2D with heightMap */
	HEIGHTMAP,

	/** Lods expand in three dimension */
	MULTI_LOD;
}