package com.seibel.lod.wrappers;

import net.minecraft.client.renderer.texture.NativeImage;


public class LigthMapWrapper
{
	static NativeImage lightMap = null;
	
	public static void setLightMap(NativeImage lightMap)
	{
		lightMap = lightMap;
	}
	
	public static int getLightValue(int skyLight, int blockLight)
	{
		return lightMap.getPixelRGBA(skyLight, blockLight);
	}
}
