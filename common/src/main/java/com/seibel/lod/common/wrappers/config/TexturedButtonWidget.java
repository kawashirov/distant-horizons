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
 
package com.seibel.lod.common.wrappers.config;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Creates a button with a texture on it
 */
public class TexturedButtonWidget extends ImageButton {
    #if MC_VERSION_1_17_1 || MC_VERSION_1_18_1 || MC_VERSION_1_18_2
    public TexturedButtonWidget(int x, int y, int width, int height, int u, int v, ResourceLocation texture, OnPress pressAction) {
        super(x, y, width, height, u, v, texture, pressAction);
    }
    #endif

    public TexturedButtonWidget(int x, int y, int width, int height, int u, int v, int hoveredVOffset, ResourceLocation texture, int textureWidth, int textureHeight, OnPress pressAction) {
        super(x, y, width, height, u, v, hoveredVOffset, texture, textureWidth, textureHeight, pressAction);
    }

    public TexturedButtonWidget(int x, int y, int width, int height, int u, int v, int hoveredVOffset, ResourceLocation texture, int textureWidth, int textureHeight, OnPress pressAction, Component text) {
        super(x, y, width, height, u, v, hoveredVOffset, texture, textureWidth, textureHeight, pressAction, text);
    }

    public TexturedButtonWidget(int x, int y, int width, int height, int u, int v, int hoveredVOffset, ResourceLocation texture, int textureWidth, int textureHeight, OnPress pressAction, OnTooltip tooltipSupplier, Component text) {
        super(x, y, width, height, u, v, hoveredVOffset, texture, textureWidth, textureHeight, pressAction, tooltipSupplier, text);
    }

    @Override
    public void renderButton(PoseStack matrices, int mouseX, int mouseY, float delta) {
        #if MC_VERSION_1_17_1 || MC_VERSION_1_18_1 || MC_VERSION_1_18_2
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        #elif MC_VERSION_1_16_5
        Minecraft.getInstance().getTextureManager().bind(WIDGETS_LOCATION);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);
        #endif

        int i = this.getYImage(this.isHovered);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        this.blit(matrices, this.x, this.y, 0, 46 + i * 20, this.width / 2, this.height);
        this.blit(matrices, this.x + this.width / 2, this.y, 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height);

        super.renderButton(matrices, mouseX, mouseY, delta);
    }
}
