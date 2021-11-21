/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
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

package com.seibel.lod.forge.wrappers.block;

import java.util.Objects;

import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;

import net.minecraft.util.math.BlockPos;

/**
 * @author James Seibel
 * @version 11-20-2021
 */
public class BlockPosWrapper extends AbstractBlockPosWrapper
{
	private final BlockPos.Mutable blockPos;
	
	
	public BlockPosWrapper()
	{
		this.blockPos = new BlockPos.Mutable(0, 0, 0);
	}
	
	public BlockPosWrapper(int x, int y, int z)
	{
		this.blockPos = new BlockPos.Mutable(x, y, z);
	}
	
	@Override
	public void set(int x, int y, int z)
	{
		blockPos.set(x, y, z);
	}
	
	@Override
	public int getX()
	{
		return blockPos.getX();
	}
	
	@Override
	public int getY()
	{
		return blockPos.getY();
	}
	
	@Override
	public int getZ()
	{
		return blockPos.getZ();
	}
	
	@Override
	public int get(LodDirection.Axis axis)
	{
		return axis.choose(getX(), getY(), getZ());
	}
	
	public BlockPos.Mutable getBlockPos()
	{
		return blockPos;
	}
	
	@Override
	public boolean equals(Object o)
	{
		return blockPos.equals(o);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(blockPos);
	}
	
	@Override
	public BlockPosWrapper offset(int x, int y, int z)
	{
		blockPos.set(blockPos.getX() + x, blockPos.getY() + y, blockPos.getZ() + z);
		return this;
	}
	
}
