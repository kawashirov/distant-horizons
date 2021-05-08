package com.backsun.lod.builders.lodTemplates;

import com.backsun.lod.objects.LodChunk;
import com.backsun.lod.objects.LodDimension;

import net.minecraft.client.renderer.BufferBuilder;

/**
 * TODO
 * Chunks smoothly transition between
 * each other, unless a neighboring chunk
 * is at a significantly different height.
 * 
 * @author James Seibel
 * @version 05-07-2021
 */
public class DynamicLodTemplate extends AbstractLodTemplate
{
	@Override
	public void addLodToBuffer(BufferBuilder buffer,
			LodDimension lodDim, LodChunk lod, 
			double xOffset, double yOffset, double zOffset, 
			boolean debugging)
	{
		System.err.println("DynamicLodTemplate not implemented!");
	}
}
