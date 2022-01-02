/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.forge.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.seibel.lod.common.Config;
import com.seibel.lod.common.clouds.CloudBufferSingleton;
import com.seibel.lod.common.clouds.CloudTexture;
import com.seibel.lod.common.clouds.NoiseCloudHandler;
import com.seibel.lod.common.wrappers.McObjectConverter;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.objects.math.Mat4f;

import net.minecraft.client.renderer.RenderType;


import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;

import java.nio.FloatBuffer;
import java.util.Random;

/**
 * This class is used to mix in my rendering code
 * before Minecraft starts rendering blocks.
 * If this wasn't done, and we used Forge's
 * render last event, the LODs would render on top
 * of the normal terrain.
 *
 * This is also the mixin for rendering the clouds
 *
 * @author coolGi2007
 * @author James Seibel
 * @version 12-31-2021
 */
@Mixin(LevelRenderer.class)
public class MixinWorldRenderer
{
	@Final @Shadow private static ResourceLocation CLOUDS_LOCATION;
	@Shadow private final int ticks;
	@Final @Shadow @NotNull private final Minecraft minecraft;
	@Shadow private int prevCloudX;
	@Shadow private int prevCloudY;
	@Shadow private int prevCloudZ;
	@Shadow @NotNull private Vec3 prevCloudColor;
	@Shadow @NotNull private CloudStatus prevCloudsType;
	@Shadow private boolean generateClouds;
	@Shadow @NotNull private VertexBuffer cloudBuffer;
	@Unique private boolean initializedClouds = false;

	private static float previousPartialTicks = 0;

	public MixinWorldRenderer() {
		throw new NullPointerException("Null cannot be cast to non-null type.");
	}

	@Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
	public void renderClouds(PoseStack poseStack, Matrix4f projectionMatrix, float tickDelta, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
		if (Config.Client.Graphics.CloudQuality.customClouds) {
			TextureManager textureManager = Minecraft.getInstance().getTextureManager();
			registerClouds(textureManager);
			NoiseCloudHandler.update();

			if (minecraft.level.dimension() == ClientLevel.OVERWORLD) {
				CloudTexture cloudTexture = NoiseCloudHandler.cloudTextures.get(NoiseCloudHandler.cloudTextures.size() - 1);
				renderCloudLayer(poseStack, projectionMatrix, tickDelta, cameraX, cameraY, cameraZ, (float) (Config.Client.Graphics.CloudQuality.cloudHeight + 0.01 /* Make clouds a bit higher so it dosnt do janky stuff */), 0, 1, 1, cloudTexture.resourceLocation);
			}

			ci.cancel();
		}

		// get the partial ticks since renderChunkLayer doesn't
		// have access to them
		previousPartialTicks = tickDelta;
	}

	// HEAD or RETURN
	@Inject(at = @At("HEAD"), method = "renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLcom/mojang/math/Matrix4f;)V")
	private void renderChunkLayer(RenderType renderType, PoseStack modelViewMatrixStack, double cameraXBlockPos, double cameraYBlockPos, double cameraZBlockPos, Matrix4f projectionMatrix, CallbackInfo callback)
	{
		// only render before solid blocks
		if (renderType.equals(RenderType.solid()))
		{
			Mat4f mcModelViewMatrix = McObjectConverter.Convert(modelViewMatrixStack.last().pose());
			Mat4f mcProjectionMatrix = McObjectConverter.Convert(projectionMatrix);

			ClientApi.INSTANCE.renderLods(mcModelViewMatrix, mcProjectionMatrix, previousPartialTicks);
		}
	}








	// TODO: Move these outside of the mixin
	// When moved out then put credit to https://github.com/misterslime/fabulousclouds-fabric

	private void registerClouds(TextureManager textureManager) {
		if (!this.initializedClouds) {
			Random random = new Random();

			NoiseCloudHandler.initCloudTextures(CLOUDS_LOCATION);

			for(CloudTexture cloudTexture : NoiseCloudHandler.cloudTextures) {
				cloudTexture.initNoise(random);

				DynamicTexture texture = cloudTexture.getNativeImage();
				textureManager.register(cloudTexture.resourceLocation, texture);
				cloudTexture.setTexture(texture);
			}

			this.initializedClouds = true;
		}
	}

