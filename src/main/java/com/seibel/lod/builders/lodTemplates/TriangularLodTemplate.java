package com.seibel.lod.builders.lodTemplates;

import com.seibel.lod.enums.LodDetail;
import com.seibel.lod.objects.LodChunk;
import com.seibel.lod.objects.LodDimension;

import net.minecraft.client.renderer.BufferBuilder;

/**
 * TODO #21 TriangularLodTemplate
 * Builds each LOD chunk as a singular rectangular prism.
 * 
 * @author James Seibel
 * @version 06-16-2021
 */
public class TriangularLodTemplate extends AbstractLodTemplate
{
	@Override
	public void addLodToBuffer(BufferBuilder buffer,
			LodDimension lodDim, LodChunk lod, 
			double xOffset, double yOffset, double zOffset, 
			boolean debugging)
	{
		System.err.println("DynamicLodTemplate not implemented!");
	}

	@Override
	public int getBufferMemoryForSingleLod(LodDetail detail) {
		// TODO Auto-generated method stub
		return 0;
	}
}
