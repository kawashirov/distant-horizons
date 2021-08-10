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
package com.seibel.lod.builders.lodNodeTemplates;
import java.awt.Color;

import com.seibel.lod.enums.LodDetail;
import com.seibel.lod.enums.ShadingMode;
import com.seibel.lod.handlers.LodConfig;
import com.seibel.lod.objects.LodQuadTree;
import com.seibel.lod.objects.LodQuadTreeDimension;
import com.seibel.lod.objects.LodQuadTreeNode;

import com.seibel.lod.util.LodUtil;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.AxisAlignedBB;


/**
 * Builds LODs as rectangular prisms.
 *
 * @author James Seibel
 * @version 8-8-2021
 */
public class CubicLodNodeTemplate extends AbstractLodNodeTemplate
{
    public CubicLodNodeTemplate()
    {

    }
    
    @Override
    public void addLodToBuffer(BufferBuilder buffer,
                               LodQuadTreeDimension lodDim, LodQuadTreeNode lod,
                               double xOffset, double yOffset, double zOffset,
                               boolean debugging) {
        AxisAlignedBB bbox;

        // Add this LOD to the BufferBuilder
        int halfWidth = LodQuadTreeNode.CHUNK_WIDTH / 2;
		LodDetail detail = LodConfig.CLIENT.lodDetail.get();
/*
		bbox = generateBoundingBox(
				lod.getLodDataPoint().height,
				lod.getLodDataPoint().depth,
				lod.width,
				xOffset - halfWidth,
				yOffset,
				zOffset - halfWidth);

		if (bbox != null) {
			addBoundingBoxToBuffer(buffer, bbox, lod.getLodDataPoint().color);
		}

 */
		LodQuadTree chunkTree = lodDim.getLevelFromPos(lod.posX, lod.posZ, lod.detailLevel);
		for(int i = 0; i < detail.dataPointLengthCount * detail.dataPointLengthCount; i++) {
			int startX = detail.startX[i];
			int startZ = detail.startZ[i];
			int posX = LodUtil.convertLevelPos((int) xOffset+startX,0, detail.detailLevel);
			int posZ = LodUtil.convertLevelPos((int) zOffset+startZ,0, detail.detailLevel);;
			//LodQuadTreeNode newLod = chunkTree.getNodeAtPos(posX ,posZ ,detail.detailLevel);
			LodQuadTreeNode newLod = lodDim.getLodFromCoordinates(posX ,posZ ,detail.detailLevel);
			if(newLod != null) {
				bbox = generateBoundingBox(
						newLod.getLodDataPoint().height,
						newLod.getLodDataPoint().depth,
						newLod.width,
						xOffset + startX,
						yOffset,
						zOffset + startZ);

				if (bbox != null) {
					addBoundingBoxToBuffer(buffer, bbox, newLod.getLodDataPoint().color);
				}
			}
		}

    }


	
	
	
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



