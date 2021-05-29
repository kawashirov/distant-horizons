package com.backsun.lod.builders.lodTemplates;

import java.awt.Color;

import com.backsun.lod.enums.ColorDirection;
import com.backsun.lod.enums.LodDetail;
import com.backsun.lod.objects.LodChunk;
import com.backsun.lod.objects.LodDimension;
import com.backsun.lod.util.LodConfig;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Builds each LOD chunk as a singular rectangular prism.
 * 
 * @author James Seibel
 * @version 05-29-2021
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
		
		
		// Add this LOD to the BufferBuilder
		// using the quality setting set by the config
		switch(LodConfig.CLIENT.lodDetail.get())
		{
		case SINGLE:
			// returns null if the lod is empty at the given location
			bbox = generateBoundingBox(lod, LodChunk.WIDTH, xOffset, yOffset, zOffset);
			
			if (bbox != null)
			{
				addBoundingBoxToBuffer(buffer, bbox, generateLodColors(lod, false));
			}
			
			break;
			
		case QUAD:
			
			// TODO use the adjacent chunks to generate quarter sections
//			width = LodChunk.WIDTH / LodDetail.QUAD.value;
//			
//			for(int i = 0; i < LodDetail.QUAD.value; i++)
//			{
//				for(int j = 0; j < LodDetail.QUAD.value; j++)
//				{
//					int x = i * width;
//					int z = j * width;
//					
//					// returns null if the lod is empty at the given location
//					bbox = generateBoundingBox(lod, x, z, width, xOffset - (width / 2) + x, yOffset, zOffset - (width / 2) + z);
//					
//					if (bbox != null)
//					{
//						Color[] colors = generateLodColors(lod, x, z, debugging);
//						
//						addBoundingBoxToBuffer(buffer, bbox, colors);
//					}
//				}
//			}
			break;
		} // case
	}
	
	
	
	
	private AxisAlignedBB generateBoundingBox(LodChunk lod, int width, double xOffset, double yOffset, double zOffset)
	{
		int topPoint = lod.getHeight();
		int bottomPoint = lod.getDepth();
		
		// don't add an LOD if it is empty
		if (topPoint == -1 && bottomPoint == -1)
			return null;
		
		if (bottomPoint == topPoint)
		{
			// if the top and bottom points are at the same height
			// render this LOD as 1 block thick
			topPoint++;
		}
		
		return new AxisAlignedBB(0, bottomPoint, 0, width, topPoint, width).offset(xOffset, yOffset, zOffset);
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
	 * Determine the color for each side of this LOD.
	 */
	private Color[] generateLodColors(LodChunk lod, boolean debugging)
	{
		Color[] colors = new Color[ColorDirection.values().length];
		
		if (!debugging)
		{
			// if NOT debugging, look to the config to determine
			// how this LOD should be colored
			switch (LodConfig.CLIENT.lodColorStyle.get())
			{
			case TOP:
				// only add the top's color to the array
				for(ColorDirection dir : ColorDirection.values())
					colors[dir.value] = lod.getColor(ColorDirection.TOP);
				break;
				
			case INDIVIDUAL_SIDES:
				// add each direction's color to the array
				for(ColorDirection dir : ColorDirection.values())
					colors[dir.value] = lod.getColor(dir);
				break;
			}
		}
		else
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
		
		return colors;
	}
	
	

}
