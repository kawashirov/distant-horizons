package com.seibel.lod.forge.networking;

import com.seibel.lod.common.networking.NetworkInterface;
import com.seibel.lod.common.networking.Networking;
import com.seibel.lod.forge.fabric.api.client.networking.v1.ClientPlayNetworking;

public class NetworkHandler implements NetworkInterface {
    @Override
    public void register() {
        ClientPlayNetworking.registerGlobalReceiver(Networking.resourceLocation_meow, (client, handler, buf, responseSender) -> {
            com.seibel.lod.common.networking.NetworkHandler.receivePacketClient(client, handler, buf);
        });
    }
}
