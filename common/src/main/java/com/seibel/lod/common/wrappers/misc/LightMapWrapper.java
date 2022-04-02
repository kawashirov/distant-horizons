package com.seibel.lod.common.wrappers.misc;

import com.mojang.blaze3d.platform.NativeImage;
import com.seibel.lod.core.wrapperInterfaces.misc.ILightMapWrapper;
import net.minecraft.client.renderer.LightTexture;

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
    public int bind() {
        tex.turnOnLightLayer();
        return 3553; //GL.GL_TEXTURE_2D
    }

    @Override
    public void unbind() {
        tex.turnOffLightLayer();
    }
}
