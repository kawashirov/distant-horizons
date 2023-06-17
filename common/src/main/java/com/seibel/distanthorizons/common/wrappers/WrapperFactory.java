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
import com.seibel.distanthorizons.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
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

/**
 * This handles creating abstract wrapper objects.
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
	
	
	/**
	 * Note: when this is updated for different MC versions, make sure you also update the documentation in
	 * {@link IDhApiWorldGenerator#generateChunks} and the type list in {@link WrapperFactory#createChunkWrapperErrorMessage}. <br><br>
	 * 
	 * For full method documentation please see: {@link IWrapperFactory#createChunkWrapper}
	 * @see IWrapperFactory#createChunkWrapper
	 */
	public IChunkWrapper createChunkWrapper(Object[] objectArray) throws ClassCastException
	{
		if (objectArray.length == 1 && objectArray[0] instanceof IChunkWrapper) // alternate option if "instanceof" can't be compiled down to older JRE versions "IChunkWrapper.class.isInstance(objectArray[0])" Feel free to delete this comment if we've compiled the mod for 1.16 or earlier - James
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
		// correct number of parameters from the API
		else if (objectArray.length == 2)
		{
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
	}
	/** Note: when this is updated for different MC versions, make sure you also update the documentation in {@link IDhApiWorldGenerator#generateChunks}. */
	private static String createChunkWrapperErrorMessage(Object[] objectArray)
	{
		StringBuilder message = new StringBuilder(
				"Chunk wrapper creation failed. \n" +
				"Expected parameters: " +
				"[" + ChunkAccess.class.getName() + "], " +
				"[" + LevelReader.class.getName() + "]. \n");
		
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
