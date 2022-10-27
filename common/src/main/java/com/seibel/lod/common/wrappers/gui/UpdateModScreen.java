package com.seibel.lod.common.wrappers.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.jar.updater.SelfUpdater;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

public class UpdateModScreen extends Screen {
    private Screen parent;
    private String newVersion;

    #if PRE_MC_1_19
    public static net.minecraft.network.chat.TranslatableComponent translate (String str, Object... args) {
        return new net.minecraft.network.chat.TranslatableComponent(str, args);
    }
    #else
    public static net.minecraft.network.chat.MutableComponent translate (String str, Object... args) {
            return net.minecraft.network.chat.Component.translatable(str, args);
    }
    #endif

    public UpdateModScreen(Screen parent, String newVersion) {
        super(translate(ModInfo.ID + ".updater.title"));
        this.parent = parent;
        this.newVersion = newVersion;
    }

    @Override
    protected void init() {
        super.init();


        this.addBtn(
                new Button(this.width / 2 - 155, this.height / 2, 150, 20, translate(ModInfo.ID + ".updater.update"), (btn) -> {
                    SelfUpdater.deleteOldOnClose = true;
                    SelfUpdater.updateMod();
                    this.onClose();
                })
        );
        this.addBtn(
                new Button(this.width / 2 + 5, this.height / 2, 150, 20, translate(ModInfo.ID + ".updater.later"), (btn) -> {
                    this.onClose();
                })
        );
        this.addBtn(
                new Button(this.width / 2 - 155, this.height / 2 + 25, 150, 20, translate(ModInfo.ID + ".updater.never"), (btn) -> {
                    Config.Client.AutoUpdater.enableAutoUpdater.set(false);
                    this.onClose();
                })
        );
        this.addBtn(
                new Button(this.width / 2 + 5, this.height / 2 + 25, 150, 20, translate(ModInfo.ID + ".updater.silentUpdate"), (btn) -> {
                    Config.Client.AutoUpdater.promptForUpdate.set(false);
                    SelfUpdater.updateMod();
                    this.onClose();
                })
        );

    }

    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices); // Render background

        // Render the text's
        drawCenteredString(matrices, this.font, translate(ModInfo.ID + ".updater.text1"), this.width / 2, this.height / 2 - 40, 0xFFFFFF);
        drawCenteredString(matrices, this.font, translate(ModInfo.ID + ".updater.text2", ModInfo.VERSION, this.newVersion), this.width / 2, this.height / 2 - 25, 0xFFFFFF);

        super.render(matrices, mouseX, mouseY, delta); // Render the buttons

        // TODO: Add tooltips
    }

    @Override
    public void onClose() {
        Objects.requireNonNull(minecraft).setScreen(this.parent); // Goto the parent screen
    }




    // addRenderableWidget in 1.17 and over
    // addButton in 1.16 and below
    private void addBtn(Button button) {
		#if PRE_MC_1_17_1
        this.addButton(button);
		#else
        this.addRenderableWidget(button);
		#endif
    }
}