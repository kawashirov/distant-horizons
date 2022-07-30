package com.seibel.lod.common.wrappers.block.cache;

import com.seibel.lod.common.wrappers.McObjectConverter;
import com.seibel.lod.core.objects.DHBlockPos;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Arrays;

public class ServerBlockStateCache {
    public final BlockState state;
    public final LevelReader level;
    public final BlockPos pos;

    public ServerBlockStateCache(BlockState blockState, ILevelWrapper samplingLevel, DHBlockPos samplingPos) {
        state = blockState;
        level = (LevelReader) samplingLevel.unwrapLevel();
        pos = McObjectConverter.Convert(samplingPos);
        resolveShapes();
    }

    boolean noCollision = false;
    boolean[] occludeFaces = null;
    boolean[] fullFaces = null;
    boolean isShapeResolved = false;
    public void resolveShapes() {
        if (isShapeResolved) return;
        if (state.getFluidState().isEmpty()) {
            noCollision = state.getCollisionShape(level, pos).isEmpty();
            occludeFaces = new boolean[6];
            if (state.canOcclude()) {
				for (Direction dir : Direction.values()) {
                    // Note: isEmpty() isn't quite correct... best would be a isFull() or something...
                    occludeFaces[McObjectConverter.Convert(dir).ordinal()]
							= !state.getFaceOcclusionShape(level, pos, dir).isEmpty();
				}
            }

            VoxelShape voxelShape = state.getShape(level, pos);
            fullFaces = new boolean[6];
            if (!voxelShape.isEmpty()) {
                for (Direction dir : Direction.values()) {
                    VoxelShape faceShape = voxelShape.getFaceShape(dir);
                    AABB aabb = faceShape.bounds();
                    boolean xFull = aabb.minX <= 0.01 && aabb.maxX >= 0.99;
                    boolean yFull = aabb.minY <= 0.01 && aabb.maxY >= 0.99;
                    boolean zFull = aabb.minZ <= 0.01 && aabb.maxZ >= 0.99;
                    fullFaces[McObjectConverter.Convert(dir).ordinal()] =
                            (xFull || dir.getAxis().equals(Direction.Axis.X))
                            && (yFull || dir.getAxis().equals(Direction.Axis.Y))
                            && (zFull || dir.getAxis().equals(Direction.Axis.Z));
                }
            }
        } else { // Liquid Block. Treat as full block
            occludeFaces = new boolean[6];
            Arrays.fill(occludeFaces, true);
            fullFaces = new boolean[6];
            Arrays.fill(fullFaces, true);
        }
    }

}
