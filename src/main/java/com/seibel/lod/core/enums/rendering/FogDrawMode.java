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
 * FOG_ENABLED, <br>
 * FOG_DISABLED <br>
 * 
 * @author James Seibel
 * @version 11-27-2021
 */
public enum FogDrawMode
{
	/**
	 * Use whatever Fog setting optifine is using.
	 * If optifine isn't installed this defaults to ALWAYS_DRAW_FOG.
	 */
	USE_OPTIFINE_SETTING,
	
	FOG_ENABLED,
	FOG_DISABLED
}
