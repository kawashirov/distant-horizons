package com.backsun.lod.builders.lodTemplates;

import java.awt.Color;

import com.backsun.lod.enums.ColorDirection;
import com.backsun.lod.enums.LodCorner;
import com.backsun.lod.objects.LodChunk;
import com.backsun.lod.objects.LodDimension;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Builds each LOD chunk as a singular rectangular prism.
 * 
 * @author James Seibel
 * @version 05-07-2021
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
		// add the new box to the array
		int topPoint = getValidHeightPoint(lod.top);
		int bottomPoint = getValidHeightPoint(lod.bottom);
		
		// don't draw an LOD if it is empty
		if (topPoint == -1 && bottomPoint == -1)
			return;
		
		
		Color c = null;
		
		if (!debugging)
		{
			// add the color to the array
			c = lod.colors[ColorDirection.TOP.value];
		}
		else
		{
			// if debugging draw the squares as a black and white checker board
			if ((lod.x + lod.z) % 2 == 0)
				c = debugWhite;
			else
				c = debugBlack;
		}
		
		AxisAlignedBB bb = new AxisAlignedBB(0, bottomPoint, 0, LodChunk.WIDTH, topPoint, LodChunk.WIDTH).offset(xOffset, yOffset, zOffset);
		
		if (bb.minY != bb.maxY)
		{
			// top (facing up)
			addPosAndColor(buffer, bb.minX, bb.maxY, bb.minZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.minX, bb.maxY, bb.maxZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.maxX, bb.maxY, bb.maxZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.maxX, bb.maxY, bb.minZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			// bottom (facing down)
			addPosAndColor(buffer, bb.maxX, bb.minY, bb.minZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.maxX, bb.minY, bb.maxZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.minX, bb.minY, bb.maxZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.minX, bb.minY, bb.minZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());

			// south (facing -Z) 
			addPosAndColor(buffer, bb.maxX, bb.minY, bb.maxZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.maxX, bb.maxY, bb.maxZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.minX, bb.maxY, bb.maxZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.minX, bb.minY, bb.maxZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			// north (facing +Z)
			addPosAndColor(buffer, bb.minX, bb.minY, bb.minZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.minX, bb.maxY, bb.minZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.maxX, bb.maxY, bb.minZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.maxX, bb.minY, bb.minZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());

			// west (facing -X)
			addPosAndColor(buffer, bb.minX, bb.minY, bb.minZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.minX, bb.minY, bb.maxZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.minX, bb.maxY, bb.maxZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.minX, bb.maxY, bb.minZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			// east (facing +X)
			addPosAndColor(buffer, bb.maxX, bb.maxY, bb.minZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.maxX, bb.maxY, bb.maxZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.maxX, bb.minY, bb.maxZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.maxX, bb.minY, bb.minZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
		}
		else
		{
			// render this LOD as one block thick
			
			// top (facing up)
			addPosAndColor(buffer, bb.minX, bb.minY+1, bb.minZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.minX, bb.minY+1, bb.maxZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.maxX, bb.minY+1, bb.maxZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.maxX, bb.minY+1, bb.minZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			// bottom (facing down)
			addPosAndColor(buffer, bb.maxX, bb.minY, bb.minZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.maxX, bb.minY, bb.maxZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.minX, bb.minY, bb.maxZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.minX, bb.minY, bb.minZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());

			// south (facing -Z) 
			addPosAndColor(buffer, bb.maxX, bb.minY, bb.maxZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.maxX, bb.minY+1, bb.maxZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.minX, bb.minY+1, bb.maxZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.minX, bb.minY, bb.maxZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			// north (facing +Z)
			addPosAndColor(buffer, bb.minX, bb.minY, bb.minZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.minX, bb.minY+1, bb.minZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.maxX, bb.minY+1, bb.minZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.maxX, bb.minY, bb.minZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());

			// west (facing -X)
			addPosAndColor(buffer, bb.minX, bb.minY, bb.minZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.minX, bb.minY, bb.maxZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.minX, bb.minY+1, bb.maxZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.minX, bb.minY+1, bb.minZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			// east (facing +X)
			addPosAndColor(buffer, bb.maxX, bb.minY+1, bb.minZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.maxX, bb.minY+1, bb.maxZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.maxX, bb.minY, bb.maxZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
			addPosAndColor(buffer, bb.maxX, bb.minY, bb.minZ, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
		}
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
}
