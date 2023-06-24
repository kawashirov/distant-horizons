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

package com.seibel.distanthorizons.common.wrappers;

import java.nio.FloatBuffer;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

#if PRE_MC_1_19_3
import com.mojang.math.Matrix4f;
#else
import org.joml.Matrix4f;
#endif

import com.seibel.distanthorizons.core.enums.ELodDirection;
import com.seibel.distanthorizons.core.pos.DhBlockPos;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.coreapi.util.math.Mat4f;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;

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
    private static void storeMatrix(Matrix4f matrix, FloatBuffer buffer) {
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
        #if PRE_MC_1_19_4
        matrix.transpose(); // In 1.19.3 and later, we no longer need to transpose it
        #endif
        return matrix;
    }


    static final Direction[] directions;
    static final ELodDirection[] lodDirections;
    static {
    	ELodDirection[] lodDirs = ELodDirection.values();
    	directions = new Direction[lodDirs.length];
    	lodDirections = new ELodDirection[lodDirs.length];
		for (ELodDirection lodDir : lodDirs)
		{
			Direction dir;
			switch (lodDir.name().toUpperCase())
			{
				case "DOWN":
					dir = Direction.DOWN;
					break;
				case "UP":
					dir = Direction.UP;
					break;
				case "NORTH":
					dir = Direction.NORTH;
					break;
				case "SOUTH":
					dir = Direction.SOUTH;
					break;
				case "WEST":
					dir = Direction.WEST;
					break;
				case "EAST":
					dir = Direction.EAST;
					break;
				default:
					dir = null;
					break;
			}
			
			if (dir == null)
			{
				throw new IllegalArgumentException("Invalid direction on init mapping: " + lodDir);
			}
			directions[lodDir.ordinal()] = dir;
			lodDirections[dir.ordinal()] = lodDir;
		}
    }

    public static BlockPos Convert(DhBlockPos wrappedPos) {
    	return new BlockPos(wrappedPos.x, wrappedPos.y, wrappedPos.z);
    }
    public static ChunkPos Convert(DhChunkPos wrappedPos) {
        return new ChunkPos(wrappedPos.x, wrappedPos.z);
    }

    public static Direction Convert(ELodDirection lodDirection)
    {
        return directions[lodDirection.ordinal()];
    }
    public static ELodDirection Convert(Direction direction)
    {
        return lodDirections[direction.ordinal()];
    }
    public static void DebugCheckAllPackers() {
        BiConsumer<Integer, Integer> func = (x, z) -> DhChunkPos._DebugCheckPacker(x,z,ChunkPos.asLong(x,z));
        func.accept(0,0);
        func.accept(12345,134);
        func.accept(-12345,-134);
        func.accept(-30000000/16,30000000/16);
        func.accept(30000000/16,-30000000/16);
        func.accept(30000000/16,30000000/16);
        func.accept(-30000000/16,-30000000/16);
        Consumer<BlockPos> func2 = (p) -> DhBlockPos._DebugCheckPacker(p.getX(),p.getY(),p.getZ(),p.asLong());
        func2.accept(new BlockPos(0,0,0));
        func2.accept(new BlockPos(12345,134,123));
        func2.accept(new BlockPos(-12345,-134,-80));
        func2.accept(new BlockPos(-30000000, 2047, 30000000));
        func2.accept(new BlockPos(30000000, -2048, -30000000));
        func2.accept(new BlockPos(30000000, 2047, 30000000));
        func2.accept(new BlockPos(-30000000, -2048, -30000000));
    }
}
