package com.seibel.lod.enums;

/** 
 * None, Dynamic, Always
 * 
 * <p>
 * This represents how far the LODs should overlap with
 * the vanilla Minecraft terrain.
 * 
 * @author James Seibel
 * @version 10-11-2021
 */
public enum VanillaOverdraw
{
	/** Never draw LODs where a minecraft chunk could be. */
	NEVER,
	
	/** Draw LODs over the farther minecraft chunks. */
	DYNAMIC,
	
	/** Draw LODs over all minecraft chunks. */
	ALWAYS,
}