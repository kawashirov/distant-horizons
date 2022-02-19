/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.seibel.lod.forge.fabric.impl.networking.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;
import com.seibel.lod.forge.fabric.api.client.networking.v1.ClientLoginNetworking;
import com.seibel.lod.forge.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import com.seibel.lod.forge.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.seibel.lod.forge.fabric.api.networking.v1.PacketByteBufs;
import com.seibel.lod.forge.fabric.impl.networking.ChannelInfoHolder;
import com.seibel.lod.forge.fabric.impl.networking.GlobalReceiverRegistry;
import com.seibel.lod.forge.fabric.impl.networking.NetworkHandlerExtensions;
import com.seibel.lod.forge.fabric.impl.networking.NetworkingImpl;
import com.seibel.lod.forge.mixins.fabric.mixin.networking.accessor.ConnectScreenAccessor;
import com.seibel.lod.forge.mixins.fabric.mixin.networking.accessor.MinecraftClientAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;

public final class ClientNetworkingImpl {
	public static final GlobalReceiverRegistry<ClientLoginNetworking.LoginQueryRequestHandler> LOGIN = new GlobalReceiverRegistry<>();
	public static final GlobalReceiverRegistry<ClientPlayNetworking.PlayChannelHandler> PLAY = new GlobalReceiverRegistry<>();
	private static ClientPlayNetworkAddon currentPlayAddon;

	public static ClientPlayNetworkAddon getAddon(ClientPacketListener handler) {
		return (ClientPlayNetworkAddon) ((NetworkHandlerExtensions) handler).getAddon();
	}

	public static ClientLoginNetworkAddon getAddon(ClientHandshakePacketListenerImpl handler) {
		return (ClientLoginNetworkAddon) ((NetworkHandlerExtensions) handler).getAddon();
	}

	public static Packet<?> createPlayC2SPacket(ResourceLocation channelName, FriendlyByteBuf buf) {
		return new ServerboundCustomPayloadPacket(channelName, buf);
	}

	/**
	 * Due to the way logging into a integrated or remote dedicated server will differ, we need to obtain the login client connection differently.
	 */
	@Nullable
	public static Connection getLoginConnection() {
		final Connection connection = ((MinecraftClientAccessor) Minecraft.getInstance()).getConnection();

		// Check if we are connecting to an integrated server. This will set the field on MinecraftClient
		if (connection != null) {
			return connection;
		} else {
			// We are probably connecting to a remote server.
			// Check if the ConnectScreen is the currentScreen to determine that:
			if (Minecraft.getInstance().screen instanceof ConnectScreen) {
				return ((ConnectScreenAccessor) Minecraft.getInstance().screen).getConnection();
			}
		}

		// We are not connected to a server at all.
		return null;
	}

	@Nullable
	public static ClientPlayNetworkAddon getClientPlayAddon() {
		// Since Minecraft can be a bit weird, we need to check for the play addon in a few ways:
		// If the client's player is set this will work
		if (Minecraft.getInstance().getConnection() != null) {
			currentPlayAddon = null; // Shouldn't need this anymore
			return ClientNetworkingImpl.getAddon(Minecraft.getInstance().getConnection());
		}

		// We haven't hit the end of onGameJoin yet, use our backing field here to access the network handler
		if (currentPlayAddon != null) {
			return currentPlayAddon;
		}

		// We are not in play stage
		return null;
	}

	public static void setClientPlayAddon(ClientPlayNetworkAddon addon) {
		currentPlayAddon = addon;
	}

	public static void clientInit() {
		// Reference cleanup for the locally stored addon if we are disconnected
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			currentPlayAddon = null;
		});

		// Register a login query handler for early channel registration.
		ClientLoginNetworking.registerGlobalReceiver(NetworkingImpl.EARLY_REGISTRATION_CHANNEL, (client, handler, buf, listenerAdder) -> {
			int n = buf.readVarInt();
			List<ResourceLocation> ids = new ArrayList<>(n);

			for (int i = 0; i < n; i++) {
				ids.add(buf.readResourceLocation());
			}

			((ChannelInfoHolder) handler.getConnection()).getPendingChannelsNames().addAll(ids);
			NetworkingImpl.LOGGER.debug("Received accepted channels from the server");

			FriendlyByteBuf response = PacketByteBufs.create();
			Collection<ResourceLocation> channels = ClientPlayNetworking.getGlobalReceivers();
			response.writeVarInt(channels.size());

			for (ResourceLocation id : channels) {
				response.writeResourceLocation(id);
			}

			NetworkingImpl.LOGGER.debug("Sent accepted channels to the server");
			return CompletableFuture.completedFuture(response);
		});
	}
}
