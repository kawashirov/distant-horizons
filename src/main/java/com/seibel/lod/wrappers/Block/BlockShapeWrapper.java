
package com.seibel.lod.wrappers.Block;

import com.seibel.lod.wrappers.Chunk.ChunkWrapper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SixWayBlock;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


//This class wraps the minecraft Block class
public class BlockShapeWrapper
{
	//set of block which require tint
	public static final ConcurrentMap<Block, BlockShapeWrapper> blockShapeWrapperMap = new ConcurrentHashMap<>();
	public static BlockShapeWrapper WATER_SHAPE = new BlockShapeWrapper();
	
	private final Block block;
	private boolean toAvoid;
	private boolean nonFull;
	private boolean noCollision;
	
	/**Constructor only require for the block instance we are wrapping**/
	public BlockShapeWrapper(Block block, ChunkWrapper chunkWrapper, BlockPosWrapper blockPosWrapper)
	{
		this.block = block;
		this.nonFull = false;
		this.noCollision = false;
		this.toAvoid = ofBlockToAvoid();
		setupShapes(chunkWrapper, blockPosWrapper);
		//System.out.println(block + " non full " + nonFull + " no collision " + noCollision + " to avoid " + toAvoid);
	}
	
	private BlockShapeWrapper()
	{
		this.block = Blocks.WATER;
		this.nonFull = false;
		this.noCollision = false;
		this.toAvoid = false;
	}
	
	/**
	 * this return a wrapper of the block in input
	 * @param block Block object to wrap
	 */
	static public BlockShapeWrapper getBlockShapeWrapper(Block block, ChunkWrapper chunkWrapper, BlockPosWrapper blockPosWrapper)
	{
		//first we check if the block has already been wrapped
		if (blockShapeWrapperMap.containsKey(block) && blockShapeWrapperMap.get(block) != null)
			return blockShapeWrapperMap.get(block);
		
		
		//if it hasn't been created yet, we create it and save it in the map
		BlockShapeWrapper blockWrapper = new BlockShapeWrapper(block, chunkWrapper, blockPosWrapper);
		blockShapeWrapperMap.put(block, blockWrapper);
		
		//we return the newly created wrapper
		return blockWrapper;
	}
	
	private void setupShapes(ChunkWrapper chunkWrapper, BlockPosWrapper blockPosWrapper)
	{
		IBlockReader chunk = chunkWrapper.getChunk();
		BlockPos blockPos = blockPosWrapper.getBlockPos();
		boolean noCollisionSetted = false;
		boolean nonFullSetted = false;
		if (!block.defaultBlockState().getFluidState().isEmpty() || block instanceof SixWayBlock)
		{
			noCollisionSetted = true;
			nonFullSetted = true;
			noCollision = false;
			nonFull = false;
		}
		if (!nonFullSetted)
		{
			VoxelShape voxelShape = block.defaultBlockState().getShape(chunk, blockPos);
			
			if (!voxelShape.isEmpty())
			{
				AxisAlignedBB bbox = voxelShape.bounds();
				double xWidth = (bbox.maxX - bbox.minX);
				double yWidth = (bbox.maxY - bbox.minY);
				double zWidth = (bbox.maxZ - bbox.minZ);
				nonFull = xWidth < 1 && zWidth < 1 && yWidth < 1;
			}
			else
			{
				nonFull = false;
			}
		}
		
		if (!noCollisionSetted)
		{
			VoxelShape collisionShape = block.defaultBlockState().getCollisionShape(chunk, blockPos);
			noCollision = collisionShape.isEmpty();
		}
	}
	
	public boolean ofBlockToAvoid()
	{
		return block.equals(Blocks.AIR)
					   || block.equals(Blocks.CAVE_AIR)
					   || block.equals(Blocks.BARRIER)
					   || block.equals(Blocks.VOID_AIR);
	}
//-----------------//
//Avoidance getters//
//-----------------//
	
	
	public boolean isNonFull()
	{
		return nonFull;
	}
	
	public boolean hasNoCollision()
	{
		return noCollision;
	}
	
	public boolean isToAvoid()
	{
		return toAvoid;
	}
	
	
	
	
	@Override public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (!(o instanceof BlockShapeWrapper))
			return false;
		BlockShapeWrapper that = (BlockShapeWrapper) o;
		return Objects.equals(block, that.block);
	}
	
	@Override public int hashCode()
	{
		return Objects.hash(block);
	}
}