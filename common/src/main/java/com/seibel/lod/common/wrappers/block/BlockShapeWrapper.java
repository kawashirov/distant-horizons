
package com.seibel.lod.common.wrappers.block;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.seibel.lod.core.wrapperInterfaces.block.IBlockShapeWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.common.wrappers.chunk.ChunkWrapper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;


/**
 * @author James Seibel
 * @version 11-21-2021
 */
public class BlockShapeWrapper implements IBlockShapeWrapper
{
    //set of block which require tint
    public static final ConcurrentMap<Block, BlockShapeWrapper> blockShapeWrapperMap = new ConcurrentHashMap<>();
    public static BlockShapeWrapper WATER_SHAPE = new BlockShapeWrapper();

    private final Block block;
    private final boolean toAvoid;
    private boolean nonFull;
    private boolean noCollision;

    /**Constructor only require for the block instance we are wrapping**/
    public BlockShapeWrapper(Block block, IChunkWrapper chunkWrapper, int x, int y, int z)
    {
        this.block = block;
        this.nonFull = false;
        this.noCollision = false;
        this.toAvoid = ofBlockToAvoid();
        setupShapes(chunkWrapper, x, y, z);
        System.out.println(block + " non full " + nonFull + " no collision " + noCollision + " to avoid " + toAvoid);
    }

    private BlockShapeWrapper()
    {
        this.block = Blocks.WATER;
        this.nonFull = false;
        this.noCollision = false;
        this.toAvoid = false;
    }

    /**
     * this return a wrapper of the block in input
     * @param block Block object to wrap
     */
    static public BlockShapeWrapper getBlockShapeWrapper(Block block, ChunkWrapper chunkWrapper, int x, int y, int z)
    {
        //first we check if the block has already been wrapped
        if (blockShapeWrapperMap.containsKey(block) && blockShapeWrapperMap.get(block) != null)
            return blockShapeWrapperMap.get(block);


        //if it hasn't been created yet, we create it and save it in the map
        BlockShapeWrapper blockWrapper = new BlockShapeWrapper(block, chunkWrapper, x, y, z);
        blockShapeWrapperMap.put(block, blockWrapper);

        //we return the newly created wrapper
        return blockWrapper;
    }

    private void setupShapes(IChunkWrapper chunkWrapper, int x, int y, int z)
    {
        ChunkAccess chunk = ((ChunkWrapper) chunkWrapper).getChunk();
        BlockPos blockPos = new BlockPos(x, y, z);
        boolean noCollisionSetted = false;
        boolean nonFullSetted = false;
        if (!block.defaultBlockState().getFluidState().isEmpty())// || block instanceof SixWayBlock)
        {
            noCollisionSetted = true;
            nonFullSetted = true;
            noCollision = false;
            nonFull = false;
        }
        if (!nonFullSetted)
        {
            VoxelShape voxelShape = block.defaultBlockState().getShape(chunk, blockPos);

            if (!voxelShape.isEmpty())
            {
                AABB bbox = voxelShape.bounds();
                double xWidth = (bbox.maxX - bbox.minX);
                double yWidth = (bbox.maxY - bbox.minY);
                double zWidth = (bbox.maxZ - bbox.minZ);
                nonFull = xWidth < 1 && zWidth < 1 && yWidth < 1;
            }
            else
            {
                nonFull = false;
            }
        }

        if (!noCollisionSetted)
        {
            VoxelShape collisionShape = block.defaultBlockState().getCollisionShape(chunk, blockPos);
            noCollision = collisionShape.isEmpty();
        }
    }

    @Override
    public boolean ofBlockToAvoid()
    {
        return block.equals(Blocks.AIR)
                || block.equals(Blocks.CAVE_AIR)
                || block.equals(Blocks.BARRIER);
    }
//-----------------//
//Avoidance getters//
//-----------------//


    @Override
    public boolean isNonFull()
    {
        return nonFull;
    }

    @Override
    public boolean hasNoCollision()
    {
        return noCollision;
    }

    @Override
    public boolean isToAvoid()
    {
        return toAvoid;
    }




    @Override public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof BlockShapeWrapper))
            return false;
        BlockShapeWrapper that = (BlockShapeWrapper) o;
        return Objects.equals(block, that.block);
    }

    @Override public int hashCode()
    {
        return Objects.hash(block);
    }
}