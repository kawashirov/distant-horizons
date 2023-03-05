package com.seibel.lod.common.wrappers.gui;

import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.config.ConfigBase;
import com.seibel.lod.core.config.gui.ConfigScreen;
import net.minecraft.client.gui.screens.Screen;

public class GetConfigScreen {
    public static type useScreen = type.Classic;
    public static enum type {
        Classic,
        OpenGL;
    }

    public static Screen getScreen(Screen parent) {
        if (useScreen == type.Classic) {
            return ClassicConfigGUI.getScreen(ConfigBase.INSTANCE, parent, "client");
        } else if (useScreen == type.OpenGL) {
            return MinecraftScreen.getScreen(parent, new ConfigScreen(), ModInfo.ID + ".title");
        }
        return null;
    }
}