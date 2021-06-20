package com.seibel.lod.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashSet;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.seibel.lod.builders.LodBufferBuilder;
import com.seibel.lod.enums.FogDistance;
import com.seibel.lod.enums.FogQuality;
import com.seibel.lod.handlers.LodConfigHandler;
import com.seibel.lod.handlers.ReflectionHandler;
import com.seibel.lod.objects.LodChunk;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.objects.NearFarBuffer;
import com.seibel.lod.objects.NearFarFogSetting;
import com.seibel.lod.proxy.ClientProxy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.potion.Effects;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;


/**
 * This is where all the magic happens. <br>
 * This is where LODs are draw to the world. 
 * 
 * @author James Seibel
 * @version 06-19-2021
 */
public class LodRender
{
	/** this is the light used when rendering the LODs,
	 * it should be something different than what is used by Minecraft */
	private static final int LOD_GL_LIGHT_NUMBER = GL11.GL_LIGHT2;
	
	/** 
	 * 64 MB by default is the maximum amount of memory that
	 * can be directly allocated. <br><br>
	 * 
	 * I know there are commands to change that amount
	 * (specifically "-XX:MaxDirectMemorySize"), but
	 * I have no idea how to access that amount. <br> 
	 * So I guess this will be the hard limit for now. <br><br>
	 * 
	 * https://stackoverflow.com/questions/50499238/bytebuffer-allocatedirect-and-xmx
	 */
	public static final int MAX_ALOCATEABLE_DIRECT_MEMORY = 64 * 1024 * 1024;
	
	
	/** If true the LODs colors will be replaced with
	 * a checkerboard, this can be used for debugging. */
	public boolean debugging = false;
	
	private Minecraft mc;
	private GameRenderer gameRender;
	private IProfiler profiler;
	private float farPlaneDistance;
	private ReflectionHandler reflectionHandler;
	
	
	/** This is used to generate the buildable buffers */
	private LodBufferBuilder lodBufferBuilder;
	
	/** The buffers that are used to draw LODs using near fog */
	private volatile BufferBuilder drawableNearBuffer;
	/** The buffers that are used to draw LODs using far fog */
	private volatile BufferBuilder drawableFarBuffer;
	
	/** This is the VertexBuffer used to draw any LODs that use near fog */
	private volatile VertexBuffer nearVbo;
	/** This is the VertexBuffer used to draw any LODs that use far fog */
	private volatile VertexBuffer farVbo;
	public static final VertexFormat LOD_VERTEX_FORMAT = DefaultVertexFormats.POSITION_COLOR;
	
	/** This is used to determine if the LODs should be regenerated */
	private int previousChunkRenderDistance = 0;
	/** This is used to determine if the LODs should be regenerated */
	private int prevChunkX = 0;
	/** This is used to determine if the LODs should be regenerated */
	private int prevChunkZ = 0;
	/** This is used to determine if the LODs should be regenerated */
	private FogDistance prevFogDistance = FogDistance.NEAR_AND_FAR;
	
	/** if this is true the LOD buffers should be regenerated,
	 * provided they aren't already being regenerated. */
	private volatile boolean regen = false;
	
	/** This HashSet contains every chunk that Vanilla Minecraft
	 *  is going to render */
	public HashSet<ChunkPos> vanillaRenderedChunks = new HashSet<>();
	
	
	
