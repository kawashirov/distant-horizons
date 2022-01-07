/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.fabric;

import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.api.EventApi;
import com.seibel.lod.common.wrappers.chunk.ChunkWrapper;
import com.seibel.lod.common.wrappers.world.DimensionTypeWrapper;
import com.seibel.lod.common.wrappers.world.WorldWrapper;

import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.fabric.mixins.events.MixinClientLevel;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.mixin.event.lifecycle.client.ClientChunkManagerMixin;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.LevelChunk;
import org.lwjgl.glfw.GLFW;

/**
 * This handles all events sent to the client,
 * and is the starting point for most of the mod.
 *
 * @author coolGi2007
 * @author Ran
 * @version 11-23-2021
 */
public class ClientProxy
{
	private final EventApi eventApi = EventApi.INSTANCE;


	/**
	 * Registers Fabric Events
	 * @author Ran
	 */
	public void registerEvents() {
		// TODO: Fix this if it's wrong

		/* World Events */
		//ServerTickEvents.START_SERVER_TICK.register(this::serverTickEvent);
		ServerTickEvents.END_SERVER_TICK.register(this::serverTickEvent);

		/* World Events */
		//ServerChunkEvents.CHUNK_LOAD.register(this::chunkLoadEvent);
		ClientChunkEvents.CHUNK_LOAD.register(this::chunkLoadEvent);



		/* World Events */
		ServerWorldEvents.LOAD.register((server, level) -> this.worldLoadEvent(level));
		ServerWorldEvents.UNLOAD.register((server, level) -> this.worldUnloadEvent(level));

		/* The Client World Events are in the mixins
		Client world load event is in MixinClientLevel
		Client world unload event is in MixinMinecraft */
		/* The save events are in MixinServerLevel */

		/* Keyboard Events */
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player != null) onKeyInput();
		});
	}


	public void serverTickEvent(MinecraftServer server)
	{
		eventApi.serverTickEvent();
	}

	public void chunkLoadEvent(LevelAccessor level, LevelChunk chunk)
	{
		eventApi.chunkLoadEvent(new ChunkWrapper(chunk), DimensionTypeWrapper.getDimensionTypeWrapper(level.dimensionType()));
	}

	public void worldSaveEvent()
	{
		eventApi.worldSaveEvent();
	}

	/** This is also called when a new dimension loads */
	public void worldLoadEvent(Level level)
	{
		if (level != null) {
			eventApi.worldLoadEvent(WorldWrapper.getWorldWrapper(level));
		}
	}

	public void worldUnloadEvent(Level level)
	{
		if (level != null) {
			eventApi.worldUnloadEvent(WorldWrapper.getWorldWrapper(level));
		}
	}

	/**
	 * Can someone tell me how to make this better
	 * @author Ran
	 *
	 * public void blockChangeEvent(BlockEventData event) {
	 * 		// we only care about certain block events
	 * 		if (event.getClass() == BlockEventData.BreakEvent.class ||
	 * 				event.getClass() == BlockEventData.EntityPlaceEvent.class ||
	 * 				event.getClass() == BlockEventData.EntityMultiPlaceEvent.class ||
	 * 				event.getClass() == BlockEventData.FluidPlaceBlockEvent.class ||
	 * 				event.getClass() == BlockEventData.PortalSpawnEvent.class)
	 *        {
	 * 			IChunkWrapper chunk = new ChunkWrapper(event.getWorld().getChunk(event.getPos()));
	 * 			DimensionTypeWrapper dimType = DimensionTypeWrapper.getDimensionTypeWrapper(event.getWorld().dimensionType());
	 *
	 * 			// recreate the LOD where the blocks were changed
	 * 			eventApi.blockChangeEvent(chunk, dimType);
	 *        }
	 * }
	 */
	public void blockChangeEvent(LevelAccessor world, BlockPos pos) {
		IChunkWrapper chunk = new ChunkWrapper(world.getChunk(pos));
		DimensionTypeWrapper dimType = DimensionTypeWrapper.getDimensionTypeWrapper(world.dimensionType());

		// recreate the LOD where the blocks were changed
		eventApi.blockChangeEvent(chunk, dimType);
	}



	// The debug mode keybinding, which will be registered
	public static final KeyMapping DebugToggle = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.lod.DebugToggle", GLFW.GLFW_KEY_F8, "key.lod.category"));

	// The draw toggle keybinding, which will be registered
	public static final KeyMapping DrawToggle = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.lod.DrawToggle", GLFW.GLFW_KEY_F6, "key.lod.category"));

	boolean PreDebugToggle = false;
	boolean PreDrawToggle = false;
	public void onKeyInput() {
		ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
		if (CONFIG.client().advanced().debugging().getDebugKeybindingsEnabled())
		{
			// Only activates when you press the key
			if (DebugToggle.isDown() && DebugToggle.isDown() != PreDebugToggle)
				CONFIG.client().advanced().debugging().setDebugMode(CONFIG.client().advanced().debugging().getDebugMode().getNext());

			if (DrawToggle.isDown() && DrawToggle.isDown() != PreDebugToggle)
				CONFIG.client().advanced().debugging().setDrawLods(!CONFIG.client().advanced().debugging().getDrawLods());
		}
		PreDebugToggle = DebugToggle.isDown();
		PreDrawToggle = DrawToggle.isDown();
	}
}
