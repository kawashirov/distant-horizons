package com.seibel.distanthorizons.common.wrappers.block;

import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;

import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

#if MC_1_16_5 || MC_1_17_1
import net.minecraft.core.Registry;
#elif MC_1_18_2 || MC_1_19_2
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
#else
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.EmptyBlockGetter;
#endif

public class BlockStateWrapper implements IBlockStateWrapper
{
	/** example "minecraft:plains" */
	public static final String RESOURCE_LOCATION_SEPARATOR = ":";
	/** example "minecraft:water_STATE_{level:0}" */
	public static final String STATE_STRING_SEPARATOR = "_STATE_";
	
	
	// must be defined before AIR, otherwise a null pointer will be thrown
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
    public static final ConcurrentHashMap<BlockState, BlockStateWrapper> WRAPPER_BY_BLOCK_STATE = new ConcurrentHashMap<>();
	
	public static String AIR_STRING = "AIR";
	public static final BlockStateWrapper AIR = new BlockStateWrapper(null);
	
	public static final String[] RENDERER_IGNORED_BLOCKS_RESOURCE_LOCATIONS = { AIR_STRING, "minecraft:barrier", "minecraft:structure_void", "minecraft:light" };
	
	public static HashSet<IBlockStateWrapper> rendererIgnoredBlocks = null;
	
	
	
	public final BlockState blockState;
	
	/**
	 * Cached so it can be quickly used as a semi-stable hashing method. <br>
	 * This may also fix the issue where we can serialize and save after a level has been shut down.
	 */
	private String serializationResult = null;
	
	
	
	//==============//
	// constructors //
	//==============//
	
	public static BlockStateWrapper fromBlockState(BlockState blockState)
	{
		if (blockState == null || blockState.isAir())
		{
			return AIR;
		}
		
		return WRAPPER_BY_BLOCK_STATE.computeIfAbsent(blockState, newBlockState -> new BlockStateWrapper(newBlockState));
	}
	
	private BlockStateWrapper(BlockState blockState) 
	{
		this.blockState = blockState;
		LOGGER.trace("Created BlockStateWrapper for ["+blockState+"]");
	}
	
	
	
	//================//
	// helper methods //
	//================//
	
	/** 
	 * Requires a {@link ILevelWrapper} since {@link BlockStateWrapper#deserialize(String,ILevelWrapper)} also requires one. 
	 * This way the method won't accidentally be called before the deserialization can be completed.
	 */
	public static HashSet<IBlockStateWrapper> getRendererIgnoredBlocks(ILevelWrapper levelWrapper)
	{
		// use the cached version if possible
		if (rendererIgnoredBlocks != null)
		{
			return rendererIgnoredBlocks;
		}
		
		
		// deserialize each of the given resource locations
		HashSet<IBlockStateWrapper> blockStateWrappers = new HashSet<>();
		for (String blockResourceLocation : RENDERER_IGNORED_BLOCKS_RESOURCE_LOCATIONS)
		{
			try
			{
				BlockStateWrapper DefaultBlockStateToIgnore = (BlockStateWrapper) deserialize(blockResourceLocation, levelWrapper);
				blockStateWrappers.add(DefaultBlockStateToIgnore);
				
				if (DefaultBlockStateToIgnore == AIR)
				{
					continue;
				}
				
				// add all possible blockstates (to account for light blocks with different light values and such)
				List<BlockState> blockStatesToIgnore = DefaultBlockStateToIgnore.blockState.getBlock().getStateDefinition().getPossibleStates();
				for (BlockState blockState : blockStatesToIgnore)
				{
					BlockStateWrapper newBlockToIgnore = BlockStateWrapper.fromBlockState(blockState);
					blockStateWrappers.add(newBlockToIgnore);
				}
			}
			catch (IOException e)
			{
				LOGGER.warn("Unable to deserialize rendererIgnoredBlock with the resource location: ["+blockResourceLocation+"]. Error: "+e.getMessage(), e);
			}
		}
		
		rendererIgnoredBlocks = blockStateWrappers;
		return rendererIgnoredBlocks;
	}
	
	
	
	//=================//
	// wrapper methods //
	//=================//
	
