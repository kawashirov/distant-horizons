package com.seibel.lod.common.networking;

import net.minecraft.network.FriendlyByteBuf;

/**
 * @author Ran
 */
public interface NetworkInterface {
    void send(FriendlyByteBuf packetByteBuf);

    FriendlyByteBuf receive();
}
