package com.seibel.lod.builders.lodNodeTemplates;

import com.seibel.lod.objects.LodQuadTreeNode;
import com.seibel.lod.objects.LodQuadTreeDimension;
import net.minecraft.client.renderer.BufferBuilder;

/**
 * TODO #21 TriangularLodTemplate
 * Builds each LOD chunk as a singular rectangular prism.
 * 
 * @author James Seibel
 * @version 06-16-2021
 */
public class TriangularLodNodeTemplate extends AbstractLodNodeTemplate
{
	@Override
	public void addLodToBuffer(BufferBuilder buffer,
							   LodQuadTreeDimension lodDim, LodQuadTreeNode lod,
							   double xOffset, double yOffset, double zOffset,
							   boolean debugging)
	{
		System.err.println("DynamicLodTemplate not implemented!");
	}

	@Override
	public int getBufferMemoryForSingleLod() {
		// TODO Auto-generated method stub
		return 0;
	}
}
