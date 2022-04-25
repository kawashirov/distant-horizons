/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package com.seibel.lod.common.wrappers.block;

import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public class BlockDetailMap
{
	
	private static ConcurrentHashMap<BlockState, BlockDetailWrapper> map = new ConcurrentHashMap<BlockState, BlockDetailWrapper>();
	
	private BlockDetailMap() {}
	
	public static BlockDetailWrapper getOrMakeBlockDetailCache(BlockState bs, BlockPos pos, LevelReader getter) {
		if (!bs.getFluidState().isEmpty()) {
			bs = bs.getFluidState().createLegacyBlock();
		}
		BlockDetailWrapper cache = map.get(bs);
		if (cache != null) return cache;
		cache = BlockDetailWrapper.make(bs, pos, getter);
		//ApiShared.LOGGER.info("New blockDetail cache for {} to {} ", bs, cache);
		BlockDetailWrapper cacheCAS = map.putIfAbsent(bs, cache);
		return cacheCAS==null ? cache : cacheCAS;
	}
}
