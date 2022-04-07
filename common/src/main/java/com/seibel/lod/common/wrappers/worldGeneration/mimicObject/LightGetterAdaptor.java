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
 
package com.seibel.lod.common.wrappers.worldGeneration.mimicObject;

import com.seibel.lod.core.handlers.dependencyInjection.ModAccessorHandler;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.IStarlightAccessor;

import net.minecraft.world.level.BlockGetter;
#if MC_VERSION_1_17_1 || MC_VERSION_1_18_1 || MC_VERSION_1_18_2
import net.minecraft.world.level.LevelHeightAccessor;
#endif
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LightChunkGetter;

public class LightGetterAdaptor implements LightChunkGetter {
	private final BlockGetter heightGetter;
	public LightedWorldGenRegion genRegion = null;
	final boolean shouldReturnNull;

	public LightGetterAdaptor(BlockGetter heightAccessor) {
		this.heightGetter = heightAccessor;
		shouldReturnNull = ModAccessorHandler.get(IStarlightAccessor.class) != null;
	}

	public void setRegion(LightedWorldGenRegion region) {
		genRegion = region;
	}

	@Override
	public BlockGetter getChunkForLighting(int chunkX, int chunkZ) {
		if (genRegion == null)
			throw new IllegalStateException("World Gen region has not been set!");
		// May be null
		return genRegion.getChunk(chunkX, chunkZ, ChunkStatus.EMPTY, false);
	}

	@Override
	public BlockGetter getLevel() {
		return shouldReturnNull ? null : (genRegion != null ? genRegion : heightGetter);
	}

	#if MC_VERSION_1_17_1 || MC_VERSION_1_18_1 || MC_VERSION_1_18_2
	public LevelHeightAccessor getLevelHeightAccessor() {
		return heightGetter;
	}
	#endif
}