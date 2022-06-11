package com.seibel.lod.fabric;

import com.seibel.lod.common.networking.Networking;
import com.seibel.lod.common.wrappers.chunk.ChunkWrapper;
import com.seibel.lod.common.wrappers.world.WorldWrapper;
import com.seibel.lod.core.api.internal.a7.ServerApi;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.objects.a7.Server;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.Logger;

/**
 * This handles all events sent to the server,
 * and is the starting point for most of the mod.
 *
 * @author Ran
 * @version 5-11-2022
 */

// TODO
public class FabricServerProxy {
    private final ServerApi serverApi = ServerApi.INSTANCE;
    private static final Logger LOGGER = DhLoggerBuilder.getLogger("FabricServerProxy");

    private boolean isValidTime() {
        //FIXME: return true immediately if this is a dedicated server
        return !(Minecraft.getInstance().screen instanceof TitleScreen);
    }
    private WorldWrapper getLevelWrapper(Level level) {
        return WorldWrapper.getWorldWrapper(level);
    }
    /**
     * Registers Fabric Events
     * @author Ran, Tom
     */
    public void registerEvents() {
        LOGGER.info("Registering Fabric Server Events");

        /* Register the mod needed event callbacks */

        // TEST EVENT
        //ServerTickEvents.END_SERVER_TICK.register(this::tester);

        // ServerTickEvent
        ServerTickEvents.END_SERVER_TICK.register((server) -> serverApi.serverTickEvent());

        // ServerWorldLoadEvent
        //TODO: Check if both of this use the correct timed events. (i.e. is it 'ed' or 'ing' one?)
        ServerLifecycleEvents.SERVER_STARTING.register((server) -> {
            if (isValidTime()) ServerApi.INSTANCE.serverWorldLoadEvent();
        });
        // ServerWorldUnloadEvent
        ServerLifecycleEvents.SERVER_STOPPED.register((server) -> {
            if (isValidTime()) ServerApi.INSTANCE.serverWorldUnloadEvent();
        });

        // ServerLevelLoadEvent
        ServerWorldEvents.LOAD.register((server, level)
                -> {
            if (isValidTime()) ServerApi.INSTANCE.serverLevelLoadEvent(getLevelWrapper(level));
        });
        // ServerLevelUnloadEvent
        ServerWorldEvents.UNLOAD.register((server, level)
                -> {
            if (isValidTime()) ServerApi.INSTANCE.serverLevelUnloadEvent(getLevelWrapper(level));
        });

        // ServerChunkLoadEvent
        ServerChunkEvents.CHUNK_LOAD.register((server, chunk)
                -> {
            IWorldWrapper level = getLevelWrapper(chunk.getLevel());
            if (isValidTime()) ServerApi.INSTANCE.serverChunkLoadEvent(
                    new ChunkWrapper(chunk, chunk.getLevel()),
                    level);
                }
        );
        // ServerChunkSaveEvent - Done in MixinChunkMap
    }

    // This just exists here for testing purposes, it'll be removed in the future
    public void tester(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            FriendlyByteBuf payload = Networking.createNew();
            payload.writeInt(1);
            System.out.println("Sending int 1");
            Networking.send(player, payload);
        }
    }
}