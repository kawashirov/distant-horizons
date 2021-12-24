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

package com.seibel.lod.forge.wrappers.block;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.seibel.lod.core.util.ColorUtil;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockColorWrapper;
import com.seibel.lod.forge.wrappers.minecraft.MinecraftWrapper;

import net.minecraft.block.AbstractPlantBlock;
import net.minecraft.block.AbstractTopPlantBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BushBlock;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.IGrowable;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.TallGrassBlock;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.model.data.ModelDataMap;


/**
 * @author ??
 * @version 11-17-2021
 */
public class BlockColorWrapper implements IBlockColorWrapper
{
	//set of block which require tint
	public static final ConcurrentMap<Block, BlockColorWrapper> blockColorWrapperMap = new ConcurrentHashMap<>();
	public static final ModelDataMap dataMap = new ModelDataMap.Builder().build();
	public static final BlockPos blockPos = new BlockPos(0, 0, 0);
	public static final Random random = new Random(0);
	//public static BlockColourWrapper WATER_COLOR = getBlockColorWrapper(Blocks.WATER);
	public static final Direction[] directions = new Direction[] { Direction.UP, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.DOWN };
	
	private final Block block;
	private int color;
	private boolean isColored;
	private boolean toTint;
	private boolean foliageTint;
	private boolean grassTint;
	private boolean waterTint;
	
	
	/**Constructor only require for the block instance we are wrapping**/
	public BlockColorWrapper(Block block)
	{
		this.block = block;
		this.color = 0;
		this.isColored = true;
		this.toTint = false;
		this.foliageTint = false;
		this.grassTint = false;
		this.waterTint = false;
		setupColorAndTint();
		/*StringBuilder s = new StringBuilder();
		s.append(block + "\n"
								   + Integer.toHexString(
				Minecraft.getInstance().getBlockColors().createDefault().getColor(
						block.defaultBlockState(),
						(World) MinecraftWrapper.INSTANCE.getWrappedServerLevel().getLevel(),
						blockPosWrapper.getBlockPos())) + "\n"
		);
		for(Property x : Minecraft.getInstance().getBlockColors().getColoringProperties(block))
			s.append(x.getName() + " " + x.getPossibleValues() + '\n');
		System.out.println(s);*/
		//System.out.println(block + " color " + Integer.toHexString(color) + " to tint " + toTint + " folliageTint " + folliageTint + " grassTint " + grassTint + " waterTint " + waterTint);
	}
	
	
	/**
	 * this return a wrapper of the block in input
	 * @param block object of the block to wrap
	 */
	public static IBlockColorWrapper getBlockColorWrapper(Block block)
	{
		//first we check if the block has already been wrapped
		if (blockColorWrapperMap.containsKey(block) && blockColorWrapperMap.get(block) != null)
			return blockColorWrapperMap.get(block);
		
		
		//if it hasn't been created yet, we create it and save it in the map
		BlockColorWrapper blockWrapper = new BlockColorWrapper(block);
		blockColorWrapperMap.put(block, blockWrapper);
		
		//we return the newly created wrapper
		return blockWrapper;
	}
	
