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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

/**
 * This object is similar to ChunkPos or BlockPos.
 * @author James Seibel
 * @version 8-21-2021
 */
public class RegionPos
{
	public int x;
	public int z;
	
	
	/**
	 * Default Constructor <br><br>
	 * <p>
	 * Sets x and z to 0
	 */
	public RegionPos()
	{
		x = 0;
		z = 0;
	}
	
	/** simple constructor that sets x and z to new x and z. */
	public RegionPos(int newX, int newZ)
	{
		x = newX;
		z = newZ;
	}
	
	/** Converts from a BlockPos to a RegionPos */
	public RegionPos(BlockPos pos)
	{
		this(new ChunkPos(pos));
	}
	
	/** Converts from a ChunkPos to a RegionPos */
	public RegionPos(ChunkPos pos)
	{
		x = Math.floorDiv(pos.x, LodUtil.REGION_WIDTH_IN_CHUNKS);
		z = Math.floorDiv(pos.z, LodUtil.REGION_WIDTH_IN_CHUNKS);
	}
	
	/** Returns the ChunkPos at the center of this region */
	public ChunkPos chunkPos()
	{
		return new ChunkPos(
				(x * LodUtil.REGION_WIDTH_IN_CHUNKS) + LodUtil.REGION_WIDTH_IN_CHUNKS / 2,
				(z * LodUtil.REGION_WIDTH_IN_CHUNKS) + LodUtil.REGION_WIDTH_IN_CHUNKS / 2);
	}
	
	/** Returns the BlockPos at the center of this region */
	public BlockPos blockPos()
	{
		return chunkPos().getWorldPosition()
				.offset(LodUtil.CHUNK_WIDTH / 2, 0, LodUtil.CHUNK_WIDTH / 2);
	}
	
	
	@Override
	public String toString()
	{
		return "(" + x + "," + z + ")";
	}
}
