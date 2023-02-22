package com.seibel.lod.common.wrappers.block;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockStateWrapper;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class BlockStateWrapper implements IBlockStateWrapper
{
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    public static final BlockStateWrapper AIR = new BlockStateWrapper(null);
	
    public static ConcurrentHashMap<BlockState, BlockStateWrapper> WRAPPER_BY_BLOCK_STATE = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<BlockState, String> SERIAL_BY_BLOCK_STATE = new ConcurrentHashMap<>();
	
    public final BlockState blockState;
	
	
	
	BlockStateWrapper(BlockState blockState)
	{
		this.blockState = blockState;
		
		// blockState shouldn't be null, but just in case
		if (this.blockState != null)
		{
			SERIAL_BY_BLOCK_STATE.put(this.blockState, this.serialize());
		}
		
		//LOGGER.info("Created BlockStateWrapper for {}", blockState);
	}
	
	public static BlockStateWrapper fromBlockState(BlockState blockState)
	{
		if (blockState == null || blockState.isAir())
		{
			return AIR;
		}
		
		if (blockState.getFluidState().isEmpty())
		{
			return WRAPPER_BY_BLOCK_STATE.computeIfAbsent(blockState, BlockStateWrapper::new);
		}
		else
		{
			return WRAPPER_BY_BLOCK_STATE.computeIfAbsent(blockState.getFluidState().createLegacyBlock(), BlockStateWrapper::new);
		}
	}
	
	
	
	@Override
    public String serialize()
	{
		if (this.blockState == null)
		{
			return "AIR";
		}
		
		if (!SERIAL_BY_BLOCK_STATE.containsKey(this.blockState))
		{
			String newSerial = BlockState.CODEC.encodeStart(JsonOps.COMPRESSED, this.blockState).get().orThrow().toString();
			SERIAL_BY_BLOCK_STATE.put(this.blockState, newSerial);
		}
		String serial = SERIAL_BY_BLOCK_STATE.get(this.blockState);
		return serial;
	}
	
	public static BlockStateWrapper deserialize(String str) throws IOException
	{
		if (str.equals("AIR"))
		{
			return AIR;
		}
		try
		{
			return new BlockStateWrapper(BlockState.CODEC.decode(JsonOps.COMPRESSED, JsonParser.parseString(str)).get().orThrow().getFirst());
		}
		catch (Exception e)
		{
			throw new IOException("Failed to deserialize BlockStateWrapper", e);
		}
	}

    @Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		else if (o == null || getClass() != o.getClass())
		{
			return false;
		}
		
		BlockStateWrapper that = (BlockStateWrapper) o;
		return Objects.equals(blockState, that.blockState);
	}

    @Override
    public int hashCode() { return Objects.hash(blockState); }
	
	@Override
	public Object getWrappedMcObject_UNSAFE() { return this.blockState; }
	
	@Override
	public boolean isAir() { return this.isAir(this.blockState); }
	public boolean isAir(BlockState blockState) { return blockState == null || blockState.isAir(); }
	
}
