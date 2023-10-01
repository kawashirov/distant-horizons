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

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

#if PRE_MC_1_17_1
import net.minecraft.client.Minecraft;
#else
import net.minecraft.client.renderer.GameRenderer;
#endif

#if MC_1_20_2
import net.minecraft.client.gui.components.WidgetSprites;
#endif


/**
 * Creates a button with a texture on it
 */
// TODO: Is this still needed? Can we switch to vanilla's ImageButton?
// 
// Note after 1.20.2: They changed how buttons work, and are asking for a WidgetSprites.
// This should probably still exist so that code only needs to be changed here to work on all mc versions
public class TexturedButtonWidget extends ImageButton
{
	#if MC_1_20_2
	public static WidgetSprites locationToSprite(int width, int height, int u, int v, int hoveredVOffset, ResourceLocation resourceLocation)
	{
		// FIXME[1.20.2]: Fix this
		//Registries.
		//
		//ResourceLocation unfocused = new ResourceLocation();
        //ResourceLocation focused = new ResourceLocation(resourceLocation.getNamespace(), resourceLocation.getPath());
		
		return new WidgetSprites(resourceLocation, resourceLocation);
	}
	#endif
	
	public TexturedButtonWidget(int x, int y, int width, int height, int u, int v, int hoveredVOffset, ResourceLocation texture, int textureWidth, int textureHeight, OnPress pressAction, Component text)
	{
		#if MC_1_20_2
		super(x, y, width, height, locationToSprite(textureWidth, textureHeight, u, v, hoveredVOffset, texture), pressAction, text);
		#else
		super(x, y, width, height, u, v, hoveredVOffset, texture, textureWidth, textureHeight, pressAction, text);
		#endif
	}
	
	#if PRE_MC_1_19_2
	public TexturedButtonWidget(int x, int y, int width, int height, int u, int v, int hoveredVOffset, ResourceLocation texture, int textureWidth, int textureHeight, OnPress pressAction, OnTooltip tooltipSupplier, Component text)
	{
		super(x, y, width, height, u, v, hoveredVOffset, texture, textureWidth, textureHeight, pressAction, tooltipSupplier, text);
	}
	
	@Override
	public void renderButton(PoseStack matrices, int mouseX, int mouseY, float delta)
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
		this.blit(matrices, this.x, this.y, 0, 46 + i * 20, this.width / 2, this.height);
		this.blit(matrices, this.x + this.width / 2, this.y, 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height);
		
		super.renderButton(matrices, mouseX, mouseY, delta);
	}
    #endif
}