	private void addBoundingBoxToBuffer(BufferBuilder buffer, AxisAlignedBB bb, Color c)
	{
		Color topColor = c;
		Color northSouthColor = c;
		Color eastWestColor = c;
		Color bottomColor = c;

		// darken the bottom and side colors if requested
		if (LodConfig.CLIENT.shadingMode.get() == ShadingMode.DARKEN_SIDES)
		{
			// the side colors are different because
			// when using fast lighting in Minecraft the north/south
			// and east/west sides are different in a similar way
			int northSouthDarkenAmount = 25;
			int eastWestDarkenAmount = 50;
			int bottomDarkenAmount = 75;
			
			northSouthColor = new Color(Math.max(0, c.getRed() - northSouthDarkenAmount), Math.max(0, c.getGreen() - northSouthDarkenAmount), Math.max(0, c.getBlue() - northSouthDarkenAmount), c.getAlpha());
			eastWestColor = new Color(Math.max(0, c.getRed() - eastWestDarkenAmount), Math.max(0, c.getGreen() - eastWestDarkenAmount), Math.max(0, c.getBlue() - eastWestDarkenAmount), c.getAlpha());
			bottomColor = new Color(Math.max(0, c.getRed() - bottomDarkenAmount), Math.max(0, c.getGreen() - bottomDarkenAmount), Math.max(0, c.getBlue() - bottomDarkenAmount), c.getAlpha());
		}


		// apply the user specified saturation and brightness
		float saturationMultiplier = LodConfig.CLIENT.saturationMultiplier.get().floatValue();
		float brightnessMultiplier = LodConfig.CLIENT.brightnessMultiplier.get().floatValue();
		
		topColor = applySaturationAndBrightnessMultipliers(topColor, saturationMultiplier, brightnessMultiplier);
		northSouthColor = applySaturationAndBrightnessMultipliers(northSouthColor, saturationMultiplier, brightnessMultiplier);
		bottomColor = applySaturationAndBrightnessMultipliers(bottomColor, saturationMultiplier, brightnessMultiplier);
		
		
		
        // top (facing up)
		addPosAndColor(buffer, bb.minX, bb.maxY, bb.minZ, topColor.getRed(), topColor.getGreen(), topColor.getBlue(), topColor.getAlpha());
		addPosAndColor(buffer, bb.minX, bb.maxY, bb.maxZ, topColor.getRed(), topColor.getGreen(), topColor.getBlue(), topColor.getAlpha());
		addPosAndColor(buffer, bb.maxX, bb.maxY, bb.maxZ, topColor.getRed(), topColor.getGreen(), topColor.getBlue(), topColor.getAlpha());
		addPosAndColor(buffer, bb.maxX, bb.maxY, bb.minZ, topColor.getRed(), topColor.getGreen(), topColor.getBlue(), topColor.getAlpha());
        // bottom (facing down)
		addPosAndColor(buffer, bb.maxX, bb.minY, bb.minZ, bottomColor.getRed(), bottomColor.getGreen(), bottomColor.getBlue(), bottomColor.getAlpha());
		addPosAndColor(buffer, bb.maxX, bb.minY, bb.maxZ, bottomColor.getRed(), bottomColor.getGreen(), bottomColor.getBlue(), bottomColor.getAlpha());
		addPosAndColor(buffer, bb.minX, bb.minY, bb.maxZ, bottomColor.getRed(), bottomColor.getGreen(), bottomColor.getBlue(), bottomColor.getAlpha());
		addPosAndColor(buffer, bb.minX, bb.minY, bb.minZ, bottomColor.getRed(), bottomColor.getGreen(), bottomColor.getBlue(), bottomColor.getAlpha());

        // south (facing -Z)
		addPosAndColor(buffer, bb.maxX, bb.minY, bb.maxZ, northSouthColor.getRed(), northSouthColor.getGreen(), northSouthColor.getBlue(), northSouthColor.getAlpha());
		addPosAndColor(buffer, bb.maxX, bb.maxY, bb.maxZ, northSouthColor.getRed(), northSouthColor.getGreen(), northSouthColor.getBlue(), northSouthColor.getAlpha());
		addPosAndColor(buffer, bb.minX, bb.maxY, bb.maxZ, northSouthColor.getRed(), northSouthColor.getGreen(), northSouthColor.getBlue(), northSouthColor.getAlpha());
		addPosAndColor(buffer, bb.minX, bb.minY, bb.maxZ, northSouthColor.getRed(), northSouthColor.getGreen(), northSouthColor.getBlue(), northSouthColor.getAlpha());
        // north (facing +Z)
		addPosAndColor(buffer, bb.minX, bb.minY, bb.minZ, northSouthColor.getRed(), northSouthColor.getGreen(), northSouthColor.getBlue(), northSouthColor.getAlpha());
		addPosAndColor(buffer, bb.minX, bb.maxY, bb.minZ, northSouthColor.getRed(), northSouthColor.getGreen(), northSouthColor.getBlue(), northSouthColor.getAlpha());
		addPosAndColor(buffer, bb.maxX, bb.maxY, bb.minZ, northSouthColor.getRed(), northSouthColor.getGreen(), northSouthColor.getBlue(), northSouthColor.getAlpha());
		addPosAndColor(buffer, bb.maxX, bb.minY, bb.minZ, northSouthColor.getRed(), northSouthColor.getGreen(), northSouthColor.getBlue(), northSouthColor.getAlpha());

        // west (facing -X)
		addPosAndColor(buffer, bb.minX, bb.minY, bb.minZ, eastWestColor.getRed(), eastWestColor.getGreen(), eastWestColor.getBlue(), eastWestColor.getAlpha());
		addPosAndColor(buffer, bb.minX, bb.minY, bb.maxZ, eastWestColor.getRed(), eastWestColor.getGreen(), eastWestColor.getBlue(), eastWestColor.getAlpha());
		addPosAndColor(buffer, bb.minX, bb.maxY, bb.maxZ, eastWestColor.getRed(), eastWestColor.getGreen(), eastWestColor.getBlue(), eastWestColor.getAlpha());
		addPosAndColor(buffer, bb.minX, bb.maxY, bb.minZ, eastWestColor.getRed(), eastWestColor.getGreen(), eastWestColor.getBlue(), eastWestColor.getAlpha());
        // east (facing +X)
		addPosAndColor(buffer, bb.maxX, bb.maxY, bb.minZ, eastWestColor.getRed(), eastWestColor.getGreen(), eastWestColor.getBlue(), eastWestColor.getAlpha());
		addPosAndColor(buffer, bb.maxX, bb.maxY, bb.maxZ, eastWestColor.getRed(), eastWestColor.getGreen(), eastWestColor.getBlue(), eastWestColor.getAlpha());
		addPosAndColor(buffer, bb.maxX, bb.minY, bb.maxZ, eastWestColor.getRed(), eastWestColor.getGreen(), eastWestColor.getBlue(), eastWestColor.getAlpha());
		addPosAndColor(buffer, bb.maxX, bb.minY, bb.minZ, eastWestColor.getRed(), eastWestColor.getGreen(), eastWestColor.getBlue(), eastWestColor.getAlpha());
    }
	
	
	
    @Override
    public int getBufferMemoryForSingleNode(int detailLevel)
    {
        // (sidesOnACube * pointsInASquare * (positionPoints + colorPoints))) * howManyPointsPerLodChunk
        return (6 * 4 * (3 + 4));
    }
    
}
