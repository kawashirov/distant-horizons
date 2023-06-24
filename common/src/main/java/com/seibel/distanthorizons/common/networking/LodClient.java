package com.seibel.distanthorizons.common.networking;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import net.minecraft.client.Minecraft;

public class LodClient {
    public static final LodClient INSTANCE = new LodClient();

    private final Minecraft client;
    private final NetworkHandler networkHandler;

    private LodClient() {
        client = Minecraft.getInstance();
        this.networkHandler = new NetworkHandler();
    }

    public void connect() {

    }

    public void disconnect() {

    }
}
