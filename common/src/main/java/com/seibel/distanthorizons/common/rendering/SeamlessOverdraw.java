package com.seibel.distanthorizons.common.rendering;

#if PRE_MC_1_19_4
import com.mojang.math.Matrix4f;
#else
import org.joml.Matrix4f;
#endif
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.util.RenderUtil;
import com.seibel.distanthorizons.coreapi.util.math.Mat4f;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;

public class SeamlessOverdraw
{
	/**
	 * Proof-of-concept experimental option, not intended for normal use. <br>
	 * (Poorly) replaces Minecraft's far clip plane so it lines up with DH's near clip plane.
	 */
	public static float[] overwriteMinecraftNearFarClipPlanes(Matrix4f minecraftProjectionMatrix, float previousPartialTicks)
	{
		float[] matrixFloatArray;
		
		#if PRE_MC_1_19_4
		FloatBuffer matrixFloatBuffer = FloatBuffer.allocate(16);
		minecraftProjectionMatrix.store(matrixFloatBuffer);
		matrixFloatArray = matrixFloatBuffer.array();
		#else
		// Passing float buffers in caused native code crashes, so we are passing in a float array instead			
		matrixFloatArray = new float[16];
		minecraftProjectionMatrix.get(matrixFloatArray);
		#endif
		
		return overwriteMinecraftNearFarClipPlanes(matrixFloatArray, previousPartialTicks);
	}
	
	public static float[] overwriteMinecraftNearFarClipPlanes(Mat4f minecraftProjectionMatrix, float previousPartialTicks)
	{
		return overwriteMinecraftNearFarClipPlanes(minecraftProjectionMatrix.getValuesAsArray(), previousPartialTicks);
	}
	
	private static float[] overwriteMinecraftNearFarClipPlanes(float[] projectionMatrixFloatArray, float previousPartialTicks)
	{
		float dhFarClipPlane = RenderUtil.getNearClipPlaneDistanceInBlocks(previousPartialTicks);
		
		// works for fabric, bad not for forge for some reason :/
		float farClip = dhFarClipPlane * 5.1f; // magic number found via trial and error, James has no idea what it represents, except that it makes the seam between DH and vanilla rendering pretty close
		float nearClip = 0.5f; // this causes issues with some vanilla rendering, specifically the wireframe around selected blocks is slightly off. Unfortunately the ratio between the near and far clip plane can't be easily modified without completely screwing up the rendering. 
		
		// these may be the wrong index locations in any version of MC other than 1.18.2
		projectionMatrixFloatArray[10] = -((farClip + nearClip) / (farClip - nearClip)); // near clip plane
		projectionMatrixFloatArray[11] = -((2 * farClip * nearClip) / (farClip - nearClip)); // far clip plane
		
		
		return projectionMatrixFloatArray;
	}
	
	
	
	//================//
	// helper methods //
	//================//
	
	public static void applyLegacyProjectionMatrix(float[] projectionMatrixFloatArray)
	{
		int glMatrixMode = GL15.glGetInteger(GL15.GL_MATRIX_MODE);
		GL15.glMatrixMode(GL15.GL_PROJECTION);
		
		GL15.glLoadMatrixf(projectionMatrixFloatArray);
		
		GL15.glMatrixMode(glMatrixMode);
	}
	
}
