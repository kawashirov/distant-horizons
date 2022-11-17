package com.seibel.lod.common.wrappers.gui.updater;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.seibel.lod.common.wrappers.gui.TexturedButtonWidget;
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.jar.JarUtils;
import com.seibel.lod.core.jar.updater.SelfUpdater;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * The screen that pops up if the mod has an update.
 *
 * @author coolGi
 */
// TODO: After finishing the config, rewrite this in openGL as well
// TODO: Make this
public class ChangelogScreen extends Screen {
    private Screen parent;
    private String version;


    public ChangelogScreen(Screen parent, String version) {
        super(translate(ModInfo.ID + ".updater.title"));
        this.parent = parent;
        this.version = version;
    }

    @Override
    protected void init() {
        super.init();


        this.addBtn( // Close
                new Button(5, this.height - 25, 100, 20, translate(ModInfo.ID + ".general.back"), (btn) -> {
                    this.onClose();
                })
        );

    }

    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices); // Render background


        // Render the text
        drawCenteredString(matrices, this.font, new TextComponent("Some changelog thing\nIsn't done yet so pls wait"), this.width / 2, this.height / 2 - 35, 0xFFFFFF);

        super.render(matrices, mouseX, mouseY, delta); // Render the buttons
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
}