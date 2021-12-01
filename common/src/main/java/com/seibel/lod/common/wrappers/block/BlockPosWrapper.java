package com.seibel.lod.common.wrappers.block;

import java.util.Objects;

import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;

import net.minecraft.core.BlockPos;


/**
 * @author James Seibel
 * @version 11-21-2021
 */
public class BlockPosWrapper extends AbstractBlockPosWrapper
{
    private BlockPos.MutableBlockPos blockPos;


    public BlockPosWrapper()
    {
        this.blockPos = new BlockPos.MutableBlockPos(0,0,0);
    }

    public BlockPosWrapper(int x, int y, int z)
    {
        this.blockPos = new BlockPos.MutableBlockPos(x, y, z);
    }



    @Override
    public void set(int x, int y, int z)
    {
        blockPos.set(x, y, z);
    }

    @Override
    public int getX()
    {
        return blockPos.getX();
    }

    @Override
    public int getY()
    {
        return blockPos.getY();
    }

    @Override
    public int getZ()
    {
        return blockPos.getZ();
    }

    @Override
    public int get(LodDirection.Axis axis)
    {
        return axis.choose(getX(), getY(), getZ());
    }

    public BlockPos.MutableBlockPos getBlockPos()
    {
        return blockPos;
    }

    @Override public boolean equals(Object o)
    {
        return blockPos.equals(o);
    }

    @Override public int hashCode()
    {
        return Objects.hash(blockPos);
    }

    @Override
    public BlockPosWrapper offset(int x, int y, int z)
    {
        blockPos.set(blockPos.getX() + x, blockPos.getY() + y, blockPos.getZ() + z);
        return this;
    }

}
