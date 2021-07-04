package com.seibel.lod.enums;

/**
 * USE_OPTIFINE_FOG_SETTING, <br>
 * NEVER_DRAW_FOG, <br>
 * ALWAYS_DRAW_FOG_FAST, <br>
 * ALWAYS_DRAW_FOG_FANCY <br>
 * 
 * @author James Seibel
 * @version 7-03-2021
 */
public enum FogDrawOverride
{
	/** Use whatever Fog setting optifine is using.
	 * If optifine isn't installed this defaults to ALWAYS_DRAW_FOG. */
	USE_OPTIFINE_FOG_SETTING,
	
	/** Never draw fog on the LODs */
	NEVER_DRAW_FOG,
	
	/** Always draw fog on the LODs */
	ALWAYS_DRAW_FOG_FAST,
	
	/** Always draw fog on the LODs */
	ALWAYS_DRAW_FOG_FANCY;
}