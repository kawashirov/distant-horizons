package com.seibel.distanthorizons.common.wrappers.gui.updater;

import com.mojang.blaze3d.vertex.PoseStack;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.wrapperInterfaces.IVersionConstants;
import com.seibel.distanthorizons.coreapi.ModInfo;
import com.seibel.distanthorizons.core.jar.installer.MarkdownFormatter;
import com.seibel.distanthorizons.core.jar.installer.ModrinthGetter;
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
import net.minecraft.network.chat.TextComponent;

import java.util.*;

/**
 * The screen that pops up if the mod has an update.
 *
 * @author coolGi
 */
// TODO: After finishing the config, rewrite this in openGL as well
// TODO: Make this
public class ChangelogScreen extends Screen {
    private Screen parent;
    private String versionID;
    private List<String> changelog;
    private TextArea changelogArea;

    public ChangelogScreen(Screen parent) {
        this(parent, null);

        if (!ModrinthGetter.initted) // Make sure the modrinth stuff is initted
            ModrinthGetter.init();
        if (!ModrinthGetter.initted) // If its not initted the just close the screen
            onClose();

        setupChangelog(ModrinthGetter.getLatestIDForVersion(SingletonInjector.INSTANCE.get(IVersionConstants.class).getMinecraftVersion()));
    }

    public ChangelogScreen(Screen parent, String versionID) {
        super(translate(ModInfo.ID + ".updater.title"));
        this.parent = parent;
        this.versionID = versionID;

        if (versionID != null)
            setupChangelog(versionID);
    }

    private void setupChangelog(String versionID) {
        this.changelog = new ArrayList<>();

        // Put the new version name at the very top of the change log
        this.changelog.add("§lChangelog for " + ModrinthGetter.releaseNames.get(versionID) + "§r");
        this.changelog.add("");
        this.changelog.add("");

        // Get the release changelog and split it by the new lines
        List<String> unwrappedChangelog =
                List.of(new MarkdownFormatter.MinecraftFormat().convertTo( // This formats markdown to minecraft's "§" characters
                        ModrinthGetter.changeLogs.get(versionID)
                ).split("\\n"));
        // Makes the words wrap around to not go off the screen
        for (String str: unwrappedChangelog) {
            this.changelog.addAll(
                    MarkdownFormatter.splitString(str, 75)
            );
        }
        // Debugging
//        System.out.println(this.changelog);
    }



    @Override
    protected void init() {
        super.init();


        this.addBtn( // Close
                new Button(5, this.height - 25, 100, 20, translate(ModInfo.ID + ".general.back"), (btn) -> {
                    this.onClose();
                })
        );


        this.changelogArea = new TextArea(this.minecraft, this.width*2, this.height, 32, this.height - 32, 10);
        for (int i = 0; i < changelog.size(); i++) {
            this.changelogArea.addButton(new TextComponent(changelog.get(i)));
//            drawString(matrices, this.font, changelog.get(i), this.width / 2 - 175, this.height / 2 - 100 + i*10, 0xFFFFFF);
        }

    }

    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices); // Render background

        // Set the scroll position to the mouse height relative to the screen
        // This is a bit of a hack as we cannot scroll on this area
        this.changelogArea.scrollAmount = ((double) mouseY)/((double) this.height) * 1.1 * this.changelogArea.getMaxScroll();

        this.changelogArea.render(matrices, mouseX, mouseY, delta); // Render the changelog

        super.render(matrices, mouseX, mouseY, delta); // Render the buttons

        drawCenteredString(matrices, font, title, width / 2, 15, 0xFFFFFF); // Render title
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

    #if PRE_MC_1_19
    public static net.minecraft.network.chat.TranslatableComponent translate (String str, Object... args) {
        return new net.minecraft.network.chat.TranslatableComponent(str, args);
    }
    #else
    public static net.minecraft.network.chat.MutableComponent translate (String str, Object... args) {
            return net.minecraft.network.chat.Component.translatable(str, args);
    }
    #endif










    public static class TextArea extends ContainerObjectSelectionList<ButtonEntry> {
        Font textRenderer;

        public TextArea(Minecraft minecraftClient, int i, int j, int k, int l, int m) {
            super(minecraftClient, i, j, k, l, m);
            this.centerListVertically = false;
            textRenderer = minecraftClient.font;
        }

        public void addButton(Component text) {
            this.addEntry(ButtonEntry.create(text));
        }

        @Override
        public int getRowWidth() {
            return 10000;
        }
    }

    public static class ButtonEntry extends ContainerObjectSelectionList.Entry<ButtonEntry> {
        private static final Font textRenderer = Minecraft.getInstance().font;
        private final Component text;
        private final List<AbstractWidget> children = new ArrayList<>();

        private ButtonEntry(Component text) {
            this.text = text;
        }

        public static ButtonEntry create(Component text) {
            return new ButtonEntry(text);
        }

        @Override
        public void render(PoseStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            GuiComponent.drawString(matrices, textRenderer, text, 12, y + 5, 0xFFFFFF);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return children;
        }
		#if POST_MC_1_17_1
        @Override
        public List<? extends NarratableEntry> narratables() {
            return children;
        }
		#endif
    }
}