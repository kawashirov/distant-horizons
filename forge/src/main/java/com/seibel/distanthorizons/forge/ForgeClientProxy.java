/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2023 James Seibel
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

package com.seibel.distanthorizons.forge;

import com.seibel.distanthorizons.common.util.ProxyUtil;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.common.wrappers.world.ServerLevelWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;

import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.distanthorizons.coreapi.ModInfo;
//import io.netty.buffer.ByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;

import net.minecraft.client.multiplayer.ClientLevel;
#if PRE_MC_1_19_2
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
#else
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
#endif

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.level.chunk.ChunkAccess;

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
//import net.minecraftforge.network.NetworkRegistry;
//import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * This handles all events sent to the client,
 * and is the starting point for most of the mod.
 *
 * @author James_Seibel
 * @version 2023-7-27
 */
public class ForgeClientProxy
{
	private static final IMinecraftClientWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();

//	private static SimpleChannel multiversePluginChannel;
	
	
	#if PRE_MC_1_19_2
	private static LevelAccessor GetLevel(WorldEvent e) { return e.getWorld(); }
	#else
	private static LevelAccessor GetEventLevel(LevelEvent e) { return e.getLevel(); }
	#endif
	
	
	
	//=============//
	// tick events //
	//=============//
	
	@SubscribeEvent
	public void clientTickEvent(TickEvent.ClientTickEvent event)
	{
		if (event.phase == TickEvent.Phase.START)
		{
			ClientApi.INSTANCE.clientTickEvent();
		}
	}
	
	
	
	//==============//
	// world events //
	//==============//
	
	@SubscribeEvent
	#if PRE_MC_1_19_2
	public void clientLevelLoadEvent(WorldEvent.Load event)
	#else
	public void clientLevelLoadEvent(LevelEvent.Load event)
	#endif
	{
		LOGGER.info("level load");
		
		#if PRE_MC_1_19_2
		LevelAccessor level = event.getWorld();
		#else
		LevelAccessor level = event.getLevel();
		#endif
		if (!(level instanceof ClientLevel))
		{
			return;
		}
		
		ClientLevel clientLevel = (ClientLevel) level;
		IClientLevelWrapper clientLevelWrapper = ClientLevelWrapper.getWrapper(clientLevel);
		// TODO this causes a crash due to level being set to null somewhere
		ClientApi.INSTANCE.clientLevelLoadEvent(clientLevelWrapper);
	}
	@SubscribeEvent
	#if PRE_MC_1_19_2
	public void clientLevelUnloadEvent(WorldEvent.Unload event)
	#else
	public void clientLevelUnloadEvent(LevelEvent.Load event)
	#endif
	{
		LOGGER.info("level unload");
		
		#if PRE_MC_1_19_2
		LevelAccessor level = event.getWorld();
		#else
		LevelAccessor level = event.getLevel();
		#endif
		if (!(level instanceof ClientLevel))
		{
			return;
		}
		
		ClientLevel clientLevel = (ClientLevel) level;
		IClientLevelWrapper clientLevelWrapper = ClientLevelWrapper.getWrapper(clientLevel);
		ClientApi.INSTANCE.clientLevelUnloadEvent(clientLevelWrapper);
	}
	
	
	
	//==============//
	// chunk events //
	//==============//
	
