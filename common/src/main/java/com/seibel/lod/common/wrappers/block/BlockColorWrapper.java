package com.seibel.lod.common.wrappers.block;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.seibel.lod.common.wrappers.minecraft.MinecraftWrapper;
import com.seibel.lod.core.util.ColorUtil;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockColorWrapper;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;


/**
 * @author James Seibel
 * @version 11-21-2021
 */
public class BlockColorWrapper implements IBlockColorWrapper
{
    //set of block which require tint
    public static final ConcurrentMap<Block, BlockColorWrapper> blockColorWrapperMap = new ConcurrentHashMap<>();
    //    public static final ModelDataMap dataMap = new ModelDataMap.Builder().build();
    public static final AbstractBlockPosWrapper blockPos = new BlockPosWrapper(0, 0, 0);
    public static final Random random = new Random(0);
    //public static BlockColourWrapper WATER_COLOR = getBlockColorWrapper(Blocks.WATER);
    public static final Direction[] directions = new Direction[] { Direction.UP, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.DOWN };

    private final Block block;
    private int color;
    private boolean isColored;
    private boolean toTint;
    private boolean foliageTint;
    private boolean grassTint;
    private boolean waterTint;


    /**Constructor only require for the block instance we are wrapping**/
    public BlockColorWrapper(Block block)
    {
        this.block = block;
        this.color = 0;
        this.isColored = true;
        this.toTint = false;
        this.foliageTint = false;
        this.grassTint = false;
        this.waterTint = false;
        setupColorAndTint();
		/*StringBuilder s = new StringBuilder();
		s.append(block + "\n"
								   + Integer.toHexString(
				Minecraft.getInstance().getBlockColors().createDefault().getColor(
						block.defaultBlockState(),
						(World) MinecraftWrapper.INSTANCE.getWrappedServerLevel().getLevel(),
						blockPosWrapper.getBlockPos())) + "\n"
		);
		for(Property x : Minecraft.getInstance().getBlockColors().getColoringProperties(block))
			s.append(x.getName() + " " + x.getPossibleValues() + '\n');
		System.out.println(s);*/
        //System.out.println(block + " color " + Integer.toHexString(color) + " to tint " + toTint + " folliageTint " + folliageTint + " grassTint " + grassTint + " waterTint " + waterTint);
    }


    /**
     * this return a wrapper of the block in input
     * @param block object of the block to wrap
     */
    public static IBlockColorWrapper getBlockColorWrapper(Block block)
    {
        //first we check if the block has already been wrapped
    	BlockColorWrapper colorWrapper = blockColorWrapperMap.get(block);
        if (colorWrapper != null)
            return colorWrapper;

        //if it hasn't been created yet, we create it and save it in the map
        colorWrapper = new BlockColorWrapper(block);
        BlockColorWrapper colorWrapperCAS = blockColorWrapperMap.putIfAbsent(block, colorWrapper);
        //we return the newly created wrapper
        return colorWrapperCAS==null ? colorWrapper : colorWrapperCAS;
    }