	private void renderCloudLayer(PoseStack poseStack, Matrix4f projectionMatrix, float tickDelta, double cameraX, double cameraY, double cameraZ, float cloudHeight, float cloudOffset, float cloudScale, float speedMod, ResourceLocation resourceLocation) {
		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		RenderSystem.enableDepthTest();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.depthMask(true);
		float scale = 12.0F * cloudScale;
		double speed = ((this.ticks + tickDelta) * (0.03F * speedMod));
		double posX = (cameraX + speed) / scale;
		double posY = (cloudHeight - (float) cameraY + cloudOffset) / cloudScale;
		double posZ = cameraZ / scale + 0.33000001311302185D;
		posX -= Math.floor(posX / 2048.0D) * 2048;
		posZ -= Math.floor(posZ / 2048.0D) * 2048;
		float adjustedX = (float) (posX - (double) Math.floor(posX));
		float adjustedY = (float) (posY / 4.0D - (double) Math.floor(posY / 4.0D)) * 4.0F;
		float adjustedZ = (float) (posZ - (double) Math.floor(posZ));
		Vec3 cloudColor = minecraft.level.getCloudColor(tickDelta);
		int floorX = (int) Math.floor(posX);
		int floorY = (int) Math.floor(posY / 4.0D);
		int floorZ = (int) Math.floor(posZ);
		if (floorX != this.prevCloudX || floorY != this.prevCloudY || floorZ != this.prevCloudZ || this.minecraft.options.getCloudsType() != this.prevCloudsType || this.prevCloudColor.distanceToSqr(cloudColor) > 2.0E-4D) {
			this.prevCloudX = floorX;
			this.prevCloudY = floorY;
			this.prevCloudZ = floorZ;
			this.prevCloudColor = cloudColor;
			this.prevCloudsType = this.minecraft.options.getCloudsType();
			this.generateClouds = true;
		}

		RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
		
		//Setup custom projection matrix and override minecraft's.
		//create needed objects
		ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
		FloatBuffer customBuffer = CloudBufferSingleton.INSTANCE.customBuffer;
		FloatBuffer mcBuffer = CloudBufferSingleton.INSTANCE.mcBuffer;
		//create clip values.
		//far clip is our clip plane value
		float farClip = (CONFIG.client().graphics().quality().getLodChunkRenderDistance() * LodUtil.CHUNK_WIDTH) * LodUtil.CHUNK_WIDTH / 2;
		//near clip is mc clip plane value
		float nearClip = 0.05f;
		float matNearClip = -((farClip + nearClip) / (farClip - nearClip));
		float matFarClip = -((2 * farClip * nearClip) / (farClip - nearClip));
		
		//store projectionMatrix values to buffers
		projectionMatrix.store(customBuffer);
		projectionMatrix.store(mcBuffer);
		//change values on our custom buffer
		customBuffer.put(10, matNearClip);
		customBuffer.put(14, matFarClip);
		//load values from our buffer to the projection matrix
		projectionMatrix.load(customBuffer);
		
		if (this.generateClouds) {
			this.generateClouds = false;
			Tesselator tessellator = Tesselator.getInstance();
			BufferBuilder bufferBuilder = tessellator.getBuilder();
			if (this.cloudBuffer != null) this.cloudBuffer.close();

			this.cloudBuffer = new VertexBuffer();
			this.buildCloudLayer(bufferBuilder, posX, posY, posZ, cloudOffset, cloudScale, cloudColor);
			bufferBuilder.end();
			this.cloudBuffer.upload(bufferBuilder);
		}

		RenderSystem.setShaderTexture(0, resourceLocation);
		FogRenderer.levelFogColor();
		poseStack.pushPose();
		poseStack.scale(scale, cloudScale, scale);
		poseStack.translate(-adjustedX, adjustedY, -adjustedZ);
		if (this.cloudBuffer != null) {
			int cloudMainIndex = this.prevCloudsType == CloudStatus.FANCY ? 0 : 1;

			for (int cloudIndex = 1; cloudMainIndex <= cloudIndex; ++cloudMainIndex) {
				if (cloudMainIndex == 0) {
					RenderSystem.colorMask(false, false, false, false);
				} else {
					RenderSystem.colorMask(true, true, true, true);
				}

				ShaderInstance shader = RenderSystem.getShader();
				this.cloudBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
			}
		}
		//reset the projection matrix to original values
		projectionMatrix.load(mcBuffer);
		poseStack.popPose();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	private void buildCloudLayer(BufferBuilder bufferBuilder, double cloudX, double cloudY, double cloudZ, float offset, float scale, Vec3 color) {
		float lowpFracAccur = (float) Math.pow(2.0, -8);
		float mediumpFracAccur = (float) Math.pow(2.0, -10);
		float viewDistance = 8;
		float cloudThickness = 4.0f;
		float adjustedCloudX = (float)Math.floor(cloudX) * lowpFracAccur;
		float adjustedCloudZ = (float)Math.floor(cloudZ) * lowpFracAccur;
		float redTop = (float)color.x;
		float greenTop = (float)color.y;
		float blueTop = (float)color.z;
		float redEW = redTop * 0.9f;
		float greenEW = greenTop * 0.9f;
		float blueEW = blueTop * 0.9f;
		float redBottom = redTop * 0.7f;
		float greenBottom = greenTop * 0.7f;
		float blueBottom = blueTop * 0.7f;
		float redNS = redTop * 0.8f;
		float greenNS = greenTop * 0.8f;
		float blueNS = blueTop * 0.8f;
		bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
		float adjustedCloudY = (float)Math.floor(cloudY / cloudThickness) * cloudThickness;

		if (this.prevCloudsType == CloudStatus.FANCY) {
			int scaledViewDistance = (int) (((Config.Client.Graphics.CloudQuality.extendClouds ? Config.Client.Graphics.Quality.lodChunkRenderDistance : minecraft.options.renderDistance) / 2) / scale) / 2;

			for (int x = -scaledViewDistance - 1; x <= scaledViewDistance; ++x) {
				for (int z = -scaledViewDistance - 1; z <= scaledViewDistance; ++z) {
					int n3;
					float scaledX = x * viewDistance;
					float scaledZ = z * viewDistance;
					if (adjustedCloudY > -5.0f) {
						bufferBuilder.vertex(scaledX + 0.0f, adjustedCloudY + 0.0f, scaledZ + 8.0f).uv((scaledX + 0.0f) * lowpFracAccur + adjustedCloudX, (scaledZ + 8.0f) * lowpFracAccur + adjustedCloudZ).color(redBottom, greenBottom, blueBottom, 0.8f).normal(0.0f, -1.0f, 0.0f).endVertex();
						bufferBuilder.vertex(scaledX + 8.0f, adjustedCloudY + 0.0f, scaledZ + 8.0f).uv((scaledX + 8.0f) * lowpFracAccur + adjustedCloudX, (scaledZ + 8.0f) * lowpFracAccur + adjustedCloudZ).color(redBottom, greenBottom, blueBottom, 0.8f).normal(0.0f, -1.0f, 0.0f).endVertex();
						bufferBuilder.vertex(scaledX + 8.0f, adjustedCloudY + 0.0f, scaledZ + 0.0f).uv((scaledX + 8.0f) * lowpFracAccur + adjustedCloudX, (scaledZ + 0.0f) * lowpFracAccur + adjustedCloudZ).color(redBottom, greenBottom, blueBottom, 0.8f).normal(0.0f, -1.0f, 0.0f).endVertex();
						bufferBuilder.vertex(scaledX + 0.0f, adjustedCloudY + 0.0f, scaledZ + 0.0f).uv((scaledX + 0.0f) * lowpFracAccur + adjustedCloudX, (scaledZ + 0.0f) * lowpFracAccur + adjustedCloudZ).color(redBottom, greenBottom, blueBottom, 0.8f).normal(0.0f, -1.0f, 0.0f).endVertex();
					}
					if (adjustedCloudY <= 5.0f) {
						bufferBuilder.vertex(scaledX + 0.0f, adjustedCloudY + cloudThickness - mediumpFracAccur, scaledZ + 8.0f).uv((scaledX + 0.0f) * lowpFracAccur + adjustedCloudX, (scaledZ + 8.0f) * lowpFracAccur + adjustedCloudZ).color(redTop, greenTop, blueTop, 0.8f).normal(0.0f, 1.0f, 0.0f).endVertex();
						bufferBuilder.vertex(scaledX + 8.0f, adjustedCloudY + cloudThickness - mediumpFracAccur, scaledZ + 8.0f).uv((scaledX + 8.0f) * lowpFracAccur + adjustedCloudX, (scaledZ + 8.0f) * lowpFracAccur + adjustedCloudZ).color(redTop, greenTop, blueTop, 0.8f).normal(0.0f, 1.0f, 0.0f).endVertex();
						bufferBuilder.vertex(scaledX + 8.0f, adjustedCloudY + cloudThickness - mediumpFracAccur, scaledZ + 0.0f).uv((scaledX + 8.0f) * lowpFracAccur + adjustedCloudX, (scaledZ + 0.0f) * lowpFracAccur + adjustedCloudZ).color(redTop, greenTop, blueTop, 0.8f).normal(0.0f, 1.0f, 0.0f).endVertex();
						bufferBuilder.vertex(scaledX + 0.0f, adjustedCloudY + cloudThickness - mediumpFracAccur, scaledZ + 0.0f).uv((scaledX + 0.0f) * lowpFracAccur + adjustedCloudX, (scaledZ + 0.0f) * lowpFracAccur + adjustedCloudZ).color(redTop, greenTop, blueTop, 0.8f).normal(0.0f, 1.0f, 0.0f).endVertex();
					}
					if (x > -1) {
						for (n3 = 0; n3 < 8; ++n3) {
							bufferBuilder.vertex(scaledX + (float)n3 + 0.0f, adjustedCloudY + 0.0f, scaledZ + 8.0f).uv((scaledX + (float)n3 + 0.5f) * lowpFracAccur + adjustedCloudX, (scaledZ + 8.0f) * lowpFracAccur + adjustedCloudZ).color(redEW, greenEW, blueEW, 0.8f).normal(-1.0f, 0.0f, 0.0f).endVertex();
							bufferBuilder.vertex(scaledX + (float)n3 + 0.0f, adjustedCloudY + cloudThickness, scaledZ + 8.0f).uv((scaledX + (float)n3 + 0.5f) * lowpFracAccur + adjustedCloudX, (scaledZ + 8.0f) * lowpFracAccur + adjustedCloudZ).color(redEW, greenEW, blueEW, 0.8f).normal(-1.0f, 0.0f, 0.0f).endVertex();
							bufferBuilder.vertex(scaledX + (float)n3 + 0.0f, adjustedCloudY + cloudThickness, scaledZ + 0.0f).uv((scaledX + (float)n3 + 0.5f) * lowpFracAccur + adjustedCloudX, (scaledZ + 0.0f) * lowpFracAccur + adjustedCloudZ).color(redEW, greenEW, blueEW, 0.8f).normal(-1.0f, 0.0f, 0.0f).endVertex();
							bufferBuilder.vertex(scaledX + (float)n3 + 0.0f, adjustedCloudY + 0.0f, scaledZ + 0.0f).uv((scaledX + (float)n3 + 0.5f) * lowpFracAccur + adjustedCloudX, (scaledZ + 0.0f) * lowpFracAccur + adjustedCloudZ).color(redEW, greenEW, blueEW, 0.8f).normal(-1.0f, 0.0f, 0.0f).endVertex();
						}
					}
					if (x <= 1) {
						for (n3 = 0; n3 < 8; ++n3) {
							bufferBuilder.vertex(scaledX + (float)n3 + 1.0f - mediumpFracAccur, adjustedCloudY + 0.0f, scaledZ + 8.0f).uv((scaledX + (float)n3 + 0.5f) * lowpFracAccur + adjustedCloudX, (scaledZ + 8.0f) * lowpFracAccur + adjustedCloudZ).color(redEW, greenEW, blueEW, 0.8f).normal(1.0f, 0.0f, 0.0f).endVertex();
							bufferBuilder.vertex(scaledX + (float)n3 + 1.0f - mediumpFracAccur, adjustedCloudY + cloudThickness, scaledZ + 8.0f).uv((scaledX + (float)n3 + 0.5f) * lowpFracAccur + adjustedCloudX, (scaledZ + 8.0f) * lowpFracAccur + adjustedCloudZ).color(redEW, greenEW, blueEW, 0.8f).normal(1.0f, 0.0f, 0.0f).endVertex();
							bufferBuilder.vertex(scaledX + (float)n3 + 1.0f - mediumpFracAccur, adjustedCloudY + cloudThickness, scaledZ + 0.0f).uv((scaledX + (float)n3 + 0.5f) * lowpFracAccur + adjustedCloudX, (scaledZ + 0.0f) * lowpFracAccur + adjustedCloudZ).color(redEW, greenEW, blueEW, 0.8f).normal(1.0f, 0.0f, 0.0f).endVertex();
							bufferBuilder.vertex(scaledX + (float)n3 + 1.0f - mediumpFracAccur, adjustedCloudY + 0.0f, scaledZ + 0.0f).uv((scaledX + (float)n3 + 0.5f) * lowpFracAccur + adjustedCloudX, (scaledZ + 0.0f) * lowpFracAccur + adjustedCloudZ).color(redEW, greenEW, blueEW, 0.8f).normal(1.0f, 0.0f, 0.0f).endVertex();
						}
					}
					if (z > -1) {
						for (n3 = 0; n3 < 8; ++n3) {
							bufferBuilder.vertex(scaledX + 0.0f, adjustedCloudY + cloudThickness, scaledZ + (float)n3 + 0.0f).uv((scaledX + 0.0f) * lowpFracAccur + adjustedCloudX, (scaledZ + (float)n3 + 0.5f) * lowpFracAccur + adjustedCloudZ).color(redNS, greenNS, blueNS, 0.8f).normal(0.0f, 0.0f, -1.0f).endVertex();
							bufferBuilder.vertex(scaledX + 8.0f, adjustedCloudY + cloudThickness, scaledZ + (float)n3 + 0.0f).uv((scaledX + 8.0f) * lowpFracAccur + adjustedCloudX, (scaledZ + (float)n3 + 0.5f) * lowpFracAccur + adjustedCloudZ).color(redNS, greenNS, blueNS, 0.8f).normal(0.0f, 0.0f, -1.0f).endVertex();
							bufferBuilder.vertex(scaledX + 8.0f, adjustedCloudY + 0.0f, scaledZ + (float)n3 + 0.0f).uv((scaledX + 8.0f) * lowpFracAccur + adjustedCloudX, (scaledZ + (float)n3 + 0.5f) * lowpFracAccur + adjustedCloudZ).color(redNS, greenNS, blueNS, 0.8f).normal(0.0f, 0.0f, -1.0f).endVertex();
							bufferBuilder.vertex(scaledX + 0.0f, adjustedCloudY + 0.0f, scaledZ + (float)n3 + 0.0f).uv((scaledX + 0.0f) * lowpFracAccur + adjustedCloudX, (scaledZ + (float)n3 + 0.5f) * lowpFracAccur + adjustedCloudZ).color(redNS, greenNS, blueNS, 0.8f).normal(0.0f, 0.0f, -1.0f).endVertex();
						}
					}
					if (z > 1) continue;
					for (n3 = 0; n3 < 8; ++n3) {
						bufferBuilder.vertex(scaledX + 0.0f, adjustedCloudY + cloudThickness, scaledZ + (float)n3 + 1.0f - mediumpFracAccur).uv((scaledX + 0.0f) * lowpFracAccur + adjustedCloudX, (scaledZ + (float)n3 + 0.5f) * lowpFracAccur + adjustedCloudZ).color(redNS, greenNS, blueNS, 0.8f).normal(0.0f, 0.0f, 1.0f).endVertex();
						bufferBuilder.vertex(scaledX + 8.0f, adjustedCloudY + cloudThickness, scaledZ + (float)n3 + 1.0f - mediumpFracAccur).uv((scaledX + 8.0f) * lowpFracAccur + adjustedCloudX, (scaledZ + (float)n3 + 0.5f) * lowpFracAccur + adjustedCloudZ).color(redNS, greenNS, blueNS, 0.8f).normal(0.0f, 0.0f, 1.0f).endVertex();
						bufferBuilder.vertex(scaledX + 8.0f, adjustedCloudY + 0.0f, scaledZ + (float)n3 + 1.0f - mediumpFracAccur).uv((scaledX + 8.0f) * lowpFracAccur + adjustedCloudX, (scaledZ + (float)n3 + 0.5f) * lowpFracAccur + adjustedCloudZ).color(redNS, greenNS, blueNS, 0.8f).normal(0.0f, 0.0f, 1.0f).endVertex();
						bufferBuilder.vertex(scaledX + 0.0f, adjustedCloudY + 0.0f, scaledZ + (float)n3 + 1.0f - mediumpFracAccur).uv((scaledX + 0.0f) * lowpFracAccur + adjustedCloudX, (scaledZ + (float)n3 + 0.5f) * lowpFracAccur + adjustedCloudZ).color(redNS, greenNS, blueNS, 0.8f).normal(0.0f, 0.0f, 1.0f).endVertex();
					}
				}
			}
		} else {
			int scaledRenderDistance = (int) ((Config.Client.Graphics.CloudQuality.extendClouds ? Config.Client.Graphics.Quality.lodChunkRenderDistance : minecraft.options.renderDistance) / scale);

			for (int x = -scaledRenderDistance; x < scaledRenderDistance; x += scaledRenderDistance) {
				for (int z = -scaledRenderDistance; z < scaledRenderDistance; z += scaledRenderDistance) {
					bufferBuilder.vertex(x, adjustedCloudY, z + scaledRenderDistance).uv((float)x * lowpFracAccur + adjustedCloudX, (float)(z + scaledRenderDistance) * lowpFracAccur + adjustedCloudZ).color(redTop, greenTop, blueTop, 0.8f).normal(0.0f, -1.0f, 0.0f).endVertex();
					bufferBuilder.vertex(x + scaledRenderDistance, adjustedCloudY, z + scaledRenderDistance).uv((float)(x + scaledRenderDistance) * lowpFracAccur + adjustedCloudX, (float)(z + scaledRenderDistance) * lowpFracAccur + adjustedCloudZ).color(redTop, greenTop, blueTop, 0.8f).normal(0.0f, -1.0f, 0.0f).endVertex();
					bufferBuilder.vertex(x + scaledRenderDistance, adjustedCloudY, z).uv((float)(x + scaledRenderDistance) * lowpFracAccur + adjustedCloudX, (float)z * lowpFracAccur + adjustedCloudZ).color(redTop, greenTop, blueTop, 0.8f).normal(0.0f, -1.0f, 0.0f).endVertex();
					bufferBuilder.vertex(x, adjustedCloudY, z).uv((float)x * lowpFracAccur + adjustedCloudX, (float)z * lowpFracAccur + adjustedCloudZ).color(redTop, greenTop, blueTop, 0.8f).normal(0.0f, -1.0f, 0.0f).endVertex();
				}
			}
		}
	}
}