	@SubscribeEvent
	public void rightClickBlockEvent(PlayerInteractEvent.RightClickBlock event)
	{
		LOGGER.trace("interact or block place event at blockPos: " + event.getPos());
		
		#if PRE_MC_1_19_2
		LevelAccessor level = event.getWorld();
		#else
		LevelAccessor level = event.getLevel();
		#endif
		
		ChunkAccess chunk = level.getChunk(event.getPos());
		this.onBlockChangeEvent(level, chunk);
	}
	@SubscribeEvent
	public void leftClickBlockEvent(PlayerInteractEvent.LeftClickBlock event)
	{
		LOGGER.trace("break or block attack at blockPos: " + event.getPos());
		
		#if PRE_MC_1_19_2
		LevelAccessor level = event.getWorld();
		#else
		LevelAccessor level = event.getLevel();
		#endif
		
		ChunkAccess chunk = level.getChunk(event.getPos());
		this.onBlockChangeEvent(level, chunk);
	}
	private void onBlockChangeEvent(LevelAccessor level, ChunkAccess chunk)
	{
		ILevelWrapper wrappedLevel = ProxyUtil.getLevelWrapper(level);
		SharedApi.INSTANCE.chunkBlockChangedEvent(new ChunkWrapper(chunk, level, wrappedLevel), wrappedLevel);
	}
	
	
	@SubscribeEvent
	public void clientChunkLoadEvent(ChunkEvent.Load event)
	{
		ILevelWrapper wrappedLevel = ProxyUtil.getLevelWrapper(GetEventLevel(event));
		IChunkWrapper chunk = new ChunkWrapper(event.getChunk(), GetEventLevel(event), wrappedLevel);
		SharedApi.INSTANCE.chunkLoadEvent(chunk, wrappedLevel);
	}
	@SubscribeEvent
	public void clientChunkUnloadEvent(ChunkEvent.Unload event)
	{
		ILevelWrapper wrappedLevel = ProxyUtil.getLevelWrapper(GetEventLevel(event));
		IChunkWrapper chunk = new ChunkWrapper(event.getChunk(), GetEventLevel(event), wrappedLevel);
		SharedApi.INSTANCE.chunkSaveEvent(chunk, wrappedLevel);
	}
	
	
	
	//==============//
	// key bindings //
	//==============//
	
	@SubscribeEvent
	public void registerKeyBindings(#if PRE_MC_1_19_2 InputEvent.KeyInputEvent #else InputEvent.Key #endif event)
	{
		if (Minecraft.getInstance().player == null)
		{
			return;
		}
		if (event.getAction() != GLFW.GLFW_PRESS)
		{
			return;
		}
		
		ClientApi.INSTANCE.keyPressedEvent(event.getKey());
	}
	
	
	
	//============//
	// networking //
	//============//
	
	/** @param event this is just to ensure the event is called at the right time, if it is called outside the {@link FMLClientSetupEvent} event, the binding may fail */
	public static void setupNetworkingListeners(FMLClientSetupEvent event)
	{
//		multiversePluginChannel = NetworkRegistry.newSimpleChannel(
//				new ResourceLocation(ModInfo.NETWORKING_RESOURCE_NAMESPACE, ModInfo.MULTIVERSE_PLUGIN_NAMESPACE),
//				// network protocol version
//				() -> ModInfo.MULTIVERSE_PLUGIN_PROTOCOL_VERSION +"",
//				// client accepted versions
//				ForgeClientProxy::isReceivedProtocolVersionAcceptable,
//				// server accepted versions
//				ForgeClientProxy::isReceivedProtocolVersionAcceptable
//		);
//		
//		multiversePluginChannel.registerMessage(0/*should be incremented for each simple channel we listen to*/, ByteBuf.class,
//				// encoder
//				(pack, friendlyByteBuf) -> { },
//				// decoder
//				(friendlyByteBuf) -> friendlyByteBuf.asByteBuf(),
//				// message consumer
//				(nettyByteBuf, contextRef) ->
//				{
//					ClientApi.INSTANCE.serverMessageReceived(nettyByteBuf);
//					contextRef.get().setPacketHandled(true);
//				}
//		);
	}
	
	public static boolean isReceivedProtocolVersionAcceptable(String versionString)
	{
		if (versionString.toLowerCase().contains("allowvanilla"))
		{
			// allow using networking on vanilla servers
			return true;
		}
		else if (versionString.toLowerCase().contains("absent"))
		{
			// allow using networking even if DH isn't installed on the server
			return true;
		}
		else
		{
			// DH is installed on the server, check if the version is valid to use
			try
			{
				int version = Integer.parseInt(versionString);
				return ModInfo.MULTIVERSE_PLUGIN_PROTOCOL_VERSION == version;
			}
			catch (NumberFormatException ignored)
			{
				return false;
			}
		}
	}
	
}
