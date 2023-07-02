package com.seibel.distanthorizons.common.wrappers.level;

import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.level.IServerEnhancedClientLevel;
import net.minecraft.client.multiplayer.ClientLevel;

public class ServerEnhancedClientLevel extends ClientLevelWrapper implements IServerEnhancedClientLevel {

    private final String serverKey;

    public ServerEnhancedClientLevel(ClientLevel level, String serverKey) {
        super(level);
        this.serverKey = serverKey;
    }

    @Override
    public String getServerWorldKey() {
        return this.serverKey;
    }
}
