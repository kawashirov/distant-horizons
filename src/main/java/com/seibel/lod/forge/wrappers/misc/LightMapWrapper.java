package com.seibel.lod.forge.wrappers.misc;

import com.mojang.blaze3d.platform.NativeImage;
import com.seibel.lod.core.wrapperInterfaces.misc.ILightMapWrapper;

/**
 * @author James Seibel
 * @version 11-21-2021
 */
public class LightMapWrapper implements ILightMapWrapper
{
	static NativeImage lightMap = null;
	
	public LightMapWrapper(NativeImage newLightMap)
	{
		lightMap = newLightMap;
	}
	
	public static void setLightMap(NativeImage newLightMap)
	{
		lightMap = newLightMap;
	}
	
	@Override
	public int getLightValue(int skyLight, int blockLight)
	{
		return lightMap.getPixelRGBA(skyLight, blockLight);
	}
}
