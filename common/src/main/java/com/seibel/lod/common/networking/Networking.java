package com.seibel.lod.common.networking;

import com.seibel.lod.common.LodCommonMain;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
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
        return new FriendlyByteBuf(Unpooled.buffer());
    }


    /*
     * All code below is from the fabric api
     * Which is licensed under the Apache License 2.0
     */

    /**
     * Sends a packet to a player.
     *
     * @param player the player to send the packet to
     * @param channelName the channel of the packet
     * @param buf the payload of the packet.
     */
    public static void send(ServerPlayer player, ResourceLocation channelName, FriendlyByteBuf buf) {
        Objects.requireNonNull(player, "Server player entity cannot be null");
        Objects.requireNonNull(channelName, "Channel name cannot be null");
        Objects.requireNonNull(buf, "Packet byte buf cannot be null");

        player.connection.send(createS2CPacket(channelName, buf));
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

        return createPlayC2SPacket(channelName, buf);
    }

    public static Packet<?> createPlayC2SPacket(ResourceLocation channel, FriendlyByteBuf buf) {
        return new ClientboundCustomPayloadPacket(channel, buf);
    }

}
