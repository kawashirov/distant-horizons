package com.seibel.lod.wrappers.Block;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import java.util.Objects;

public class BlockPosWrapper {
    private final BlockPos.Mutable blockPos;


    public BlockPosWrapper()
    {
        this.blockPos = new BlockPos.Mutable(0,0,0);
    }

    public BlockPosWrapper(int x, int y, int z)
    {
        this.blockPos = new BlockPos.Mutable(x, y, z);
    }

    public void set(int x, int y, int z)
    {
        blockPos.set(x, y, z);
    }

    public int getX()
    {
        return blockPos.getX();
    }

    public int getY()
    {
        return blockPos.getY();
    }

    public int getZ()
    {
        return blockPos.getZ();
    }

    public int get(Direction.Axis axis)
    {
        return axis.choose(getX(), getY(), getZ());
    }

    public BlockPos.Mutable getBlockPos()
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

    public BlockPosWrapper offset(int x, int y, int z)
    {
        blockPos.set(blockPos.getX() + x, blockPos.getY() + y, blockPos.getZ() + z);
        return this;
    }

}
