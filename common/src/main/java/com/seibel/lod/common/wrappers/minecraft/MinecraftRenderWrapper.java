package com.seibel.lod.common.wrappers.minecraft;

import java.awt.*;
import java.util.HashSet;

import com.mojang.blaze3d.platform.NativeImage;
import com.seibel.lod.common.wrappers.misc.LightMapWrapper;
import com.seibel.lod.core.handlers.IReflectionHandler;
import com.seibel.lod.core.handlers.ReflectionHandler;
import com.seibel.lod.core.util.LodUtil;
import net.minecraft.client.renderer.LightTexture;
import org.lwjgl.opengl.GL20;

import com.mojang.math.Vector3f;
import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.objects.math.Vec3d;
import com.seibel.lod.core.objects.math.Vec3f;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.lod.common.wrappers.McObjectConverter;
import com.seibel.lod.common.wrappers.block.BlockPosWrapper;
import com.seibel.lod.common.wrappers.chunk.ChunkPosWrapper;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelRenderer.RenderChunkInfo;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.CompiledChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;


/**
 * A singleton that contains everything
 * related to rendering in Minecraft.
 *
 * @author James Seibel
 * @version 12-14-2021
 */
public class MinecraftRenderWrapper implements IMinecraftRenderWrapper
{
    public static final MinecraftRenderWrapper INSTANCE = new MinecraftRenderWrapper();

    private static final MinecraftWrapper MC_WRAPPER = MinecraftWrapper.INSTANCE;

    private static final Minecraft MC = Minecraft.getInstance();
    private static final GameRenderer GAME_RENDERER = MC.gameRenderer;



    @Override
    public Vec3f getLookAtVector()
    {
        Camera camera = GAME_RENDERER.getMainCamera();
        Vector3f cameraDir = camera.getLookVector();
        return new Vec3f(cameraDir.x(), cameraDir.y(), cameraDir.z());
    }

    @Override
    public AbstractBlockPosWrapper getCameraBlockPosition()
    {
        Camera camera = GAME_RENDERER.getMainCamera();
        BlockPos blockPos = camera.getBlockPosition();
        return new BlockPosWrapper(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    @Override
    public boolean playerHasBlindnessEffect()
    {
        return MC.player.getActiveEffectsMap().get(MobEffects.BLINDNESS) != null;
    }

    @Override
    public Vec3d getCameraExactPosition()
    {
        Camera camera = GAME_RENDERER.getMainCamera();
        Vec3 projectedView = camera.getPosition();

        return new Vec3d(projectedView.x, projectedView.y, projectedView.z);
    }

    @Override
    public Mat4f getDefaultProjectionMatrix(float partialTicks)
    {
        return McObjectConverter.Convert(GAME_RENDERER.getProjectionMatrix(GAME_RENDERER.getFov(GAME_RENDERER.getMainCamera(), partialTicks, true)));
    }

    @Override
    public double getGamma()
    {
        return MC.options.gamma;
    }

    @Override
    public Color getFogColor() {
        float[] colorValues = new float[4];
        GL20.glGetFloatv(GL20.GL_FOG_COLOR, colorValues);
        return new Color(colorValues[0], colorValues[1], colorValues[2], colorValues[3]);
    }

    @Override
    public Color getSkyColor() {
        if (MC.level.dimensionType().hasSkyLight()) {
            Vec3 colorValues = MC.level.getSkyColor(MC.gameRenderer.getMainCamera().getPosition(), MC.getFrameTime());
            return new Color((float) colorValues.x, (float) colorValues.y, (float) colorValues.z);
        } else
            return new Color(0, 0, 0);
    }

    @Override
    public double getFov(float partialTicks)
    {
        return GAME_RENDERER.getFov(GAME_RENDERER.getMainCamera(), partialTicks, true);
    }

    /** Measured in chunks */
    @Override
    public int getRenderDistance()
    {
        return MC.options.renderDistance;
    }

    @Override
    public int getScreenWidth()
    {
        return MC.getWindow().getWidth();
    }
    @Override
    public int getScreenHeight()
    {
        return MC.getWindow().getHeight();
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
    public HashSet<AbstractChunkPosWrapper> getVanillaRenderedChunks()
    {
        HashSet<AbstractChunkPosWrapper> loadedPos = new HashSet<>();

        // Wow, those are some long names!

        // go through every RenderInfo to get the compiled chunks
        LevelRenderer renderer = MC.levelRenderer;
        for (LevelRenderer.RenderChunkInfo worldRenderer$LocalRenderInformationContainer : renderer.renderChunks)
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


    @Override
    public int[] getLightmapPixels()
    {
        LightTexture tex = GAME_RENDERER.lightTexture();
        tex.tick(); // This call makes no sense, but it fixes pause menu flicker bug
        NativeImage lightMapPixels = tex.lightPixels;
        LightMapWrapper lightMap = new LightMapWrapper(lightMapPixels);


        int lightMapHeight = getLightmapTextureHeight();
        int lightMapWidth = getLightmapTextureWidth();

        int pixels[] = new int[lightMapWidth * lightMapHeight];
        for (int u = 0; u < lightMapWidth; u++)
        {
            for (int v = 0; v < lightMapWidth; v++)
            {
                // this could probably be kept as a int, but
                // it is easier to test and see the colors when debugging this way.
                // When creating a new release this should be changed to the int version.
                Color c = LodUtil.intToColor(lightMap.getLightValue(u, v));

                // these should both create a totally white image
//					int col =
//							Integer.MAX_VALUE;
//					int col =
//							0b11111111 + // red
//							(0b11111111 << 8) + // green
//							(0b11111111 << 16) + // blue
//							(0b11111111 << 24); // blue

                int col =
                        ((c.getRed() & 0xFF) << 16) | // blue
                        ((c.getGreen() & 0xFF) << 8) | // green
                        ((c.getBlue() & 0xFF)) | // red
                        ((c.getAlpha() & 0xFF) << 24); // alpha

                        
                // 2D array stored in a 1D array.
                // Thank you Tim from College ;)
                pixels[u * lightMapWidth + v] = col;
            }
        }

        return pixels;
    }


    @Override
    public int getLightmapTextureHeight()
    {
        int height = -1;

        LightTexture lightTexture = GAME_RENDERER.lightTexture();
        if (lightTexture != null)
        {
            NativeImage tex = lightTexture.lightPixels;
            if (tex != null)
            {
                height = tex.getHeight();
            }
        }

        return height;
    }

    @Override
    public int getLightmapTextureWidth()
    {
        int width = -1;

        LightTexture lightTexture = GAME_RENDERER.lightTexture();
        if (lightTexture != null)
        {
            NativeImage tex = lightTexture.lightPixels;
            if (tex != null)
            {
                width = tex.getWidth();
            }
        }

        return width;
    }


    @Override
    public int getLightmapGLFormat() {
        int glFormat = -1;

        LightTexture lightTexture = GAME_RENDERER.lightTexture();
        if (lightTexture != null) {
            NativeImage tex = lightTexture.lightPixels;
            if (tex != null) {
                glFormat = tex.format().glFormat();
            }
        }

        return glFormat;
    }
}
