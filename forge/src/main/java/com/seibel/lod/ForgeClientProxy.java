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

package com.seibel.lod;

import com.seibel.lod.common.wrappers.world.ClientLevelWrapper;
import com.seibel.lod.common.wrappers.world.ServerLevelWrapper;
import com.seibel.lod.core.api.internal.a7.ClientApi;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.event.world.ChunkDataEvent;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import com.seibel.lod.common.wrappers.chunk.ChunkWrapper;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * This handles all events sent to the client,
 * and is the starting point for most of the mod.
 * 
 * @author James_Seibel
 * @version 11-12-2021
 */
public class ForgeClientProxy
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();

	@SubscribeEvent
	public void clientTickEvent(TickEvent.ClientTickEvent event)
	{
		if (event.phase == TickEvent.Phase.START)
		{
			ClientApi.INSTANCE.clientTickEvent();
		}
	}

	@SubscribeEvent
	public void clientLevelLoadEvent(WorldEvent.Load event)
	{
		if (event.getWorld() instanceof ClientLevel)
		{
			ClientApi.INSTANCE.clientLevelLoadEvent(ClientLevelWrapper.getWrapper((ClientLevel) event.getWorld()));
		}
	}
	@SubscribeEvent
	public void clientLevelUnloadEvent(WorldEvent.Unload event)
	{
		if (event.getWorld() instanceof ClientLevel)
		{
			ClientApi.INSTANCE.clientLevelUnloadEvent(ClientLevelWrapper.getWrapper((ClientLevel) event.getWorld()));
		}
	}

	@SubscribeEvent
	public void clientChunkLoadEvent(ChunkDataEvent.Load event)
	{
		if (event.getWorld() instanceof ClientLevel)
		{
			ClientLevelWrapper wrappedLevel = ClientLevelWrapper.getWrapper((ClientLevel) event.getWorld());
			IChunkWrapper chunk = new ChunkWrapper(event.getChunk(), event.getWorld(), wrappedLevel);
			ClientApi.INSTANCE.clientChunkLoadEvent(chunk, ClientLevelWrapper.getWrapper((ClientLevel) event.getWorld()));
		}
	}
	@SubscribeEvent
	public void clientChunkSaveEvent(ChunkDataEvent.Save event)
	{
		if (event.getWorld() instanceof ClientLevel)
		{
			ClientLevelWrapper wrappedLevel = ClientLevelWrapper.getWrapper((ClientLevel) event.getWorld());
			IChunkWrapper chunk = new ChunkWrapper(event.getChunk(), event.getWorld(), wrappedLevel);
			ClientApi.INSTANCE.clientChunkSaveEvent(chunk, ClientLevelWrapper.getWrapper((ClientLevel) event.getWorld()));
		}
	}

	// RendererStartupEvent - Done in MixinGameRenderer
	// RendererShutdownEvent - Done in MixinGameRenderer
	// ClientRenderLevelTerrainEvent - Done in MixinGameRenderer

	// Register KeyBindings
	@SubscribeEvent
	public void registerKeyBindings(InputEvent.KeyInputEvent event)
	{
		if (Minecraft.getInstance().player == null) return;
		if (event.getAction() != GLFW.GLFW_PRESS) return;
		ClientApi.INSTANCE.keyPressedEvent(event.getKey());
	}

//	@SubscribeEvent
//	public void serverTickEvent(TickEvent.ServerTickEvent event)
//	{
//		if (event.phase != TickEvent.Phase.START) return;
//		eventApi.serverTickEvent();
//	}
////
//
//	@SubscribeEvent
//	public void chunkLoadEvent(ChunkEvent.Load event)
//	{
//		clientApi.clientChunkLoadEvent(new ChunkWrapper(event.getChunk(), event.getWorld()), LevelWrapper.getWorldWrapper(event.getWorld()));
//	}
//
//	@SubscribeEvent
//	public void worldSaveEvent(WorldEvent.Save event)
//	{
//		eventApi.worldSaveEvent();
//	}
//
//	/** This is also called when a new dimension loads */
//	@SubscribeEvent
//	public void worldLoadEvent(WorldEvent.Load event)
//	{
//		if (Minecraft.getInstance().screen instanceof TitleScreen) return;
//		if (event.getWorld() != null) {
//			eventApi.worldLoadEvent(LevelWrapper.getWorldWrapper(event.getWorld()));
//		}
//	}
//
//	@SubscribeEvent
//	public void worldUnloadEvent(WorldEvent.Unload event)
//	{
//		eventApi.worldUnloadEvent(LevelWrapper.getWorldWrapper(event.getWorld()));
//	}
//
//	@SubscribeEvent
//	public void blockChangeEvent(BlockEvent event)
//	{
//		// we only care about certain block events
//		if (event.getClass() == BlockEvent.BreakEvent.class ||
//				event.getClass() == BlockEvent.EntityPlaceEvent.class ||
//				event.getClass() == BlockEvent.EntityMultiPlaceEvent.class ||
//				event.getClass() == BlockEvent.FluidPlaceBlockEvent.class ||
//				event.getClass() == BlockEvent.PortalSpawnEvent.class)
//		{
//			IChunkWrapper chunk = new ChunkWrapper(event.getWorld().getChunk(event.getPos()), event.getWorld());
//			DimensionTypeWrapper dimType = DimensionTypeWrapper.getDimensionTypeWrapper(event.getWorld().dimensionType());
//
//			// recreate the LOD where the blocks were changed
//			eventApi.blockChangeEvent(chunk, dimType);
//		}
//	}
//
//	@SubscribeEvent
//	public void onKeyInput(InputEvent.KeyInputEvent event)
//	{
//		if (Minecraft.getInstance().player == null) return;
//		if (event.getAction() != GLFW.GLFW_PRESS) return;
//		clientApi.keyPressedEvent(event.getKey());
//	}
//
	
	
}
