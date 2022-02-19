package com.seibel.lod.fabric.networking;

import com.seibel.lod.common.networking.NetworkInterface;
import com.seibel.lod.common.networking.Networking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class NetworkHandler implements NetworkInterface {
    @Override
    public void register() {
        ClientPlayNetworking.registerGlobalReceiver(Networking.resourceLocation_meow, (client, handler, buf, responseSender) -> {
            com.seibel.lod.common.networking.NetworkHandler.receivePacketClient(client, handler, buf);
        });
    }
}
