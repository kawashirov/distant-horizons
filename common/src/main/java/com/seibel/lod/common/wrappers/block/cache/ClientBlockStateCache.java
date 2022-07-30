package com.seibel.lod.common.wrappers.block.cache;

import com.seibel.lod.common.wrappers.McObjectConverter;
import com.seibel.lod.common.wrappers.block.*;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.objects.DHBlockPos;
import com.seibel.lod.core.util.ColorUtil;
import com.seibel.lod.core.wrapperInterfaces.world.IClientLevelWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
#if POST_MC_1_19
import net.minecraft.util.RandomSource;
#endif
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Random;

public class ClientBlockStateCache {

	#if PRE_MC_1_19
    public static final Random random = new Random(0);
	#else
    public static final RandomSource random = RandomSource.create();
	#endif

    public final BlockState state;
    public final LevelReader level;
    public final BlockPos pos;
    public ClientBlockStateCache(BlockState blockState, IClientLevelWrapper samplingLevel, DHBlockPos samplingPos) {
        state = blockState;
        level = (LevelReader) samplingLevel.unwrapLevel();
        pos = McObjectConverter.Convert(samplingPos);
        resolveColors();
    }

    boolean isColorResolved = false;
    int baseColor = 0; //TODO: Impl per-face color
    boolean needShade = true;
    boolean needPostTinting = false;
    int tintIndex = 0;


    public static final int FLOWER_COLOR_SCALE = 5;

    enum ColorMode {
        Default,
        Flower,
        Leaves;
        static ColorMode getColorMode(Block b) {
            if (b instanceof LeavesBlock) return Leaves;
            if (b instanceof FlowerBlock) return Flower;
            return Default;
        }
    }
    //TODO: Perhaps make this not just use the first frame?
    private static int calculateColorFromTexture(TextureAtlasSprite texture, ColorMode colorMode) {
        int count = 0;
        double alpha = 0;
        double red = 0;
        double green = 0;
        double blue = 0;
        int tempColor;
        {
            // textures normally use u and v instead of x and y
            for (int u = 0; u < texture.getWidth(); u++)
            {
                for (int v = 0; v < texture.getHeight(); v++)
                {
                    //note: Minecraft color format is: 0xAA BB GG RR
                    //________ DH mod color format is: 0xAA RR GG BB
                    //OpenGL RGBA format native order: 0xRR GG BB AA
                    //_ OpenGL RGBA format Java Order: 0xAA BB GG RR
                    tempColor = TextureAtlasSpriteWrapper.getPixelRGBA(texture, 0, u, v);

                    double r = ((tempColor & 0x000000FF)      )/255.;
                    double g = ((tempColor & 0x0000FF00) >>> 8)/255.;
                    double b = ((tempColor & 0x00FF0000) >>> 16)/255.;
                    double a = ((tempColor & 0xFF000000) >>> 24)/255.;
                    int scale = 1;

                    if (colorMode == ColorMode.Leaves) {
                        r *= a;
                        g *= a;
                        b *= a;
                        a = 1.;
                    } else if (a==0.) {
                        continue;
                    } else if (colorMode == ColorMode.Flower && (g+0.1<b || g+0.1<r)) {
                        scale = FLOWER_COLOR_SCALE;
                    }

                    count += scale;
                    alpha += a*a*scale;
                    red += r*r*scale;
                    green += g*g*scale;
                    blue += b*b*scale;
                }
            }
        }

        if (count == 0)
            // this block is entirely transparent
            tempColor = ColorUtil.rgbToInt(255,255,0,255);
        else
        {
            // determine the average color
            tempColor = ColorUtil.rgbToInt(
                    (int) (Math.sqrt(alpha/count)*255.),
                    (int) (Math.sqrt(red / count)*255.),
                    (int) (Math.sqrt(green / count)*255.),
                    (int) (Math.sqrt(blue / count)*255.));
        }
        // TODO: Remove this when transparency is added!
        double colorAlpha = ColorUtil.getAlpha(tempColor)/255.;
        tempColor = ColorUtil.rgbToInt(
                ColorUtil.getAlpha(tempColor),
                (int)(ColorUtil.getRed(tempColor) * colorAlpha),
                (int)(ColorUtil.getGreen(tempColor) * colorAlpha),
                (int)(ColorUtil.getBlue(tempColor) * colorAlpha)
        );
        return tempColor;
    }
    private static final Direction[] DIRECTION_ORDER = {Direction.UP, Direction.NORTH, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.DOWN};

    private void resolveColors() {
        if (isColorResolved) return;
        if (state.getFluidState().isEmpty()) {
            List<BakedQuad> quads = null;
            for (Direction direction : DIRECTION_ORDER)
            {
                quads = Minecraft.getInstance().getModelManager().getBlockModelShaper().
                        getBlockModel(state).getQuads(state, direction, random);
                if (!quads.isEmpty() &&
                        !(state.getBlock() instanceof RotatedPillarBlock && direction == Direction.UP))
                    break;
            };
            if (quads == null || quads.isEmpty()) {
                quads = Minecraft.getInstance().getModelManager().getBlockModelShaper().
                        getBlockModel(state).getQuads(state, null, random);
            }
            if (quads != null && !quads.isEmpty()) {
                needPostTinting = quads.get(0).isTinted();
                needShade = quads.get(0).isShade();
                tintIndex = quads.get(0).getTintIndex();
                baseColor = calculateColorFromTexture(
                        #if PRE_MC_1_17_1 quads.get(0).sprite,
						#else quads.get(0).getSprite(), #endif
                ColorMode.getColorMode(state.getBlock()));
            } else { // Backup method.
                needPostTinting = false;
                needShade = false;
                tintIndex = 0;
                baseColor = calculateColorFromTexture(Minecraft.getInstance().getModelManager().getBlockModelShaper().getParticleIcon(state),
                        ColorMode.getColorMode(state.getBlock()));
            }
        } else { // Liquid Block
            needPostTinting = true;
            needShade = false;
            tintIndex = 0;
            baseColor = calculateColorFromTexture(Minecraft.getInstance().getModelManager().getBlockModelShaper().getParticleIcon(state),
                    ColorMode.getColorMode(state.getBlock()));
        }
        isColorResolved = true;
    }



    private BlockAndTintGetter wrapColorResolver(LevelReader level) {
        int blendDistance = Config.Client.Graphics.Quality.lodBiomeBlending.get();
        if (blendDistance == 0) {
            return new TintGetterOverrideFast(level);
        } else {
            return new TintGetterOverrideSmooth(level, blendDistance);
        }
    }

    public int getAndResolveFaceColor(BiomeWrapper biome)
    {
        // FIXME: impl per-face colors
        if (!needPostTinting) return baseColor;
        int tintColor = Minecraft.getInstance().getBlockColors()
                .getColor(state, wrapColorResolver(level), pos, tintIndex); //FIXME: Use biome? Hack the ColorResolver?
        if (tintColor == -1) return baseColor;
        return ColorUtil.multiplyARGBwithRGB(baseColor, tintColor);
    }

}
