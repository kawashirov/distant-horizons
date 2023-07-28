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

package com.seibel.distanthorizons.fabric;

import com.seibel.distanthorizons.common.rendering.SeamlessOverdraw;
import com.seibel.distanthorizons.common.wrappers.McObjectConverter;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.mojang.blaze3d.platform.InputConstants;
import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;

import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IImmersivePortalsAccessor;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.ISodiumAccessor;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.fabric.wrappers.modAccessor.ImmersivePortalsAccessor;
import com.seibel.distanthorizons.fabric.wrappers.modAccessor.SodiumAccessor;
import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

import java.nio.FloatBuffer;
import java.util.HashSet;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.HitResult;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

/**
 * This handles all events sent to the client,
 * and is the starting point for most of the mod.
 * 
 * @author coolGi
 * @author Ran
 * @version 2023-7-27
 */
@Environment(EnvType.CLIENT)
public class FabricClientProxy
{
	private final ClientApi clientApi = ClientApi.INSTANCE;
	private static final IMinecraftClientWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
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
		
		
		//========================//
		// register mod accessors //
		//========================//
		
		SodiumAccessor sodiumAccessor = (SodiumAccessor) ModAccessorInjector.INSTANCE.get(ISodiumAccessor.class);
		ImmersivePortalsAccessor immersivePortalsAccessor = (ImmersivePortalsAccessor) ModAccessorInjector.INSTANCE.get(IImmersivePortalsAccessor.class);
		
		
		
		//=============//
		// tick events //
		//=============//
		
		ClientTickEvents.START_CLIENT_TICK.register((client) -> { ClientApi.INSTANCE.clientTickEvent(); });
		
		
		
		//==============//
		// chunk events //
		//==============//
		
		// ClientChunkLoadEvent
		ClientChunkEvents.CHUNK_LOAD.register((level, chunk) ->
		{
			IClientLevelWrapper wrappedLevel = ClientLevelWrapper.getWrapper(level);
			ClientApi.INSTANCE.clientChunkLoadEvent(new ChunkWrapper(chunk, level, wrappedLevel), wrappedLevel);
		});
		
		// (kinda) block break event
		AttackBlockCallback.EVENT.register((player, level, interactionHand, blockPos, direction) ->
		{
			// if we have access to the server, use the chunk save event instead 
			if (MC.clientConnectedToDedicatedServer())
			{
				// Since fabric doesn't have a client-side break-block API event, this is the next best thing
				ChunkAccess chunk = level.getChunk(blockPos);
				if (chunk != null)
				{
					LOGGER.trace("attack block at blockPos: " + blockPos);
					
					IClientLevelWrapper wrappedLevel = ClientLevelWrapper.getWrapper((ClientLevel) level);
					ClientApi.INSTANCE.clientChunkLoadEvent(
							new ChunkWrapper(chunk, level, wrappedLevel),
							wrappedLevel
					);
				}
			}
			
			// don't stop the callback
			return InteractionResult.PASS;
		});
		
		// (kinda) block place event
		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> 
		{
			// if we have access to the server, use the chunk save event instead 
			if (MC.clientConnectedToDedicatedServer())
			{
				// Since fabric doesn't have a client-side place-block API event, this is the next best thing
				if (hitResult.getType() == HitResult.Type.BLOCK
						&& !hitResult.isInside())
				{
					ChunkAccess chunk = level.getChunk(hitResult.getBlockPos());
					if (chunk != null)
					{
						LOGGER.trace("use block at blockPos: " + hitResult.getBlockPos());
						
						IClientLevelWrapper wrappedLevel = ClientLevelWrapper.getWrapper((ClientLevel) level);
						ClientApi.INSTANCE.clientChunkLoadEvent(
								new ChunkWrapper(chunk, level, wrappedLevel),
								wrappedLevel
						);
					}
				}
			}
			
			// don't stop the callback
			return InteractionResult.PASS;
		});
		
		
		// Client Chunk Save
		ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) ->
		{
			IClientLevelWrapper wrappedLevel = ClientLevelWrapper.getWrapper(level);
			ClientApi.INSTANCE.clientChunkSaveEvent(new ChunkWrapper(chunk, level, wrappedLevel), wrappedLevel);
		});
		
		
		
		//==============//
		// render event //
		//==============//
		
		// Client Render Level
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
				this.clientApi.renderLods(ClientLevelWrapper.getWrapper(renderContext.world()),
						McObjectConverter.Convert(renderContext.matrixStack().last().pose()),
						McObjectConverter.Convert(renderContext.projectionMatrix()),
						renderContext.tickDelta());
				
				
				// experimental proof-of-concept option
				if (Config.Client.Advanced.Graphics.AdvancedGraphics.seamlessOverdraw.get())
				{
					float[] matrixFloatArray = SeamlessOverdraw.overwriteMinecraftNearFarClipPlanes(renderContext.projectionMatrix(), renderContext.tickDelta());
					
					#if PRE_MC_1_19_4
					renderContext.projectionMatrix().load(FloatBuffer.wrap(matrixFloatArray));
					#else
					renderContext.projectionMatrix().set(matrixFloatArray);
					#endif
				}
			}

			if (immersivePortalsAccessor != null)
			{
				immersivePortalsAccessor.partialTicks = renderContext.tickDelta();
			}
		});

		// Debug keyboard event
		// FIXME: Use better hooks so it doesn't trigger key press events in text boxes
		ClientTickEvents.END_CLIENT_TICK.register(client -> 
		{
			if (client.player != null && !(Minecraft.getInstance().screen instanceof TitleScreen))
			{
				this.onKeyInput();
			}
		});
		
		
		
		//==================//
		// networking event //
		//==================//
		
		// TODO add forge equivalent
		ClientPlayNetworking.registerGlobalReceiver(new ResourceLocation("distant_horizons", "world_control"), // TODO move these strings into a constant somewhere
			(Minecraft client, ClientPacketListener handler, FriendlyByteBuf byteBuffer, PacketSender responseSender) ->
			{
				// converting to a ByteBuf is necessary otherwise Fabric will complain when the game boots
				ByteBuf nettyByteBuf = byteBuffer.asByteBuf();
				ClientApi.INSTANCE.serverMessageReceived(nettyByteBuf);
			});
	}
	
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
