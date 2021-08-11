/*
 *    This file is part of the LOD Mod, licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.seibel.lod.objects;

import com.seibel.lod.util.LodUtil;

/**
 * A LodRegion is a 32x32
 * 2D array of LodChunk objects.
 * Each LodRegion corresponds to
 * one file in the file system.
 * 
 * @author James Seibel
 * @version 6-12-2021
 */
public class LodRegion
{
	/**	X coordinate of this region */
	public final int x;
	/** Z coordinate of this region */
	public final int z;
	
	private LodChunk chunks[][];
	
	
	public LodRegion(int regionX, int regionZ)
	{
		x = regionX;
		z = regionZ;
		
		chunks = new LodChunk[LodUtil.REGION_WIDTH_IN_CHUNKS][LodUtil.REGION_WIDTH_IN_CHUNKS];
	}
	
	
	/**
	 * Add the given LOD to this region at the coordinate
	 * stored in the LOD. If an LOD already exists at the given
	 * coordinates it will be overwritten.
	 */
	public void addLod(LodChunk lod)
	{
		// we use ABS since LODs can be negative, but if they are
		// the region will negative first, therefore we don't have to
		// store the LOD chunks at negative indexes since we search 
		// LOD the region first
		int xIndex = Math.abs(lod.x % LodUtil.REGION_WIDTH_IN_CHUNKS);
		int zIndex = Math.abs(lod.z % LodUtil.REGION_WIDTH_IN_CHUNKS);
		
		chunks[xIndex][zIndex] = lod;
	}
	
	/**
	 * Get the LodChunk at the given X and Z coordinates
	 * in this region.
	 * <br>
	 * Returns null if the LodChunk doesn't exist or 
	 * is outside the loaded area.
	 */
	public LodChunk getLod(int chunkX, int chunkZ)
	{
		// since we add LOD's with ABS, we get them the same way
		int arrayX = Math.abs(chunkX % LodUtil.REGION_WIDTH_IN_CHUNKS);
		int arrayZ = Math.abs(chunkZ % LodUtil.REGION_WIDTH_IN_CHUNKS);
		
		return chunks[arrayX][arrayZ];
	}
	
	
	/**
	 * Returns all LodChunks in this region
	 */
	public LodChunk[][] getAllLods()
	{
		return chunks;
	}
	
	
	
	@Override
	public String toString()
	{
		String s = "";
		
		s += "x: " + x + " z: " + z + "\t";
		
		return s;
	}
}
