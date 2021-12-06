package com.seibel.lod.fabric.wrappers.minecraft;

import java.awt.*;
import java.util.HashSet;

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
        Camera camera = gameRenderer.getMainCamera();
        Vector3f cameraDir = camera.getLookVector();
        return new Vec3f(cameraDir.x(), cameraDir.y(), cameraDir.z());
    }

    @Override
    public AbstractBlockPosWrapper getCameraBlockPosition()
    {
        Camera camera = gameRenderer.getMainCamera();
        BlockPos blockPos = camera.getBlockPosition();
        return new BlockPosWrapper(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    @Override
    public boolean playerHasBlindnessEffect()
    {
        return mc.player.getActiveEffectsMap().get(MobEffects.BLINDNESS) != null;
    }

    @Override
    public Vec3d getCameraExactPosition()
    {
        Camera camera = gameRenderer.getMainCamera();
        Vec3 projectedView = camera.getPosition();

        return new Vec3d(projectedView.x, projectedView.y, projectedView.z);
    }

    @Override
    public Mat4f getDefaultProjectionMatrix(float partialTicks)
    {
        return McObjectConverter.Convert(gameRenderer.getProjectionMatrix(gameRenderer.getFov(gameRenderer.getMainCamera(), partialTicks, true)));
    }

    @Override
    public double getGamma()
    {
        return mc.options.gamma;
    }

    @Override
    public Color getFogColor() {
        float[] colorValues = new float[4];
        GL20.glGetFloatv(GL20.GL_FOG_COLOR, colorValues);
        return new Color(colorValues[0], colorValues[1], colorValues[2], colorValues[3]);
    }

    @Override
    public Color getSkyColor() {
        if (mc.level.dimensionType().hasSkyLight()) {
            Vec3 colorValues = mc.level.getSkyColor(mc.gameRenderer.getMainCamera().getPosition(), mc.getFrameTime());
            return new Color((float) colorValues.x, (float) colorValues.y, (float) colorValues.z);
        } else
            return new Color(0, 0, 0);
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
        LevelRenderer renderer = mc.levelRenderer;
        // TODO[1.18]: Fix this
        /*
        for (RenderChunkInfo worldRenderer$LocalRenderInformationContainer : renderer.renderChunks)
        {
            CompiledChunk compiledChunk = worldRenderer$LocalRenderInformationContainer.chunk.getCompiledChunk();
            if (!compiledChunk.hasNoRenderableLayers())
            {
                // add the ChunkPos for every rendered chunk
                BlockPos bpos = worldRenderer$LocalRenderInformationContainer.chunk.getOrigin();

                loadedPos.add(new ChunkPosWrapper(bpos));
            }
        }
         */

        return loadedPos;
    }

}
