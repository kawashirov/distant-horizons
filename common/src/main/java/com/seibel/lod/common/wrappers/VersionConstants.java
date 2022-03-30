package com.seibel.lod.common.wrappers;

import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.wrapperInterfaces.IVersionConstants;

/**
 * @author James Seibel
 * @version 3-3-2022
 */
public class VersionConstants implements IVersionConstants {
    public static final VersionConstants INSTANCE = new VersionConstants();


    private VersionConstants() {

    }


    @Override
    public int getMinimumWorldHeight() {
        return 0;
    }

    @Override
    public int getWorldGenerationCountPerThread() {
        return 1;
    }

	@Override
	public boolean isVanillaRenderedChunkSquare()
	{
		return false;
	}
}