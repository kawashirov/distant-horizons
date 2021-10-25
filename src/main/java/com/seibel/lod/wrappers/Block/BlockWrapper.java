package com.seibel.lod.wrappers.Block;

import com.seibel.lod.util.ColorUtil;
import com.seibel.lod.wrappers.MinecraftWrapper;
import net.minecraft.block.*;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraftforge.client.model.data.ModelDataMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


//This class wraps the minecraft Block class
public class BlockWrapper
{
	//set of block which require tint
	public static final ConcurrentMap<Block, BlockWrapper> blockWrapperMap = new ConcurrentHashMap<>();
	public static final ModelDataMap dataMap = new ModelDataMap.Builder().build();
	public static Random random = new Random(0);
	
	
	private Block block;
	private boolean nonFull;
	private boolean noCollision;
	private int color;
	private boolean isColored;
	private boolean toTint;
	private boolean leavesTint;
	private boolean grassTint;
	private boolean waterTint;
	
	
	
	/**Constructor only require for the block instance we are wrapping**/
	public BlockWrapper(Block block)
	{
		this.nonFull = true;
		this.noCollision = true;
		this.color = 0;
		this.isColored = true;
		this.toTint = false;
		this.block = block;
		setupColorAndTint();
		setupShapes();
	}
	
	/**
	 * this return a wrapper of the block in input
	 * @param block Block object to wrap
	 */
	static public BlockWrapper getBlockWrapper(Block block)
	{
		//first we check if the block has already been wrapped
		if(blockWrapperMap.containsKey(block) && blockWrapperMap.get(block) != null)
			return blockWrapperMap.get(block);
		
		
		//if it hasn't been created yet, we create it and save it in the map
		BlockWrapper blockWrapper = new BlockWrapper(block);
		blockWrapperMap.put(block, blockWrapper);
		
		//we return the newly created wrapper
		return blockWrapper;
	}
	
	
	/**
	 * Generate the color of the given block from its texture
	 * and store it for later use.
	 */
	private void setupColorAndTint()
	{
		MinecraftWrapper mc = MinecraftWrapper.INSTANCE;
		TextureAtlasSprite texture;
		List<BakedQuad> quads = null;
		
		boolean isTinted = false;
		int listSize = 0;
		
		// first step is to check if this block has a tinted face
		for (Direction direction : Direction.values())
		{
			quads = mc.getModelManager().getBlockModelShaper().getBlockModel(block.defaultBlockState()).getQuads(block.defaultBlockState(), direction, random, dataMap);
			listSize = Math.max(listSize, quads.size());
			for (BakedQuad bakedQuad : quads)
			{
				isTinted |= bakedQuad.isTinted();
			}
		}
		
		//if it contains a tinted face then we store this block in the toTint set
		if(isTinted)
			this.toTint = true;
		
		//now we get the first non empty face
		for (Direction direction : Direction.values())
		{
			quads = mc.getModelManager().getBlockModelShaper().getBlockModel(block.defaultBlockState()).getQuads(block.defaultBlockState(), direction, random, dataMap);
			if (!quads.isEmpty())
				break;
		}
		
		//the quads list is not empty we extract the first one
		if (!quads.isEmpty())
			texture = quads.get(0).getSprite();
		else
		{
			return;
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
			for (int u = 0; u < texture.getHeight(); u++)
			{
				for (int v = 0; v < texture.getWidth(); v++)
				{
					if (texture.isTransparent(frameIndex, u, v))
						continue;
					
					tempColor = texture.getPixelRGBA(frameIndex, u, v);
					
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
		if (grassInstance() && this.toTint)
			this.grassTint = true;
		
		if (leavesInstance() && this.toTint)
			this.leavesTint = true;
		
		if (waterIstance() && this.toTint)
			this.waterTint = true;
		
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
	
	private void setupShapes(){
	
	}
	
	//--------------//
	//Colors getters//
	//--------------//
	
	public boolean hasColor()
	{
		return isColored;
	}
	
	public int getColor()
	{
		return color;
	}
	
	//------------//
	//Tint getters//
	//------------//
	
	
	public boolean hasTint()
	{
		return toTint;
	}
	
	public boolean hasGrassTint()
	{
		return grassTint;
	}
	
	public boolean hasLeavesTint()
	{
		return leavesTint;
	}
	
	public boolean hasWaterTint()
	{
		return waterTint;
	}
	
	
	@Override public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (!(o instanceof BlockWrapper))
			return false;
		BlockWrapper that = (BlockWrapper) o;
		return Objects.equals(block, that.block);
	}
	
	@Override public int hashCode()
	{
		return Objects.hash(block);
	}
	
}
