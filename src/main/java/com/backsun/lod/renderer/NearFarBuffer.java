package com.backsun.lod.renderer;

import net.minecraft.client.renderer.BufferBuilder;

/**
 * This object is just a replacement for an array
 * to make things easier to understand in the LodRenderer
 * and BuildBufferThread.
 * 
 * @author James Seibel
 * @version 02-13-2021
 */
public class NearFarBuffer
{
	public BufferBuilder nearBuffer;
	
	public BufferBuilder farBuffer;
	
	
	NearFarBuffer(BufferBuilder newNearBuffer, BufferBuilder newFarBuffer)
	{
		nearBuffer = newNearBuffer;
		farBuffer = newFarBuffer;
	}
}
