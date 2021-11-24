/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
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

package com.seibel.lod.core.util;

import java.awt.Color;

import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;

/**
 * 
 * @author Cola
 * @author Leonardo Amato
 * @version 11-13-2021
 */
public class ColorUtil
{
	private static final IMinecraftWrapper MC = SingletonHandler.get(IMinecraftWrapper.class);
	
	
	public static int rgbToInt(int red, int green, int blue)
	{
		return (0xFF << 24) | (red << 16) | (green << 8) | blue;
	}
	
	public static int rgbToInt(int alpha, int red, int green, int blue)
	{
		return (alpha << 24) | (red << 16) | (green << 8) | blue;
	}
	
	/** Returns a value between 0 and 255 */
	public static int getAlpha(int color)
	{
		return (color >> 24) & 0xFF;
	}
	
	/** Returns a value between 0 and 255 */
	public static int getRed(int color)
	{
		return (color >> 16) & 0xFF;
	}
	
	/** Returns a value between 0 and 255 */
	public static int getGreen(int color)
	{
		return (color >> 8) & 0xFF;
	}
	
	/** Returns a value between 0 and 255 */
	public static int getBlue(int color)
	{
		return color & 0xFF;
	}
	
	public static int applyShade(int color, int shade)
	{
		if (shade < 0)
			return (getAlpha(color) << 24) | (Math.max(getRed(color) + shade, 0) << 16) | (Math.max(getGreen(color) + shade, 0) << 8) | Math.max(getBlue(color) + shade, 0);
		else
			return (getAlpha(color) << 24) | (Math.min(getRed(color) + shade, 255) << 16) | (Math.min(getGreen(color) + shade, 255) << 8) | Math.min(getBlue(color) + shade, 255);
	}
	
	public static int applyShade(int color, float shade)
	{
		if (shade < 1)
			return (getAlpha(color) << 24) | ((int) Math.max(getRed(color) * shade, 0) << 16) | ((int) Math.max(getGreen(color) * shade, 0) << 8) | (int) Math.max(getBlue(color) * shade, 0);
		else
			return (getAlpha(color) << 24) | ((int) Math.min(getRed(color) * shade, 255) << 16) | ((int) Math.min(getGreen(color) * shade, 255) << 8) | (int) Math.min(getBlue(color) * shade, 255);
	}
	
	/** This method apply the lightmap to the color to use */
	public static int applyLightValue(int color, int skyLight, int blockLight)
	{
		int lightColor = MC.getColorIntFromLightMap(blockLight, skyLight);
		int red = ColorUtil.getBlue(lightColor);
		int green = ColorUtil.getGreen(lightColor);
		int blue = ColorUtil.getRed(lightColor);
		
		return ColorUtil.multiplyRGBcolors(color, ColorUtil.rgbToInt(red, green, blue));
	}
	
	/** Edit the given color as an HSV (Hue Saturation Value) color */
	public static int applySaturationAndBrightnessMultipliers(int color, float saturationMultiplier, float brightnessMultiplier)
	{
		float[] hsv = Color.RGBtoHSB(getRed(color), getGreen(color), getBlue(color), null);
		return Color.getHSBColor(
				hsv[0], // hue
				LodUtil.clamp(0.0f, hsv[1] * saturationMultiplier, 1.0f),
				LodUtil.clamp(0.0f, hsv[2] * brightnessMultiplier, 1.0f)).getRGB();
	}
	
	/** Multiply 2 RGB colors */
	public static int multiplyRGBcolors(int color1, int color2)
	{
		return ((getAlpha(color1) * getAlpha(color2) / 255) << 24) | ((getRed(color1) * getRed(color2) / 255) << 16) | ((getGreen(color1) * getGreen(color2) / 255) << 8) | (getBlue(color1) * getBlue(color2) / 255);
	}
	
	@SuppressWarnings("unused")
	public static String toString(int color)
	{
		return Integer.toHexString(getAlpha(color)) + " " +
				Integer.toHexString(getRed(color)) + " " +
				Integer.toHexString(getGreen(color)) + " " +
				Integer.toHexString(getBlue(color));
	}
}
