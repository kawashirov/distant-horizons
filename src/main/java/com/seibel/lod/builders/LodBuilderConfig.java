package com.seibel.lod.builders;

/**
 * This is used to easily configure how LodChunks are generated.
 * Generally this will only be used if we want to generate a
 * LodChunk using a incomplete Chunk, otherwise the defaults
 * work best for a fully generated chunk (IE has correct surface blocks).
 * 
 * @author James Seibel
 * @version 6-27-2021
 */
public class LodBuilderConfig
{
	/** default false */
	public boolean useHeightmap;
	/** default false */
	public boolean useBiomeColors;
	/** default true */
	public boolean useSolidBlocksInColorGen;
	
	/** default settings for a normal chunk 
	 * useHeightmap = false
	 * useBiomeColors = false
	 * useSolidBlocksInColorGen = true
	 */
	public LodBuilderConfig()
	{
		useHeightmap = false;
		useBiomeColors = false;
		useSolidBlocksInColorGen = true;
	}
	
	/**
	 * @param newUseHeightmap default = false
	 * @param newUseBiomeColors default = false
	 * @param newUseSolidBlocksInBiomeColor default = true
	 */
	public LodBuilderConfig(boolean newUseHeightmap, boolean newUseBiomeColors, boolean newUseSolidBlocksInBiomeColor)
	{
		useHeightmap = newUseHeightmap;
		useBiomeColors = newUseBiomeColors;
		useSolidBlocksInColorGen = newUseSolidBlocksInBiomeColor;
	}
}