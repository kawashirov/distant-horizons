package com.seibel.lod.forge.networking;

import com.seibel.lod.common.networking.NetworkInterface;
import com.seibel.lod.common.networking.Networking;
import com.seibel.lod.forge.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.seibel.lod.forge.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * @author Ran
 */
public class NetworkHandler implements NetworkInterface {
    @Override
    public void register_Client() {
        ClientPlayNetworking.registerGlobalReceiver(Networking.resourceLocation_meow, (client, handler, buf, responseSender) -> {
            com.seibel.lod.common.networking.NetworkHandler.receivePacketClient(client, handler, buf);
        });
    }

    @Override
    public void register_Server() {
        ServerPlayNetworking.registerGlobalReceiver(Networking.resourceLocation_meow, (server, player, handler, buf, responseSender) -> {
            com.seibel.lod.common.networking.NetworkHandler.receivePacketServer(server, player, handler, buf);
        });
    }
}
