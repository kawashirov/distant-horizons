/*
 *    This file is part of the LOD Mod, licensed under the GNU GPL v3 License.
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
package com.seibel.lod.builders.lodTemplates;

import java.awt.Color;

import com.seibel.lod.enums.ShadingMode;
import com.seibel.lod.handlers.LodConfig;
import com.seibel.lod.objects.DataPoint;
import com.seibel.lod.objects.LevelPos.LevelPos;
import com.seibel.lod.util.ColorUtil;
import com.seibel.lod.util.LodUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/**
 * Builds LODs as rectangular prisms.
 *
 * @author James Seibel
 * @version 8-10-2021
 */
public class CubicLodTemplate extends AbstractLodTemplate
{
    private final int CULL_OFFSET = 16;

    public CubicLodTemplate()
    {

    }

    @Override
    public void addLodToBuffer(BufferBuilder buffer, BlockPos playerBlockPos, short[] data, short[][][] adjData,
                               LevelPos levelPos, boolean debugging)
    {
        AxisAlignedBB bbox;

        int width = 1 << levelPos.detailLevel;

        // add each LOD for the detail level
        bbox = generateBoundingBox(
                DataPoint.getHeight(data),
                DataPoint.getDepth(data),
                width,
                levelPos.posX * width,
                0,
                levelPos.posZ * width);

        int color = DataPoint.getColor(data);
        if (LodConfig.CLIENT.debugMode.get())
        {
            color = LodUtil.DEBUG_DETAIL_LEVEL_COLORS[levelPos.detailLevel].getRGB();
        }

        if (bbox != null)
        {
            addBoundingBoxToBuffer(buffer, bbox, color, playerBlockPos, adjData);
        }

    }

    /*
     * @Override public void addLodToBuffer(BufferBuilder buffer,
     * LodQuadTreeDimension lodDim, LodQuadTreeNode lod, double xOffset, double
     * yOffset, double zOffset, boolean debugging) { AxisAlignedBB bbox;
     *
     * bbox = generateBoundingBox( lod.getLodDataPoint().height,
     * lod.getLodDataPoint().depth, lod.width, xOffset, yOffset, zOffset);
     *
     * Color color = lod.getLodDataPoint().color;
     *
     * if (bbox != null) { addBoundingBoxToBuffer(buffer, bbox, color); }
     *
     * }
     */

    private AxisAlignedBB generateBoundingBox(int height, int depth, int width, double xOffset, double yOffset, double zOffset)
    {
        // don't add an LOD if it is empty
        if (height == -1 && depth == -1)
            return null;

        if (depth == height)
        {
            // if the top and bottom points are at the same height
            // render this LOD as 1 block thick
            height++;
        }

        return new AxisAlignedBB(0, depth, 0, width, height, width).move(xOffset, yOffset, zOffset);
    }

