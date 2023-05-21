package com.seibel.lod.common.wrappers.gui.updater;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.seibel.lod.common.wrappers.gui.TexturedButtonWidget;
import com.seibel.lod.coreapi.ModInfo;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.jar.JarUtils;
import com.seibel.lod.core.jar.installer.ModrinthGetter;
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
    private String newVersionID;


    public UpdateModScreen(Screen parent, String newVersionID) {
        super(translate(ModInfo.ID + ".updater.title"));
        this.parent = parent;
        this.newVersionID = newVersionID;
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


            // Logo image
            this.addBtn(new ImageButton(
                    // Where the button is on the screen
                    this.width / 2 - 65, this.height / 2 - 110,
                    // Width and height of the button
                    130, 65,
                    // Offset
                    0, 0,
                    // Some textuary stuff
                    0, logoLocation, 130, 65,
                    // Create the button and tell it where to go
                    // For now it goes to the client option by default
                    (buttonWidget) -> System.out.println("Nice, you found an easter egg :)"), // TODO: Add a proper easter egg to pressing the logo (maybe with confetti)
                    // Add a title to the button
                    translate(ModInfo.ID + ".updater.title")
            ));
        } catch (Exception e) { e.printStackTrace(); }


        this.addBtn(new TexturedButtonWidget(
                // Where the button is on the screen
                this.width / 2 - 97, this.height / 2 + 8,
                // Width and height of the button
                20, 20,
                // Offset
                0, 0,
                // Some textuary stuff
                0, new ResourceLocation(ModInfo.ID, "textures/gui/changelog.png"), 20, 20,
                // Create the button and tell it where to go
                (buttonWidget) -> Objects.requireNonNull(minecraft).setScreen(new ChangelogScreen(this, this.newVersionID)), // TODO: Add a proper easter egg to pressing the logo (maybe with confetti)
                // Add a title to the button
                translate(ModInfo.ID + ".updater.title")
        ));


        this.addBtn( // Update
                new Button(this.width / 2 - 75, this.height / 2 + 8, 150, 20, translate(ModInfo.ID + ".updater.update"), (btn) -> {
                    SelfUpdater.deleteOldOnClose = true;
                    SelfUpdater.updateMod();
                    this.onClose();
                })
        );
        this.addBtn( // Silent update
                new Button(this.width / 2 - 75, this.height / 2 + 30, 150, 20, translate(ModInfo.ID + ".updater.silent"), (btn) -> {
                    Config.Client.AutoUpdater.promptForUpdate.set(false);
                    SelfUpdater.updateMod();
                    this.onClose();
                })
        );
        this.addBtn( // Later (not now)
                new Button(this.width / 2 + 2, this.height / 2 + 70, 100, 20, translate(ModInfo.ID + ".updater.later"), (btn) -> {
                    this.onClose();
                })
        );
        this.addBtn( // Never
                new Button(this.width / 2 - 102, this.height / 2 + 70, 100, 20, translate(ModInfo.ID + ".updater.never"), (btn) -> {
                    Config.Client.AutoUpdater.enableAutoUpdater.set(false);
                    this.onClose();
                })
        );

    }

    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices); // Render background


        // Render the text's
        drawCenteredString(matrices, this.font, translate(ModInfo.ID + ".updater.text1"), this.width / 2, this.height / 2 - 35, 0xFFFFFF);
        drawCenteredString(matrices, this.font, translate(ModInfo.ID + ".updater.text2", ModInfo.VERSION, ModrinthGetter.releaseNames.get(this.newVersionID)), this.width / 2, this.height / 2 -20, 0x52FD52);

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