package com.seibel.lod.common.wrappers.block.cache;

import com.seibel.lod.common.wrappers.block.BiomeWrapper;
import com.seibel.lod.common.wrappers.world.ClientLevelWrapper;
import com.seibel.lod.core.objects.DHBlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.ConcurrentHashMap;

public class ClientBlockDetailMap {
    private final ConcurrentHashMap<BlockState, ClientBlockStateCache> blockCache = new ConcurrentHashMap<>();
    //private final ConcurrentHashMap<#if PRE_MC_1_18_2 Biome #else Holder<Biome> #endif, Biome> biomeMap = new ConcurrentHashMap<>();
    private final ClientLevelWrapper level;
    public ClientBlockDetailMap(ClientLevelWrapper level) { this.level = level; }

    public ClientBlockStateCache getBlockStateData(BlockState state, DHBlockPos pos) { //TODO: Allow a per pos unique setting
        return blockCache.computeIfAbsent(state, (s) -> new ClientBlockStateCache(s, level, new DHBlockPos(0,0,0)));
    }

    public void clear() { blockCache.clear(); }

    public int getColor(BlockState state, BiomeWrapper biome, DHBlockPos pos) {
    	return getBlockStateData(state, pos).getAndResolveFaceColor(biome);
    }
}
