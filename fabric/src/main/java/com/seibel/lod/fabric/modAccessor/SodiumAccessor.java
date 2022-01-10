package com.seibel.lod.fabric.modAccessor;

import java.util.HashSet;
import java.util.stream.Collectors;

import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.ISodiumAccessor;


import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.LevelHeightAccessor;

public class SodiumAccessor implements ISodiumAccessor {
	private final IMinecraftRenderWrapper MC_RENDER = SingletonHandler.get(IMinecraftRenderWrapper.class);

    @Override
    public String getModName() {
    	return "Sodium-Fabric-1.17.1";
	}

	@Override
	public HashSet<AbstractChunkPosWrapper> getNormalRenderedChunks() {
		SodiumWorldRenderer renderer = SodiumWorldRenderer.instance();
		LevelHeightAccessor height = Minecraft.getInstance().level;
		
		// TODO: Maybe use a mixin to make this more efficient
		return MC_RENDER.getMaximumRenderedChunks().stream().filter((AbstractChunkPosWrapper chunk) -> {
			return (renderer.isBoxVisible(
					chunk.getMinBlockX()+1, height.getMinBuildHeight()+1, chunk.getMinBlockZ()+1,
					chunk.getMinBlockX()+15, height.getMaxBuildHeight()-1, chunk.getMinBlockZ()+15));
		}).collect(Collectors.toCollection(HashSet::new));
	}
}
