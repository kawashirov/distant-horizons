package com.seibel.lod.builders.lodNodeTemplates;

import com.seibel.lod.objects.LodQuadTreeNode;
import com.seibel.lod.objects.LodQuadTreeDimension;
import net.minecraft.client.renderer.BufferBuilder;

/**
 * This is the abstract class used to create different
 * BufferBuilders.
 * 
 * @author James Seibel
 * @version 06-16-2021
 */
public abstract class AbstractLodNodeTemplate
{
	public abstract void addLodToBuffer(BufferBuilder buffer,
										LodQuadTreeDimension lodDim, LodQuadTreeNode lod,
										double xOffset, double yOffset, double zOffset,
										boolean debugging);
	
	/** add the given position and color to the buffer */
	protected void addPosAndColor(BufferBuilder buffer, 
			double x, double y, double z, 
			int red, int green, int blue, int alpha)
	{
		buffer.vertex(x, y, z).color(red, green, blue, alpha).endVertex();
	}
	
	/** Returns in bytes how much buffer memory is required
	 * for one LOD object */
	public abstract int getBufferMemoryForSingleLod();
	
	
	
}
