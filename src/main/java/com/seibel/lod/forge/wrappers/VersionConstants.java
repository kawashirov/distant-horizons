package com.seibel.lod.forge.wrappers;

import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.wrapperInterfaces.IVersionConstants;

/**
 * @author James Seibel
 * @version 12-11-2021
 */
public class VersionConstants implements IVersionConstants
{
	public static final VersionConstants INSTANCE = new VersionConstants();
	
	
	private VersionConstants()
	{
		
	}
	
	
	
	@Override
	public int getMinimumWorldHeight()
	{
		return 0;
	}
	
	@Override
	public boolean isWorldGeneratorSingleThreaded(DistanceGenerationMode distanceGenerationMode)
	{
		switch (distanceGenerationMode)
		{
		default:
		case NONE:
		case BIOME_ONLY:
		case BIOME_ONLY_SIMULATE_HEIGHT:
		case SURFACE:
		case FEATURES:
			return false;
		
		case FULL:
			return true;
		}
	}
	
}
