package com.seibel.lod.common.wrappers.worldGeneration.mimicObject;

import com.seibel.lod.core.api.ModAccessorApi;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.IStarlightAccessor;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LightChunkGetter;

public class LightGetterAdaptor implements LightChunkGetter {
	private final BlockGetter heightGetter;
	public LightedWorldGenRegion genRegion = null;
	final boolean shouldReturnNull;

	public LightGetterAdaptor(BlockGetter heightAccessor) {
		this.heightGetter = heightAccessor;
		shouldReturnNull = ModAccessorApi.get(IStarlightAccessor.class) != null;
	}

	public void setRegion(LightedWorldGenRegion region) {
		genRegion = region;
	}

	@Override
	public BlockGetter getChunkForLighting(int chunkX, int chunkZ) {
		if (genRegion == null)
			throw new IllegalStateException("World Gen region has not been set!");
		// May be null
		return genRegion.getChunk(chunkX, chunkZ, ChunkStatus.EMPTY, false);
	}

	@Override
	public BlockGetter getLevel() {
		return shouldReturnNull ? null : (genRegion != null ? genRegion : heightGetter);
	}
}