package com.seibel.lod.common.wrappers.block;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockStateWrapper;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class BlockStateWrapper implements IBlockStateWrapper
{
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    public static final BlockStateWrapper AIR = new BlockStateWrapper(null);

    public static ConcurrentHashMap<BlockState, BlockStateWrapper> cache = new ConcurrentHashMap<>();

    public static BlockStateWrapper fromBlockState(BlockState blockState)
	{
        if (blockState == null || blockState.isAir())
			return AIR;
		
        if (blockState.getFluidState().isEmpty())
            return cache.computeIfAbsent(blockState, BlockStateWrapper::new);
        else
            return cache.computeIfAbsent(blockState.getFluidState().createLegacyBlock(), BlockStateWrapper::new);
    }

    public final BlockState blockState;
    BlockStateWrapper(BlockState blockState) {
        this.blockState = blockState;
        //LOGGER.info("Created BlockStateWrapper for {}", blockState);
    }

    @Override
    public String serialize() {
        if (blockState == null) {
            return "AIR";
        }
        return BlockState.CODEC.encodeStart(JsonOps.COMPRESSED, blockState).get().orThrow().toString();
    }

    public static BlockStateWrapper deserialize(String str) throws IOException {
        if (str.equals("AIR")) {
            return AIR;
        }
        try {
            return new BlockStateWrapper(
                    BlockState.CODEC.decode(JsonOps.COMPRESSED, JsonParser.parseString(str)).get().orThrow().getFirst()
            );
        } catch (Exception e) {
            throw new IOException("Failed to deserialize BlockStateWrapper", e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockStateWrapper that = (BlockStateWrapper) o;
        return Objects.equals(blockState, that.blockState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockState);
    }
	
	
	@Override
	public Object getWrappedMcObject_UNSAFE() { return this.blockState; }
	
	@Override
	public boolean isAir() { return this.isAir(this.blockState); }
	public boolean isAir(BlockState blockState) { return blockState == null || blockState.isAir(); }
	
	
	
	
	
	
	
}
