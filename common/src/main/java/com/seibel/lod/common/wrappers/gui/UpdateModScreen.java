package com.seibel.lod.common.wrappers.gui;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.jar.JarUtils;
import com.seibel.lod.core.jar.updater.SelfUpdater;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * The screen that pops up if the mod has an update.
 *
 * @author coolGi
 */
// TODO: After finishing the config, rewrite this in openGL as well
// and also maybe add this suggestion https://discord.com/channels/881614130614767666/1035863487110467625/1035949054485594192
public class UpdateModScreen extends Screen {
    private Screen parent;
    private String newVersion;


    public UpdateModScreen(Screen parent, String newVersion) {
        super(translate(ModInfo.ID + ".updater.title"));
        this.parent = parent;
        this.newVersion = newVersion;
    }

    @Override
    protected void init() {
        super.init();


        try {
            // We cannot get assets from the root of the mod so we use this hack
            // TODO: Load the icon.png and logo.png in the mod initialise rather than here
            ResourceLocation logoLocation = new ResourceLocation(ModInfo.ID, "logo.png");
            Minecraft.getInstance().getTextureManager().register(
                    logoLocation,
                    new DynamicTexture(NativeImage.read(JarUtils.accessFile("logo.png")))
            );


            this.addBtn(new ImageButton(
                    // Where the button is on the screen
                    this.width / 2 - 100, this.height / 2 - 110,
                    // Width and height of the button
                    200, 100,
                    // Offset
                    0, 0,
                    // Some textuary stuff
                    0, logoLocation, 200, 100,
                    // Create the button and tell it where to go
                    // For now it goes to the client option by default
                    (buttonWidget) -> System.out.println("Nice, you found an easter egg :)"), // TODO: Add a proper easter egg to pressing the logo (maybe with confetti)
                    // Add a title to the button
                    translate(ModInfo.ID + ".updater.title")
            ));
        } catch (Exception e) { e.printStackTrace(); }


        this.addBtn(
                new Button(this.width / 2 - 155, this.height / 2 + 40, 150, 20, translate(ModInfo.ID + ".updater.update"), (btn) -> {
                    SelfUpdater.deleteOldOnClose = true;
                    SelfUpdater.updateMod();
                    this.onClose();
                })
        );
        this.addBtn(
                new Button(this.width / 2 + 5, this.height / 2 + 40, 150, 20, translate(ModInfo.ID + ".updater.silent"), (btn) -> {
                    Config.Client.AutoUpdater.promptForUpdate.set(false);
                    SelfUpdater.updateMod();
                    this.onClose();
                })
        );
        this.addBtn(
                new Button(this.width / 2 + 5, this.height / 2 + 65, 150, 20, translate(ModInfo.ID + ".updater.later"), (btn) -> {
                    this.onClose();
                })
        );
        this.addBtn(
                new Button(this.width / 2 - 155, this.height / 2 + 65, 150, 20, translate(ModInfo.ID + ".updater.never"), (btn) -> {
                    Config.Client.AutoUpdater.enableAutoUpdater.set(false);
                    this.onClose();
                })
        );

    }

    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices); // Render background


        // Render the text's
        drawCenteredString(matrices, this.font, translate(ModInfo.ID + ".updater.text1"), this.width / 2, this.height / 2, 0xFFFFFF);
        drawCenteredString(matrices, this.font, translate(ModInfo.ID + ".updater.text2", ModInfo.VERSION, this.newVersion), this.width / 2, this.height / 2 + 15, 0x52FD52);

        // TODO: add the tooltips for the buttons
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