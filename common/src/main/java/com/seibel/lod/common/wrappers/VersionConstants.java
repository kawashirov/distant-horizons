
package com.seibel.lod.common.wrappers;

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
		return true;
	}
	
	@Override
	public int getWorldGenerationCountPerThread()
	{
		return 1;
	}
	
	
	@Override
	public boolean hasBatchGenerationImplementation()
	{
		return true;
	}


	@Override
	public boolean isVanillaRenderedChunkSquare()
	{
		return false;
	}
}