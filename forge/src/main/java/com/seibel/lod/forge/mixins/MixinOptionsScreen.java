/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package com.seibel.lod.forge.mixins;

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
        if (SingletonHandler.get(ILodConfigWrapperSingleton.class).client().getOptionsButton()) {
            TexturedButtonWidget widget = new TexturedButtonWidget(
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
                    new TranslatableComponent("text.autoconfig." + ModInfo.ID + ".title"));
            
            #if MC_VERSION_1_18_1 || MC_VERSION_1_18_2
            this.addRenderableWidget(widget);
            #else
            this.addWidget(widget);
            #endif
        }
    }
}
