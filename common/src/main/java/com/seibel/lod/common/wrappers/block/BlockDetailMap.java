package com.seibel.lod.common.wrappers.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.ConcurrentHashMap;

public class BlockDetailMap
{
    private static ConcurrentHashMap<BlockState, BlockDetailWrapper> map = new ConcurrentHashMap<BlockState, BlockDetailWrapper>();

    private BlockDetailMap() {}

    public static BlockDetailWrapper getOrMakeBlockDetailCache(BlockState bs, BlockPos pos, LevelReader getter) {
        BlockDetailWrapper cache = map.get(bs);
        if (cache != null) return cache;
        if (bs.getFluidState().isEmpty()) {
            cache = BlockDetailWrapper.make(bs, pos, getter);
        } else {
            cache = BlockDetailWrapper.make(bs.getFluidState().createLegacyBlock(), pos, getter);
        }
        BlockDetailWrapper cacheCAS = map.putIfAbsent(bs, cache);
        return cacheCAS==null ? cache : cacheCAS;
    }
}
