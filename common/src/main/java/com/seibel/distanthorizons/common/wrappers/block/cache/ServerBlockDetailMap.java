/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2023 James Seibel
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

package com.seibel.distanthorizons.common.wrappers.block.cache;

import java.util.concurrent.ConcurrentHashMap;

import com.seibel.distanthorizons.common.wrappers.world.ServerLevelWrapper;
import com.seibel.distanthorizons.core.pos.DhBlockPos;
import net.minecraft.world.level.block.state.BlockState;


public class ServerBlockDetailMap
{
	private final ConcurrentHashMap<BlockState, ServerBlockStateCache> blockCache = new ConcurrentHashMap<>();
	//private final ConcurrentHashMap<#if PRE_MC_1_18_2 Biome #else Holder<Biome> #endif, Biome> biomeMap = new ConcurrentHashMap<>();
	private final ServerLevelWrapper level;
	public ServerBlockDetailMap(ServerLevelWrapper level) { this.level = level; }
	
	public ServerBlockStateCache getBlockStateData(BlockState state, DhBlockPos pos)
	{ //TODO: Allow a per pos unique setting
		return blockCache.computeIfAbsent(state, (s) -> new ServerBlockStateCache(s, level, new DhBlockPos(0, 0, 0)));
	}
	
	public void clear() { blockCache.clear(); }
	
}
