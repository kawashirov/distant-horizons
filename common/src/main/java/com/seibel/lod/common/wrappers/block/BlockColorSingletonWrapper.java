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

package com.seibel.lod.common.wrappers.block;

import com.seibel.lod.core.wrapperInterfaces.block.IBlockColorSingletonWrapper;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockColorWrapper;

import net.minecraft.world.level.block.Blocks;


/**
 * Contains methods that would have been static in BlockColorWrapper.
 * Since interfaces can't create/implement static methods we have
 * to split the object up in two.
 *
 * @author James Seibel
 * @version 11-17-2021
 */
public class BlockColorSingletonWrapper implements IBlockColorSingletonWrapper
{
    public static final BlockColorSingletonWrapper INSTANCE = new BlockColorSingletonWrapper();

    @Override
    public IBlockColorWrapper getWaterColor()
    {
        return BlockColorWrapper.getBlockColorWrapper(Blocks.WATER.defaultBlockState(), new BlockPosWrapper(0,0, 0));
    }
}

