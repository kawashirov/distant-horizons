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
 * USE_DEFAULT_FOG_COLOR, <br>
 * USE_SKY_COLOR, <br>
 * 
 * @author James Seibel
 * @version 11-27-2021
 */
public enum FogColorMode
{
	/** Fog uses Minecraft's fog color. */
	USE_WORLD_FOG_COLOR,
	
	/**
	 * Replicates the effect of the clear sky mod.
	 * Making the fog blend in with the sky better
	 * https://www.curseforge.com/minecraft/mc-mods/clear-skies
	 * https://www.curseforge.com/minecraft/mc-mods/clear-skies-forge-port
	 * For it to look good you need one of those mods
	 */
	USE_SKY_COLOR,
}
