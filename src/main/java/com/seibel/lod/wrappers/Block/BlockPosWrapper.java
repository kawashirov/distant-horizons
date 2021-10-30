package com.seibel.lod.wrappers.Block;

import com.seibel.lod.util.ColorUtil;
import net.minecraft.block.*;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.model.data.ModelDataMap;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


//This class wraps the minecraft BlockPos.Mutable (and BlockPos) class
public class BlockPosWrapper
{
	private final BlockPos.Mutable blockPos;
	
	
	public BlockPosWrapper()
	{
		this.blockPos = new BlockPos.Mutable(0,0,0);
	}
	
	public void set(int x, int y, int z)
	{
		blockPos.set(x, y, z);
	}
	
	public int getX()
	{
		return blockPos.getX();
	}
	
	public int getY()
	{
		return blockPos.getY();
	}
	
	public int getZ()
	{
		return blockPos.getZ();
	}
	
	public BlockPos.Mutable getBlockPos()
	{
		return blockPos;
	}
	
	@Override public boolean equals(Object o)
	{
		return blockPos.equals(o);
	}
	
	@Override public int hashCode()
	{
		return Objects.hash(blockPos);
	}
	
}
