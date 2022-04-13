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

package com.seibel.lod.forge.fabric.impl.networking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.seibel.lod.forge.fabric.api.networking.v1.PacketByteBufs;
import com.seibel.lod.forge.fabric.api.networking.v1.ServerLoginConnectionEvents;
import com.seibel.lod.forge.fabric.api.networking.v1.ServerLoginNetworking;
import com.seibel.lod.forge.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;

public final class NetworkingImpl {
	public static final String MOD_ID = "fabric-networking-api-v1";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	/**
	 * Id of packet used to register supported channels.
	 */
	public static final ResourceLocation REGISTER_CHANNEL = new ResourceLocation("minecraft", "register");
	/**
	 * Id of packet used to unregister supported channels.
	 */
	public static final ResourceLocation UNREGISTER_CHANNEL = new ResourceLocation("minecraft", "unregister");
	/**
	 * Id of the packet used to declare all currently supported channels.
	 * Dynamic registration of supported channels is still allowed using {@link NetworkingImpl#REGISTER_CHANNEL} and {@link NetworkingImpl#UNREGISTER_CHANNEL}.
	 */
	public static final ResourceLocation EARLY_REGISTRATION_CHANNEL = new ResourceLocation(MOD_ID, "early_registration");

	public static void init() {
		// Login setup
		ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
			// Send early registration packet
			FriendlyByteBuf buf = PacketByteBufs.create();
			Collection<ResourceLocation> channelsNames = ServerPlayNetworking.getGlobalReceivers();
			buf.writeVarInt(channelsNames.size());

			for (ResourceLocation id : channelsNames) {
				buf.writeResourceLocation(id);
			}

			sender.sendPacket(EARLY_REGISTRATION_CHANNEL, buf);
			NetworkingImpl.LOGGER.debug("Sent accepted channels to the client for \"{}\"", handler.getUserName());
		});

		ServerLoginNetworking.registerGlobalReceiver(EARLY_REGISTRATION_CHANNEL, (server, handler, understood, buf, synchronizer, sender) -> {
			if (!understood) {
				// The client is likely a vanilla client.
				return;
			}

			int n = buf.readVarInt();
			List<ResourceLocation> ids = new ArrayList<>(n);

			for (int i = 0; i < n; i++) {
				ids.add(buf.readResourceLocation());
			}

			((ChannelInfoHolder) handler.getConnection()).getPendingChannelsNames().addAll(ids);
			NetworkingImpl.LOGGER.debug("Received accepted channels from the client for \"{}\"", handler.getUserName());
		});
	}

	public static boolean isReservedPlayChannel(ResourceLocation channelName) {
		return channelName.equals(REGISTER_CHANNEL) || channelName.equals(UNREGISTER_CHANNEL);
	}
}
