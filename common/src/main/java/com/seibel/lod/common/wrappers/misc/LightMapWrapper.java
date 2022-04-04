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