	/**
	 * Generate the color of the given block from its texture
	 * and store it for later use.
	 */
	private void setupColorAndTint()
	{
		BlockState blockState = block.defaultBlockState();
		BlockPosWrapper blockPosWrapper = new BlockPosWrapper();
		MinecraftWrapper mc = MinecraftWrapper.INSTANCE;
		TextureAtlasSprite texture;
		List<BakedQuad> quads = null;
		
		//boolean isTinted = false;
		//int listSize = 0;
		
		// first step is to check if this block has a tinted face
		//for (Direction direction : directions)
		//{
		//	quads = mc.getModelManager().getBlockModelShaper().getBlockModel(block.defaultBlockState()).getQuads(blockState, direction, random, dataMap);
		//	listSize = Math.max(listSize, quads.size());
		//	for (BakedQuad bakedQuad : quads)
		//	{
		//		isTinted |= bakedQuad.isTinted();
		//	}
		//}
		
		//if it contains a tinted face then we store this block in the toTint set
		//if (isTinted)
		//	this.toTint = true;
		
		//now we get the first non-empty face
		for (Direction direction : directions)
		{
			quads = mc.getModelManager().getBlockModelShaper().getBlockModel(block.defaultBlockState()).getQuads(blockState, direction, random, dataMap);
			if (!quads.isEmpty())
				break;
		}
		
		//the quads list is not empty we extract the first one
		if (!quads.isEmpty())
		{
			isColored = true;
			texture = quads.get(0).getSprite();
		}
		else
		{
			isColored = true;
			texture = mc.getModelManager().getBlockModelShaper().getTexture(block.defaultBlockState(), mc.getClientWorld(), blockPosWrapper.getBlockPos());
		}
		
		int count = 0;
		int alpha = 0;
		int red = 0;
		int green = 0;
		int blue = 0;
		int numberOfGreyPixel = 0;
		int tempColor;
		int colorMultiplier;
		
		// generate the block's color
		for (int frameIndex = 0; frameIndex < texture.getFrameCount(); frameIndex++)
		{
			// textures normally use u and v instead of x and y
			for (int u = 0; u < texture.getWidth(); u++)
			{
				for (int v = 0; v < texture.getHeight(); v++)
				{
					
					tempColor = texture.getPixelRGBA(frameIndex, u, v);
					
					if (ColorUtil.getAlpha(texture.getPixelRGBA(frameIndex, u, v)) == 0)
						continue;
					
					// determine if this pixel is gray
					int colorMax = Math.max(Math.max(ColorUtil.getBlue(tempColor), ColorUtil.getGreen(tempColor)), ColorUtil.getRed(tempColor));
					int colorMin = 4 + Math.min(Math.min(ColorUtil.getBlue(tempColor), ColorUtil.getGreen(tempColor)), ColorUtil.getRed(tempColor));
					boolean isGray = colorMax < colorMin;
					if (isGray)
						numberOfGreyPixel++;
					
					
					// for flowers, weight their non-green color higher
					if (block instanceof FlowerBlock && (!(ColorUtil.getGreen(tempColor) > (ColorUtil.getBlue(tempColor) + 30)) || !(ColorUtil.getGreen(tempColor) > (ColorUtil.getRed(tempColor) + 30))))
						colorMultiplier = 5;
					else
						colorMultiplier = 1;
					
					
					// add to the running averages
					count += colorMultiplier;
					alpha += ColorUtil.getAlpha(tempColor) * colorMultiplier;
					red += ColorUtil.getBlue(tempColor) * colorMultiplier;
					green += ColorUtil.getGreen(tempColor) * colorMultiplier;
					blue += ColorUtil.getRed(tempColor) * colorMultiplier;
				}
			}
		}
		
		
		if (count == 0)
			// this block is entirely transparent
			tempColor = 0;
		else
		{
			// determine the average color
			alpha /= count;
			red /= count;
			green /= count;
			blue /= count;
			tempColor = ColorUtil.rgbToInt(alpha, red, green, blue);
		}
		
		// determine if this block should use the biome color tint
		if ((grassInstance() || leavesInstance() || waterIstance()) && (float) numberOfGreyPixel / count > 0.75f)
			this.toTint = true;
		
		// we check which kind of tint we need to apply
		this.grassTint = grassInstance() && toTint;
		
		this.foliageTint = leavesInstance() && toTint;
		
		this.waterTint = waterIstance() && toTint;
		
		color = tempColor;
	}
	
	/** determine if the given block should use the biome's grass color */
	private boolean grassInstance()
	{
		return block instanceof GrassBlock
					   || block instanceof BushBlock
					   || block instanceof IGrowable
					   || block instanceof AbstractPlantBlock
					   || block instanceof AbstractTopPlantBlock
					   || block instanceof TallGrassBlock;
	}
	
	/** determine if the given block should use the biome's foliage color */
	private boolean leavesInstance()
	{
		return block instanceof LeavesBlock
					   || block == Blocks.VINE
					   || block == Blocks.SUGAR_CANE;
	}
	
	/** determine if the given block should use the biome's foliage color */
	private boolean waterIstance()
	{
		return block == Blocks.WATER;
	}
	
	@Override
	public String getName(){
		return block.getName().toString();
	}
	
//--------------//
//Colors getters//
//--------------//
	
	@Override
	public boolean hasColor()
	{
		return isColored;
	}
	
	@Override
	public int getColor()
	{
		return color;
	}

//------------//
//Tint getters//
//------------//
	
	
	@Override
	public boolean hasTint()
	{
		return toTint;
	}
	
	@Override
	public boolean hasGrassTint()
	{
		return grassTint;
	}
	
	@Override
	public boolean hasFolliageTint()
	{
		return foliageTint;
	}
	
	@Override
	public boolean hasWaterTint()
	{
		return waterTint;
	}
	
	
	
	
	@Override public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (!(o instanceof BlockColorWrapper))
			return false;
		BlockColorWrapper that = (BlockColorWrapper) o;
		return Objects.equals(block, that.block);
	}
	
	@Override public int hashCode()
	{
		return Objects.hash(block);
	}
	
}

