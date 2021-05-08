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
		
		
		Color[] c = new Color[ColorDirection.values().length];
		
		// generate the color for this LOD
		if (debugging)
		{
			// if debugging draw the squares as a black and white checker board
			if ((lod.x + lod.z) % 2 == 0)
				for(ColorDirection dir : ColorDirection.values())
					// have each direction be the same
					// color if debugging
					c[dir.value] = debugWhite;
			else
				for(ColorDirection dir : ColorDirection.values())
					c[dir.value] = debugBlack;
		}
		else
		{
			switch (LodConfig.CLIENT.lodColorStyle.get())
			{
			case TOP:
				// only add the top's color to the array
				for(ColorDirection dir : ColorDirection.values())
					c[dir.value] = lod.colors[ColorDirection.TOP.value];
				break;
				
			case INDIVIDUAL_SIDES:
				// add each direction's color to the array
				for(ColorDirection dir : ColorDirection.values())
					c[dir.value] = lod.colors[dir.value];
				break;
			}
		}
		
		
		// TODO add different geometry quality levels
		AxisAlignedBB bb = new AxisAlignedBB(0, bottomPoint, 0, LodChunk.WIDTH, topPoint, LodChunk.WIDTH).offset(xOffset, yOffset, zOffset);
		
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
		addPosAndColor(buffer, bb.maxX, bb.minY, bb.maxZ, c[ColorDirection.S.value].getRed(), c[ColorDirection.S.value].getGreen(), c[ColorDirection.S.value].getBlue(), c[ColorDirection.S.value].getAlpha());
		addPosAndColor(buffer, bb.maxX, bb.maxY, bb.maxZ, c[ColorDirection.S.value].getRed(), c[ColorDirection.S.value].getGreen(), c[ColorDirection.S.value].getBlue(), c[ColorDirection.S.value].getAlpha());
		addPosAndColor(buffer, bb.minX, bb.maxY, bb.maxZ, c[ColorDirection.S.value].getRed(), c[ColorDirection.S.value].getGreen(), c[ColorDirection.S.value].getBlue(), c[ColorDirection.S.value].getAlpha());
		addPosAndColor(buffer, bb.minX, bb.minY, bb.maxZ, c[ColorDirection.S.value].getRed(), c[ColorDirection.S.value].getGreen(), c[ColorDirection.S.value].getBlue(), c[ColorDirection.S.value].getAlpha());
		// north (facing +Z)
		addPosAndColor(buffer, bb.minX, bb.minY, bb.minZ, c[ColorDirection.N.value].getRed(), c[ColorDirection.N.value].getGreen(), c[ColorDirection.N.value].getBlue(), c[ColorDirection.N.value].getAlpha());
		addPosAndColor(buffer, bb.minX, bb.maxY, bb.minZ, c[ColorDirection.N.value].getRed(), c[ColorDirection.N.value].getGreen(), c[ColorDirection.N.value].getBlue(), c[ColorDirection.N.value].getAlpha());
		addPosAndColor(buffer, bb.maxX, bb.maxY, bb.minZ, c[ColorDirection.N.value].getRed(), c[ColorDirection.N.value].getGreen(), c[ColorDirection.N.value].getBlue(), c[ColorDirection.N.value].getAlpha());
		addPosAndColor(buffer, bb.maxX, bb.minY, bb.minZ, c[ColorDirection.N.value].getRed(), c[ColorDirection.N.value].getGreen(), c[ColorDirection.N.value].getBlue(), c[ColorDirection.N.value].getAlpha());

		// west (facing -X)
		addPosAndColor(buffer, bb.minX, bb.minY, bb.minZ, c[ColorDirection.W.value].getRed(), c[ColorDirection.W.value].getGreen(), c[ColorDirection.W.value].getBlue(), c[ColorDirection.W.value].getAlpha());
		addPosAndColor(buffer, bb.minX, bb.minY, bb.maxZ, c[ColorDirection.W.value].getRed(), c[ColorDirection.W.value].getGreen(), c[ColorDirection.W.value].getBlue(), c[ColorDirection.W.value].getAlpha());
		addPosAndColor(buffer, bb.minX, bb.maxY, bb.maxZ, c[ColorDirection.W.value].getRed(), c[ColorDirection.W.value].getGreen(), c[ColorDirection.W.value].getBlue(), c[ColorDirection.W.value].getAlpha());
		addPosAndColor(buffer, bb.minX, bb.maxY, bb.minZ, c[ColorDirection.W.value].getRed(), c[ColorDirection.W.value].getGreen(), c[ColorDirection.W.value].getBlue(), c[ColorDirection.W.value].getAlpha());
		// east (facing +X)
		addPosAndColor(buffer, bb.maxX, bb.maxY, bb.minZ, c[ColorDirection.E.value].getRed(), c[ColorDirection.E.value].getGreen(), c[ColorDirection.E.value].getBlue(), c[ColorDirection.E.value].getAlpha());
		addPosAndColor(buffer, bb.maxX, bb.maxY, bb.maxZ, c[ColorDirection.E.value].getRed(), c[ColorDirection.E.value].getGreen(), c[ColorDirection.E.value].getBlue(), c[ColorDirection.E.value].getAlpha());
		addPosAndColor(buffer, bb.maxX, bb.minY, bb.maxZ, c[ColorDirection.E.value].getRed(), c[ColorDirection.E.value].getGreen(), c[ColorDirection.E.value].getBlue(), c[ColorDirection.E.value].getAlpha());
		addPosAndColor(buffer, bb.maxX, bb.minY, bb.minZ, c[ColorDirection.E.value].getRed(), c[ColorDirection.E.value].getGreen(), c[ColorDirection.E.value].getBlue(), c[ColorDirection.E.value].getAlpha());
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
