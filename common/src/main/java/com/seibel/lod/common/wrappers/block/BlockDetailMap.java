package com.seibel.lod.common.wrappers.block;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.seibel.lod.core.api.ApiShared;
import com.seibel.lod.core.util.ColorUtil;
import com.seibel.lod.core.wrapperInterfaces.block.BlockDetail;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockDetailMap
{
    public static final int FLOWER_COLOR_SCALE = 5;

    public static final Random random = new Random(0);

    //TODO: Perhaps make this not just use the first frame?
    //FIXME: Stuff is wrong.
    private static int calculateColorFromTexture(TextureAtlasSprite texture, boolean useFlowerScaling, boolean useFastLeaf) {

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

                    if (useFastLeaf) {
                        r *= a;
                        g *= a;
                        b *= a;
                        a = 1.;
                    } else if (a==0.) {
                        continue;
                    } else if (useFlowerScaling && (g+0.1<b || g+0.1<r)) {
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


    static class BlockDetailCache {
        static BlockDetailCache NULL_BLOCK_DETAIL = new BlockDetailCache();

        private static final Block[] BLOCK_TO_AVOID = {Blocks.AIR, Blocks.CAVE_AIR, Blocks.BARRIER};

        private static final Direction[] DIRECTION_ORDER = {Direction.UP, Direction.NORTH, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.DOWN};

        ConcurrentHashMap<Biome, BlockDetail> biomeDetailMap = null;
        BlockDetail blockDetail;
        BlockDetail defaultTintedDetail = null;
        boolean requireResolving;
        @SuppressWarnings("unused")
        boolean requireShade; //TODO: Add back using this in renderer
        @SuppressWarnings("unused")
        boolean scaleFlowerColor; //FIXME: Do I need to scale the tint color???
        int tintIndex;

        static boolean isBlockToBeAvoid(Block b) {
            for (Block bta : BLOCK_TO_AVOID)
                if (bta==b) return true;
            return false;
        }

        static boolean hasNoCollision(BlockState bs, BlockPos pos, LevelReader getter) {
            if (!bs.getFluidState().isEmpty() || bs.getBlock() instanceof LiquidBlock) // Is blockState a fluid?
                return false;
            if (bs.getCollisionShape(getter, pos).isEmpty())
                return true;
            return false;
        }
        static boolean hasOnlyNonFullFace(BlockState bs, BlockPos pos, LevelReader getter) {
            if (!bs.getFluidState().isEmpty() || bs.getBlock() instanceof LiquidBlock) // Is blockState a fluid?
                return false;
            VoxelShape voxelShape = bs.getShape(getter, pos);
            if (voxelShape.isEmpty()) return true;
            AABB bbox = voxelShape.bounds();
            double xWidth = (bbox.maxX - bbox.minX);
            double yWidth = (bbox.maxY - bbox.minY);
            double zWidth = (bbox.maxZ - bbox.minZ);
            return xWidth < 1 && zWidth < 1 && yWidth < 1;
        }

        static BlockDetailCache make(BlockState bs, BlockPos pos, LevelReader getter) {
            boolean noCol, nonFull, canOcclude;
            if(!bs.getFluidState().isEmpty()) {
                bs = bs.getFluidState().createLegacyBlock();
                FluidState fs = bs.getFluidState();
                fs.getType();
                noCol = false;
                nonFull = false;
                canOcclude = false;
                BlockDetailCache result = new BlockDetailCache(fs);
                //ApiShared.LOGGER.info(fs.toString()+" = ["+result+"]");
                return result;
            } else {
                if (bs.getRenderShape() != RenderShape.MODEL) return NULL_BLOCK_DETAIL;
                if (isBlockToBeAvoid(bs.getBlock())) return NULL_BLOCK_DETAIL;
                //BlocksToAvoid toAvoid = CONFIG.client().worldGenerator().getBlocksToAvoid();
                noCol = hasNoCollision(bs, pos, getter);
                nonFull = hasOnlyNonFullFace(bs, pos, getter);
                canOcclude = bs.canOcclude();
                List<BakedQuad> quads = null;
                for (Direction direction : DIRECTION_ORDER)
                {
                    quads = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(bs).getQuads(bs, direction, random);
                    if (!quads.isEmpty() && !(bs.getBlock() instanceof RotatedPillarBlock && direction == Direction.UP))
                        break;
                };
                if (quads == null || quads.isEmpty()) return NULL_BLOCK_DETAIL;
                BlockDetailCache result = new BlockDetailCache(canOcclude, noCol, nonFull, quads.get(0),
                        bs.getBlock() instanceof FlowerBlock, bs.getBlock() instanceof LeavesBlock);
                // ApiShared.LOGGER.info(bs.toString()+" = ["+result+"]");
                return result;
            }
        }

        BlockDetailCache(boolean isFullBlock, boolean noCol, boolean nonFull, BakedQuad quad, boolean useFlowerScaling, boolean useFastLeaf) {
            requireResolving = quad.isTinted();
            requireShade = quad.isShade();
            tintIndex = quad.getTintIndex();
            scaleFlowerColor = useFlowerScaling;
            blockDetail = new BlockDetail(calculateColorFromTexture(quad.getSprite(), useFlowerScaling, useFastLeaf), isFullBlock, noCol, nonFull);
            if (quad.isTinted()) biomeDetailMap = new ConcurrentHashMap<Biome, BlockDetail>();
        }

        BlockDetailCache(FluidState fluid) {
            requireResolving = true; // TODO: Maybe in the future recheck that there really is no way to see if a fluid needs tinting
            requireShade = false;
            tintIndex = 0; // Vanilla doesn't use this index currently. (Checked at 1.18.X, See BlockColors.class)
            scaleFlowerColor = false;
            TextureAtlasSprite text = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(fluid.createLegacyBlock()).getParticleIcon();
            blockDetail = new BlockDetail(calculateColorFromTexture(text, false, false), true, false, false);
            biomeDetailMap = new ConcurrentHashMap<Biome, BlockDetail>();
        }

        private BlockDetailCache()
        {
            //DUMMY CREATOR
        }

        BlockDetail getResolvedBlockDetail(BlockState bs, int x, int y, int z, LevelReader getter) {
            if (!requireResolving) return blockDetail;
            BlockPos pos = new BlockPos(x,y,z);
            Biome biome = getter.getBiome(pos);
            BlockDetail tintDetail = biomeDetailMap.get(biome);
            if (tintDetail == null) {
                if (!bs.getFluidState().isEmpty()) bs = bs.getFluidState().createLegacyBlock();
                BlockColors bc = Minecraft.getInstance().getBlockColors();
                int tintColor = bc.getColor(bs, getter, pos, tintIndex);
                tintColor = ColorUtil.multiplyARGBwithRGB(blockDetail.color, tintColor);
                tintDetail = new BlockDetail(tintColor, blockDetail.isFullBlock,
                        blockDetail.hasNoCollision, blockDetail.hasOnlyNonFullFace);
                BlockDetail tintDetailCAS = biomeDetailMap.putIfAbsent(biome, tintDetail);
                if (tintDetailCAS != null) tintDetail = tintDetailCAS;
            }
            return tintDetail;
        }

        // Note: this one won't resolve biome based or pos based colors. (Kinda like GUI block icons)
        BlockDetail getResolvedBlockDetail(BlockState bs) {
            if (!requireResolving) return blockDetail;
            if (defaultTintedDetail != null) return defaultTintedDetail;

            BlockColors bc = Minecraft.getInstance().getBlockColors();
            if (!bs.getFluidState().isEmpty()) bs = bs.getFluidState().createLegacyBlock();
            int tintColor = bc.getColor(bs, null, null, tintIndex);
            if (tintColor == -1) {
                defaultTintedDetail = blockDetail;
            }
            else {
                defaultTintedDetail = new BlockDetail(ColorUtil.multiplyARGBwithRGB(blockDetail.color, tintColor),
                        blockDetail.isFullBlock, blockDetail.hasNoCollision, blockDetail.hasOnlyNonFullFace);
            }
            return defaultTintedDetail;
        }

        @Override
        public String toString() {
            return "[BlockDetail: "+blockDetail+", RequireResolving: "+requireResolving+", requireShade: "+requireShade+", scaleFlowerColor: "+scaleFlowerColor+"]";
        }

    }



    private static ConcurrentHashMap<BlockState, BlockDetailCache> map = new ConcurrentHashMap<BlockState, BlockDetailCache>();

    private BlockDetailMap() {}

    private static BlockDetailCache getOrMakeBlockDetailCache(BlockState bs, BlockPos pos, LevelReader getter) {
        BlockDetailCache cache = map.get(bs);
        if (cache != null) return cache;
        if (bs.getFluidState().isEmpty()) {
            cache = BlockDetailCache.make(bs, pos, getter);
        } else {
            cache = BlockDetailCache.make(bs.getFluidState().createLegacyBlock(), pos, getter);
        }
        BlockDetailCache cacheCAS = map.putIfAbsent(bs, cache);
        return cacheCAS==null ? cache : cacheCAS;
    }


    // Return null means skip the block
    public static BlockDetail getBlockDetail(BlockState bs) {
        BlockDetailCache cache = getOrMakeBlockDetailCache(bs, new BlockPos(0, 0, 0), null);
        if (cache == BlockDetailCache.NULL_BLOCK_DETAIL) return null;
        return cache.getResolvedBlockDetail(bs);
    }

    // Return null means skip the block
    public static BlockDetail getBlockDetailWithCompleteTint(BlockState bs, int x, int y, int z, LevelReader tintGetter) {
        BlockDetailCache cache = getOrMakeBlockDetailCache(bs, new BlockPos(x,y,z), tintGetter);
        if (cache == BlockDetailCache.NULL_BLOCK_DETAIL) return null;
        return cache.getResolvedBlockDetail(bs, x, y, z, tintGetter);
    }
}
