/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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

import java.nio.FloatBuffer;

#if PRE_MC_1_19_3
import com.mojang.math.Matrix4f;
#else
import org.joml.Matrix4f;
#endif
import com.seibel.lod.common.wrappers.block.BlockPosWrapper;
import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * This class converts to and from Minecraft objects (Ex: Matrix4f)
 * and objects we created (Ex: Mat4f).
 *
 * @author James Seibel
 * @version 11-20-2021
 */
public class McObjectConverter
{
    /** 4x4 float matrix converter */
    public static Mat4f Convert(Matrix4f mcMatrix)
    {
        FloatBuffer buffer = FloatBuffer.allocate(16);
        #if PRE_MC_1_19_3
        mcMatrix.store(buffer);
        #else
        mcMatrix.store(buffer);
        #endif
        Mat4f matrix = new Mat4f(buffer);
        matrix.transpose();
        return matrix;
    }


    static final Direction[] directions;
    static final LodDirection[] lodDirections;
    static {
    	LodDirection[] lodDirs = LodDirection.values();
    	directions = new Direction[lodDirs.length];
    	lodDirections = new LodDirection[lodDirs.length];
    	for (LodDirection lodDir : lodDirs) {
    		Direction dir = Enum.valueOf(Direction.class, lodDir.name());
            if (dir == null) {
                throw new IllegalArgumentException("Invalid direction on init mapping: " + lodDir);
            }
    		directions[lodDir.ordinal()] = dir;
    		lodDirections[dir.ordinal()] = lodDir;
    	}
    }
    
    public static BlockPos Convert(AbstractBlockPosWrapper wrappedPos) {
    	return new BlockPos(wrappedPos.getX(),wrappedPos.getY(), wrappedPos.getZ());
    }
    
    
    public static Direction Convert(LodDirection lodDirection)
    {
        return directions[lodDirection.ordinal()];
    }
    public static LodDirection Convert(Direction direction)
    {
        return lodDirections[direction.ordinal()];
    }
}
