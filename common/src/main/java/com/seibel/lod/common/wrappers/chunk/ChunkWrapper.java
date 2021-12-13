package com.seibel.lod.common.wrappers.chunk;

import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockColorWrapper;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockShapeWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.lod.common.wrappers.WrapperUtil;
import com.seibel.lod.common.wrappers.block.BlockColorWrapper;
import com.seibel.lod.common.wrappers.block.BlockPosWrapper;
import com.seibel.lod.common.wrappers.block.BlockShapeWrapper;
import com.seibel.lod.common.wrappers.world.BiomeWrapper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 *
 * @author James Seibel
 * @version 11-21-2021
 */
public class ChunkWrapper implements IChunkWrapper
{
    private ChunkAccess chunk;
    private final int CHUNK_SECTION_SHIFT = 4;
    private final int CHUNK_SECTION_MASK = 0b1111;
    private final int CHUNK_SIZE_SHIFT = 4;
    private final int CHUNK_SIZE_MASK = 0b1111;

    @Override
    public int getHeight(){
        return chunk.getMaxBuildHeight();
    }

    @Override
    public boolean isPositionInWater(int x, int y, int z)
    {
        BlockState blockState = chunk.getSections()[y >> CHUNK_SECTION_SHIFT].getBlockState(x & CHUNK_SIZE_MASK, y & CHUNK_SECTION_MASK, z & CHUNK_SIZE_MASK);

        //This type of block is always in water
        if((blockState.getBlock() instanceof LiquidBlock))// && !(blockState.getBlock() instanceof IWaterLoggable))
            return true;

        //This type of block could be in water
        if(blockState.getOptionalValue(BlockStateProperties.WATERLOGGED).isPresent() && blockState.getOptionalValue(BlockStateProperties.WATERLOGGED).get())
            return true;

        return false;
    }

    @Override
    public int getHeightMapValue(int xRel, int zRel)
    {
        return chunk.getOrCreateHeightmapUnprimed(WrapperUtil.DEFAULT_HEIGHTMAP).getFirstAvailable(xRel, zRel);
    }

    @Override
    public IBiomeWrapper getBiome(int x, int y, int z)
    {
        return BiomeWrapper.getBiomeWrapper(chunk.getBiomes().getNoiseBiome((x & CHUNK_SIZE_MASK) >> 2, y >> 2, (z & CHUNK_SIZE_MASK) >> 2));
    }

    @Override
    public IBlockColorWrapper getBlockColorWrapper(int x, int y, int z)
    {
        Block block = chunk.getSections()[y >> CHUNK_SECTION_SHIFT].getBlockState(x & CHUNK_SIZE_MASK, y & CHUNK_SECTION_MASK, z & CHUNK_SIZE_MASK).getBlock();
        return BlockColorWrapper.getBlockColorWrapper(block);
    }

    @Override
    public IBlockShapeWrapper getBlockShapeWrapper(int x, int y, int z)
    {
        LevelChunkSection section = chunk.getSections()[y >> CHUNK_SECTION_SHIFT];
        if (section == null) return null;
        Block block = section.getBlockState(x & CHUNK_SIZE_MASK, y & CHUNK_SECTION_MASK, z & CHUNK_SIZE_MASK).getBlock();
        return BlockShapeWrapper.getBlockShapeWrapper(block, this, x, y, z);
    }

    public ChunkWrapper(ChunkAccess chunk)
    {
        this.chunk = chunk;
    }

    public ChunkAccess getChunk() {
        return chunk;
    }

    @Override
    public int getChunkPosX(){
        return chunk.getPos().x;
    }

    @Override
    public int getChunkPosZ(){
        return chunk.getPos().z;
    }

    @Override
    public int getRegionPosX(){
        return chunk.getPos().getRegionX();
    }

    @Override
    public int getRegionPosZ(){
        return chunk.getPos().getRegionZ();
    }

    @Override
    public int getMaxY(int x, int z) {
        return chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    @Override
    public int getMaxX(){
        return chunk.getPos().getMaxBlockX();
    }
    @Override
    public int getMaxZ(){
        return chunk.getPos().getMaxBlockZ();
    }
    @Override
    public int getMinX(){
        return chunk.getPos().getMinBlockX();
    }
    @Override
    public int getMinZ() {
        return chunk.getPos().getMinBlockZ();
    }

    @Override
    public boolean isLightCorrect(){
        return chunk.isLightCorrect();
    }

    public boolean isWaterLogged(int x, int y, int z)
    {
        LevelChunkSection section = chunk.getSections()[y >> CHUNK_SECTION_SHIFT];
        if (section == null) return false;
        BlockState blockState = section.getBlockState(x & CHUNK_SIZE_MASK, y & CHUNK_SECTION_MASK, z & CHUNK_SIZE_MASK);

		//This type of block is always in water
        return (!(blockState.getBlock() instanceof LiquidBlockContainer) && (blockState.getBlock() instanceof SimpleWaterloggedBlock))
                && (blockState.hasProperty(BlockStateProperties.WATERLOGGED) && blockState.getValue(BlockStateProperties.WATERLOGGED));
    }

    @Override
    public int getEmittedBrightness(int x, int y, int z)
    {
        BlockPos blockPos = new BlockPos(x,y,z);

        return chunk.getLightEmission(blockPos);
    }
}
