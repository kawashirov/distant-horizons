package com.seibel.lod.wrappers;

import com.mojang.blaze3d.platform.NativeImage;

public class LightMapWrapper
{
	static NativeImage lightMap = null;
	
	public static void setLightMap(NativeImage newLightMap)
	{
		lightMap = newLightMap;
	}
	
	public static int getLightValue(int skyLight, int blockLight)
	{
		return lightMap.getPixelRGBA(skyLight, blockLight);
	}
}
