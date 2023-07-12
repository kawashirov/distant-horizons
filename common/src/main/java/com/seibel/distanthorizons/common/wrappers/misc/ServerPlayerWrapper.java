package com.seibel.distanthorizons.common.wrappers.misc;

import com.google.common.collect.MapMaker;
import com.seibel.distanthorizons.common.wrappers.world.ServerLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IServerPlayerWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IServerLevelWrapper;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public class ServerPlayerWrapper implements IServerPlayerWrapper {
    private static final ConcurrentMap<ServerPlayer, ServerPlayerWrapper>
            serverPlayerWrapperMap = new MapMaker().weakKeys().makeMap();

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

    public IServerLevelWrapper getLevel() 
	{
		#if PRE_MC_1_20_1
		return ServerLevelWrapper.getWrapper(this.serverPlayer.getLevel());
		#else
		return ServerLevelWrapper.getWrapper(this.serverPlayer.serverLevel());
		#endif
    }

    public Object getWrappedMcObject() {
        return serverPlayer;
    }

    @Override
    public String toString() {
        return "Wrapped{" + serverPlayer.toString() + "}";
    }
}
