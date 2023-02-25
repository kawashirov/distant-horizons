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

package com.seibel.lod.fabric;

import com.seibel.lod.common.wrappers.McObjectConverter;
import com.seibel.lod.common.wrappers.world.ClientLevelWrapper;
import com.seibel.lod.core.api.internal.ClientApi;
import com.mojang.blaze3d.platform.InputConstants;
import com.seibel.lod.common.wrappers.chunk.ChunkWrapper;

import com.seibel.lod.core.dependencyInjection.ModAccessorInjector;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.ISodiumAccessor;
import com.seibel.lod.fabric.wrappers.modAccessor.SodiumAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

import java.util.HashSet;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
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
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	// TODO we shouldn't be filtering keys on the Forge/Fabric side, only in ClientApi
	private static final int[] KEY_TO_CHECK_FOR = { GLFW.GLFW_KEY_F6, GLFW.GLFW_KEY_F8, GLFW.GLFW_KEY_P};
	
	HashSet<Integer> previouslyPressKeyCodes = new HashSet<>();
	
	
	
	/**
	 * Registers Fabric Events
	 * @author Ran
	 */
	public void registerEvents()
	{
		LOGGER.info("Registering Fabric Client Events");
		
		
		/* Register the mod needed event callbacks */
		
		// ClientTickEvent
		ClientTickEvents.START_CLIENT_TICK.register((client) -> 
		{
			//LOGGER.info("ClientTickEvent.START_CLIENT_TICK");
			ClientApi.INSTANCE.clientTickEvent();
		});
		
		// ClientLevelLoadEvent - Done in MixinClientPacketListener
		// ClientLevelUnloadEvent - Done in MixinClientPacketListener
		
		// ClientChunkLoadEvent
		// TODO: Is using setClientLightReady one still better?
		//#if PRE_MC_1_18_1 // in 1.18+, we use mixin hook in setClientLightReady(true)
		ClientChunkEvents.CHUNK_LOAD.register((level, chunk) ->
		{
			ClientLevelWrapper wrappedLevel = ClientLevelWrapper.getWrapper(level);
			ClientApi.INSTANCE.clientChunkLoadEvent(
					new ChunkWrapper(chunk, level, wrappedLevel),
					wrappedLevel
			);
		});
		//#endif
		// ClientChunkSaveEvent
		ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) ->
		{
			ClientLevelWrapper wrappedLevel = ClientLevelWrapper.getWrapper(level);
			ClientApi.INSTANCE.clientChunkSaveEvent(
					new ChunkWrapper(chunk, level, wrappedLevel),
					wrappedLevel
			);
		});
		
		// RendererStartupEvent - Done in MixinGameRenderer
		// RendererShutdownEvent - Done in MixinGameRenderer
		
		SodiumAccessor sodiumAccessor = (SodiumAccessor) ModAccessorInjector.INSTANCE.get(ISodiumAccessor.class);
		
		// ClientRenderLevelTerrainEvent
		WorldRenderEvents.AFTER_SETUP.register((renderContext) ->
		{
			if (sodiumAccessor != null)
			{
				sodiumAccessor.levelWrapper = ClientLevelWrapper.getWrapper(renderContext.world());
				sodiumAccessor.mcModelViewMatrix = McObjectConverter.Convert(renderContext.matrixStack().last().pose());
				sodiumAccessor.mcProjectionMatrix = McObjectConverter.Convert(renderContext.projectionMatrix());
				sodiumAccessor.partialTicks = renderContext.tickDelta();
			}
			else
			{
				clientApi.renderLods(ClientLevelWrapper.getWrapper(renderContext.world()),
						McObjectConverter.Convert(renderContext.matrixStack().last().pose()),
						McObjectConverter.Convert(renderContext.projectionMatrix()),
						renderContext.tickDelta());
			}
		});

		// Debug keyboard event
		// FIXME: Use better hooks so it doesn't trigger even in text boxes
		ClientTickEvents.END_CLIENT_TICK.register(client -> 
		{
			if (client.player != null && isValidTime())
			{
				onKeyInput();
			}
		});
	}
	
	private boolean isValidTime() { return !(Minecraft.getInstance().screen instanceof TitleScreen); }

//	public void blockChangeEvent(LevelAccessor world, BlockPos pos) {
//		if (!isValidTime()) return;
//		IChunkWrapper chunk = new ChunkWrapper(world.getChunk(pos), world);
//		DimensionTypeWrapper dimType = DimensionTypeWrapper.getDimensionTypeWrapper(world.dimensionType());
//
//		// recreate the LOD where the blocks were changed
//		// TODO: serverApi.blockChangeEvent(chunk, dimType);
//	}
	
	public void onKeyInput()
	{
		HashSet<Integer> currentKeyDown = new HashSet<>();
		
		// Note: Minecraft's InputConstants is same as GLFW Key values
		//TODO: Use mixin to hook directly into the GLFW Keyboard event in minecraft KeyboardHandler
		// Check all keys we need
		for (int keyCode = GLFW.GLFW_KEY_A; keyCode <= GLFW.GLFW_KEY_Z; keyCode++)
		{
			if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), keyCode))
			{
				currentKeyDown.add(keyCode);
			}
		}
		
		for (int keyCode : KEY_TO_CHECK_FOR)
		{
			if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), keyCode))
			{
				currentKeyDown.add(keyCode);
			}
		}
		
		// Diff and trigger events
		for (int keyCode : currentKeyDown)
		{
			if (!previouslyPressKeyCodes.contains(keyCode))
			{
				ClientApi.INSTANCE.keyPressedEvent(keyCode);
			}
		}
		
		// Update the set
		previouslyPressKeyCodes = currentKeyDown;
	}
	
}
