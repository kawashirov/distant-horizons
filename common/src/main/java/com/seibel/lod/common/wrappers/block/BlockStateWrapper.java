package com.seibel.lod.common.wrappers.block;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockStateWrapper;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

public class BlockStateWrapper implements IBlockStateWrapper {
    public final BlockState blockState;
    public BlockStateWrapper(BlockState blockState) {
        this.blockState = blockState;
    }

    @Override
    public String serialize() {
        return BlockState.CODEC.encodeStart(JsonOps.COMPRESSED, blockState).get().orThrow().toString();
    }

    public static BlockStateWrapper deserialize(String str) {
        return new BlockStateWrapper(
                BlockState.CODEC.decode(JsonOps.COMPRESSED, JsonParser.parseString(str)).get().orThrow().getFirst()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockStateWrapper that = (BlockStateWrapper) o;
        return Objects.equals(blockState, that.blockState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockState);
    }
}
