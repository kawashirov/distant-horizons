package com.seibel.lod.builders.lodTemplates;

import java.awt.Color;
import java.util.EnumSet;

import com.seibel.lod.enums.ColorDirection;
import com.seibel.lod.enums.RelativeChunkPos;
import com.seibel.lod.objects.LodChunk;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.util.LodConfig;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Builds LODs as rectangular prisms.
 * 
 * @author James Seibel
 * @version 05-31-2021
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
		switch(LodConfig.CLIENT.lodDetail.get())
		{
		// add a single LOD object for this chunk
		case SINGLE:
			
			// returns null if the lod is empty at the given location
			bbox = generateBoundingBox(centerLod.getHeight(), centerLod.getDepth(), LodChunk.WIDTH, xOffset, yOffset, zOffset);
			
			if (bbox != null)
			{
				addBoundingBoxToBuffer(buffer, bbox, generateLodColors(centerLod, false));
			}
			
			break;
		
		// add 4 LOD objects for this chunk
		case DOUBLE:
			/*
			 * This method generates LODs using the LodChunks that
			 * are adjacent to create an average quarter and thus 
			 * smooth the transition between chunks.
			 */
			
			// get the adjacent LodChunks
			LodChunk[] lods = new LodChunk[RelativeChunkPos.values().length];
			for(RelativeChunkPos pos : RelativeChunkPos.values())
				lods[pos.index] = lodDim.getLodFromCoordinates(centerLod.x + pos.x, centerLod.z + pos.z);
			
			
			int halfWidth = LodChunk.WIDTH / 2;
			
			// use the adjacent chunks to generate quarter sections
			for(EnumSet<RelativeChunkPos> set : RelativeChunkPos.CORNERS)
			{
				int x = 0;
				int z = 0;
				
				// Weight the center LodChunk by this amount
				// when taking the average.
				// this should be between 3 and 6; 
				// if set to 1 (no extra weight)
				// then the chunks don't appear to be averaged.
				int centerWeight = 3;
				
				// how many LodChunks adjacent to the center
				// are valid?
				int validPoints = centerWeight;
				
				int avgHeight = centerLod.getHeight() * centerWeight;
				int avgDepth = centerLod.getDepth() * centerWeight;
				
				int[][] colorAverages = new int[ColorDirection.values().length][3];
				Color[] colorToAdd = generateLodColors(centerLod, debugging);
				for(int i = 0; i < centerWeight; i++)
					colorAverages = addColorToColorAverages(colorAverages, colorToAdd);
				
				for(RelativeChunkPos cornerPos : set)
				{
					// set the x and y location based on which
					// corner we are working on
					if (RelativeChunkPos.DIAGONAL.contains(cornerPos))
					{
						x = Math.min(cornerPos.x, 0) * halfWidth;
						z = Math.min(cornerPos.z, 0) * halfWidth;
					}
					
					LodChunk cornerLod = lods[cornerPos.index];
					if (cornerLod != null && !cornerLod.isLodEmpty())
					{
						validPoints++;
						
						avgHeight += cornerLod.getHeight();
						avgDepth += cornerLod.getDepth();
						
						// only generate average colors if we aren't debugging
						// (this is to prevent everything from becoming grey)
						if (!debugging)
							colorToAdd = generateLodColors(cornerLod, debugging);
						else
							colorToAdd = generateLodColors(centerLod, debugging);
						// add to the running color average
						colorAverages = addColorToColorAverages(colorAverages, colorToAdd);
					}
				}
				
				
				// convert the heights into actual averages
				avgHeight /= validPoints;
				avgDepth /= validPoints;
				// calculate the average colors
				Color[] colors = new Color[ColorDirection.values().length];
				for(ColorDirection dir : ColorDirection.values())
				{
					for(int rgbIndex = 0; rgbIndex < 3; rgbIndex++)
						colorAverages[dir.value][rgbIndex] /= validPoints;
					colors[dir.value] = new Color(colorAverages[dir.value][0], colorAverages[dir.value][1], colorAverages[dir.value][2]);
				}
				
				
				// returns null if the lod is empty at the given location
				bbox = generateBoundingBox(avgHeight, avgDepth, halfWidth, xOffset - (halfWidth / 2) + x + 12, yOffset, zOffset - (halfWidth / 2) + z + 12);
				
				if (bbox != null)
				{
					addBoundingBoxToBuffer(buffer, bbox, colors);
				}
			}
			break;
		} // case
	}
	
	
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
