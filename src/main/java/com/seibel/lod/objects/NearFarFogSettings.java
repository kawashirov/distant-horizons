package com.seibel.lod.objects;

import com.seibel.lod.enums.FogDistance;
import com.seibel.lod.enums.FogQuality;

/**
 * This object is just a replacement for an array
 * to make things easier to understand in the LodRenderer.
 * 
 * @author James Seibel
 * @version 7-03-2021
 */
public class NearFarFogSettings
{
	public NearOrFarSetting near = new NearOrFarSetting(FogDistance.NEAR);
	public NearOrFarSetting far = new NearOrFarSetting(FogDistance.FAR);
	
	/** If true that means Minecraft is
	 * rendering fog along side us */
	public boolean vanillaIsRenderingFog = true;
	
	public NearFarFogSettings()
	{
		
	}
	
	
	
	/**
	 * This holds all relevant data to rendering fog at either
	 * near or far distances.
	 */
	public class NearOrFarSetting
	{
		public FogQuality quality = FogQuality.FANCY;
		public FogDistance distance = FogDistance.FAR;
		
		/** If true this section should render with fog */
		public boolean enabled = true;
		
		public NearOrFarSetting(FogDistance newFogDistance)
		{
			distance = newFogDistance;
		}
	}
}
