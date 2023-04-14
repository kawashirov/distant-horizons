package com.seibel.lod.quilt.wrappers.modAccessor;

import com.seibel.lod.core.wrapperInterfaces.modAccessor.IBCLibAccessor;
import ru.bclib.config.ClientConfig;
import ru.bclib.config.Configs;
import ru.bclib.util.BackgroundInfo;

import java.awt.*;

public class BCLibAccessor implements IBCLibAccessor {
    @Override
    public String getModName() {
        return "BCLib";
    }

    public void setRenderCustomFog(boolean newValue) {
        // Change the value of CUSTOM_FOG_RENDERING in the bclib client config
        Configs.CLIENT_CONFIG.set(ClientConfig.CUSTOM_FOG_RENDERING, newValue);
    }

    @Override
    public Color getFogColor() {
        return new Color(BackgroundInfo.fogColorRed, BackgroundInfo.fogColorGreen, BackgroundInfo.fogColorBlue);
    }
}