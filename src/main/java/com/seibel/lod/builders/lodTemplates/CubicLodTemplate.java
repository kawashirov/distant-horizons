package com.seibel.lod.builders.lodTemplates;

import java.awt.Color;

import com.seibel.lod.enums.ColorDirection;
import com.seibel.lod.enums.LodDetail;
import com.seibel.lod.objects.LodChunk;
import com.seibel.lod.objects.LodDimension;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Builds LODs as rectangular prisms.
 * 
 * @author James Seibel
 * @version 06-12-2021
 */
public class CubicLodTemplate extends AbstractLodTemplate
{
	
	public CubicLodTemplate()
	{
		
	}
	
	
	@Override
	public void addLodToBuffer(BufferBuilder buffer,
			LodDimension lodDim, LodChunk centerLod, 
			double xOffset, double yOffset, double zOffset, 
			boolean debugging)
	{
		AxisAlignedBB bbox;
		
		// Add this LOD to the BufferBuilder
		// using the quality setting set by the config
		LodDetail detail = LodChunk.DETAIL; //LodConfig.CLIENT.lodDetail.get();
		
		int halfWidth = detail.width / 2;
		
		for(int i = 0; i < detail.lengthCount * detail.lengthCount; i++)
		{
			int startX = detail.startX[i];
			int startZ = detail.startZ[i];
			int endX = detail.endX[i];
			int endZ = detail.endZ[i];
			
			// returns null if the lod is empty at the given location
			bbox = generateBoundingBox(
					centerLod.getAverageHeightOverArea(startX, startZ, endX, endZ), 
					centerLod.getAverageDepthOverArea(startX, startZ, endX, endZ), 
					detail.width, 
					xOffset - (halfWidth / 2) + detail.startX[i],
					yOffset, 
					zOffset - (halfWidth / 2) + detail.startZ[i]);
			
			if (bbox != null)
			{
				addBoundingBoxToBuffer(buffer, bbox, centerLod.getAverageColorOverArea(startX, startZ, endX, endZ, debugging));
			}
		}
	}
	
