package com.seibel.distanthorizons.common.wrappers.block;

import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class BlockStateWrapper implements IBlockStateWrapper
{
	/** example "minecraft:plains" */
	public static final String RESOURCE_LOCATION_SEPARATOR = ":";
	/** example "minecraft:water_state_{level:0}" */
	public static final String STATE_STRING_SEPARATOR = "_STATE_";
	
	public static final BlockStateWrapper AIR = new BlockStateWrapper(null);
	
	
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    
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
	
	
	@Override
    public String serialize()
	{
		if (this.blockState == null)
		{
			return "AIR";
		}
		
		ResourceLocation resourceLocation = Registry.BLOCK.getKey(this.blockState.getBlock());
		String resourceStateString = resourceLocation.getNamespace() + RESOURCE_LOCATION_SEPARATOR + resourceLocation.getPath()
										+ STATE_STRING_SEPARATOR + serializeBlockStateProperties(this.blockState);
		return resourceStateString;
	}
	
	public static BlockStateWrapper deserialize(String resourceStateString) throws IOException
	{
		if (resourceStateString.equals("AIR") || resourceStateString.equals("")) // the empty string shouldn't normally happen, but just in case
		{
			return AIR;
		}
		
		
		
		// parse the BlockState
		int stateSeparatorIndex = resourceStateString.indexOf(STATE_STRING_SEPARATOR);
		if (stateSeparatorIndex == -1)
		{
			throw new IOException("Unable to parse BlockState out of string: ["+resourceStateString+"].");
		}
		String blockStatePropertiesString = resourceStateString.substring(stateSeparatorIndex+STATE_STRING_SEPARATOR.length());
		resourceStateString =  resourceStateString.substring(0, stateSeparatorIndex);
		
		// parse the resource location
		int resourceSeparatorIndex = resourceStateString.indexOf(RESOURCE_LOCATION_SEPARATOR);
		if (resourceSeparatorIndex == -1)
		{
			throw new IOException("Unable to parse Resource Location out of string: ["+resourceStateString+"].");
		}
		ResourceLocation resourceLocation = new ResourceLocation(resourceStateString.substring(0, resourceSeparatorIndex), resourceStateString.substring(resourceSeparatorIndex+1));
		
		
		
		// attempt to get the BlockState from all possible BlockStates
		try
		{
			Block block = Registry.BLOCK.get(resourceLocation);
			
			BlockState foundState = null;
			List<BlockState> possibleStateList = block.getStateDefinition().getPossibleStates();
			for (BlockState possibleState : possibleStateList)
			{
				String possibleStatePropertiesString = serializeBlockStateProperties(possibleState);
				if (possibleStatePropertiesString.equals(blockStatePropertiesString))
				{
					foundState = possibleState;
					break;
				}
			}
			
			// use the default if no state was found
			if (foundState == null)
			{
				LOGGER.debug("Unable to find BlockState for Block ["+resourceLocation+"] with properties: ["+blockStatePropertiesString+"].");
				foundState = block.defaultBlockState();
			}
			return new BlockStateWrapper(foundState);
		}
		catch (Exception e)
		{
			throw new IOException("Failed to deserialize the string ["+resourceStateString+"] into a BlockStateWrapper: "+e.getMessage(), e);
		}
    }
	
	/** used to compare and save BlockStates based on their properties */
	private static String serializeBlockStateProperties(BlockState blockState)
	{
		// get the property list for this block (doesn't contain this block state's values, just the names and possible values)
		java.util.Collection<net.minecraft.world.level.block.state.properties.Property<?>> blockPropertyCollection = blockState.getProperties();
		
		// alphabetically sort the list so they are always in the same order
		List<net.minecraft.world.level.block.state.properties.Property<?>> sortedBlockPropteryList = new ArrayList<>(blockPropertyCollection);
		sortedBlockPropteryList.sort((a,b) -> a.getName().compareTo(b.getName()));
		
		
		StringBuilder stringBuilder = new StringBuilder();
		for (net.minecraft.world.level.block.state.properties.Property<?> property : sortedBlockPropteryList)
		{
			String propertyName = property.getName();
			
			String value = "NULL";
			if (blockState.hasProperty(property))
			{
				value = blockState.getValue(property).toString();
			}
			
			stringBuilder.append("{");
			stringBuilder.append(propertyName).append(RESOURCE_LOCATION_SEPARATOR).append(value);
			stringBuilder.append("}");
		}
		
		return stringBuilder.toString();
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
