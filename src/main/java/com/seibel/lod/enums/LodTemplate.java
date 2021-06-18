package com.seibel.lod.enums;

import com.seibel.lod.builders.lodTemplates.AbstractLodTemplate;
import com.seibel.lod.builders.lodTemplates.CubicLodTemplate;
import com.seibel.lod.builders.lodTemplates.DynamicLodTemplate;
import com.seibel.lod.builders.lodTemplates.TriangularLodTemplate;

/**
 * Cubic, Triangular, Dynamic
 * 
 * @author James Seibel
 * @version 06-16-2021
 */
public enum LodTemplate
{
	// used for position

	/** Chunks are rendered as
	 * rectangular prisms. */
	CUBIC(new CubicLodTemplate()),
	
	/** Chunks smoothly transition between
	 * each other. */
	TRIANGULAR(new TriangularLodTemplate()),
	
	/** Chunks smoothly transition between
	 * each other, unless a neighboring chunk
	 * is at a significantly different height. */
	DYNAMIC(new DynamicLodTemplate());
	
	
	public final AbstractLodTemplate template;
	
	private LodTemplate(AbstractLodTemplate newTemplate)
	{
		template = newTemplate;
	}
	
	
	public int getBufferMemoryForSingleLod(LodDetail detail)
	{
		return template.getBufferMemoryForSingleLod(detail);
	}
}
