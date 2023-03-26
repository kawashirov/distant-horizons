package com.seibel.lod.common.wrappers.gui;

import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.config.ConfigBase;
import com.seibel.lod.core.config.gui.JavaFXConfigScreen;
import com.seibel.lod.core.config.gui.OpenGLConfigScreen;
import net.minecraft.client.gui.screens.Screen;

public class GetConfigScreen {
    public static type useScreen = type.JavaFX;
    public static enum type {
        Classic,
        @Deprecated
        OpenGL, // This was jsut an attempt, it didnt work out and we are going to change to javafx soon
        JavaFX;
    }

    public static Screen getScreen(Screen parent) {
        return switch (useScreen) {
            case Classic -> ClassicConfigGUI.getScreen(ConfigBase.INSTANCE, parent, "client");
            case OpenGL -> MinecraftScreen.getScreen(parent, new OpenGLConfigScreen(), ModInfo.ID + ".title");
            case JavaFX -> MinecraftScreen.getScreen(parent, new JavaFXConfigScreen(), ModInfo.ID + ".title");
            default -> null;
        };
    }
}