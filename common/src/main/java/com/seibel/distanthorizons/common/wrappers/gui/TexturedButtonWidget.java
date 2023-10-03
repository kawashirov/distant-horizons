/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2023 James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.distanthorizons.common.wrappers.gui;

import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
#if PRE_MC_1_20_1
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
#else
import net.minecraft.client.gui.GuiGraphics;
#endif
#if POST_MC_1_20_2
import net.minecraft.client.gui.components.WidgetSprites;
#endif

/**
 * Creates a button with a texture on it (and a background) that works with all mc versions
 * 
 * @author coolGi
 * @version 2023-10-03
 */
public class TexturedButtonWidget extends ImageButton
{
	public final boolean renderBackground;
	#if POST_MC_1_20_2
	public static WidgetSprites locationToSprite(ResourceLocation resourceLocation, int u, int v)
	{
		// FIXME[1.20.2]: Change the `-1`'s to use resourceLocation's texture values
		return locationToSprite(resourceLocation, u, v, -1, -1, -1);
	}
	public static WidgetSprites locationToSprite(ResourceLocation resourceLocation, int u, int v, int textureWidth, int textureHeight, int hoveredVOffset)
	{
		// FIXME[1.20.2]: No idea how widget sprite works, and not documented anywhere
		/*
			Several things need to be done to fix this
				- First of all, what do each of the paramiters mean
					We know that they may relate to the normal, deactivated, hovered, and pressed textures, but which is which. And are these guesses correct?
				- How do we move the texture
					As Minecraft is now asking for several textures, instead of one big texture, what is the plan on getting this to work?
					- One strategy is to split the textures in our assets, then combine them for older versions
					- Another is to split it for newer versions
					Either of those requires the texture files to be changed, and new resources to be mapped
		 */
		return new WidgetSprites(resourceLocation, resourceLocation);
	}
	#endif
	
	
	
	
	#if POST_MC_1_17_1
	public TexturedButtonWidget(int x, int y, int width, int height, int u, int v, ResourceLocation texture, OnPress pressAction) {
		this(x, y, width, height, u, v, texture, pressAction, true);
	}
	public TexturedButtonWidget(int x, int y, int width, int height, int u, int v, ResourceLocation texture, OnPress pressAction, boolean renderBackground)
	{
		#if PRE_MC_1_20_2
		super(x, y, width, height, u, v, texture, pressAction);
		#else
		super(x, y, width, height, locationToSprite(texture, u, v), pressAction);
		#endif
		this.renderBackground = renderBackground;
	}
    #endif
	
	public TexturedButtonWidget(int x, int y, int width, int height, int u, int v, int hoveredVOffset, ResourceLocation texture, int textureWidth, int textureHeight, OnPress pressAction) {
		this(x, y, width, height, u, v, hoveredVOffset, texture, textureWidth, textureHeight, pressAction, true);
	}
	public TexturedButtonWidget(int x, int y, int width, int height, int u, int v, int hoveredVOffset, ResourceLocation texture, int textureWidth, int textureHeight, OnPress pressAction, boolean renderBackground)
	{
		#if PRE_MC_1_20_2
		super(x, y, width, height, u, v, hoveredVOffset, texture, textureWidth, textureHeight, pressAction);
		#else
		super(x, y, width, height, locationToSprite(texture, u, v, textureWidth, textureHeight, hoveredVOffset), pressAction);
		#endif
		this.renderBackground = renderBackground;
	}
	
	public TexturedButtonWidget(int x, int y, int width, int height, int u, int v, int hoveredVOffset, ResourceLocation texture, int textureWidth, int textureHeight, OnPress pressAction, Component text) {
		this(x, y, width, height, u, v, hoveredVOffset, texture, textureWidth, textureHeight, pressAction, text, true);
	}
	public TexturedButtonWidget(int x, int y, int width, int height, int u, int v, int hoveredVOffset, ResourceLocation texture, int textureWidth, int textureHeight, OnPress pressAction, Component text, boolean renderBackground)
	{
		#if PRE_MC_1_20_2
		super(x, y, width, height, u, v, hoveredVOffset, texture, textureWidth, textureHeight, pressAction, text);
		#else
		super(x, y, locationToSprite(texture, u, v, textureWidth, textureHeight, hoveredVOffset), pressAction, text);
		#endif
		this.renderBackground = renderBackground;
	}
	
	
	#if PRE_MC_1_19_4
	@Override
	public void renderButton(PoseStack matrices, int mouseX, int mouseY, float delta) {
		if (this.renderBackground) // Renders the background of the button
		{
			#if PRE_MC_1_17_1
			Minecraft.getInstance().getTextureManager().bind(WIDGETS_LOCATION);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);
			#else
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
			#endif

			int i = this.getYImage(this.isHovered);
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.enableDepthTest();
			#if PRE_MC_1_19_4
			this.blit(matrices, this.x, this.y, 0, 46 + i * 20, this.width / 2, this.height);
			this.blit(matrices, this.x + this.width / 2, this.y, 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height);
			#else
			this.blit(matrices, this.getX(), this.getY(), 0, 46 + i * 20, this.getWidth() / 2, this.getHeight());
			this.blit(matrices, this.getX() + this.getWidth() / 2, this.getY(), 200 - this.width / 2, 46 + i * 20, this.getWidth() / 2, this.getHeight());
			#endif
		}

		super.renderButton(matrices, mouseX, mouseY, delta);
	}
	#else
	@Override
    #if PRE_MC_1_20_1
    public void renderWidget(PoseStack matrices, int mouseX, int mouseY, float delta)
    {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
    #else
	public void renderWidget(GuiGraphics matrices, int mouseX, int mouseY, float delta)
	{
    #endif
		if (this.renderBackground) // Renders the background of the button
		{
			int i = 1;
			if (!this.active)           i = 0;
			else if (this.isHovered)    i = 2;

            #if PRE_MC_1_20_1
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
			
            this.blit(matrices, this.getX(), this.getY(), 0, 46 + i * 20, this.getWidth() / 2, this.getHeight());
            this.blit(matrices, this.getX() + this.getWidth() / 2, this.getY(), 200 - this.width / 2, 46 + i * 20, this.getWidth() / 2, this.getHeight());
            #elif MC_1_20_1
			matrices.blit(WIDGETS_LOCATION, this.getX(), this.getY(), 0, 46 + i * 20, this.getWidth() / 2, this.getHeight());
			matrices.blit(WIDGETS_LOCATION, this.getX() + this.getWidth() / 2, this.getY(), 200 - this.width / 2, 46 + i * 20, this.getWidth() / 2, this.getHeight());
			#else
			// FIXME[1.20.2]: Later changed `.enabled()` to a proper thing taking the hovered and active into account
			matrices.blit(sprites.enabled(), this.getX(), this.getY(), 0, 46 + i * 20, this.getWidth() / 2, this.getHeight());
			matrices.blit(sprites.enabled(), this.getX() + this.getWidth() / 2, this.getY(), 200 - this.width / 2, 46 + i * 20, this.getWidth() / 2, this.getHeight());
            #endif
		}
		
		super.renderWidget(matrices, mouseX, mouseY, delta);
	}
    #endif
}
