package com.seibel.lod.common.wrappers.config;

import com.seibel.lod.core.config.gui.ConfigScreen;
import net.minecraft.client.gui.screens.Screen;

public class GetConfigScreen {
    public static type useScreen = type.Classic;
    public static enum type {
        Classic,
        OpenGL,
        New;
    }

    public static Screen getScreen(Screen parent) {
        if (useScreen == type.Classic) {
            return ClassicConfigGUI.getScreen(parent, "");
        } else if (useScreen == type.OpenGL) {
            return ConfigScreenMC.getScreen(parent, new ConfigScreen());
        } else if (useScreen == type.New) {
            System.out.println("This is not made yet");
            return null;
        }

        return null;
    }
}