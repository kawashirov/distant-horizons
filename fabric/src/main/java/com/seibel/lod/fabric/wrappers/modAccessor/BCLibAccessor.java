package com.seibel.lod.fabric.wrappers.modAccessor;

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
        // This disabled fog from rendering within bclib
        Configs.CLIENT_CONFIG.set(ClientConfig.CUSTOM_FOG_RENDERING, newValue);
    }
}