	public LodRender(LodBufferBuilder newLodBufferBuilder)
	{
		mc = Minecraft.getInstance();
		gameRender = mc.gameRenderer;
		
		reflectionHandler = new ReflectionHandler();
		lodBufferBuilder = newLodBufferBuilder;
	}
	
	
	/**
	 * Besides drawing the LODs this method also starts
	 * the async process of generating the Buffers that hold those LODs.
	 * 
	 * @param newDimension The dimension to draw, if null doesn't replace the current dimension.
	 * @param partialTicks how far into the current tick this method was called.
	 */
	public void drawLODs(LodDimension lodDim, float partialTicks, IProfiler newProfiler)
	{		
		if (lodDim == null)
		{
			// if there aren't any loaded LodChunks
			// don't try drawing anything
			return;
		}
		
		
		
		
		
		
		//===============//
		// initial setup //
		//===============//
		
		profiler = newProfiler;
		profiler.startSection("LOD setup");
		
		ClientPlayerEntity player = mc.player;
		
		// should LODs be regenerated?
		if ((int)player.getPosX() / LodChunk.WIDTH != prevChunkX ||
			(int)player.getPosZ() / LodChunk.WIDTH != prevChunkZ ||
			previousChunkRenderDistance != mc.gameSettings.renderDistanceChunks ||
			prevFogDistance != LodConfigHandler.CLIENT.fogDistance.get())
		{
			// yes
			regen = true;
			
			prevChunkX = (int)player.getPosX() / LodChunk.WIDTH;
			prevChunkZ = (int)player.getPosZ() / LodChunk.WIDTH;
			prevFogDistance = LodConfigHandler.CLIENT.fogDistance.get();
		}
		else
		{
			// nope, the player hasn't moved, the
			// render distance hasn't changed, and
			// the dimension is the same
		}
		
		// did the user change the debug setting?
		if (LodConfigHandler.CLIENT.debugMode.get() != debugging)
		{
			debugging = LodConfigHandler.CLIENT.debugMode.get();
			regen = true;
		}
		
		
		// determine how far the game's render distance is currently set
		int renderDistWidth = mc.gameSettings.renderDistanceChunks;
		farPlaneDistance = renderDistWidth * LodChunk.WIDTH;
		
		// set how big the LODs will be and how far they will go
		int totalLength = (int) farPlaneDistance * LodConfigHandler.CLIENT.lodChunkRadiusMultiplier.get() * 2;
		int numbChunksWide = (totalLength / LodChunk.WIDTH);
		
		// see if the chunks Minecraft is going to render are the
		// same as last time
		// (This is done so we only render LODs )
		HashSet<ChunkPos> newRenderedChunks = getRenderedChunks(); 
		if (!vanillaRenderedChunks.containsAll(newRenderedChunks))
		{
			regen = true;
			vanillaRenderedChunks = newRenderedChunks;
		}
		
		
				
		
		
		//=================//
		// create the LODs //
		//=================//
		
		// only regenerate the LODs if:
		// 1. we want to regenerate LODs
		// 2. we aren't already regenerating the LODs
		// 3. we aren't waiting for the build and draw buffers to swap
		//		(this is to prevent thread conflicts)
		if (regen && !lodBufferBuilder.generatingBuffers && !lodBufferBuilder.newBuffersAvaliable())
		{
			// this will mainly happen when the view distance is changed
			if (drawableNearBuffer == null || drawableFarBuffer == null || 
				previousChunkRenderDistance != mc.gameSettings.renderDistanceChunks)
				setupBuffers(numbChunksWide);
			
			// generate the LODs on a separate thread to prevent stuttering or freezing
			lodBufferBuilder.generateLodBuffersAsync(this, lodDim, player.getPosX(), player.getPosZ(), numbChunksWide);
			
			// the regen process has been started,
			// it will be done when lodBufferBuilder.newBuffersAvaliable
			// is true
			regen = false;
		}
		
		// replace the buffers used to draw and build,
		// this is only done when the createLodBufferGenerationThread
		// has finished executing on a parallel thread.
		if (lodBufferBuilder.newBuffersAvaliable())
		{
			swapBuffers();
		}
		
		
		
		
		
		//===========================//
		// GL settings for rendering //
		//===========================//
		
		// set the required open GL settings
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_COLOR_MATERIAL);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		
		// disable the lights Minecraft uses
		GL11.glDisable(GL11.GL_LIGHT0);
		GL11.glDisable(GL11.GL_LIGHT1);
		
