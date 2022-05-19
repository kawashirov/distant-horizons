package com.seibel.lod.fabric;

import com.seibel.lod.common.networking.Networking;
import com.seibel.lod.core.api.internal.EventApi;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * This handles all events sent to the server,
 * and is the starting point for most of the mod.
 *
 * @author Ran
 * @version 5-11-2022
 */

// TODO
public class FabricServerProxy {
    private final EventApi eventApi = EventApi.INSTANCE;

    /**
     * Registers Fabric Events
     * @author Ran
     */
    public void registerEvents() {
        ServerTickEvents.END_SERVER_TICK.register(this::serverTickEvent);
    }

    public void serverTickEvent(MinecraftServer server) {
//        eventApi.serverTickEvent();

        // This just exists here for testing purposes, it'll be removed in the future
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            FriendlyByteBuf payload = Networking.createNew();
            payload.writeInt(1);
            System.out.println("Sending int 1");
            Networking.send(player, payload);
        }
    }
}