    /**
     * Generate the color of the given block from its texture
     * and store it for later use.
     */
    private void setupColorAndTint()
    {
        BlockState blockState = block.defaultBlockState();
        //BlockPosWrapper blockPosWrapper = new BlockPosWrapper();
        MinecraftWrapper mc = MinecraftWrapper.INSTANCE;
        TextureAtlasSprite texture;
        List<BakedQuad> quads = null;

        //boolean isTinted = false;
        //int listSize = 0;

        // first step is to check if this block has a tinted face
        //for (Direction direction : directions)
        //{
        //    quads = mc.getModelManager().getBlockModelShaper().getBlockModel(block.defaultBlockState()).getQuads(blockState, direction, random);
        //    listSize = Math.max(listSize, quads.size());
        //    for (BakedQuad bakedQuad : quads)
        //    {
        //        isTinted |= bakedQuad.isTinted();
        //    }
        //}

        //if it contains a tinted face then we store this block in the toTint set
        //if (isTinted)
        //    this.toTint = true;

        //now we get the first non-empty face
        for (Direction direction : directions)
        {
            quads = mc.getModelManager().getBlockModelShaper().getBlockModel(block.defaultBlockState()).getQuads(blockState, direction, random);
            if (!quads.isEmpty() && !(block instanceof RotatedPillarBlock && direction == Direction.UP))
                break;
        }

        //the quads list is not empty we extract the first one
        if (!quads.isEmpty())
        {
            isColored = true;
            texture = quads.get(0).getSprite();
        }
        else
        {
            isColored = true;
            texture = mc.getModelManager().getBlockModelShaper().getParticleIcon(block.defaultBlockState());
        }

        int count = 0;
        int alpha = 0;
        int red = 0;
        int green = 0;
        int blue = 0;
        int numberOfGreyPixel = 0;
        int tempColor;
        int colorMultiplier;

        // generate the block's color
//        for (int frameIndex = 0; frameIndex < texture.getFrameCount(); frameIndex++)
        boolean lookForTint = grassInstance() || leavesInstance();
    
        int frameIndex = 0; // TODO
        {
            // textures normally use u and v instead of x and y
            for (int u = 0; u < texture.getWidth(); u++)
            {
                for (int v = 0; v < texture.getHeight(); v++)
                {
                    tempColor = TextureAtlasSpriteWrapper.getPixelRGBA(texture, frameIndex, u, v);

                    if (ColorUtil.getAlpha(TextureAtlasSpriteWrapper.getPixelRGBA(texture, frameIndex, u, v)) == 0)
                        continue;

                    if (lookForTint)
                    {
                        // determine if this pixel is gray
                        int colorMax = Math.max(Math.max(ColorUtil.getBlue(tempColor), ColorUtil.getGreen(tempColor)), ColorUtil.getRed(tempColor));
                        int colorMin = 16 + Math.min(Math.min(ColorUtil.getBlue(tempColor), ColorUtil.getGreen(tempColor)), ColorUtil.getRed(tempColor));
                        boolean isGray = colorMax < colorMin;
                        if (isGray)
                            numberOfGreyPixel++;
                    }


                    // for flowers, weight their non-green color higher
                    if (block instanceof FlowerBlock && (!(ColorUtil.getGreen(tempColor) > (ColorUtil.getBlue(tempColor) + 30)) || !(ColorUtil.getGreen(tempColor) > (ColorUtil.getRed(tempColor) + 30))))
                        colorMultiplier = 5;
                    else
                        colorMultiplier = 1;


                    // add to the running averages
                    count += colorMultiplier;
                    alpha += ColorUtil.getAlpha(tempColor) * ColorUtil.getAlpha(tempColor) * colorMultiplier;
                    red += ColorUtil.getBlue(tempColor) * ColorUtil.getBlue(tempColor) * colorMultiplier;
                    green += ColorUtil.getGreen(tempColor) * ColorUtil.getGreen(tempColor) * colorMultiplier;
                    blue += ColorUtil.getRed(tempColor) * ColorUtil.getRed(tempColor) * colorMultiplier;
                }
            }
        }


        if (count == 0)
            // this block is entirely transparent
            tempColor = 0;
        else
        {
            // determine the average color
            tempColor = ColorUtil.rgbToInt(
                    (int) Math.sqrt(alpha / count),
                    (int) Math.sqrt(red / count),
                    (int) Math.sqrt(green / count),
                    (int) Math.sqrt(blue / count));
        }

        // determine if this block should use the biome color tint
        if ((lookForTint && (float) numberOfGreyPixel / count > 0.75f) || waterInstance())
            this.toTint = true;

        // we check which kind of tint we need to apply
        this.grassTint = grassInstance() && toTint;

        this.foliageTint = leavesInstance() && toTint;

        this.waterTint = waterInstance();
        
        //hardcoded leaves
        if (block == Blocks.SPRUCE_LEAVES)
            color = ColorUtil.multiplyRGBcolors(tempColor, 0xFF619961);
        else if (block == Blocks.BIRCH_LEAVES)
            color = ColorUtil.multiplyRGBcolors(tempColor, 0xFF80A755);
        else
            color = tempColor;
    }

    /** determine if the given block should use the biome's grass color */
    private boolean grassInstance()
    {
        return block instanceof GrassBlock
                || block instanceof BushBlock
//                || block instanceof IGrowable
//                || block instanceof AbstractPlantBlock
//                || block instanceof AbstractTopPlantBlock
                || block instanceof TallGrassBlock;
    }

    /** determine if the given block should use the biome's foliage color */
    private boolean leavesInstance()
    {
        return (block instanceof LeavesBlock && block != Blocks.SPRUCE_LEAVES && block != Blocks.BIRCH_LEAVES/* && block != Blocks.AZALEA_LEAVES && block != Blocks.FLOWERING_AZALEA_LEAVES*/)
                || block == Blocks.VINE
                || block == Blocks.SUGAR_CANE;
    }

    /** determine if the given block should use the biome's foliage color */
    private boolean waterInstance()
    {
        return block == Blocks.WATER;
    }

    @Override
    public String getName(){
        return block.getName().toString();
    }

//--------------//
//Colors getters//
//--------------//

    @Override
    public boolean hasColor()
    {
        return isColored;
    }

    @Override
    public int getColor()
    {
        return color;
    }

//------------//
//Tint getters//
//------------//


    @Override
    public boolean hasTint()
    {
        return toTint;
    }

    @Override
    public boolean hasGrassTint()
    {
        return grassTint;
    }

    @Override
    public boolean hasFolliageTint()
    {
        return foliageTint;
    }

    @Override
    public boolean hasWaterTint()
    {
        return waterTint;
    }




    @Override public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof BlockColorWrapper))
            return false;
        BlockColorWrapper that = (BlockColorWrapper) o;
        return Objects.equals(block, that.block);
    }

    @Override public int hashCode()
    {
        return Objects.hash(block);
    }
}
