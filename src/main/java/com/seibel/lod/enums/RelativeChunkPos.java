package com.seibel.lod.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;

/** 
 * NE, SE, SW, NW <br>
 * NORTH, SOUTH, EAST, WEST, <br>
 * CENTER
 * 
 * @author James Seibel
 * @version 05-30-2021
 */
public enum RelativeChunkPos
{
	/** +X, -Z */
	NE(0, 1,-1),
	/** +X, +Z */
	SE(1, 1,1),
	/** -X, +Z */
	SW(2, -1,1),
	/** -X, -Z */
	NW(3, -1,-1),
	
	/** -Z */
	NORTH(4, 0,-1),
	/** +Z */
	SOUTH(5, 0,1),
	
	/** +X */
	EAST(6, 1,0),
	/** -X */
	WEST(7, -1,0),
	
	CENTER(8, 0,0);
	
	/** index used when referencing objects in an array */
	public final int index;
	
	/** position relative to X CENTER */
	public final int x;
	/** position relative to Z CENTER */
	public final int z;
	
	/** NORTH, SOUTH, EAST, WEST */
	public static EnumSet<RelativeChunkPos> ADJACENT = EnumSet.of(NORTH, SOUTH, EAST, WEST);
	/** NE, NW, SE, SW */
	public static EnumSet<RelativeChunkPos> DIAGONAL = EnumSet.of(NE, NW, SE, SW);
	
	
	public static EnumSet<RelativeChunkPos> NW_CORNER = EnumSet.of(WEST, NW, NORTH);
	public static EnumSet<RelativeChunkPos> NE_CORNER = EnumSet.of(EAST, NE, NORTH);
	public static EnumSet<RelativeChunkPos> SW_CORNER = EnumSet.of(WEST, SW, SOUTH);
	public static EnumSet<RelativeChunkPos> SE_CORNER = EnumSet.of(EAST, SE, SOUTH);
	
	/** NW_CORNER, NE_CORNER, SW_CORNER, SE_CORNER <br>
	 * Contains the 3 points surrounding a corner. */
	public static ArrayList<EnumSet<RelativeChunkPos>> CORNERS = new ArrayList<EnumSet<RelativeChunkPos>>( Arrays.asList(NW_CORNER, NE_CORNER, SW_CORNER, SE_CORNER) );
	
	private RelativeChunkPos(int newIndex, int newX, int newZ)
	{
		index = newIndex;
		x = newX;
		z = newZ;
	}
}


