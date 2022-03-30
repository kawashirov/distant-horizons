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

package com.seibel.lod.common.wrappers;

import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;
import com.seibel.lod.core.wrapperInterfaces.worldGeneration.AbstractBatchGenerationEnvionmentWrapper;
import com.seibel.lod.common.wrappers.block.BlockPosWrapper;
import com.seibel.lod.common.wrappers.chunk.ChunkPosWrapper;
import com.seibel.lod.common.wrappers.worldGeneration.BatchGenerationEnvironment;

/**
 * This handles creating abstract wrapper objects.
 *
 * @author James Seibel
 * @version 11-20-2021
 */
public class WrapperFactory implements IWrapperFactory {
    public static final WrapperFactory INSTANCE = new WrapperFactory();


    @Override
    public AbstractBlockPosWrapper createBlockPos() {
        return new BlockPosWrapper();
    }

    @Override
    public AbstractBlockPosWrapper createBlockPos(int x, int y, int z) {
        return new BlockPosWrapper(x, y, z);
    }

    @Override
    public AbstractChunkPosWrapper createChunkPos() {
        return new ChunkPosWrapper();
    }

    @Override
    public AbstractChunkPosWrapper createChunkPos(long xAndZPositionCombined) {
        return new ChunkPosWrapper(xAndZPositionCombined);
    }

    @Override
    public AbstractChunkPosWrapper createChunkPos(int x, int z) {
        return new ChunkPosWrapper(x, z);
    }

    @Override
    public AbstractChunkPosWrapper createChunkPos(AbstractChunkPosWrapper newChunkPos) {
        return new ChunkPosWrapper(newChunkPos);
    }

    @Override
    public AbstractChunkPosWrapper createChunkPos(AbstractBlockPosWrapper blockPos) {
        return new ChunkPosWrapper(blockPos);
    }
    
    @Override
    public AbstractBatchGenerationEnvionmentWrapper createBatchGenerator(LodBuilder newLodBuilder,
			LodDimension newLodDimension, IWorldWrapper worldWrapper) {
		return new BatchGenerationEnvironment(worldWrapper, newLodBuilder, newLodDimension);
	}
}
