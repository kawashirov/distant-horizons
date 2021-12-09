package com.seibel.lod.common.wrappers.config;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Creates a button with a texture on it
 */
public class TexturedButtonWidget extends ImageButton {
    public TexturedButtonWidget(int x, int y, int width, int height, int u, int v, ResourceLocation texture, OnPress pressAction) {
        super(x, y, width, height, u, v, texture, pressAction);
    }
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
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        int i = this.getYImage(this.isHovered);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        this.blit(matrices, this.x, this.y, 0, 46 + i * 20, this.width / 2, this.height);
        this.blit(matrices, this.x + this.width / 2, this.y, 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height);

        super.renderButton(matrices, mouseX, mouseY, delta);
    }
}
