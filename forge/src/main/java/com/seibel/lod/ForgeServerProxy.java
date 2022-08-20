package com.seibel.lod;

import com.seibel.lod.common.networking.Networking;
import com.seibel.lod.common.wrappers.chunk.ChunkWrapper;
import com.seibel.lod.common.wrappers.world.ClientLevelWrapper;
import com.seibel.lod.common.wrappers.world.ServerLevelWrapper;
import com.seibel.lod.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.lod.core.api.internal.a7.ClientApi;
import com.seibel.lod.core.api.internal.a7.ServerApi;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class ForgeServerProxy {
    private final ServerApi serverApi = ServerApi.INSTANCE;
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    private final boolean isDedicated;
    public static Supplier<Boolean> isGenerationThreadChecker = null;

    public ForgeServerProxy(boolean isDedicated) {
        this.isDedicated = isDedicated;
        isGenerationThreadChecker = BatchGenerationEnvironment::isCurrentThreadDistantGeneratorThread;
    }
    private boolean isValidTime() {
        if (isDedicated) return true;

        //FIXME: This may cause init issue...
        return !(Minecraft.getInstance().screen instanceof TitleScreen);
    }
    private ServerLevelWrapper getLevelWrapper(ServerLevel level) {
        return ServerLevelWrapper.getWrapper(level);
    }


    // ServerTickEvent (at end)
    @SubscribeEvent
    private void serverTickEvent(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (isValidTime()) serverApi.serverTickEvent();
        }
    }

    // ServerWorldLoadEvent
    @SubscribeEvent
    private void dedicatedWorldLoadEvent(ServerStartedEvent event) {
        if (isValidTime()) serverApi.serverWorldLoadEvent(isDedicated);
    }

    // ServerWorldUnloadEvent
    @SubscribeEvent
    private void serverWorldUnloadEvent(ServerStoppingEvent event) {
        if (isValidTime()) serverApi.serverWorldUnloadEvent();
    }

    // ServerLevelLoadEvent
    @SubscribeEvent
    private void serverLevelLoadEvent(WorldEvent.Load event) {
        if (isValidTime()) {
            if (event.getWorld() instanceof ServerLevel) {
                serverApi.serverLevelLoadEvent(getLevelWrapper((ServerLevel) event.getWorld()));
            }
        }
    }

    // ServerLevelUnloadEvent
    @SubscribeEvent
    private void serverLevelUnloadEvent(WorldEvent.Unload event) {
        if (isValidTime()) {
            if (event.getWorld() instanceof ServerLevel) {
                serverApi.serverLevelUnloadEvent(getLevelWrapper((ServerLevel) event.getWorld()));
            }
        }
    }

    @SubscribeEvent
    public void serverChunkLoadEvent(ChunkDataEvent.Load event)
    {
        if (isValidTime()) {
            if (event.getWorld() instanceof ServerLevel) {
                ServerLevelWrapper wrappedLevel = ServerLevelWrapper.getWrapper((ServerLevel) event.getWorld());
                IChunkWrapper chunk = new ChunkWrapper(event.getChunk(), event.getWorld(), wrappedLevel);
                serverApi.serverChunkLoadEvent(chunk, getLevelWrapper((ServerLevel) event.getWorld()));
            }
        }
    }
    @SubscribeEvent
    public void serverChunkSaveEvent(ChunkDataEvent.Save event)
    {
        if (isValidTime()) {
            if (event.getWorld() instanceof ServerLevel) {
                ServerLevelWrapper wrappedLevel = ServerLevelWrapper.getWrapper((ServerLevel) event.getWorld());
                IChunkWrapper chunk = new ChunkWrapper(event.getChunk(), event.getWorld(), wrappedLevel);
                serverApi.serverChunkSaveEvent(chunk, getLevelWrapper((ServerLevel) event.getWorld()));
            }
        }
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
