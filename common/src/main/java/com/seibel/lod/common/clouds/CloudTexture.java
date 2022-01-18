package com.seibel.lod.common.clouds;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.*;

public class CloudTexture {

    public List<PixelCoordinate> pixels = new LinkedList<PixelCoordinate>() {};

    public SimplexNoise noise;
    public DynamicTexture cloudsTexture;
    public ResourceLocation resourceLocation;
    public double cloudiness;

    public CloudTexture(ResourceLocation resourceLocation) {
        this.resourceLocation = resourceLocation;
    }

    public void updateImage() {
        // Comment to not update the sky
//        SkyCoverGenerators.cloudySkyUpdate(random, this.noise, this.cloudsTexture.getPixels(), pixels, this.cloudiness);
    }

    public void updatePixels() {
        pixels.removeIf(pixel -> !fadePixel(Objects.requireNonNull(this.cloudsTexture.getPixels()), pixel.posX, pixel.posZ, pixel.fading));

        this.cloudsTexture.upload();
    }

    public boolean fadePixel(NativeImage image, int x, int z, boolean fading) {
        int color = image.getPixelRGBA(x, z);
        int alpha = (color >> 24) & 0xFF;
        //int alpha = image.getLuminanceOrAlpha(x, z) + 128;

        if (fading) alpha -= 5;
        else alpha += 5;

        int newColor = alpha << 24 | 255 << 16 | 255 << 8 | 255;
        //int newColor = NativeImage.combine(alpha, 255, 255, 255);
        image.setPixelRGBA(x, z, newColor);

        if (alpha <= 0) {
            image.setPixelRGBA(x, z, 0);
            return false;
        } else return alpha < 255;
    }

    public void setTexture(DynamicTexture texture) {
        this.cloudsTexture = texture;
    }

    /** Generates the noise at the start of the game */
    public void initNoise(Random random) {
//        this.noise = new SimplexNoise(new WorldgenRandom(random.nextLong()));
        this.noise = new SimplexNoise(new LegacyRandomSource(random.nextLong()));
    }

    public DynamicTexture getNativeImage() {
        NativeImage image = new NativeImage(SkyCoverGenerators.CLOUD_TEXTURE.getWidth(), SkyCoverGenerators.CLOUD_TEXTURE.getHeight(), false);

        // Switch these out to clear sky
        // Never comment both or something weird will happen
        SkyCoverGenerators.normalSkyGenerator(image);

        return new DynamicTexture(image);
    }

    public static class PixelCoordinate {
        public int posX;
        public int posZ;
        public boolean fading;

        public PixelCoordinate(int posX, int posZ, boolean fading) {
            this.posX = posX;
            this.posZ = posZ;
            this.fading = fading;
        }
    }
}
