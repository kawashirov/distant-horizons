package com.seibel.lod.fabric.wrappers.modAccessor;

import java.util.HashSet;
import java.util.stream.Collectors;

import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.ISodiumAccessor;


import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.LevelHeightAccessor;

public class SodiumAccessor implements ISodiumAccessor {
	private final IWrapperFactory factory = SingletonHandler.get(IWrapperFactory.class);
	private final IMinecraftRenderWrapper MC_RENDER = SingletonHandler.get(IMinecraftRenderWrapper.class);

	@Override
	public String getModName() {
		return "Sodium-Fabric";
	}

	@Override
	public HashSet<AbstractChunkPosWrapper> getNormalRenderedChunks() {
		SodiumWorldRenderer renderer = SodiumWorldRenderer.instance();
		LevelHeightAccessor height =  Minecraft.getInstance().level;

		#if MC_VERSION_1_18_1 || MC_VERSION_1_18_2
		// 0b11 = Lighted chunk & loaded chunk
		return renderer.getChunkTracker().getChunks(0b00).filter(
			(long l) -> {
				return true;
			}).mapToObj((long l) -> {
				return (AbstractChunkPosWrapper)factory.createChunkPos(l);
			}).collect(Collectors.toCollection(HashSet::new));
		#else
		// TODO: Maybe use a mixin to make this more efficient, and maybe ignore changes behind the camera
		return MC_RENDER.getMaximumRenderedChunks().stream().filter((AbstractChunkPosWrapper chunk) -> {
			return (renderer.isBoxVisible(
					chunk.getMinBlockX()+1, height.getMinBuildHeight()+1, chunk.getMinBlockZ()+1,
					chunk.getMinBlockX()+15, height.getMaxBuildHeight()-1, chunk.getMinBlockZ()+15));
		}).collect(Collectors.toCollection(HashSet::new));
		#endif
	}
}
