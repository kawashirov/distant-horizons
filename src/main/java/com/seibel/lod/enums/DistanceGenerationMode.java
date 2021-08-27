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
 * <p>
 * In order of fastest to slowest.
 *
 * @author James Seibel
 * @author Leonardo Amato
 * @version 8-7-2021
 */
public enum DistanceGenerationMode
{
	/**
	 * Don't generate anything
	 */
	NONE((byte) 0),

	/**
	 * Only generate the biomes and use biome
	 * grass/foliage color, water color, or ice color
	 * to generate the color.
	 * Doesn't generate height, everything is shown at sea level.
	 * Multithreaded - Fastest (2-5 ms)
	 */
	BIOME_ONLY((byte) 1),

	/**
	 * Same as BIOME_ONLY, except instead
	 * of always using sea level as the LOD height
	 * different biome types (mountain, ocean, forest, etc.)
	 * use predetermined heights to simulate having height data.
	 */
	BIOME_ONLY_SIMULATE_HEIGHT((byte) 2),

	/**
	 * Generate the world surface,
	 * this does NOT include caves, trees,
	 * or structures.
	 * Multithreaded - Faster (10-20 ms)
	 */
	SURFACE((byte) 3),

	/**
	 * Generate everything except structures.
	 * NOTE: This may cause world generation bugs or instability,
	 * since some features cause concurrentModification exceptions.
	 * Multithreaded - Fast (15-20 ms)
	 */
	FEATURES((byte) 4),

	/**
	 * Ask the server to generate/load each chunk.
	 * This is the most compatible, but causes server/simulation lag.
	 * This will also show player made structures if you
	 * are adding the mod to a pre-existing world.
	 * Singlethreaded - Slow (15-50 ms, with spikes up to 200 ms)
	 */
	SERVER((byte) 5);


	/**
	 * The higher the number the more complete the generation is.
	 */
	public final byte complexity;

	DistanceGenerationMode(byte complexity)
	{
		this.complexity = complexity;
	}
}
