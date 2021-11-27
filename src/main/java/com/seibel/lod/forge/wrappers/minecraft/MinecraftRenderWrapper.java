package com.seibel.lod.forge.wrappers.minecraft;

import java.awt.Color;
import java.util.HashSet;

import org.lwjgl.opengl.GL15;

import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.objects.math.Vec3d;
import com.seibel.lod.core.objects.math.Vec3f;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.lod.forge.wrappers.McObjectConverter;
import com.seibel.lod.forge.wrappers.block.BlockPosWrapper;
import com.seibel.lod.forge.wrappers.chunk.ChunkPosWrapper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.CompiledChunk;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

/**
 * A singleton that contains everything
 * related to rendering in Minecraft.
 * 
 * @author James Seibel
 * @version 11-26-2021
 */
public class MinecraftRenderWrapper implements IMinecraftRenderWrapper
{
	public static final MinecraftRenderWrapper INSTANCE = new MinecraftRenderWrapper();
	
	private final GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
	private final static Minecraft mc = Minecraft.getInstance();
	
	
	
	@Override
	public Vec3f getLookAtVector()
	{
		ActiveRenderInfo camera = gameRenderer.getMainCamera();
		Vector3f cameraDir = camera.getLookVector();
		return new Vec3f(cameraDir.x(), cameraDir.y(), cameraDir.z());
	}
	
	@Override
	public AbstractBlockPosWrapper getCameraBlockPosition()
	{
		ActiveRenderInfo camera = gameRenderer.getMainCamera();
		BlockPos blockPos = camera.getBlockPosition();
		return new BlockPosWrapper(blockPos.getX(), blockPos.getY(), blockPos.getZ());
	}
	
	@Override
	public boolean playerHasBlindnessEffect()
	{
		return mc.player.getActiveEffectsMap().get(Effects.BLINDNESS) != null;
	}
	
	@Override
	public Vec3d getCameraExactPosition()
	{
		ActiveRenderInfo camera = gameRenderer.getMainCamera();
		Vector3d projectedView = camera.getPosition();
		
		return new Vec3d(projectedView.x, projectedView.y, projectedView.z);
	}
	
	@Override
	public Mat4f getDefaultProjectionMatrix(float partialTicks)
	{
		return McObjectConverter.Convert(gameRenderer.getProjectionMatrix(gameRenderer.getMainCamera(), partialTicks, true));
	}
	
	@Override
	public double getGamma()
	{
		return mc.options.gamma;
	}
	
	@Override
	public Color getFogColor()
	{
		float[] colorValues = new float[4];
		GL15.glGetFloatv(GL15.GL_FOG_COLOR, colorValues);
		return new Color(colorValues[0], colorValues[1], colorValues[2], colorValues[3]);
	}
	
	@Override
	public double getFov(float partialTicks)
	{
		return gameRenderer.getFov(gameRenderer.getMainCamera(), partialTicks, true);
	}
	
	/** Measured in chunks */
	@Override
	public int getRenderDistance()
	{
		return mc.options.renderDistance;
	}
	
	@Override
	public int getScreenWidth()
	{
		return mc.getWindow().getWidth();	
	}
	@Override
	public int getScreenHeight()
	{
		return mc.getWindow().getHeight();
	}
	
	/**
	 * This method returns the ChunkPos of all chunks that Minecraft
	 * is going to render this frame. <br><br>
	 * <p>
	 * Note: This isn't perfect. It will return some chunks that are outside
	 * the clipping plane. (For example, if you are high above the ground some chunks
	 * will be incorrectly added, even though they are outside render range).
	 */
	@Override
	public HashSet<AbstractChunkPosWrapper> getRenderedChunks()
	{
		HashSet<AbstractChunkPosWrapper> loadedPos = new HashSet<>();
		
		// Wow, those are some long names!
		
		// go through every RenderInfo to get the compiled chunks
		WorldRenderer renderer = mc.levelRenderer;
		for (WorldRenderer.LocalRenderInformationContainer worldRenderer$LocalRenderInformationContainer : renderer.renderChunks)
		{
			CompiledChunk compiledChunk = worldRenderer$LocalRenderInformationContainer.chunk.getCompiledChunk();
			if (!compiledChunk.hasNoRenderableLayers())
			{
				// add the ChunkPos for every rendered chunk
				BlockPos bpos = worldRenderer$LocalRenderInformationContainer.chunk.getOrigin();
				
				loadedPos.add(new ChunkPosWrapper(bpos));
			}
		}
		
		return loadedPos;
	}
}
