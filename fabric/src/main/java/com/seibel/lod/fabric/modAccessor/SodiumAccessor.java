package com.seibel.lod.fabric.modAccessor;

import java.util.HashSet;
import java.util.stream.Collectors;

import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.ISodiumAccessor;

import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;

public class SodiumAccessor implements ISodiumAccessor {
	private final IMinecraftRenderWrapper MC_RENDER = SingletonHandler.get(IMinecraftRenderWrapper.class);
	
    @Override
    public String getModName() {
        return "Sodium-Fabric-1.16.5";
    }

	@Override
	public HashSet<AbstractChunkPosWrapper> getNormalRenderedChunks() {
		SodiumWorldRenderer renderer = SodiumWorldRenderer.getInstance();
		LevelAccessor height = Minecraft.getInstance().level;
		// TODO: Maybe use a mixin to make this more efficient
		return MC_RENDER.getMaximumRenderedChunks().stream().filter((AbstractChunkPosWrapper chunk) -> {
			FakeChunkEntity AABB = new FakeChunkEntity(chunk.getX(), chunk.getZ(), height.getMaxBuildHeight());
			return (renderer.isEntityVisible(AABB));
		}).collect(Collectors.toCollection(HashSet::new));
	}
	
	private static class FakeChunkEntity extends Entity {
		public int cx;
		public int cz;
		public int my;
		public FakeChunkEntity(int chunkX, int chunkZ, int maxHeight) {
			super(EntityType.AREA_EFFECT_CLOUD, null);
			cx = chunkX;
			cz = chunkZ;
			my = maxHeight;
		}
		@Override
		public AABB getBoundingBoxForCulling() {
			return new AABB(cx*16+1, 1, cz*16+1,
					cx*16+15, my-1, cz*16+15);
		}
		@Override
		protected void defineSynchedData() {}
		@Override
		protected void readAdditionalSaveData(CompoundTag paramCompoundTag) {}
		@Override
		protected void addAdditionalSaveData(CompoundTag paramCompoundTag) {}
		@Override
		public Packet<?> getAddEntityPacket() {
			throw new UnsupportedOperationException("This is a FAKE CHUNK ENTITY... For tricking the Sodium to check a AABB.");
		}}

}
