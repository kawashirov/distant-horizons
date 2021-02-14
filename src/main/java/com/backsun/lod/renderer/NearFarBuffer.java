package com.backsun.lod.renderer;

import java.nio.ByteBuffer;

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
	public ByteBuffer nearBuffer;
	
	public ByteBuffer farBuffer;
	
	
	NearFarBuffer(ByteBuffer newNearBuffer, ByteBuffer newFarBuffer)
	{
		nearBuffer = newNearBuffer;
		farBuffer = newFarBuffer;
	}
}