		// get the default projection matrix so we can
		// reset it after drawing the LODs
		float[] defaultProjMatrix = new float[16];
        GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, defaultProjMatrix);
		
		Matrix4f modelViewMatrix = generateModelViewMatrix(partialTicks);
		
		setupProjectionMatrix(partialTicks);
		setupLighting(lodDim, partialTicks);
		
		NearFarFogSetting fogSetting = determineFogSettings();
		
		// determine the current fog settings so they can be
		// reset after drawing the LODs
		float defaultFogStartDist = GL11.glGetFloat(GL11.GL_FOG_START);
		float defaultFogEndDist = GL11.glGetFloat(GL11.GL_FOG_END);
		int defaultFogMode = GL11.glGetInteger(GL11.GL_FOG_MODE);

		
		
		
		
		
		
		//===========//
		// rendering //
		//===========//
		profiler.endStartSection("LOD draw");
		
		setupFog(fogSetting.nearFogSetting, reflectionHandler.getFogQuality());
		sendLodsToGpuAndDraw(nearVbo, modelViewMatrix);
		
		setupFog(fogSetting.farFogSetting, reflectionHandler.getFogQuality());
		sendLodsToGpuAndDraw(farVbo, modelViewMatrix);
		
		
		
		
		
		//=========//
		// cleanup //
		//=========//
		
		profiler.endStartSection("LOD cleanup");
		
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(LOD_GL_LIGHT_NUMBER);
		// re-enable the lights Minecraft uses
		GL11.glEnable(GL11.GL_LIGHT0);
		GL11.glEnable(GL11.GL_LIGHT1);
		RenderSystem.disableLighting();
		
		// this can't be called until after the buffers are built
		// because otherwise the buffers may be set to the wrong size
		previousChunkRenderDistance = mc.gameSettings.renderDistanceChunks;
		
		// reset the fog settings so the normal chunks
		// will be drawn correctly
		RenderSystem.fogStart(defaultFogStartDist);
		RenderSystem.fogEnd(defaultFogEndDist);
		RenderSystem.fogMode(defaultFogMode);
		
		// reset the projection matrix so anything drawn after
		// the LODs will use the correct projection matrix
		Matrix4f mvm = new Matrix4f(defaultProjMatrix);
		mvm.transpose();
		gameRender.resetProjectionMatrix(mvm);
		
		// clear the depth buffer so anything drawn is drawn
		// over the LODs
		GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
		
		
		// end of internal LOD profiling
		profiler.endSection();
	}
	
	

	/**
	 * This is where the actual drawing happens.
	 * 
	 * @param buffers the buffers sent to the GPU to draw
	 */
	private void sendLodsToGpuAndDraw(VertexBuffer vbo, Matrix4f modelViewMatrix)
	{
		if (vbo == null)
			return;
		
        vbo.bindBuffer();
        // 0L is the starting pointer
        LOD_VERTEX_FORMAT.setupBufferState(0L);
        
        vbo.draw(modelViewMatrix, GL11.GL_QUADS);
        
        VertexBuffer.unbindBuffer();
        LOD_VERTEX_FORMAT.clearBufferState();
	}
	
	
	
	
	
	
	
	//=================//
	// Setup Functions //
	//=================//
	
	@SuppressWarnings("deprecation")
	private void setupFog(FogDistance fogDistance, FogQuality fogQuality)
	{
		if(fogQuality == FogQuality.OFF)
		{
			FogRenderer.resetFog();
			RenderSystem.disableFog();
			return;
		}
		
		if(fogDistance == FogDistance.NEAR_AND_FAR)
		{
			throw new IllegalArgumentException("setupFog doesn't accept the NEAR_AND_FAR fog distance.");
		}

		// the multipliers are percentages
		// of the regular view distance.
		
		if(fogDistance == FogDistance.NEAR)
		{
			// the reason that I wrote fogEnd then fogStart backwards
			// is because we are using fog backwards to how
			// it is normally used, with it hiding near objects
			// instead of far objects.
			
			if (fogQuality == FogQuality.FANCY)
			{
				RenderSystem.fogEnd(farPlaneDistance * 1.75f);
				RenderSystem.fogStart(farPlaneDistance * 1.95f);
			}
			else if(fogQuality == FogQuality.FAST)
			{
				// for the far fog of the normal chunks
				// to start right where the LODs' end use:
				// end = 0.8f, start = 1.5f
				
				RenderSystem.fogEnd(farPlaneDistance * 1.5f);
				RenderSystem.fogStart(farPlaneDistance * 2.0f);
			}
		}
		else if(fogDistance == FogDistance.FAR)
		{
			if (fogQuality == FogQuality.FANCY)
			{
				RenderSystem.fogStart(farPlaneDistance * 0.85f * LodConfigHandler.CLIENT.lodChunkRadiusMultiplier.get());
				RenderSystem.fogEnd(farPlaneDistance * 1.0f * LodConfigHandler.CLIENT.lodChunkRadiusMultiplier.get());
			}
			else if(fogQuality == FogQuality.FAST)
			{
				RenderSystem.fogStart(farPlaneDistance * 0.5f * LodConfigHandler.CLIENT.lodChunkRadiusMultiplier.get());
				RenderSystem.fogEnd(farPlaneDistance * 0.75f * LodConfigHandler.CLIENT.lodChunkRadiusMultiplier.get());
			}
		}
		
		RenderSystem.fogMode(GlStateManager.FogMode.LINEAR);
		RenderSystem.enableFog();
	}
	

	/**
	 * Create the model view matrix to move the LODs
	 * from object space into world space.
	 */
	private Matrix4f generateModelViewMatrix(float partialTicks)
	{
		// get all relevant camera info
		ActiveRenderInfo renderInfo = mc.gameRenderer.getActiveRenderInfo();
        Vector3d projectedView = renderInfo.getProjectedView();
		
		
		// generate the model view matrix
		MatrixStack matrixStack = new MatrixStack();
        matrixStack.push();
        // translate and rotate to the current camera location
        matrixStack.rotate(Vector3f.XP.rotationDegrees(renderInfo.getPitch()));
        matrixStack.rotate(Vector3f.YP.rotationDegrees(renderInfo.getYaw() + 180));
        matrixStack.translate(-projectedView.x, -projectedView.y, -projectedView.z);
        
        return matrixStack.getLast().getMatrix();
	}
	
	
	/**
	 * create a new projection matrix and send it over to the GPU
	 * <br><br>
	 * A lot of this code is copied from renderWorld (line 578)
	 * in the GameRender class. The code copied is anything with
	 * a matrixStack and is responsible for making sure the LOD
	 * objects distort correctly relative to the rest of the world.
	 * Distortions are caused by: standing in a nether portal,
	 * nausea potion effect, walking bobbing.
	 * 
	 * @param partialTicks how many ticks into the frame we are
	 */
	private void setupProjectionMatrix(float partialTicks)
	{
		// Note: if the LOD objects don't distort correctly
		// compared to regular minecraft terrain, make sure
		// all the transformations in renderWorld are here too
		
		MatrixStack matrixStack = new MatrixStack();
        matrixStack.push();
		
		gameRender.hurtCameraEffect(matrixStack, partialTicks);
		if (this.mc.gameSettings.viewBobbing) {
			gameRender.applyBobbing(matrixStack, partialTicks);
		}
		
		// potion and nausea effects
		float f = MathHelper.lerp(partialTicks, mc.player.prevTimeInPortal, mc.player.timeInPortal) * mc.gameSettings.screenEffectScale * mc.gameSettings.screenEffectScale;
		if (f > 0.0F) {
			int i = this.mc.player.isPotionActive(Effects.NAUSEA) ? 7 : 20;
			float f1 = 5.0F / (f * f + 5.0F) - f * 0.04F;
			f1 = f1 * f1;
			Vector3f vector3f = new Vector3f(0.0F, MathHelper.SQRT_2 / 2.0F, MathHelper.SQRT_2 / 2.0F);
			matrixStack.rotate(vector3f.rotationDegrees((gameRender.rendererUpdateCount + partialTicks) * i));
			matrixStack.scale(1.0F / f1, 1.0F, 1.0F);
			float f2 = -(gameRender.rendererUpdateCount + partialTicks) * i;
			matrixStack.rotate(vector3f.rotationDegrees(f2));
		}
		
		
		
		// this projection matrix allows us to see past the normal 
		// world render distance
		Matrix4f projectionMatrix = 
				Matrix4f.perspective(
				getFov(partialTicks, true), 
				(float)this.mc.getMainWindow().getFramebufferWidth() / (float)this.mc.getMainWindow().getFramebufferHeight(), 
				0.5F, 
				this.farPlaneDistance * LodConfigHandler.CLIENT.lodChunkRadiusMultiplier.get() * 2);
		
		// add the screen space distortions
		projectionMatrix.mul(matrixStack.getLast().getMatrix());
		gameRender.resetProjectionMatrix(projectionMatrix);
		return;
	}
	
	
	/**
	 * setup the lighting to be used for the LODs
	 */
	private void setupLighting(LodDimension lodDimension, float partialTicks)
	{
		float sunBrightness = lodDimension.dimension.hasSkyLight() ? mc.world.getSunBrightness(partialTicks) : 0.2f;
		float gammaMultiplyer = (float)mc.gameSettings.gamma - 0.5f;
		float lightStrength = ((sunBrightness / 2f) - 0.2f) + (gammaMultiplyer * 0.2f);
		
		float lightAmbient[] = {lightStrength, lightStrength, lightStrength, 1.0f};
		
		// can be used for debugging
//		if (partialTicks < 0.005)
//			ClientProxy.LOGGER.debug(lightStrength);
		
		ByteBuffer temp = ByteBuffer.allocateDirect(16);
		temp.order(ByteOrder.nativeOrder());
		GL11.glLightfv(LOD_GL_LIGHT_NUMBER, GL11.GL_AMBIENT, (FloatBuffer) temp.asFloatBuffer().put(lightAmbient).flip());
		GL11.glEnable(LOD_GL_LIGHT_NUMBER); // Enable the above lighting
		
		RenderSystem.enableLighting();
	}
	
	/**
	 * Create all buffers that will be used.
	 */
	private void setupBuffers(int numbChunksWide)
	{
		// calculate the max amount of memory needed (in bytes)
		int bufferMemory = RenderUtil.getBufferMemoryForRadiusMultiplier(LodConfigHandler.CLIENT.lodChunkRadiusMultiplier.get());
		
		// if the required memory is greater than the 
		// MAX_ALOCATEABLE_DIRECT_MEMORY lower the lodChunkRadiusMultiplier
		// to fit.
		if (bufferMemory > MAX_ALOCATEABLE_DIRECT_MEMORY)
		{
			int maxRadiusMultiplier = RenderUtil.getMaxRadiusMultiplierWithAvaliableMemory(LodConfigHandler.CLIENT.lodTemplate.get(), LodConfigHandler.CLIENT.lodDetail.get());
			
			ClientProxy.LOGGER.warn("The lodChunkRadiusMultiplier was set too high "
					+ "and had to be lowered to fit memory constraints "
					+ "from " + LodConfigHandler.CLIENT.lodChunkRadiusMultiplier.get() + " " 
					+ "to " + maxRadiusMultiplier);
			
			LodConfigHandler.CLIENT.lodChunkRadiusMultiplier.set(
					maxRadiusMultiplier);
			
			bufferMemory = RenderUtil.getBufferMemoryForRadiusMultiplier(maxRadiusMultiplier);
		}
		
		drawableNearBuffer = new BufferBuilder(bufferMemory);
		drawableFarBuffer = new BufferBuilder(bufferMemory);
		
		lodBufferBuilder.setupBuffers(bufferMemory);
	}
	
	
	
	
	
	//======================//
	// Other Misc Functions // 
	//======================//
	
	/**
	 * If this is called then the next time "drawLODs" is called
	 * the LODs will be regenerated; the same as if the player moved.
	 */
	public void regenerateLODsNextFrame()
	{
		regen = true;
	}
	
	
	/**
	 * Replace the current drawable buffers with the newly
	 * created buffers from the lodBufferBuilder.
	 */
	private void swapBuffers()
	{
		// replace the drawable buffers with
		// the newly created buffers from the lodBufferBuilder
		NearFarBuffer newBuffers = lodBufferBuilder.swapBuffers(drawableNearBuffer, drawableFarBuffer);
		drawableNearBuffer = newBuffers.nearBuffer;
		drawableFarBuffer = newBuffers.farBuffer;
		
		
		// bind the buffers with their respective VBOs
		if (nearVbo != null)
			nearVbo.close();
		
		nearVbo = new VertexBuffer(LOD_VERTEX_FORMAT);
		nearVbo.upload(drawableNearBuffer);
		
		
		if (farVbo != null)
			farVbo.close();
		
		farVbo = new VertexBuffer(LOD_VERTEX_FORMAT);
		farVbo.upload(drawableFarBuffer);
	}
	
	
	private double getFov(float partialTicks, boolean useFovSetting)
	{
		return mc.gameRenderer.getFOVModifier(mc.gameRenderer.getActiveRenderInfo(), partialTicks, useFovSetting);
	}
	
	
	/**
	 * Based on the fogDistance setting and
	 * optifine's fogQuality setting return what fog
	 * settings should be used when rendering.
	 */
	private NearFarFogSetting determineFogSettings()
	{
		NearFarFogSetting fogSetting = new NearFarFogSetting();
		
		switch(reflectionHandler.getFogQuality())
		{
		case FANCY:
			
			switch(LodConfigHandler.CLIENT.fogDistance.get())
			{
			case NEAR_AND_FAR:
				fogSetting.nearFogSetting = FogDistance.NEAR;
				fogSetting.farFogSetting = FogDistance.FAR;
				break;
				
			case NEAR:
				fogSetting.nearFogSetting = FogDistance.NEAR;
				fogSetting.farFogSetting = FogDistance.NEAR;
				break;
				
			case FAR:
				fogSetting.nearFogSetting = FogDistance.FAR;
				fogSetting.farFogSetting = FogDistance.FAR;
				break;
			}
			
			break;
		case FAST:
			// fast fog setting should only have one type of
			// fog, since the LODs are separated into a near
			// and far portion; and fast fog is rendered from the
			// frustrum's perspective instead of the camera
			
			switch(LodConfigHandler.CLIENT.fogDistance.get())
			{
			case NEAR_AND_FAR:
				fogSetting.nearFogSetting = FogDistance.NEAR;
				fogSetting.farFogSetting = FogDistance.NEAR;
				break;
				
			case NEAR:
				fogSetting.nearFogSetting = FogDistance.NEAR;
				fogSetting.farFogSetting = FogDistance.NEAR;
				break;
				
			case FAR:
				fogSetting.nearFogSetting = FogDistance.FAR;
				fogSetting.farFogSetting = FogDistance.FAR;
				break;
			}
			
			break;
		case OFF:
			break;
		
		}
		
		return fogSetting;
	}
	
	
	
	/**
	 * This method returns the ChunkPos of all chunks that Minecraft
	 * is going to render this frame. <br><br>
	 * 
	 * Note: This isn't perfect. It will return some chunks that are outside
	 * 		 the clipping plane. (For example, if you are high above the ground some chunks
	 * 		 will be incorrectly added, even though they are outside render range).
	 */
	public static HashSet<ChunkPos> getRenderedChunks()
	{
		HashSet<ChunkPos> loadedPos = new HashSet<>();
		
		Minecraft mc = Minecraft.getInstance();
		
		// Wow those are some long names!
		
		// go through every RenderInfo to get the compiled chunks
		for(WorldRenderer.LocalRenderInformationContainer 
				worldrenderer$localrenderinformationcontainer : mc.worldRenderer.renderInfos)
		{
			if (!worldrenderer$localrenderinformationcontainer.renderChunk.getCompiledChunk().isEmpty())
			{
				// add the ChunkPos for every empty compiled chunk
				BlockPos bpos = worldrenderer$localrenderinformationcontainer.renderChunk.getPosition();
				
				loadedPos.add(new ChunkPos(bpos.getX() / 16, bpos.getZ() / 16));
			}
		}
		
		return loadedPos;
	}
	
	
}