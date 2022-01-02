package com.seibel.lod.common.clouds;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

//I'm not sure if there is a better place to put these, if there is feel free to move them.
/**
 * This exists because creating a new Floatbuffer every tick to simply store floats seems to be a bad idea.
 */
public class CloudBufferSingleton
{
	
	public static final CloudBufferSingleton INSTANCE = new CloudBufferSingleton();
	
	public FloatBuffer customBuffer = MemoryUtil.memAllocFloat(16);
	public FloatBuffer mcBuffer = MemoryUtil.memAllocFloat(16);
	
	public CloudBufferSingleton(){
	
	}
}
