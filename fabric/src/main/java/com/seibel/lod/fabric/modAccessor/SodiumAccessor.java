package com.seibel.lod.fabric.modAccessor;

import java.util.HashSet;
import java.util.stream.Collectors;

import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.ISodiumAccessor;

import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;

public class SodiumAccessor implements ISodiumAccessor {
    IWrapperFactory factory = SingletonHandler.get(IWrapperFactory.class);

    @Override
    public String getModName() {
        return "Sodium-Fabric-1.18.X";
    }

    @Override
    public HashSet<AbstractChunkPosWrapper> getNormalRenderedChunks() {
        SodiumWorldRenderer renderer = SodiumWorldRenderer.instance();
        LevelHeightAccessor height =  Minecraft.getInstance().level;
        // 0b11 = Lighted chunk & loaded chunk
        return renderer.getChunkTracker().getChunks(0b11).filter(
                (long l) -> {
                    for (int i = height.getMinSection(); i<height.getMaxSection(); i++) {
                        SectionPos p = SectionPos.of(new ChunkPos(l), i);
                        if (renderer.isBoxVisible(p.minBlockX()+1, p.minBlockY()+1, p.minBlockZ()+1,
                                p.maxBlockX()-1, p.maxBlockY()-1, p.maxBlockZ()-1)) return true;
                    }
                    return false;
                }).mapToObj((long l) -> {
            return (AbstractChunkPosWrapper)factory.createChunkPos(l);
        }).collect(Collectors.toCollection(HashSet::new));
    }

}
