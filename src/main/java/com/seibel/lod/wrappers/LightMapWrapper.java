package com.seibel.lod.wrappers;

import net.minecraft.client.renderer.texture.NativeImage;


public class LightMapWrapper
{
	static NativeImage lightMap = null;
	
	public static void setLightMap(NativeImage newlightMap)
	{
		lightMap = newlightMap;
	}
	
	public static int getLightValue(int skyLight, int blockLight)
	{
		return lightMap.getPixelRGBA(skyLight, blockLight);
	}
}
