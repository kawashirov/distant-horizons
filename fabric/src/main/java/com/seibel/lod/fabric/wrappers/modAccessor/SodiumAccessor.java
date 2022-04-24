/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
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
#if PRE_MC_1_17_1
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
#else
import net.minecraft.world.level.LevelHeightAccessor;
#endif

public class SodiumAccessor implements ISodiumAccessor {
	private final IWrapperFactory factory = SingletonHandler.get(IWrapperFactory.class);
	private final IMinecraftRenderWrapper MC_RENDER = SingletonHandler.get(IMinecraftRenderWrapper.class);

	@Override
	public String getModName() {
		return "Sodium-Fabric";
	}

	#if POST_MC_1_17_1
	@Override
	public HashSet<AbstractChunkPosWrapper> getNormalRenderedChunks() {
		SodiumWorldRenderer renderer = SodiumWorldRenderer.instance();
		LevelHeightAccessor height =  Minecraft.getInstance().level;

		#if POST_MC_1_18_1
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
	#else
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
		}
	}
	#endif

}
