package com.seibel.lod.fabric.networking;

import com.seibel.lod.common.networking.NetworkInterface;
import com.seibel.lod.common.networking.Networking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

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
