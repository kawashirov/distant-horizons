package com.seibel.distanthorizons.common.wrappers.level;

import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.level.IServerEnhancedClientLevel;
import com.seibel.distanthorizons.core.level.IEnhancedServerManager;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.Objects;

public class EnhancedServerManager implements IEnhancedServerManager {

    public static EnhancedServerManager INSTANCE = new EnhancedServerManager();

    @Override
    public void registerServerEnhancedLevel(IServerEnhancedClientLevel clientLevel) {
        ClientLevelWrapper.setWrappedLevel(clientLevel);
    }



    @Override
    public IServerEnhancedClientLevel getServerEnhancedLevel(ILevelWrapper level, String worldKey) {
        Objects.requireNonNull(level);
        Objects.requireNonNull(worldKey);
        return new ServerEnhancedClientLevel((ClientLevel) level.getWrappedMcObject(), worldKey);
    }

    @Override
    public void setUseOverrideWrapper(boolean useOverrideWrapper) {
        ClientLevelWrapper.setUseOverrideWrapper(useOverrideWrapper);
    }
}
