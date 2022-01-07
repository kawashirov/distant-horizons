package com.seibel.lod.common.clouds;

import com.mojang.blaze3d.platform.NativeImage;
import com.seibel.lod.core.ModInfo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

public class SkyCoverGenerators {

    public static final int COLOR = NativeImage.combine(255, 255, 255, 255);
    public static final BufferedImage CLOUD_TEXTURE = accessFile("/assets/lod/textures/environment/clouds_small.png");

    // Where the generator for clouds could be made
    // TODO: Try to implement this https://www.reddit.com/r/Minecraft/comments/e7xol/this_is_how_clouds_should_work_gif_simulation/

    /** Generates clear sky */
    public static void clearSkyGenerator(NativeImage image) {
        for (int x = 0; x < image.getWidth(); x++) {
            for (int z = 0; z < image.getHeight(); z++) {
                image.setPixelRGBA(x, z, 0);
            }
        }
    }

    /** Generates the sky texture according to the texture */
    public static void normalSkyGenerator(NativeImage image) {
        for (int x = 0; x < CLOUD_TEXTURE.getWidth(); x++) {
            for (int z = 0; z < CLOUD_TEXTURE.getHeight(); z++) {
                image.setPixelRGBA(x, z, ((CLOUD_TEXTURE.getRGB(x, z) & 0x0000ff) > 130 ? COLOR : 0));
            }
        }
    }

    /** Acsess an image file from the jar */
    public static BufferedImage accessFile(String resource) {

        // this is the path within the jar file
        InputStream input = ModInfo.class.getResourceAsStream(resource);
        if (input == null) {
            // this is how we load file within editor (eg eclipse)
            input = ModInfo.class.getClassLoader().getResourceAsStream(resource);
        }

        // Turn it into an image
        BufferedImage image;
        try {
            image = ImageIO.read(input);
        } catch (Exception e) {
            image = null;
        }

        return image;
    }

    // Old code
    /*
    public static void noiseSkyGenerator(SimplexNoise noiseSampler, NativeImage image, double cloudiness) {
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

    public static void noiseSkyUpdate(Random random, SimplexNoise noiseSampler, NativeImage image, List<CloudTexture.PixelCoordinate> pixels, double cloudiness) {
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
    */
}
