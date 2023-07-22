package com.seibel.distanthorizons.common.wrappers.block;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.util.ColorUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class BlockStateWrapper implements IBlockStateWrapper
{
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    public static final BlockStateWrapper AIR = new BlockStateWrapper(null);

    public static ConcurrentHashMap<BlockState, BlockStateWrapper> cache = new ConcurrentHashMap<>();
	
	
	//==============//
	// constructors //
	//==============//
	
    public static BlockStateWrapper fromBlockState(BlockState blockState)
	{
        if (blockState == null || blockState.isAir())
        {
	        return AIR;
        }
		
        return cache.computeIfAbsent(blockState, BlockStateWrapper::new);
    }

    public final BlockState blockState;
    BlockStateWrapper(BlockState blockState)
    {
        this.blockState = blockState;
        LOGGER.trace("Created BlockStateWrapper for ["+blockState+"]");
    }
	
	
	
	//=========//
	// methods //
	//=========//
	
    @Override
    public String serialize()
	{
		if (this.blockState == null)
		{
			return "AIR";
		}
	
		return BlockState.CODEC.encodeStart(JsonOps.COMPRESSED, this.blockState).get().orThrow().toString();
	}
	
	@Override 
	public int getOpacity()
	{
		// this method isn't perfect, but works well enough for our use case
		if (this.isAir() || !this.isSolid())
		{
			// completely transparent
			return 0;
		}
		else
		{
			// completely opaque
			return 16;
		}
	}
	
	@Override
	public int getLightEmission() { return (this.blockState != null) ? this.blockState.getLightEmission() : 0; }
	
	public static BlockStateWrapper deserialize(String str) throws IOException
	{
		if (str.equals("AIR"))
		{
			return AIR;
		}
		try
		{
			return new BlockStateWrapper(
					BlockState.CODEC.decode(JsonOps.COMPRESSED, JsonParser.parseString(str)).get().orThrow().getFirst()
			);
		}
		catch (Exception e)
		{
			throw new IOException("Failed to deserialize BlockStateWrapper", e);
		}
    }

    @Override
    public boolean equals(Object obj) 
    {
        if (this == obj) 
		{
			return true;
        }
		
        if (obj == null || this.getClass() != obj.getClass())
        {
			return false;
        }
		
        BlockStateWrapper that = (BlockStateWrapper) obj;
        return Objects.equals(this.blockState, that.blockState);
    }

    @Override
    public int hashCode() { return Objects.hash(this.blockState); }
	
	
	@Override
	public Object getWrappedMcObject() { return this.blockState; }
	
	@Override
	public boolean isAir() { return this.isAir(this.blockState); }
	public boolean isAir(BlockState blockState) { return blockState == null || blockState.isAir(); }
	
	@Override
	public boolean isSolid() 
	{
        #if PRE_MC_1_20_1
        return this.blockState.getMaterial().isSolid();
        #else
        return !this.blockState.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty();
        #endif
    }
	
	@Override
	public boolean isLiquid() 
	{
		if (this.isAir())
		{
			return false;
		}
		
        #if PRE_MC_1_20_1
        return this.blockState.getMaterial().isLiquid();
        #else
        return !this.blockState.getFluidState().isEmpty();
        #endif
    }
	
}