	/*
	private int[][] addColorToColorAverages(int[][] colorAverages, Color[] colorToAdd) 
	{
		for(ColorDirection dir : ColorDirection.values())
		{
			// convert the colorToAdd to an int array
			float[] colorCompoments = new float[4];
			colorCompoments = colorToAdd[dir.value].getColorComponents(colorCompoments);
			
			// add each color component to the array
			for(int rgbIndex = 0; rgbIndex < 3; rgbIndex++)
			{
				// * 255 + 0.5 taken from the Color java class
				colorAverages[dir.value][rgbIndex] += (int) (colorCompoments[rgbIndex] * 255 + 0.5);
			}
		}
		
		return colorAverages;
	}
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
		
		return new AxisAlignedBB(0, depth, 0, width, height, width).offset(xOffset, yOffset, zOffset);
	}
	
	
	
	private void addBoundingBoxToBuffer(BufferBuilder buffer, AxisAlignedBB bb, Color[] c)
	{
		// top (facing up)
		addPosAndColor(buffer, bb.minX, bb.maxY, bb.minZ, c[ColorDirection.TOP.value].getRed(), c[ColorDirection.TOP.value].getGreen(), c[ColorDirection.TOP.value].getBlue(), c[ColorDirection.TOP.value].getAlpha());
		addPosAndColor(buffer, bb.minX, bb.maxY, bb.maxZ, c[ColorDirection.TOP.value].getRed(), c[ColorDirection.TOP.value].getGreen(), c[ColorDirection.TOP.value].getBlue(), c[ColorDirection.TOP.value].getAlpha());
		addPosAndColor(buffer, bb.maxX, bb.maxY, bb.maxZ, c[ColorDirection.TOP.value].getRed(), c[ColorDirection.TOP.value].getGreen(), c[ColorDirection.TOP.value].getBlue(), c[ColorDirection.TOP.value].getAlpha());
		addPosAndColor(buffer, bb.maxX, bb.maxY, bb.minZ, c[ColorDirection.TOP.value].getRed(), c[ColorDirection.TOP.value].getGreen(), c[ColorDirection.TOP.value].getBlue(), c[ColorDirection.TOP.value].getAlpha());
		// bottom (facing down)
		addPosAndColor(buffer, bb.maxX, bb.minY, bb.minZ, c[ColorDirection.BOTTOM.value].getRed(), c[ColorDirection.BOTTOM.value].getGreen(), c[ColorDirection.BOTTOM.value].getBlue(), c[ColorDirection.BOTTOM.value].getAlpha());
		addPosAndColor(buffer, bb.maxX, bb.minY, bb.maxZ, c[ColorDirection.BOTTOM.value].getRed(), c[ColorDirection.BOTTOM.value].getGreen(), c[ColorDirection.BOTTOM.value].getBlue(), c[ColorDirection.BOTTOM.value].getAlpha());
		addPosAndColor(buffer, bb.minX, bb.minY, bb.maxZ, c[ColorDirection.BOTTOM.value].getRed(), c[ColorDirection.BOTTOM.value].getGreen(), c[ColorDirection.BOTTOM.value].getBlue(), c[ColorDirection.BOTTOM.value].getAlpha());
		addPosAndColor(buffer, bb.minX, bb.minY, bb.minZ, c[ColorDirection.BOTTOM.value].getRed(), c[ColorDirection.BOTTOM.value].getGreen(), c[ColorDirection.BOTTOM.value].getBlue(), c[ColorDirection.BOTTOM.value].getAlpha());

		// south (facing -Z) 
		addPosAndColor(buffer, bb.maxX, bb.minY, bb.maxZ, c[ColorDirection.SOUTH.value].getRed(), c[ColorDirection.SOUTH.value].getGreen(), c[ColorDirection.SOUTH.value].getBlue(), c[ColorDirection.SOUTH.value].getAlpha());
		addPosAndColor(buffer, bb.maxX, bb.maxY, bb.maxZ, c[ColorDirection.SOUTH.value].getRed(), c[ColorDirection.SOUTH.value].getGreen(), c[ColorDirection.SOUTH.value].getBlue(), c[ColorDirection.SOUTH.value].getAlpha());
		addPosAndColor(buffer, bb.minX, bb.maxY, bb.maxZ, c[ColorDirection.SOUTH.value].getRed(), c[ColorDirection.SOUTH.value].getGreen(), c[ColorDirection.SOUTH.value].getBlue(), c[ColorDirection.SOUTH.value].getAlpha());
		addPosAndColor(buffer, bb.minX, bb.minY, bb.maxZ, c[ColorDirection.SOUTH.value].getRed(), c[ColorDirection.SOUTH.value].getGreen(), c[ColorDirection.SOUTH.value].getBlue(), c[ColorDirection.SOUTH.value].getAlpha());
		// north (facing +Z)
		addPosAndColor(buffer, bb.minX, bb.minY, bb.minZ, c[ColorDirection.NORTH.value].getRed(), c[ColorDirection.NORTH.value].getGreen(), c[ColorDirection.NORTH.value].getBlue(), c[ColorDirection.NORTH.value].getAlpha());
		addPosAndColor(buffer, bb.minX, bb.maxY, bb.minZ, c[ColorDirection.NORTH.value].getRed(), c[ColorDirection.NORTH.value].getGreen(), c[ColorDirection.NORTH.value].getBlue(), c[ColorDirection.NORTH.value].getAlpha());
		addPosAndColor(buffer, bb.maxX, bb.maxY, bb.minZ, c[ColorDirection.NORTH.value].getRed(), c[ColorDirection.NORTH.value].getGreen(), c[ColorDirection.NORTH.value].getBlue(), c[ColorDirection.NORTH.value].getAlpha());
		addPosAndColor(buffer, bb.maxX, bb.minY, bb.minZ, c[ColorDirection.NORTH.value].getRed(), c[ColorDirection.NORTH.value].getGreen(), c[ColorDirection.NORTH.value].getBlue(), c[ColorDirection.NORTH.value].getAlpha());

		// west (facing -X)
		addPosAndColor(buffer, bb.minX, bb.minY, bb.minZ, c[ColorDirection.WEST.value].getRed(), c[ColorDirection.WEST.value].getGreen(), c[ColorDirection.WEST.value].getBlue(), c[ColorDirection.WEST.value].getAlpha());
		addPosAndColor(buffer, bb.minX, bb.minY, bb.maxZ, c[ColorDirection.WEST.value].getRed(), c[ColorDirection.WEST.value].getGreen(), c[ColorDirection.WEST.value].getBlue(), c[ColorDirection.WEST.value].getAlpha());
		addPosAndColor(buffer, bb.minX, bb.maxY, bb.maxZ, c[ColorDirection.WEST.value].getRed(), c[ColorDirection.WEST.value].getGreen(), c[ColorDirection.WEST.value].getBlue(), c[ColorDirection.WEST.value].getAlpha());
		addPosAndColor(buffer, bb.minX, bb.maxY, bb.minZ, c[ColorDirection.WEST.value].getRed(), c[ColorDirection.WEST.value].getGreen(), c[ColorDirection.WEST.value].getBlue(), c[ColorDirection.WEST.value].getAlpha());
		// east (facing +X)
		addPosAndColor(buffer, bb.maxX, bb.maxY, bb.minZ, c[ColorDirection.EAST.value].getRed(), c[ColorDirection.EAST.value].getGreen(), c[ColorDirection.EAST.value].getBlue(), c[ColorDirection.EAST.value].getAlpha());
		addPosAndColor(buffer, bb.maxX, bb.maxY, bb.maxZ, c[ColorDirection.EAST.value].getRed(), c[ColorDirection.EAST.value].getGreen(), c[ColorDirection.EAST.value].getBlue(), c[ColorDirection.EAST.value].getAlpha());
		addPosAndColor(buffer, bb.maxX, bb.minY, bb.maxZ, c[ColorDirection.EAST.value].getRed(), c[ColorDirection.EAST.value].getGreen(), c[ColorDirection.EAST.value].getBlue(), c[ColorDirection.EAST.value].getAlpha());
		addPosAndColor(buffer, bb.maxX, bb.minY, bb.minZ, c[ColorDirection.EAST.value].getRed(), c[ColorDirection.EAST.value].getGreen(), c[ColorDirection.EAST.value].getBlue(), c[ColorDirection.EAST.value].getAlpha());
	}
	
	
	
	
}
