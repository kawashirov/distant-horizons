package com.seibel.lod.common.wrappers.gui;

import com.seibel.lod.core.wrapperInterfaces.config.IConfigWrapper;
import net.minecraft.client.resources.language.I18n;

public class ConfigWrapper implements IConfigWrapper {
    public static final ConfigWrapper INSTANCE = new ConfigWrapper();
    @Override
    public boolean langExists(String str) {
        return I18n.exists(str);
    }

    @Override
    public String getLang(String str) {
        return I18n.get(str);
    }
}
