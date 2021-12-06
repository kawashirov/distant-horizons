/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.core.handlers;

/**
 * 
 * @author Cola
 * @author Leonardo Amato
 * @version 11-12-2021
 */
public class ChunkFileLoader
{
	// TODO
//	public static IChunk getChunkFromFile(ChunkPos pos)
//	{	
//		LevelWrapper clientLevel = MinecraftWrapper.INSTANCE.getWrappedClientLevel();
//		if (clientLevel == null)
//			return null;
//		WorldWrapper serverWorld = LodUtil.getServerWorldFromDimension(clientLevel.getDimensionType());
//		try
//		{
//			File file = new File(serverWorld.getSaveFolder().getParent() + File.separatorChar + "region", "r." + (pos.x >> 5) + "." + (pos.z >> 5) + ".mca");
//			if(!file.exists())
//				return  null;
//			IChunk loadedChunk = ChunkSerializer.read(
//					serverWorld,
//					serverWorld.getStructureManager(),
//					serverWorld.getPoiManager(),
//					pos,
//					serverWorld.getChunkSource().chunkMap.read(pos)
//			);
//			boolean emptyChunk = true;
//			for(int i = 0; i < 16; i++){
//				for(int j = 0; j < 16; j++){
//					emptyChunk &= loadedChunk.isYSpaceEmpty(i,j);
//				}
//			}
//			if(emptyChunk)
//				return null;
//			else
//				return loadedChunk;
//		}
//		catch (Exception e)
//		{
//			return null;
//		}
//	}
}
