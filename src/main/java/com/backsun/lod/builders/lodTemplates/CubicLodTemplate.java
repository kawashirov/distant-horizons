package com.backsun.lod.builders.lodTemplates;

import java.awt.Color;

import com.backsun.lod.enums.ColorDirection;
import com.backsun.lod.enums.LodCorner;
import com.backsun.lod.objects.LodChunk;
import com.backsun.lod.objects.LodDimension;
import com.backsun.lod.util.LodConfig;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Builds each LOD chunk as a singular rectangular prism.
 * 
 * @author James Seibel
 * @version 05-19-2021
 */
public class CubicLodTemplate extends AbstractLodTemplate
{
	
	public CubicLodTemplate()
	{
		
	}
	
	
	@Override
	public void addLodToBuffer(BufferBuilder buffer,
			LodDimension lodDim, LodChunk lod, 
			double xOffset, double yOffset, double zOffset, 
			boolean debugging)
	{
		AxisAlignedBB bbox;
		int topPoint = getValidHeightPoint(lod.top);
		int bottomPoint = getValidHeightPoint(lod.bottom);
		
		// don't add an LOD if it is empty
		if (topPoint == -1 && bottomPoint == -1)
			return;
		
		if (bottomPoint == topPoint)
		{
			// if the top and bottom points are at the same height
			// render this LOD as 1 block thick
			topPoint++;
		}
		
		
		Color[] colors = generateLodColors(lod, debugging);
		
		
		// Add this LOD to the BufferBuilder
		// using the quality setting set by the config
		switch(LodConfig.CLIENT.lodGeometryQuality.get())
		{
		case SINGLE:
			bbox = new AxisAlignedBB(0, bottomPoint, 0, LodChunk.WIDTH, topPoint, LodChunk.WIDTH).offset(xOffset, yOffset, zOffset);
					
			addBoundingBoxToBuffer(buffer, bbox, colors);
			break;
			
		case  SINGLE_CLOSE_QUAD_FAR:
			// TODO
			break;
			
		case QUAD:
			
			int halfWidth = LodChunk.WIDTH / 2;
			
			addQuarterBoundingBoxToBuffer(buffer, lod, colors, LodCorner.NE, xOffset, yOffset, zOffset);
			addQuarterBoundingBoxToBuffer(buffer, lod, colors, LodCorner.NE, xOffset + halfWidth, yOffset, zOffset + halfWidth);
			addQuarterBoundingBoxToBuffer(buffer, lod, colors, LodCorner.NE, xOffset + halfWidth, yOffset, zOffset);
			addQuarterBoundingBoxToBuffer(buffer, lod, colors, LodCorner.SW, xOffset, yOffset, zOffset + halfWidth);
			
			break;
			
		}
	}
	
	private void addQuarterBoundingBoxToBuffer(BufferBuilder buffer, LodChunk lod, Color[] c, 
			LodCorner corner, double xOffset, double yOffset, double zOffset)
	{
		int topPoint = lod.top[corner.value];
		int bottomPoint = lod.bottom[corner.value];
		int halfWidth = LodChunk.WIDTH / 2;
		
		if (topPoint != -1 && bottomPoint != -1)
		{
			AxisAlignedBB bb = new AxisAlignedBB(0, bottomPoint, 0, halfWidth, topPoint, halfWidth)
					.offset(xOffset, 
							yOffset, 
							zOffset);
			addBoundingBoxToBuffer(buffer, bb, c);
		}
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
	
	
	
	
	/**
	 * @Returns -1 if there are no valid points
	 */
	private int getValidHeightPoint(short[] heightPoints)
	{
		if (heightPoints[LodCorner.NE.value] != -1)
			return heightPoints[LodCorner.NE.value];
		if (heightPoints[LodCorner.NW.value] != -1)
			return heightPoints[LodCorner.NW.value];
		if (heightPoints[LodCorner.SE.value] != -1)
			return heightPoints[LodCorner.NE.value];
		return heightPoints[LodCorner.NE.value];
	}
	
	
	/**
	 * Determine the color for each side of this LOD.
	 */
	private Color[] generateLodColors(LodChunk lod, boolean debugging)
	{
		Color[] colors = new Color[ColorDirection.values().length];
		
		if (debugging)
		{
			// if debugging draw the squares as a black and white checker board
			if ((lod.x + lod.z) % 2 == 0)
				for(ColorDirection dir : ColorDirection.values())
					// have each direction be the same
					// color if debugging
					colors[dir.value] = debugWhite;
			else
				for(ColorDirection dir : ColorDirection.values())
					colors[dir.value] = debugBlack;
		}
		else
		{
			// if NOT debugging, look to the config to determine
			// how this LOD should be colored
			switch (LodConfig.CLIENT.lodColorStyle.get())
			{
			case TOP:
				// only add the top's color to the array
				for(ColorDirection dir : ColorDirection.values())
					colors[dir.value] = lod.colors[ColorDirection.TOP.value];
				break;
				
			case INDIVIDUAL_SIDES:
				// add each direction's color to the array
				for(ColorDirection dir : ColorDirection.values())
					colors[dir.value] = lod.colors[dir.value];
				break;
			}
		}
		
		return colors;
	}
}
