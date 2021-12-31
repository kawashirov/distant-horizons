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
    	// Require access widener
        return sprite.mainImage[0].getPixelRGBA(x + sprite.framesX[frameIndex] * sprite.getWidth(), y + sprite.framesY[frameIndex] * sprite.getHeight());
    }
}
