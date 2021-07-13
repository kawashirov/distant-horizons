package com.seibel.lod.enums;

/**
 * BIOME_ONLY <br>
 * BIOME_ONLY_SIMULATE_HEIGHT <br>
 * SURFACE <br>
 * FEATURES <br>
 * SERVER <br><br>
 * 
 * In order of fastest to slowest.
 * 
 * @author James Seibel
 * @version 6-27-2021
 */
public enum DistanceGenerationMode
{
	/** No generation has be used*/
	NONE,

	/** Only generate the biomes and use biome
	 * grass/foliage color, water color, or ice color
	 * to generate the color.
	 * Doesn't generate height, everything is shown at sea level. 
	 * Multithreaded - Fastest (2-5 ms) */
	BIOME_ONLY,
	
	/**
	 * Same as BIOME_ONLY, except instead
	 * of always using sea level as the LOD height
	 * different biome types (mountain, ocean, forest, etc.)
	 * use predetermined heights to simulate having height data.
	 */
	BIOME_ONLY_SIMULATE_HEIGHT,
	
	/** Generate the world surface, 
	 * this does NOT include caves, trees,
	 * or structures. 
	 * Multithreaded - Faster (10-20 ms) */
	SURFACE,
	
	/** Generate everything except structures. 
	 * NOTE: This may cause world generation bugs or instability,
	 * since some features cause concurrentModification exceptions.
	 * Multithreaded - Fast (15-20 ms) */
	FEATURES,
	
	/** Ask the server to generate/load each chunk.
	 * This is the most compatible, but causes server/simulation lag.
	 * This will also show player made structures if you
	 * are adding the mod to a pre-existing world. 
	 * Singlethreaded - Slow (15-50 ms, with spikes up to 200 ms) */
	SERVER;
}
