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
    private static int bufferIndex(int x, int y) {
        return y * 4 + x;
    }
    /** Taken from Minecraft's com.mojang.math.Matrix4f class from 1.18.2 */
    public static void storeMatrix(Matrix4f matrix, FloatBuffer buffer) {
        #if PRE_MC_1_19_3
        matrix.store(buffer);
        #else
        // Mojang starts to use joml's Matrix4f libary in 1.19.3 so we copy their store method and use it here if its newer than 1.19.3
        buffer.put(bufferIndex(0, 0), matrix.m00());
        buffer.put(bufferIndex(0, 1), matrix.m01());
        buffer.put(bufferIndex(0, 2), matrix.m02());
        buffer.put(bufferIndex(0, 3), matrix.m03());
        buffer.put(bufferIndex(1, 0), matrix.m10());
        buffer.put(bufferIndex(1, 1), matrix.m11());
        buffer.put(bufferIndex(1, 2), matrix.m12());
        buffer.put(bufferIndex(1, 3), matrix.m13());
        buffer.put(bufferIndex(2, 0), matrix.m20());
        buffer.put(bufferIndex(2, 1), matrix.m21());
        buffer.put(bufferIndex(2, 2), matrix.m22());
        buffer.put(bufferIndex(2, 3), matrix.m23());
        buffer.put(bufferIndex(3, 0), matrix.m30());
        buffer.put(bufferIndex(3, 1), matrix.m31());
        buffer.put(bufferIndex(3, 2), matrix.m32());
        buffer.put(bufferIndex(3, 3), matrix.m33());
        #endif
    }



    /** 4x4 float matrix converter */
    public static Mat4f Convert(Matrix4f mcMatrix)
    {
        FloatBuffer buffer = FloatBuffer.allocate(16);
        storeMatrix(mcMatrix, buffer);
        Mat4f matrix = new Mat4f(buffer);
        #if PRE_MC_1_19_3
        matrix.transpose(); // In 1.19.3 and later, we no longer need to transpose it
        #endif
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
