/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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
 
package com.seibel.distanthorizons.common.wrappers.misc;

import com.mojang.blaze3d.platform.NativeImage;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.ILightMapWrapper;
import org.lwjgl.opengl.GL32;

import java.nio.ByteBuffer;

/**
 * @author James Seibel
 * @version 11-21-2021
 */
public class LightMapWrapper implements ILightMapWrapper
{
    private int textureId = 0;

    public LightMapWrapper()
    {
    }

    private void createLightmap(NativeImage image)
    {
        textureId = GL32.glGenTextures();
        GL32.glBindTexture(GL32.GL_TEXTURE_2D, textureId);
        GL32.glTexImage2D(GL32.GL_TEXTURE_2D, 0, image.format().glFormat(), image.getWidth(), image.getHeight(),
                0, image.format().glFormat(), GL32.GL_UNSIGNED_BYTE, (ByteBuffer) null);
    }

    public void uploadLightmap(NativeImage image)
    {
        int currentBind = GL32.glGetInteger(GL32.GL_TEXTURE_BINDING_2D);
        GL32.glBindTexture(GL32.GL_TEXTURE_2D, textureId);
        if (textureId == 0) {
            createLightmap(image);
        }
        // NativeImage::upload(int levelOfDetail, int xOffset, int yOffset, bool shouldCleanup?)
        image.upload(0, 0, 0, false);
        GL32.glBindTexture(GL32.GL_TEXTURE_2D, currentBind);
    }

    @Override
    public void bind() {
        GL32.glBindTexture(GL32.GL_TEXTURE_2D, textureId);
    }

    @Override
    public void unbind() {
        GL32.glBindTexture(GL32.GL_TEXTURE_2D, 0);
    }
}
