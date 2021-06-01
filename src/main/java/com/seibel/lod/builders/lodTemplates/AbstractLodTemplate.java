package com.seibel.lod.builders.lodTemplates;

import java.awt.Color;

import com.seibel.lod.objects.LodChunk;
import com.seibel.lod.objects.LodDimension;

import net.minecraft.client.renderer.BufferBuilder;

/**
 * This is the abstract class used to create different
 * BufferBuilders.
 * 
 * @author James Seibel
 * @version 05-07-2021
 */
public abstract class AbstractLodTemplate
{
	/** alpha used when drawing chunks in debug mode */
	protected int debugAlpha = 255; // 0 - 255
	protected Color debugBlack = new Color(0, 0, 0, debugAlpha);
	protected Color debugWhite = new Color(255, 255, 255, debugAlpha);
	
	
	public abstract void addLodToBuffer(BufferBuilder buffer, 
			LodDimension lodDim, LodChunk lod, 
			double xOffset, double yOffset, double zOffset, 
			boolean debugging);
	
	/** add the given position and color to the buffer */
	protected void addPosAndColor(BufferBuilder buffer, 
			double x, double y, double z, 
			int red, int green, int blue, int alpha)
	{
		buffer.pos(x, y, z).color(red, green, blue, alpha).endVertex();
	}
	
	
	
	
	
}
