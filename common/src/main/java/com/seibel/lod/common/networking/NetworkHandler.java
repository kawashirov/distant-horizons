package com.seibel.lod.common.networking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;

/**
 * This is packet handler for our mod
 * It basically handles the packets sent from the server
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
}
