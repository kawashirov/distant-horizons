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

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 *
 * @author James Seibel
 * @version 11-21-2021
 */
public class ChunkWrapper implements IChunkWrapper
{

    private ChunkAccess chunk;
    private AbstractChunkPosWrapper chunkPos;

    @Override
    public int getHeight(){
        return chunk.getMaxBuildHeight();
    }

    @Override
    public boolean isPositionInWater(AbstractBlockPosWrapper blockPos)
    {
        BlockState blockState = chunk.getBlockState(((BlockPosWrapper) blockPos).getBlockPos());

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
    public IBiomeWrapper getBiome(int xRel, int yAbs, int zRel)
    {
        return BiomeWrapper.getBiomeWrapper(chunk.getBiomes().getNoiseBiome(xRel >> 2, yAbs >> 2, zRel >> 2));
    }

    @Override
    public IBlockColorWrapper getBlockColorWrapper(AbstractBlockPosWrapper blockPos)
    {
        return BlockColorWrapper.getBlockColorWrapper(chunk.getBlockState(((BlockPosWrapper) blockPos).getBlockPos()), blockPos);
    }

    @Override
    public IBlockShapeWrapper getBlockShapeWrapper(AbstractBlockPosWrapper blockPos)
    {
        return BlockShapeWrapper.getBlockShapeWrapper(chunk.getBlockState(((BlockPosWrapper) blockPos).getBlockPos()).getBlock(), this, blockPos);
    }

    public ChunkWrapper(ChunkAccess chunk)
    {
        this.chunk = chunk;
        this.chunkPos = new ChunkPosWrapper(chunk.getPos());
    }

    public ChunkAccess getChunk(){
        return chunk;
    }
    @Override
    public AbstractChunkPosWrapper getPos()
    {
        return chunkPos;
    }

    @Override
    public boolean isLightCorrect(){
        return chunk.isLightCorrect();
    }

    public boolean
    isWaterLogged(BlockPosWrapper blockPos)
    {
        BlockState blockState = chunk.getBlockState(blockPos.getBlockPos());

//		//This type of block is always in water
//		if((blockState.getBlock() instanceof ILiquidContainer) && !(blockState.getBlock() instanceof IWaterLoggable))
//			return true;

        //This type of block could be in water
        if(blockState.getOptionalValue(BlockStateProperties.WATERLOGGED).isPresent() && blockState.getOptionalValue(BlockStateProperties.WATERLOGGED).get())
            return true;

        return false;
    }

    public int getEmittedBrightness(BlockPosWrapper blockPos)
    {
        return chunk.getLightEmission(blockPos.getBlockPos());
    }

    @Override
    public boolean isWaterLogged(AbstractBlockPosWrapper blockPos)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getEmittedBrightness(AbstractBlockPosWrapper blockPos)
    {
        // TODO Auto-generated method stub
        return 0;
    }
}
