package com.seibel.lod.util;

import java.awt.*;

public class ColorUtil
{
    public static int rgbToInt(int red, int green, int blue){
        return (0xFF << 24) | (red << 16) | (green << 8) | blue;
    }

    public static int rgbToInt( int alpha, int red, int green, int blue){
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public static int getAlpha(int color){
        return (color>>24)&0xFF;
    }

    public static int getRed(int color){
        return (color>>16)&0xFF;
    }

    public static int getGreen(int color){
        return (color>>8)&0xFF;
    }

    public static int getBlue(int color){
        return color &0xFF;
    }

    public static int applyShade(int color, int shade){
        if(shade<0)
        {
            return (getAlpha(color) << 24) | (Math.max(getRed(color) + shade, 0) << 16) | (Math.max(getGreen(color) + shade, 0) << 8) | Math.max(getBlue(color) + shade, 0);
        }else{
            return (getAlpha(color) << 24) | (Math.min(getRed(color) + shade, 255) << 16) | (Math.min(getGreen(color) + shade, 255) << 8) | Math.min(getBlue(color) + shade, 255);
        }
    }

    public static int applyShade(int color, float shade){
        if(shade<1)
        {
            return (getAlpha(color) << 24) | ((int) Math.max(getRed(color) * shade, 0) << 16) | ((int) Math.max(getGreen(color) * shade, 0) << 8) | (int) Math.max(getBlue(color) * shade, 0);
        }else{
            return (getAlpha(color) << 24) | ((int) Math.min(getRed(color) * shade, 255) << 16) | ((int) Math.min(getGreen(color) * shade, 255) << 8) |(int)  Math.min(getBlue(color) * shade, 255);
        }
    }

    /**
     * Edit the given color as a HSV (Hue Saturation Value) color.
     */
    public static int applySaturationAndBrightnessMultipliers(int color, float saturationMultiplier, float brightnessMultiplier)
    {
        float[] hsv = Color.RGBtoHSB( getRed(color), getGreen(color), getBlue(color), null);
        return Color.getHSBColor(
                hsv[0], // hue
                LodUtil.clamp(0.0f, hsv[1] * saturationMultiplier, 1.0f),
                LodUtil.clamp(0.0f, hsv[2] * brightnessMultiplier, 1.0f)).getRGB();
    }
}
