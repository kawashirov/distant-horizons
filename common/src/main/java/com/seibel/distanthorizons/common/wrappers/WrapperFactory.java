/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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

package com.seibel.distanthorizons.common.wrappers;

import com.seibel.distanthorizons.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.distanthorizons.common.wrappers.block.BiomeWrapper;
import com.seibel.distanthorizons.common.wrappers.block.BlockStateWrapper;
import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.level.IDhServerLevel;
import com.seibel.distanthorizons.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.worldGeneration.AbstractBatchGenerationEnvironmentWrapper;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.io.IOException;
import java.util.HashMap;

/**
 * This handles creating abstract wrapper objects.
 *
 * @author James Seibel
 * @version 2022-12-5
 */
public class WrapperFactory implements IWrapperFactory
{
	public static final WrapperFactory INSTANCE = new WrapperFactory();
	
	
	
	
	@Override
	public AbstractBatchGenerationEnvironmentWrapper createBatchGenerator(IDhLevel targetLevel)
	{
		if (targetLevel instanceof IDhServerLevel)
		{
			return new BatchGenerationEnvironment((IDhServerLevel) targetLevel);
		}
		else
		{
			throw new IllegalArgumentException("The target level must be a server-side level.");
		}
	}
	
	@Override
	public IBiomeWrapper deserializeBiomeWrapper(String str) throws IOException { return BiomeWrapper.deserialize(str); }
	
	@Override
	public IBlockStateWrapper deserializeBlockStateWrapper(String str) throws IOException { return BlockStateWrapper.deserialize(str); }
	
	@Override
	public IBlockStateWrapper getAirBlockStateWrapper() { return BlockStateWrapper.AIR; }
	
	@Override
	public HashMap<String, ? extends IBlockStateWrapper> getRendererIgnoredBlocks() { return BlockStateWrapper.RENDERER_IGNORED_BLOCKS; }
	
	
	/**
	 * Note: when this is updated for different MC versions, make sure you also update the documentation in
	 * {@link IDhApiWorldGenerator#generateChunks} and the type list in {@link WrapperFactory#createChunkWrapperErrorMessage}. <br><br>
	 *
	 * For full method documentation please see: {@link IWrapperFactory#createChunkWrapper}
	 *
	 * @see IWrapperFactory#createChunkWrapper
	 */
	public IChunkWrapper createChunkWrapper(Object[] objectArray) throws ClassCastException
	{
		if (objectArray.length == 1 && objectArray[0] instanceof IChunkWrapper)
		{
			try
			{
				// this path should only happen when called by Distant Horizons code
				// API implementors should never hit this path
				return (IChunkWrapper) objectArray[0];
			}
			catch (Exception e)
			{
				throw new ClassCastException(createChunkWrapperErrorMessage(objectArray));
			}
		}
		
		// MC 1.16, 1.18, 1.19, 1.20
		#if POST_MC_1_17_1 || MC_1_16_5
		else if (objectArray.length == 2)
		{
			// correct number of parameters from the API
			
			// chunk
			if (!(objectArray[0] instanceof ChunkAccess))
			{
				throw new ClassCastException(createChunkWrapperErrorMessage(objectArray));
			}
			ChunkAccess chunk = (ChunkAccess) objectArray[0];
			
			// light source
			if (!(objectArray[1] instanceof LevelReader))
			{
				throw new ClassCastException(createChunkWrapperErrorMessage(objectArray));
			}
			LevelReader lightSource = (LevelReader) objectArray[1];
			
			
			return new ChunkWrapper(chunk, lightSource, /*A DH wrapped level isn't necessary*/null);
		}
		// incorrect number of parameters from the API
		else
		{
			throw new ClassCastException(createChunkWrapperErrorMessage(objectArray));
		}
		#else
			// Intentional compiler error to bring attention to the missing wrapper function.
			// If you need to work on an unimplemented version but don't have the ability to implement this yet
			// you can comment it out, but please don't commit it. Someone will have to implement it .

			// After implementing the new version please read this method's javadocs for instructions
			// on what other locations also need to be updated, the DhAPI specifically needs to
			// be updated to state which objects this method accepts.
			not implemented for this version of Minecraft!
		#endif
	}
	/**
	 * Note: when this is updated for different MC versions,
	 * make sure you also update the documentation in {@link IDhApiWorldGenerator#generateChunks}.
	 */
	private static String createChunkWrapperErrorMessage(Object[] objectArray)
	{
		StringBuilder message = new StringBuilder(
				"Chunk wrapper creation failed. \n" +
						"Expected parameters: \n");
		
		// MC 1.16, 1.18, 1.19, 1.20
		#if POST_MC_1_17_1 || MC_1_16_5
		message.append("[" + ChunkAccess.class.getName() + "], \n");
		message.append("[" + LevelReader.class.getName() + "]. \n");
		#else
			// See preprocessor comment in createChunkWrapper() for full documentation
			not implemented for this version of Minecraft!
		#endif
		
		if (objectArray.length != 0)
		{
			message.append("Given parameters: ");
			for (Object obj : objectArray)
			{
				message.append("[").append(obj.getClass().getName()).append("], ");
			}
		}
		else
		{
			message.append(" No parameters given.");
		}
		
		return message.toString();
	}
	
	
}
