package com.seibel.lod.common.networking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/**
 * This is packet handler for our mod
 * It basically handles the packets sent from the server & client
 *
 * @author Ran
 */
public class NetworkHandler {
    // If you need the response sender then tell me
    // I'll add extra code to get the response sender
    public static void receivePacketClient(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf) {
        // TODO: Server sided stuff here
        // You can make client execute something by using client.execute(Runnable)
        // In the fabric docs it says that client.execute is ran on the render thread?
    }

    // If you need the response sender then tell me
    // I'll add extra code to get the response sender
    public static void receivePacketServer(MinecraftServer server, ServerPlayer client, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf) {
        // TODO: Server sided stuff here
    }
}
