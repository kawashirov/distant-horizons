package com.seibel.lod.common.clouds;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.List;
import java.util.Random;

public class SkyCoverGenerators {

    public static final int COLOR = NativeImage.combine(255, 255, 255, 255);

    // Where the generator for clouds could be made
    // TODO: Try to impliment this https://www.reddit.com/r/Minecraft/comments/e7xol/this_is_how_clouds_should_work_gif_simulation/

    public static void clearSkyGenerator(SimplexNoise noiseSampler, NativeImage image, double cloudiness) {
        for (int x = 0; x < 256; x++) {
            for (int z = 0; z < 256; z++) {
                if (noiseSampler.getValue(x / 16.0, 0, z / 16.0) * 2.5 < cloudiness || image.getPixelRGBA(x, z) != 0) {
                    image.setPixelRGBA(x, z, 0);
                }
            }
        }
    }

    public static void cloudySkyGenerator(SimplexNoise noiseSampler, NativeImage image, double cloudiness) {
        for (int x = 0; x < 256; x++) {
            for (int z = 0; z < 256; z++) {
                image.setPixelRGBA(x, z, COLOR);
                if (noiseSampler.getValue(x / 16.0, 0, z / 16.0) * 2.5 >= cloudiness || image.getPixelRGBA(x, z) != 0) {
                    if ((int) (noiseSampler.getValue(x / 16.0, 0, z / 16.0) * 2.5) != 0) {
                        image.setPixelRGBA(x, z, 0);
                    }
                }
            }
        }
    }

    public static void cloudySkyUpdate(Random random, SimplexNoise noiseSampler, NativeImage image, List<CloudTexture.PixelCoordinate> pixels, double cloudiness) {
        int count = random.nextInt(4000) + 4000;

        for (int i = 0; i < count; i++) {
            int x = random.nextInt(256);
            int z = random.nextInt(256);

            if (!updatingPixel(x, z, pixels)) {
                if (noiseSampler.getValue(x / 16.0, 0, z / 16.0) * 2.5 < cloudiness && image.getPixelRGBA(x, z) == 0) {
                    if ((int) (noiseSampler.getValue(x / 16.0, 0, z / 16.0) * 2.5) != 0) {
                        pixels.add(new CloudTexture.PixelCoordinate(x, z, true));
                    } else {
                        pixels.add(new CloudTexture.PixelCoordinate(x, z, false));
                    }
                } else {
                    pixels.add(new CloudTexture.PixelCoordinate(x, z, false));
                }
            }
        }
    }

    public static boolean updatingPixel(int x, int z, List<CloudTexture.PixelCoordinate> pixels) {
        for (CloudTexture.PixelCoordinate pixel : pixels) {
            if (pixel.posX == x && pixel.posZ == z) {
                return true;
            }
        }
        return false;
    }
}
