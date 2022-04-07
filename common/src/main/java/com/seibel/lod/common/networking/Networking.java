/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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
 
package com.seibel.lod.common.networking;

import com.seibel.lod.core.ModInfo;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/**
 * This class holds most of the networking code for the mod.
 * @author Ran
 */
public class Networking {
    public static final ResourceLocation resourceLocation_meow = new ResourceLocation("lod", "meow");

    public static FriendlyByteBuf createNew() {
        // TODO: Probably replace the Unpooled.buffer()
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(ModInfo.PROTOCOL_VERSION);
        return buf;
    }

    /*
     * All code below is from the fabric api and might have been modified to work with Distant Horizons
     * Which is licensed under the Apache License 2.0
     */

    /**
     * Sends a packet to a player.
     *
     * @param player the player to send the packet to
     * @param buf the payload of the packet.
     */
    public static void send(ServerPlayer player, FriendlyByteBuf buf) {
        Objects.requireNonNull(player, "Server player entity cannot be null");
        Objects.requireNonNull(resourceLocation_meow, "Channel name cannot be null");
        Objects.requireNonNull(buf, "Packet byte buf cannot be null");

        player.connection.send(createS2CPacket(resourceLocation_meow, buf));
    }

    /**
     * Sends a packet to the connected server.
     *
     * @param buf the payload of the packet
     * @throws IllegalStateException if the client is not connected to a server
     */
    public static void send(FriendlyByteBuf buf) throws IllegalStateException {
        // You cant send without a client player, so this is fine
        if (Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().send(createC2SPacket(resourceLocation_meow, buf));
            return;
        }

        throw new IllegalStateException("Cannot send packets when not in game!");
    }

    /**
     * Creates a packet which may be sent to the connected client.
     *
     * @param channelName the channel name
     * @param buf the packet byte buf which represents the payload of the packet
     * @return a new packet
     */
    public static Packet<?> createS2CPacket(ResourceLocation channelName, FriendlyByteBuf buf) {
        Objects.requireNonNull(channelName, "Channel cannot be null");
        Objects.requireNonNull(buf, "Buf cannot be null");

        return createPlayS2CPacket(channelName, buf);
    }

    public static Packet<?> createPlayS2CPacket(ResourceLocation channel, FriendlyByteBuf buf) {
        return new ClientboundCustomPayloadPacket(channel, buf);
    }

    /**
     * Creates a packet which may be sent to a the connected server.
     *
     * @param channelName the channel name
     * @param buf the packet byte buf which represents the payload of the packet
     * @return a new packet
     */
    public static Packet<?> createC2SPacket(ResourceLocation channelName, FriendlyByteBuf buf) {
        Objects.requireNonNull(channelName, "Channel name cannot be null");
        Objects.requireNonNull(buf, "Buf cannot be null");

        return createPlayC2SPacket(channelName, buf);
    }

    public static Packet<?> createPlayC2SPacket(ResourceLocation channelName, FriendlyByteBuf buf) {
        return new ServerboundCustomPayloadPacket(channelName, buf);
    }

}