	@Override
	public int getOpacity()
	{
		// this method isn't perfect, but works well enough for our use case
		if (this.isAir() || !this.blockState.canOcclude())
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
	public String serialize() { return this.serialize(null); }
	public String serialize(ILevelWrapper levelWrapper)
	{
		// the serialization result can be quickly used as a semi-stable hashing method, so it needs to be cached for speed
		if (this.serializationResult != null)
		{
			return this.serializationResult;
		}
		
		if (this.blockState == null)
		{
			return AIR_STRING;
		}
		
		
		
		#if MC_1_18_2 || MC_1_19_2 || MC_1_20_1
		// use the given level if possible, otherwise try using the currently loaded one 
		Level level = (levelWrapper != null ? (Level)levelWrapper.getWrappedMcObject() : null);
		level = (level == null ? Minecraft.getInstance().level : null);
		#endif
		
		ResourceLocation resourceLocation;
		#if MC_1_16_5 || MC_1_17_1
		resourceLocation = Registry.BLOCK.getKey(this.blockState.getBlock());
		#elif MC_1_18_2 || MC_1_19_2
		net.minecraft.core.RegistryAccess registryAccess = level.registryAccess();
		resourceLocation = registryAccess.registryOrThrow(Registry.BLOCK_REGISTRY).getKey(this.blockState.getBlock());
		#else
		net.minecraft.core.RegistryAccess registryAccess = level.registryAccess();
		resourceLocation = registryAccess.registryOrThrow(Registries.BLOCK).getKey(this.blockState.getBlock());
		#endif
		
		if (resourceLocation == null)
		{
			LOGGER.warn("unable to serialize: " + this.blockState);
		}
		
		this.serializationResult = resourceLocation.getNamespace() + RESOURCE_LOCATION_SEPARATOR + resourceLocation.getPath()
				+ STATE_STRING_SEPARATOR + serializeBlockStateProperties(this.blockState);
		
		return this.serializationResult;
	}
	
	
	/** will only work if a level is currently loaded */
	public static IBlockStateWrapper deserialize(String resourceStateString) throws IOException { return deserialize(resourceStateString, null); }
	public static IBlockStateWrapper deserialize(String resourceStateString, ILevelWrapper levelWrapper) throws IOException
	{
		if (resourceStateString.equals(AIR_STRING) || resourceStateString.equals("")) // the empty string shouldn't normally happen, but just in case
		{
			return AIR;
		}
		
		
		
		// try to parse out the BlockState
		String blockStatePropertiesString = null; // will be null if no properties were included
		int stateSeparatorIndex = resourceStateString.indexOf(STATE_STRING_SEPARATOR);
		if (stateSeparatorIndex != -1)
		{
			// blockstate properties found
			blockStatePropertiesString = resourceStateString.substring(stateSeparatorIndex + STATE_STRING_SEPARATOR.length());
			resourceStateString = resourceStateString.substring(0, stateSeparatorIndex);
		}
		
		// parse the resource location
		int resourceSeparatorIndex = resourceStateString.indexOf(RESOURCE_LOCATION_SEPARATOR);
		if (resourceSeparatorIndex == -1)
		{
			throw new IOException("Unable to parse Resource Location out of string: [" + resourceStateString + "].");
		}
		ResourceLocation resourceLocation = new ResourceLocation(resourceStateString.substring(0, resourceSeparatorIndex), resourceStateString.substring(resourceSeparatorIndex + 1));
		
		
		
		// attempt to get the BlockState from all possible BlockStates
		try
		{
			
			#if MC_1_18_2 || MC_1_19_2 || MC_1_20_1
			// use the given level if possible, otherwise try using the currently loaded one 
			Level level = (levelWrapper != null ? (Level)levelWrapper.getWrappedMcObject() : null);
			level = (level == null ? Minecraft.getInstance().level : null);
			#endif
			
			Block block;
			#if MC_1_16_5 || MC_1_17_1
			block = Registry.BLOCK.get(resourceLocation);
			#elif MC_1_18_2 || MC_1_19_2
			net.minecraft.core.RegistryAccess registryAccess = level.registryAccess();
			block = registryAccess.registryOrThrow(Registry.BLOCK_REGISTRY).get(resourceLocation);
			#else
			net.minecraft.core.RegistryAccess registryAccess = level.registryAccess();
			block = registryAccess.registryOrThrow(Registries.BLOCK).get(resourceLocation);
			#endif
			
			
			if (block == null)
			{
				// shouldn't normally happen, but here to make the compiler happy
				LOGGER.warn("Unable to find BlockState with the resourceLocation [" + resourceLocation + "] and properties: [" + blockStatePropertiesString + "]. Air will be used instead, some data may be lost.");
				return AIR;
			}
			
			
			// attempt to find the blockstate from all possibilities
			BlockState foundState = null;
			if (blockStatePropertiesString != null)
			{
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
			}
			
			// use the default if no state was found or given
			if (foundState == null)
			{
				if (blockStatePropertiesString != null)
				{
					// we should have found a blockstate, but didn't
					LOGGER.warn("Unable to find BlockState for Block [" + resourceLocation + "] with properties: [" + blockStatePropertiesString + "].");
				}
				
				foundState = block.defaultBlockState();
			}
			return new BlockStateWrapper(foundState);
		}
		catch (Exception e)
		{
			throw new IOException("Failed to deserialize the string [" + resourceStateString + "] into a BlockStateWrapper: " + e.getMessage(), e);
		}
	}
	
	/** used to compare and save BlockStates based on their properties */
	private static String serializeBlockStateProperties(BlockState blockState)
	{
		// get the property list for this block (doesn't contain this block state's values, just the names and possible values)
		java.util.Collection<net.minecraft.world.level.block.state.properties.Property<?>> blockPropertyCollection = blockState.getProperties();
		
		// alphabetically sort the list so they are always in the same order
		List<net.minecraft.world.level.block.state.properties.Property<?>> sortedBlockPropteryList = new ArrayList<>(blockPropertyCollection);
		sortedBlockPropteryList.sort((a, b) -> a.getName().compareTo(b.getName()));
		
		
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
		// the serialized value is used so we can test the contents instead of the references
		return Objects.equals(this.serialize(), that.serialize());
	}
	
	@Override
	public int hashCode() { return Objects.hash(this.serialize()); }
	
	
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
	
	@Override
	public String toString() { return this.serialize(); }
	
}
