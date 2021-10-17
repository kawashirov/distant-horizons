package com.seibel.lod.handlers;

import com.seibel.lod.util.LodUtil;
import com.seibel.lod.wrappers.MinecraftWrapper;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.datafix.DataFixesManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.chunk.storage.ChunkSerializer;
import net.minecraft.world.server.ServerWorld;

import java.io.File;

public class ChunkLoader
{
	public static IChunk getChunkFromFile(ChunkPos pos){
		
		ClientWorld clientWorld = MinecraftWrapper.INSTANCE.getClientWorld();
		if (clientWorld == null)
			return null;
		ServerWorld serverWorld = LodUtil.getServerWorldFromDimension(clientWorld.dimensionType());
		try
		{
			IChunk loadedChunk = ChunkSerializer.read(
					serverWorld,
					serverWorld.getStructureManager(),
					serverWorld.getPoiManager(),
					pos,
					serverWorld.getChunkSource().chunkMap.read(pos)
			);
			boolean emptyChunk = true;
			for(int i = 0; i < 16; i++){
				for(int j = 0; j < 16; j++){
					emptyChunk &= loadedChunk.isYSpaceEmpty(i,j);
				}
			}
			if(emptyChunk)
				return null;
			else
				return loadedChunk;
		}
		catch (Exception e)
		{
			return null;
		}
	}
}
