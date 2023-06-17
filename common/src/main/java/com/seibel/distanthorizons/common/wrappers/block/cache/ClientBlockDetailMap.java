package com.seibel.distanthorizons.common.wrappers.block.cache;

import com.seibel.distanthorizons.common.wrappers.block.BiomeWrapper;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.pos.DhBlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.ConcurrentHashMap;

public class ClientBlockDetailMap {
    private final ConcurrentHashMap<BlockState, ClientBlockStateCache> blockCache = new ConcurrentHashMap<>();
    //private final ConcurrentHashMap<#if PRE_MC_1_18_2 Biome #else Holder<Biome> #endif, Biome> biomeMap = new ConcurrentHashMap<>();
    private final ClientLevelWrapper level;
    public ClientBlockDetailMap(ClientLevelWrapper level) { this.level = level; }

    public ClientBlockStateCache getBlockStateData(BlockState state, DhBlockPos pos) { //TODO: Allow a per pos unique setting
        return blockCache.computeIfAbsent(state, (s) -> new ClientBlockStateCache(s, level, new DhBlockPos(0,0,0)));
    }

    public void clear() { blockCache.clear(); }

    public int getColor(BlockState state, BiomeWrapper biome, DhBlockPos pos) {
    	return getBlockStateData(state, pos).getAndResolveFaceColor(biome);
    }
}
