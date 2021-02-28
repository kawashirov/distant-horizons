package com.backsun.lod.objects;

import com.backsun.lod.util.enums.FogDistance;

/**
 * This object is just a replacement for an array
 * to make things easier to understand in the LodRenderer.
 * 
 * @author James Seibel
 * @version 02-27-2021
 */
public class NearFarFogSetting
{
	public FogDistance nearFogSetting = FogDistance.NEAR;
	public FogDistance farFogSetting = FogDistance.FAR;
	
	
	public NearFarFogSetting()
	{
		
	}
	
	public NearFarFogSetting(FogDistance newNearFogSetting, FogDistance newFarFogSetting)
	{
		nearFogSetting = newNearFogSetting;
		farFogSetting = newFarFogSetting;
	}
}
