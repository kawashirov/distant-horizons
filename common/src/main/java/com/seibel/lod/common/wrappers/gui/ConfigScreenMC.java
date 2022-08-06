package com.seibel.lod.common.wrappers.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.config.gui.AbstractScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
#if PRE_MC_1_19
import net.minecraft.network.chat.TranslatableComponent;
#endif

import java.util.*;

public class ConfigScreenMC {
    public static Screen getScreen(Screen parent, AbstractScreen screen) {

        return new ConfigScreenRenderer(parent, screen);
    }

    private static class ConfigScreenRenderer extends Screen {
        private final Screen parent;
        private ConfigListWidget list;
        private AbstractScreen screen;
        protected ConfigScreenRenderer(Screen parent, AbstractScreen screen) {
            #if PRE_MC_1_19
            super(new TranslatableComponent(ModInfo.ID + ".config.title"));
            #else
            super(Component.translatable(ModInfo.ID + ".config.title"));
            #endif
            this.parent = parent;
            this.screen = screen;
        }

        @Override
        protected void init() {
//            super.init();
            screen.width = this.width;
            screen.height = this.height;
            screen.init(); // Init our own config screen

            this.list = new ConfigListWidget(this.minecraft, this.width, this.height, 0, this.height, 25); // Select the area to tint
            if (this.minecraft != null && this.minecraft.level != null) // Check if in game
                this.list.setRenderBackground(false); // Disable from rendering
            this.addWidget(this.list); // Add the tint to the things to be rendered
        }

        @Override
        public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
            this.renderBackground(matrices); // Render background
            this.list.render(matrices, mouseX, mouseY, delta); // Renders the items in the render list (currently only used to tint background darker)

            screen.width = this.width;      // Is there a way to only call this when the window changes the size
            screen.height = this.height;    // Is there a way to only call this when the window changes the size
            screen.mouseX = mouseX;
            screen.mouseY = mouseY;
            screen.render(delta); // Render everything on the main screen

            super.render(matrices, mouseX, mouseY, delta); // Render the vanilla stuff (currently only used for the background and tint)
        }

        @Override
        public void tick() {
            screen.tick();
            if (screen.close)
                onClose();
        }

        @Override
        public void onClose() {
            screen.onClose();
            Objects.requireNonNull(minecraft).setScreen(this.parent); // Goto the parent screen
        }

        // For checking if it should close when you press the escape key
        @Override
        public boolean shouldCloseOnEsc() {
            return screen.shouldCloseOnEsc;
        }
    }

    public static class ConfigListWidget extends ContainerObjectSelectionList {
        public ConfigListWidget(Minecraft minecraftClient, int i, int j, int k, int l, int m) {
            super(minecraftClient, i, j, k, l, m);
            this.centerListVertically = false;
        }
    }

}