    private void addBoundingBoxToBuffer(BufferBuilder buffer, AxisAlignedBB bb, int c, BlockPos playerBlockPos, short[][][] adjData)
    {
        int topColor = c;
        int bottomColor = c;
        int northColor = c;
        int southColor = c;
        int westColor = c;
        int eastColor = c;

        // darken the bottom and side colors if requested
        if (LodConfig.CLIENT.shadingMode.get() == ShadingMode.DARKEN_SIDES)
        {
            // the side colors are different because
            // when using fast lighting in Minecraft the north/south
            // and east/west sides are different in a similar way
/*
            int northSouthDarkenAmount = -25;
            int eastWestDarkenAmount = -50;
            int bottomDarkenAmount = -75;

            northSouthColor = ColorUtil.applyShade(c,northSouthDarkenAmount);
            eastWestColor = ColorUtil.applyShade(c,eastWestDarkenAmount);
            bottomColor = ColorUtil.applyShade(c,bottomDarkenAmount);*/
/*
            float northSouthDarkenAmount = 0.80f;
            float eastWestDarkenAmount = 0.60f;
            float bottomDarkenAmount = 0.40f;*/
/**TODO OPTIMIZE THIS STEP*
            topColor = ColorUtil.applyShade(c, Minecraft.getInstance().level.getShade(Direction.UP,true));
            bottomColor = ColorUtil.applyShade(c, Minecraft.getInstance().level.getShade(Direction.DOWN,true));
            northColor = ColorUtil.applyShade(c, Minecraft.getInstance().level.getShade(Direction.NORTH,true));
            southColor = ColorUtil.applyShade(c, Minecraft.getInstance().level.getShade(Direction.SOUTH,true));
            westColor = ColorUtil.applyShade(c, Minecraft.getInstance().level.getShade(Direction.WEST,true));
            eastColor = ColorUtil.applyShade(c, Minecraft.getInstance().level.getShade(Direction.EAST,true));
        }

        // apply the user specified saturation and brightness
        float saturationMultiplier = LodConfig.CLIENT.saturationMultiplier.get().floatValue();
        float brightnessMultiplier = LodConfig.CLIENT.brightnessMultiplier.get().floatValue();

        if(saturationMultiplier != 1 || brightnessMultiplier != 1)
        {
            topColor = ColorUtil.applySaturationAndBrightnessMultipliers(topColor, saturationMultiplier, brightnessMultiplier);
            bottomColor = ColorUtil.applySaturationAndBrightnessMultipliers(bottomColor, saturationMultiplier, brightnessMultiplier);
            northColor = ColorUtil.applySaturationAndBrightnessMultipliers(northColor, saturationMultiplier, brightnessMultiplier);
            southColor = ColorUtil.applySaturationAndBrightnessMultipliers(southColor, saturationMultiplier, brightnessMultiplier);
            westColor = ColorUtil.applySaturationAndBrightnessMultipliers(westColor, saturationMultiplier, brightnessMultiplier);
            eastColor = ColorUtil.applySaturationAndBrightnessMultipliers(eastColor, saturationMultiplier, brightnessMultiplier);
        }
        int minY;
        int maxY;
        short[] data;

        int red;
        int green;
        int blue;
        int alpha;
        /**TODO make all of this more automatic if possible*/
        if (playerBlockPos.getY() > bb.maxY - CULL_OFFSET)
        {
            red = ColorUtil.getRed(topColor);
            green = ColorUtil.getGreen(topColor);
            blue = ColorUtil.getBlue(topColor);
            alpha = ColorUtil.getAlpha(topColor);
            // top (facing up)
            addPosAndColor(buffer, bb.minX, bb.maxY, bb.minZ, red, green, blue, alpha);
            addPosAndColor(buffer, bb.minX, bb.maxY, bb.maxZ, red, green, blue, alpha);
            addPosAndColor(buffer, bb.maxX, bb.maxY, bb.maxZ, red, green, blue, alpha);
            addPosAndColor(buffer, bb.maxX, bb.maxY, bb.minZ, red, green, blue, alpha);
        }
        if (playerBlockPos.getY() < bb.minY + CULL_OFFSET)
        {
            red = ColorUtil.getRed(bottomColor);
            green = ColorUtil.getGreen(bottomColor);
            blue = ColorUtil.getBlue(bottomColor);
            alpha = ColorUtil.getAlpha(bottomColor);
            // bottom (facing down)
            addPosAndColor(buffer, bb.maxX, bb.minY, bb.minZ, red, green, blue, alpha);
            addPosAndColor(buffer, bb.maxX, bb.minY, bb.maxZ, red, green, blue, alpha);
            addPosAndColor(buffer, bb.minX, bb.minY, bb.maxZ, red, green, blue, alpha);
            addPosAndColor(buffer, bb.minX, bb.minY, bb.minZ, red, green, blue, alpha);
        }

        if (playerBlockPos.getZ() > bb.minZ - CULL_OFFSET)
        {
            red = ColorUtil.getRed(northColor);
            green = ColorUtil.getGreen(northColor);
            blue = ColorUtil.getBlue(northColor);
            alpha = ColorUtil.getAlpha(northColor);
            // south (facing -Z)
            data = adjData[1][1];
            if (data == null)
            {
                addPosAndColor(buffer, bb.maxX, bb.minY, bb.maxZ, red, green, blue, alpha);
                addPosAndColor(buffer, bb.maxX, bb.maxY, bb.maxZ, red, green, blue, alpha);
                addPosAndColor(buffer, bb.minX, bb.maxY, bb.maxZ, red, green, blue, alpha);
                addPosAndColor(buffer, bb.minX, bb.minY, bb.maxZ, red, green, blue, alpha);
            } else
            {
                maxY = DataPoint.getHeight(data);
                if (maxY < bb.maxY)
                {
                    minY = (int) Math.max(maxY, bb.minY);
                    addPosAndColor(buffer, bb.maxX, minY, bb.maxZ, red, green, blue, alpha);
                    addPosAndColor(buffer, bb.maxX, bb.maxY, bb.maxZ, red, green, blue, alpha);
                    addPosAndColor(buffer, bb.minX, bb.maxY, bb.maxZ, red, green, blue, alpha);
                    addPosAndColor(buffer, bb.minX, minY, bb.maxZ, red, green, blue, alpha);
                }
                minY = DataPoint.getDepth(data);
                if (minY > bb.minY)
                {
                    maxY = (int) Math.min(minY, bb.maxX);
                    addPosAndColor(buffer, bb.maxX, bb.minY, bb.maxZ, red, green, blue, alpha);
                    addPosAndColor(buffer, bb.maxX, maxY, bb.maxZ, red, green, blue, alpha);
                    addPosAndColor(buffer, bb.minX, maxY, bb.maxZ, red, green, blue, alpha);
                    addPosAndColor(buffer, bb.minX, bb.minY, bb.maxZ, red, green, blue, alpha);
                }
            }
        }

        if (playerBlockPos.getZ() < bb.maxZ + CULL_OFFSET)
        {
            red = ColorUtil.getRed(southColor);
            green = ColorUtil.getGreen(southColor);
            blue = ColorUtil.getBlue(southColor);
            alpha = ColorUtil.getAlpha(southColor);
            data = adjData[1][0];
            // north (facing +Z)
            if (data == null)
            {
                addPosAndColor(buffer, bb.minX, bb.minY, bb.minZ, red, green, blue, alpha);
                addPosAndColor(buffer, bb.minX, bb.maxY, bb.minZ, red, green, blue, alpha);
                addPosAndColor(buffer, bb.maxX, bb.maxY, bb.minZ, red, green, blue, alpha);
                addPosAndColor(buffer, bb.maxX, bb.minY, bb.minZ, red, green, blue, alpha);
            } else
            {
                maxY = DataPoint.getHeight(data);
                if (maxY < bb.maxY)
                {
                    minY = (int) Math.max(maxY, bb.minY);
                    addPosAndColor(buffer, bb.minX, minY, bb.minZ, red, green, blue, alpha);
                    addPosAndColor(buffer, bb.minX, bb.maxY, bb.minZ, red, green, blue, alpha);
                    addPosAndColor(buffer, bb.maxX, bb.maxY, bb.minZ, red, green, blue, alpha);
                    addPosAndColor(buffer, bb.maxX, minY, bb.minZ, red, green, blue, alpha);
                }
                minY = DataPoint.getDepth(data);
                if (minY > bb.minY)
                {
                    maxY = (int) Math.min(minY, bb.maxX);
                    addPosAndColor(buffer, bb.minX, bb.minY, bb.minZ, red, green, blue, alpha);
                    addPosAndColor(buffer, bb.minX, maxY, bb.minZ, red, green, blue, alpha);
                    addPosAndColor(buffer, bb.maxX, maxY, bb.minZ, red, green, blue, alpha);
                    addPosAndColor(buffer, bb.maxX, bb.minY, bb.minZ, red, green, blue, alpha);
                }
            }
        }

        if (playerBlockPos.getX() < bb.maxX + CULL_OFFSET)
        {
            red = ColorUtil.getRed(westColor);
            green = ColorUtil.getGreen(westColor);
            blue = ColorUtil.getBlue(westColor);
            alpha = ColorUtil.getAlpha(westColor);
            // west (facing -X)
            data = adjData[0][0];
            if (data == null)
            {
                addPosAndColor(buffer, bb.minX, bb.minY, bb.minZ, red, green, blue, alpha);
                addPosAndColor(buffer, bb.minX, bb.minY, bb.maxZ, red, green, blue, alpha);
                addPosAndColor(buffer, bb.minX, bb.maxY, bb.maxZ, red, green, blue, alpha);
                addPosAndColor(buffer, bb.minX, bb.maxY, bb.minZ, red, green, blue, alpha);
            } else
            {
                maxY = DataPoint.getHeight(data);
                if (maxY < bb.maxY)
                {
                    minY = (int) Math.max(maxY, bb.minY);
                    addPosAndColor(buffer, bb.minX, minY, bb.minZ, red, green, blue, alpha);
                    addPosAndColor(buffer, bb.minX, minY, bb.maxZ, red, green, blue, alpha);
                    addPosAndColor(buffer, bb.minX, bb.maxY, bb.maxZ, red, green, blue, alpha);
                    addPosAndColor(buffer, bb.minX, bb.maxY, bb.minZ, red, green, blue, alpha);
                }
                minY = DataPoint.getDepth(data);
                if (minY > bb.minY)
                {
                    maxY = (int) Math.min(minY, bb.maxX);
                    addPosAndColor(buffer, bb.minX, bb.minY, bb.minZ, red, green, blue, alpha);
                    addPosAndColor(buffer, bb.minX, bb.minY, bb.maxZ, red, green, blue, alpha);
                    addPosAndColor(buffer, bb.minX, maxY, bb.maxZ, red, green, blue, alpha);
                    addPosAndColor(buffer, bb.minX, maxY, bb.minZ, red, green, blue, alpha);
                }
            }
        }

        if (playerBlockPos.getX() > bb.minX - CULL_OFFSET)
        {
            red = ColorUtil.getRed(eastColor);
            green = ColorUtil.getGreen(eastColor);
            blue = ColorUtil.getBlue(eastColor);
            alpha = ColorUtil.getAlpha(eastColor);
            // east (facing +X)
            data = adjData[0][1];
            if (data == null)
            {
                addPosAndColor(buffer, bb.maxX, bb.maxY, bb.minZ, red, green, blue, alpha);
                addPosAndColor(buffer, bb.maxX, bb.maxY, bb.maxZ, red, green, blue, alpha);
                addPosAndColor(buffer, bb.maxX, bb.minY, bb.maxZ, red, green, blue, alpha);
                addPosAndColor(buffer, bb.maxX, bb.minY, bb.minZ, red, green, blue, alpha);
            } else
            {
                maxY = DataPoint.getHeight(data);
                if (maxY < bb.maxY)
                {
                    minY = (int) Math.max(maxY, bb.minY);
                    addPosAndColor(buffer, bb.maxX, bb.maxY, bb.minZ, red, green, blue, alpha);
                    addPosAndColor(buffer, bb.maxX, bb.maxY, bb.maxZ, red, green, blue, alpha);
                    addPosAndColor(buffer, bb.maxX, minY, bb.maxZ, red, green, blue, alpha);
                    addPosAndColor(buffer, bb.maxX, minY, bb.minZ, red, green, blue, alpha);
                }
                minY = DataPoint.getDepth(data);
                if (minY > bb.minY)
                {
                    maxY = (int) Math.min(minY, bb.maxX);
                    addPosAndColor(buffer, bb.maxX, maxY, bb.minZ, red, green, blue, alpha);
                    addPosAndColor(buffer, bb.maxX, maxY, bb.maxZ, red, green, blue, alpha);
                    addPosAndColor(buffer, bb.maxX, bb.minY, bb.maxZ, red, green, blue, alpha);
                    addPosAndColor(buffer, bb.maxX, bb.minY, bb.minZ, red, green, blue, alpha);
                }
            }
        }
    }

    @Override
    public int getBufferMemoryForSingleNode(int detailLevel)
    {
        // (sidesOnACube * pointsInASquare * (positionPoints + colorPoints))) *
        // howManyPointsPerLodChunk
        return (6 * 4 * (3 + 4));
    }

}
