package com.seibel.distanthorizons.fabric.wrappers.modAccessor;

import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IBCLibAccessor;
import ru.bclib.config.ClientConfig;
import ru.bclib.config.Configs;

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