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
 
package com.seibel.lod.common.wrappers.block;


import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * For wrapping/utilizing around TextureAtlasSprite
 * @author Ran
 */
public class TextureAtlasSpriteWrapper {

    /**
     * This code is from Minecraft Forge
     * Which is licensed under the terms of GNU Lesser General Public License
     * as published by the Free Software Foundation version 2.1
     * of the License.
     *
     * The code has been modified to use TextureAtlasSprite
     */
    public static int getPixelRGBA(TextureAtlasSprite sprite, int frameIndex, int x, int y) {
        #if PRE_MC_1_17_1
        return sprite.mainImage[0].getPixelRGBA(
                x + sprite.framesX[frameIndex] * sprite.getWidth(),
                y + sprite.framesY[frameIndex] * sprite.getHeight());
        #elif PRE_MC_19_3
        if (sprite.animatedTexture != null) {
            x += sprite.animatedTexture.getFrameX(frameIndex) * sprite.width;
            y += sprite.animatedTexture.getFrameY(frameIndex) * sprite.height;
        }
        return sprite.mainImage[0].getPixelRGBA(x, y);
        #else
        if (sprite.contents().animatedTexture != null) {
            x += sprite.contents().animatedTexture.getFrameX(frameIndex) * sprite.contents().width();
            y += sprite.contents().animatedTexture.getFrameY(frameIndex) * sprite.contents().width();
        }
        return sprite.contents().originalImage.getPixelRGBA(x, y);
        #endif
    }
}
