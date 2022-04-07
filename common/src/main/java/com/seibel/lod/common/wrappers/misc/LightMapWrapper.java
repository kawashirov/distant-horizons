/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
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
 
package com.seibel.lod.common.wrappers.misc;

import com.mojang.blaze3d.platform.NativeImage;
import com.seibel.lod.core.wrapperInterfaces.misc.ILightMapWrapper;
import net.minecraft.client.renderer.LightTexture;
import org.lwjgl.opengl.GL32;

/**
 * @author James Seibel
 * @version 11-21-2021
 */
public class LightMapWrapper implements ILightMapWrapper
{
    static NativeImage lightMap = null;

    private LightTexture tex;

    public LightMapWrapper(NativeImage newLightMap)
    {
        lightMap = newLightMap;
    }

    public LightMapWrapper(LightTexture lightTexture) {
        tex = lightTexture;
    }

    public static void setLightMap(NativeImage newLightMap)
    {
        lightMap = newLightMap;
    }

    @Override
    public int getLightValue(int skyLight, int blockLight)
    {
        return lightMap.getPixelRGBA(skyLight, blockLight);
    }

    @Override
    public void bind() {
        GL32.glBindTexture(GL32.GL_TEXTURE_2D, tex.lightTexture.getId());
    }

    @Override
    public void unbind() {
        GL32.glBindTexture(GL32.GL_TEXTURE_2D, 0);
    }
}
