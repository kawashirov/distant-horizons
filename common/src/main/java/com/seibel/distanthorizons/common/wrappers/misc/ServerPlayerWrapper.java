package com.seibel.distanthorizons.common.wrappers.misc;

import com.google.common.collect.MapMaker;
import com.seibel.distanthorizons.common.wrappers.world.ServerLevelWrapper;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.world.AbstractDhWorld;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IServerPlayerWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IServerLevelWrapper;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.Logger;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public class ServerPlayerWrapper implements IServerPlayerWrapper {
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    private static final ConcurrentMap<ServerPlayer, ServerPlayerWrapper>
            serverPlayerWrapperMap = new MapMaker().weakKeys().weakValues().makeMap();

    private final ServerPlayer serverPlayer;

    public static ServerPlayerWrapper getWrapper(ServerPlayer serverPlayer)
    {
        return serverPlayerWrapperMap.computeIfAbsent(serverPlayer, ServerPlayerWrapper::new);
    }

    private ServerPlayerWrapper(ServerPlayer serverPlayer) {
        this.serverPlayer = serverPlayer;
    }

    public UUID getUUID() {
        return serverPlayer.getUUID();
    }

    public IServerLevelWrapper getLevel() {
        return ServerLevelWrapper.getWrapper(serverPlayer.getLevel());
    }

    public Object getWrappedMcObject() {
        return serverPlayer;
    }

    @Override
    public String toString() {
        return "Wrapped{" + serverPlayer.toString() + "}";
    }
}
