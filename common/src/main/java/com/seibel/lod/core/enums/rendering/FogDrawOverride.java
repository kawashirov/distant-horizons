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

package com.seibel.lod.core.enums.rendering;

/**
 * USE_OPTIFINE_FOG_SETTING, <br>
 * NEVER_DRAW_FOG, <br>
 * ALWAYS_DRAW_FOG_FAST, <br>
 * ALWAYS_DRAW_FOG_FANCY <br>
 * 
 * @author James Seibel
 * @version 7-3-2021
 */
public enum FogDrawOverride
{
	/**
	 * Use whatever Fog setting optifine is using.
	 * If optifine isn't installed this defaults to ALWAYS_DRAW_FOG.
	 */
	OPTIFINE_SETTING,
	
	/** Never draw fog on the LODs */
	NO_FOG,
	
	/** Always draw fast fog on the LODs */
	FAST,
	
	/** Always draw fancy fog on the LODs */
	FANCY
}