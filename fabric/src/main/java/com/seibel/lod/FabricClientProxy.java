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

import com.seibel.lod.common.wrappers.McObjectConverter;
import com.seibel.lod.common.wrappers.world.LevelWrapper;
import com.seibel.lod.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.lod.core.api.internal.a7.ClientApi;
import com.seibel.lod.core.config.Config;
import com.mojang.blaze3d.platform.InputConstants;
import com.seibel.lod.common.wrappers.chunk.ChunkWrapper;

import com.seibel.lod.core.logging.DhLoggerBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

/**
 * This handles all events sent to the client,
 * and is the starting point for most of the mod.
 * 
 * @author coolGi
 * @author Ran
 * @version 11-23-2021
 */
@Environment(EnvType.CLIENT)
public class FabricClientProxy
{
	private final ClientApi clientApi = ClientApi.INSTANCE;
	private static final Logger LOGGER = DhLoggerBuilder.getLogger("FabricClientProxy");

	/**
	 * Registers Fabric Events
	 * @author Ran
	 */
	public void registerEvents() {
		LOGGER.info("Registering Fabric Client Events");


		/* Register the mod needed event callbacks */

		// ClientTickEvent
		ClientTickEvents.START_CLIENT_TICK.register((client) -> {
			//LOGGER.info("ClientTickEvent.START_CLIENT_TICK");
			ClientApi.INSTANCE.clientTickEvent();
		});

		// ClientLevelLoadEvent - Done in MixinClientPacketListener
		// ClientLevelUnloadEvent - Done in MixinClientPacketListener

		// ClientChunkLoadEvent
		// TODO: Is using setClientLightReady one still better?
		//#if PRE_MC_1_18_1 // in 1.18+, we use mixin hook in setClientLightReady(true)
		ClientChunkEvents.CHUNK_LOAD.register((level, chunk) ->
				ClientApi.INSTANCE.clientChunkLoadEvent(
						new ChunkWrapper(chunk, level),
						LevelWrapper.getWorldWrapper(level)
				));
		//#endif
		// ClientChunkSaveEvent
		ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk)->
				ClientApi.INSTANCE.clientChunkSaveEvent(
						new ChunkWrapper(chunk, level),
						LevelWrapper.getWorldWrapper(level)
				));

		// RendererStartupEvent - Done in MixinGameRenderer
		// RendererShutdownEvent - Done in MixinGameRenderer


		// ClientRenderLevelTerrainEvent
		WorldRenderEvents.AFTER_SETUP.register((renderContext) ->
				clientApi.renderLods(getLevelWrapper(renderContext.world()),
				McObjectConverter.Convert(renderContext.projectionMatrix()),
				McObjectConverter.Convert(renderContext.matrixStack().last().pose()),
				renderContext.tickDelta())
		);

		// Debug keyboard event
		// FIXME: Use better hooks so it doesn't trigger even in text boxes
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player != null && isValidTime()) onKeyInput();
		});
	}

	private boolean isValidTime() {
		return !(Minecraft.getInstance().screen instanceof TitleScreen);
	}
	private LevelWrapper getLevelWrapper(Level level) {
		return LevelWrapper.getWorldWrapper(level);
	}

//	public void blockChangeEvent(LevelAccessor world, BlockPos pos) {
//		if (!isValidTime()) return;
//		IChunkWrapper chunk = new ChunkWrapper(world.getChunk(pos), world);
//		DimensionTypeWrapper dimType = DimensionTypeWrapper.getDimensionTypeWrapper(world.dimensionType());
//
//		// recreate the LOD where the blocks were changed
//		// TODO: serverApi.blockChangeEvent(chunk, dimType);
//	}

	private static final int[] KEY_TO_CHECK_FOR = {GLFW.GLFW_KEY_F6, GLFW.GLFW_KEY_F8};
	
	HashSet<Integer> previousKeyDown = new HashSet<Integer>();
	
	public void onKeyInput() {
		if (Config.Client.Advanced.Debugging.enableDebugKeybindings.get())
		{
			HashSet<Integer> currentKeyDown = new HashSet<Integer>();
			
			// Note: Minecraft's InputConstants is same as GLFW Key values
			//TODO: Use mixin to hook directly into the GLFW Keyboard event in minecraft KeyboardHandler
			// Check all keys we need
			for (int i = GLFW.GLFW_KEY_A; i <= GLFW.GLFW_KEY_Z; i++) {
				if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), i)) {
					currentKeyDown.add(i);
				}
			}
			for (int i : KEY_TO_CHECK_FOR) {
				if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), i)) {
					currentKeyDown.add(i);
				}
			}
			// Diff and trigger events
			for (int c : currentKeyDown) {
				if (!previousKeyDown.contains(c)) {
					ClientApi.INSTANCE.keyPressedEvent(c);
				}
			}
			// Update the set
			previousKeyDown = currentKeyDown;
		}
	}
}
