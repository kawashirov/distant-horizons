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
 
package com.seibel.distanthorizons.common.wrappers.worldGeneration.mimicObject;

import com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IStarlightAccessor;

import net.minecraft.world.level.BlockGetter;
#if POST_MC_1_17_1
import net.minecraft.world.level.LevelHeightAccessor;
#endif
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LightChunkGetter;
#if POST_MC_1_20_1
import net.minecraft.world.level.chunk.LightChunk;
#endif

public class LightGetterAdaptor implements LightChunkGetter {
	private final BlockGetter heightGetter;
	public DhLitWorldGenRegion genRegion = null;
	final boolean shouldReturnNull;

	public LightGetterAdaptor(BlockGetter heightAccessor) {
		this.heightGetter = heightAccessor;
		shouldReturnNull = ModAccessorInjector.INSTANCE.get(IStarlightAccessor.class) != null;
	}

	public void setRegion(DhLitWorldGenRegion region) {
		genRegion = region;
	}

	@Override
	public #if PRE_MC_1_20_1 BlockGetter #else LightChunk #endif getChunkForLighting(int chunkX, int chunkZ) {
		if (genRegion == null)
			throw new IllegalStateException("World Gen region has not been set!");
		// May be null
		return genRegion.getChunk(chunkX, chunkZ, ChunkStatus.EMPTY, false);
	}

	@Override
	public BlockGetter getLevel() {
		return shouldReturnNull ? null : (genRegion != null ? genRegion : heightGetter);
	}

	#if POST_MC_1_17_1
	public LevelHeightAccessor getLevelHeightAccessor() {
		return heightGetter;
	}
	#endif
}