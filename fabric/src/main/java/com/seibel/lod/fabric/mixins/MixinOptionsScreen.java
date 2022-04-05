package com.seibel.lod.fabric.mixins;

import com.seibel.lod.common.wrappers.config.ConfigGui;
import com.seibel.lod.common.wrappers.config.TexturedButtonWidget;
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

/**
 * Adds a button to the menu to goto the config
 *
 * @author coolGi2007
 * @version 12-02-2021
*/
@Mixin(OptionsScreen.class)
public class MixinOptionsScreen extends Screen {
    // Get the texture for the button
    private static final ResourceLocation ICON_TEXTURE = new ResourceLocation(ModInfo.ID,"textures/gui/button.png");
    protected MixinOptionsScreen(Component title) {
        super(title);
    }

    @Inject(at = @At("HEAD"),method = "init")
    private void lodconfig$init(CallbackInfo ci) {
        if (SingletonHandler.get(ILodConfigWrapperSingleton.class).client().getOptionsButton())
            this. #if MC_VERSION_1_16_5 addButton #else addRenderableWidget #endif
                (new TexturedButtonWidget(
                // Where the button is on the screen
                this.width / 2 - 180, this.height / 6 - 12,
                // Width and height of the button
                20, 20,
                // Offset
                0, 0,
                // Some textuary stuff
                20, ICON_TEXTURE, 20, 40,
                // Create the button and tell it where to go
                // For now it goes to the client option by default
                (buttonWidget) -> Objects.requireNonNull(minecraft).setScreen(ConfigGui.getScreen(this, "client")),
                // Add a title to the screen
                new TranslatableComponent("text.autoconfig." + ModInfo.ID + ".title")));
    }
}
