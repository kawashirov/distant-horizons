package com.seibel.lod.enums;

/** 
 * None, Half, Always
 * 
 * <p>
 * This represents how far the LODs should overlap with
 * the vanilla Minecraft terrain.
 * 
 * @author James Seibel
 * @version 10-10-2021
 */
public enum VanillaOverdraw
{
	/** Never draw LODs where a minecraft chunk could be. */
	NEVER,
	
	/** Draw LODs over the farther minecraft chunks. */
	HALF,
	
	/** Draw LODs over all minecraft chunks. */
	ALWAYS,
}