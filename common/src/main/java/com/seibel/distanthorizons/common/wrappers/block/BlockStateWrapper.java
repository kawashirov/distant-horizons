/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2023 James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.distanthorizons.common.wrappers.block;

import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;

import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

#if MC_1_16_5 || MC_1_17_1
import net.minecraft.core.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
#elif MC_1_18_2 || MC_1_19_2
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
#else
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.EmptyBlockGetter;
#endif

public class BlockStateWrapper implements IBlockStateWrapper
{
	/** example "minecraft:water" */
	public static final String RESOURCE_LOCATION_SEPARATOR = ":";
	/** example "minecraft:water_STATE_{level:0}" */
	public static final String STATE_STRING_SEPARATOR = "_STATE_";
	
	
	// must be defined before AIR, otherwise a null pointer will be thrown
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
    public static final ConcurrentHashMap<BlockState, BlockStateWrapper> WRAPPER_BY_BLOCK_STATE = new ConcurrentHashMap<>();
	
	public static final String AIR_STRING = "AIR";
	public static final BlockStateWrapper AIR = new BlockStateWrapper(null, null);
	
	// TODO: Make this changeable through the config
	public static final String[] RENDERER_IGNORED_BLOCKS_RESOURCE_LOCATIONS = { AIR_STRING, "minecraft:barrier", "minecraft:structure_void", "minecraft:light", "minecraft:tripwire" };
	public static HashSet<IBlockStateWrapper> rendererIgnoredBlocks = null;
	
	/** keep track of broken blocks so we don't log every time */
	private static final HashSet<ResourceLocation> BrokenResourceLocations = new HashSet<>();
	
	
	
	// properties //
	
	public final BlockState blockState;
	/** technically final, but since it requires a method call to generate it can't be marked as such */
	private String serialString;
	
	
	
	//==============//
	// constructors //
	//==============//
	
	public static BlockStateWrapper fromBlockState(BlockState blockState, ILevelWrapper levelWrapper)
	{
		if (blockState == null || blockState.isAir())
		{
			return AIR;
		}
		
		
		if (WRAPPER_BY_BLOCK_STATE.containsKey(blockState))
		{
			return WRAPPER_BY_BLOCK_STATE.get(blockState);
		}
		else
		{
			BlockStateWrapper newWrapper = new BlockStateWrapper(blockState, levelWrapper);
			WRAPPER_BY_BLOCK_STATE.put(blockState, newWrapper);
			return newWrapper;
		}
	}
	
	private BlockStateWrapper(BlockState blockState, ILevelWrapper levelWrapper)
	{
		this.blockState = blockState;
		this.serialString = this.serialize(levelWrapper);
		LOGGER.trace("Created BlockStateWrapper ["+this.serialString+"] for ["+blockState+"]");
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
					BlockStateWrapper newBlockToIgnore = BlockStateWrapper.fromBlockState(blockState, levelWrapper);
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
			return FULLY_TRANSPARENT;
		}
		else
		{
			// completely opaque
			return FULLY_OPAQUE;
		}
	}
	
	@Override
	public int getLightEmission() { return (this.blockState != null) ? this.blockState.getLightEmission() : 0; }
	
	@Override
	public String getSerialString() { return this.serialString; }
	
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
		return Objects.equals(this.getSerialString(), that.getSerialString());
	}
	
	@Override
	public int hashCode() { return Objects.hash(this.getSerialString()); }
	
	
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
		return this.blockState.getMaterial().isLiquid() || !this.blockState.getFluidState().isEmpty();
        #else
		return !this.blockState.getFluidState().isEmpty();
        #endif
	}
	
	@Override
	public String toString() { return this.getSerialString(); }
	
	
	
	//=======================//
	// serialization methods //
	//=======================//
	
	private String serialize(ILevelWrapper levelWrapper)
	{
		if (this.blockState == null)
		{
			return AIR_STRING;
		}
		
		
		
		// older versions of MC have a static registry
		#if !(MC_1_16_5 || MC_1_17_1)
		Level level = (Level)levelWrapper.getWrappedMcObject();
		net.minecraft.core.RegistryAccess registryAccess = level.registryAccess();
		#endif
		
		ResourceLocation resourceLocation;
		#if MC_1_16_5 || MC_1_17_1
		resourceLocation = Registry.BLOCK.getKey(this.blockState.getBlock());
		#elif MC_1_18_2 || MC_1_19_2
		resourceLocation = registryAccess.registryOrThrow(Registry.BLOCK_REGISTRY).getKey(this.blockState.getBlock());
		#else
		resourceLocation = registryAccess.registryOrThrow(Registries.BLOCK).getKey(this.blockState.getBlock());
		#endif
		
		
		
		if (resourceLocation == null)
		{
			LOGGER.warn("No ResourceLocation found, unable to serialize: " + this.blockState);
			return AIR_STRING;
		}
		
		this.serialString = resourceLocation.getNamespace() + RESOURCE_LOCATION_SEPARATOR + resourceLocation.getPath()
				+ STATE_STRING_SEPARATOR + serializeBlockStateProperties(this.blockState);
		
		return this.serialString;
	}
	
	
	/** will only work if a level is currently loaded */
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
			
			#if !(MC_1_16_5 || MC_1_17_1)
			// use the given level if possible, otherwise try using the currently loaded one 
			Level level = (levelWrapper != null ? (Level)levelWrapper.getWrappedMcObject() : null);
			level = (level == null ? Minecraft.getInstance().level : level);
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
				if (!BrokenResourceLocations.contains(resourceLocation))
				{
					BrokenResourceLocations.add(resourceLocation);
					LOGGER.warn("Unable to find BlockState with the resourceLocation [" + resourceLocation + "] and properties: [" + blockStatePropertiesString + "]. Air will be used instead, some data may be lost.");
				}
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
					if (!BrokenResourceLocations.contains(resourceLocation))
					{
						BrokenResourceLocations.add(resourceLocation);
						LOGGER.warn("Unable to find BlockState for Block [" + resourceLocation + "] with properties: [" + blockStatePropertiesString + "]. Using the default block state.");
					}
				}
				
				foundState = block.defaultBlockState();
			}
			return new BlockStateWrapper(foundState, levelWrapper);
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
	
	
	
}
