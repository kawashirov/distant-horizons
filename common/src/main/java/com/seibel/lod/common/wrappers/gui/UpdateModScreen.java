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
    private ConfigListWidget list;
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

        this.list = new ConfigListWidget(this.minecraft, this.width, this.height, 0, this.height, 25); // Select the area to tint
        this.addWidget(this.list); // Add the tint to the things to be rendered


        this.addBtn(
                new Button(this.width / 2 - 100, this.height / 2 - 20, 150, 20, translate(ModInfo.ID + ".updater.update"), (btn) -> {
                    this.onClose();
                    SelfUpdater.deleteOldOnClose = true;
                })
        );
        this.addBtn(
                new Button(this.width / 2 - 100, this.height / 2 + 5, 150, 20, translate(ModInfo.ID + ".updater.later"), (btn) -> {
                    this.onClose();
                })
        );
        this.addBtn(
                new Button(this.width / 2 - 100, this.height / 2 + 30, 150, 20, translate(ModInfo.ID + ".updater.never"), (btn) -> {
                    this.onClose();
                    Config.Client.AutoUpdater.enableAutoUpdater.set(false);
                })
        );
        this.addBtn(
                new Button(this.width / 2 - 100, this.height / 2 + 55, 150, 20, translate(ModInfo.ID + ".updater.silentUpdate"), (btn) -> {
                    this.onClose();
                    Config.Client.AutoUpdater.promptForUpdate.set(false);
                })
        );

    }

    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices); // Render background
        this.list.render(matrices, mouseX, mouseY, delta); // Renders the items in the render list (currently only used to tint background darker)

        super.render(matrices, mouseX, mouseY, delta); // Render the vanilla stuff (currently only used for the background and tint)

        drawCenteredString(matrices, this.font, translate(ModInfo.ID + ".updater.text1"), this.width / 2, this.height / 2 - 60, 0xFFFFFF);
        drawCenteredString(matrices, this.font, translate(ModInfo.ID + ".updater.text2", ModInfo.VERSION, this.newVersion), this.width / 2, this.height / 2 - 45, 0xFFFFFF);
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


    public static class ConfigListWidget extends ContainerObjectSelectionList<ButtonEntry> {
        Font textRenderer;

        public ConfigListWidget(Minecraft minecraftClient, int i, int j, int k, int l, int m) {
            super(minecraftClient, i, j, k, l, m);
            this.centerListVertically = false;
            textRenderer = minecraftClient.font;
        }

        public void addButton(AbstractWidget button, AbstractWidget resetButton, AbstractWidget indexButton, Component text) {
            this.addEntry(ButtonEntry.create(button, text, resetButton, indexButton));
        }

        @Override
        public int getRowWidth() {
            return 10000;
        }

        public Optional<AbstractWidget> getHoveredButton(double mouseX, double mouseY) {
            for (ButtonEntry buttonEntry : this.children()) {
                if (buttonEntry.button != null && buttonEntry.button.isMouseOver(mouseX, mouseY)) {
                    return Optional.of(buttonEntry.button);
                }
            }
            return Optional.empty();
        }
    }


    public static class ButtonEntry extends ContainerObjectSelectionList.Entry<ButtonEntry> {
        private static final Font textRenderer = Minecraft.getInstance().font;
        public final AbstractWidget button;
        private final AbstractWidget resetButton;
        private final AbstractWidget indexButton;
        private final Component text;
        private final List<AbstractWidget> children = new ArrayList<>();
        public static final Map<AbstractWidget, Component> buttonsWithText = new HashMap<>();

        private ButtonEntry(AbstractWidget button, Component text, AbstractWidget resetButton, AbstractWidget indexButton) {
            buttonsWithText.put(button, text);
            this.button = button;
            this.resetButton = resetButton;
            this.text = text;
            this.indexButton = indexButton;
            if (button != null)
                children.add(button);
            if (resetButton != null)
                children.add(resetButton);
            if (indexButton != null)
                children.add(indexButton);
        }

        public static ButtonEntry create(AbstractWidget button, Component text, AbstractWidget resetButton, AbstractWidget indexButton) {
            return new ButtonEntry(button, text, resetButton, indexButton);
        }

        @Override
        public void render(PoseStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            if (button != null) {
                button.y = y;
                button.render(matrices, mouseX, mouseY, tickDelta);
            }
            if (resetButton != null) {
                resetButton.y = y;
                resetButton.render(matrices, mouseX, mouseY, tickDelta);
            }
            if (indexButton != null) {
                indexButton.y = y;
                indexButton.render(matrices, mouseX, mouseY, tickDelta);
            }
            if (text != null && (!text.getString().contains("spacer") || button != null))
                GuiComponent.drawString(matrices, textRenderer, text, 12, y + 5, 0xFFFFFF);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return children;
        }

        // Only for 1.17 and over
        // Remove in 1.16 and below
		#if POST_MC_1_17_1
        @Override
        public List<? extends NarratableEntry> narratables() {
            return children;
        }
		#endif
    }
}
