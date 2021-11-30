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

package com.seibel.lod.fabric.wrappers.world;

import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeColorWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;
import com.seibel.lod.fabric.wrappers.block.BlockPosWrapper;

import net.minecraft.client.renderer.BiomeColors;

/**
 * @author Cola?
 * @version 11-15-2021
 */
public class BiomeColorWrapperSingleton implements IBiomeColorWrapperSingleton
{
	private static final BiomeColorWrapperSingleton instance = new BiomeColorWrapperSingleton();
	
	@Override
	public IBiomeColorWrapperSingleton getInstance()
	{
		return instance;
	}
	
	
	@Override
	public int getGrassColor(IWorldWrapper world, AbstractBlockPosWrapper blockPos)
	{
		return BiomeColors.getAverageGrassColor(((WorldWrapper)world).getWorld(), ((BlockPosWrapper) blockPos).getBlockPos());
	}
	@Override
	public int getWaterColor(IWorldWrapper world, AbstractBlockPosWrapper blockPos)
	{
		return BiomeColors.getAverageWaterColor(((WorldWrapper)world).getWorld(), ((BlockPosWrapper) blockPos).getBlockPos());
	}
	@Override
	public int getFoliageColor(IWorldWrapper world, AbstractBlockPosWrapper blockPos)
	{
		return BiomeColors.getAverageFoliageColor(((WorldWrapper)world).getWorld(), ((BlockPosWrapper) blockPos).getBlockPos());
	}
}
