package com.seibel.lod.common.wrappers.block;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockStateWrapper;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class BlockStateWrapper implements IBlockStateWrapper {
    public static final BlockStateWrapper AIR = new BlockStateWrapper(null);

    public static ConcurrentHashMap<BlockState, BlockStateWrapper> cache = new ConcurrentHashMap<>();

    public static BlockStateWrapper fromBlockState(BlockState blockState) {
        if (blockState == null || blockState.isAir()) return AIR;
        if (blockState.getFluidState() != null)
            return cache.computeIfAbsent(blockState.getFluidState().createLegacyBlock(), BlockStateWrapper::new);
        return cache.computeIfAbsent(blockState, BlockStateWrapper::new);
    }

    public final BlockState blockState;
    private BlockStateWrapper(BlockState blockState) {
        this.blockState = blockState;
    }

    @Override
    public String serialize() {
        if (blockState == null) {
            return "AIR";
        }
        return BlockState.CODEC.encodeStart(JsonOps.COMPRESSED, blockState).get().orThrow().toString();
    }

    public static BlockStateWrapper deserialize(String str) {
        if (str.equals("AIR")) {
            return AIR;
        }
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
