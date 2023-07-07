package com.seibel.distanthorizons.common.rendering;

import com.mojang.math.Matrix4f;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.util.RenderUtil;

import java.nio.FloatBuffer;

public class SeamlessOverdraw
{
	/**
	 * Proof-of-concept experimental option, not intended for normal use. <br>
	 * (Poorly) replaces Minecraft's far clip plane so it lines up with DH's near clip plane.
	 */
	public static FloatBuffer overwriteMinecraftNearFarClipPlanes(Matrix4f minecraftProjectionMatrix, float previousPartialTicks)
	{
		FloatBuffer matrixFloatBuffer = FloatBuffer.allocate(16);
		minecraftProjectionMatrix.store(matrixFloatBuffer);
		float[] matrixFloatArray = matrixFloatBuffer.array();
		
		float dhFarClipPlane = RenderUtil.getNearClipPlaneDistanceInBlocks(previousPartialTicks);
		float farClip = dhFarClipPlane * 5.1f; // magic number found via trial and error, James has no idea what it represents, except that it makes the seam between DH and vanilla rendering pretty close
		float nearClip = 0.5f; // this causes issues with some vanilla rendering, specifically the wireframe around selected blocks is slightly off. Unfortunately the ratio between the near and far clip plane can't be easily modified without completely screwing up the rendering. 
		
		// these may be the wrong index locations in any version of MC other than 1.18.2
		matrixFloatArray[10] = -((farClip + nearClip) / (farClip - nearClip)); // near clip plane
		matrixFloatArray[11] = -((2 * farClip * nearClip) / (farClip - nearClip)); // far clip plane
		
		matrixFloatBuffer = FloatBuffer.wrap(matrixFloatArray);
		return matrixFloatBuffer;
	}
	
}
