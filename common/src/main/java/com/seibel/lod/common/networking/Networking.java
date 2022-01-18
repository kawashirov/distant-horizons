package com.seibel.lod.common.networking;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

/**
 * This class holds most of the networking code for the mod.
 * @author Ran
 */
public class Networking {
//    public void example(int packetId) {
//        FriendlyByteBuf packetByteBuf = Networking.createNew();
//        packetByteBuf.writeInt(packetId);
//        LodCommonMain.networkInterface.send(packetByteBuf);
//    }

    public static FriendlyByteBuf createNew() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }
}
