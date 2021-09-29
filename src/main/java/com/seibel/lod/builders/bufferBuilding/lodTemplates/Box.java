package com.seibel.lod.builders.bufferBuilding.lodTemplates;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.seibel.lod.config.LodConfig;
import com.seibel.lod.enums.DebugMode;
import com.seibel.lod.util.ColorUtil;
import com.seibel.lod.util.DataPointUtil;
import com.seibel.lod.wrappers.MinecraftWrapper;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

/**
 * Similar to Minecraft's AxisAlignedBoundingBox.
 * 
 * @author Leonardo Amato
 * @version 9-25-2021
 */
public class Box
{
	
	public static final int ADJACENT_HEIGHT_INDEX = 0;
	public static final int ADJACENT_DEPTH_INDEX = 1;
	
	public static final int X = 0;
	public static final int Y = 1;
	public static final int Z = 2;
	
	public static final int MIN = 0;
	public static final int MAX = 1;
	
	public static final int VOID_FACE = 0;
	
	/** The six cardinal directions */
	public static final Direction[] DIRECTIONS = new Direction[]{
			Direction.UP,
			Direction.DOWN,
			Direction.WEST,
			Direction.EAST,
			Direction.NORTH,
			Direction.SOUTH};
	
	/** North, South, East, West */
	public static final Direction[] ADJ_DIRECTIONS = new Direction[]{
			Direction.EAST,
			Direction.WEST,
			Direction.SOUTH,
			Direction.NORTH};
	
	/** TODO what does this represent? I don't understand the name */
	@SuppressWarnings("serial")
	public static final Map<Direction, int[][]> DIRECTION_VERTEX_MAP = new HashMap<Direction, int[][]>()
	{{
		put(Direction.UP, new int[][]{
			{0, 1, 0},
			{0, 1, 1},
			{1, 1, 1},
			{1, 1, 0}});
		put(Direction.DOWN, new int[][]{
			{1, 0, 0},
			{1, 0, 1},
			{0, 0, 1},
			{0, 0, 0}});
		put(Direction.EAST, new int[][]{
			{1, 1, 0},
			{1, 1, 1},
			{1, 0, 1},
			{1, 0, 0}});
		put(Direction.WEST, new int[][]{
			{0, 0, 0},
			{0, 0, 1},
			{0, 1, 1},
			{0, 1, 0}});
		put(Direction.SOUTH, new int[][]{
			{1, 0, 1},
			{1, 1, 1},
			{0, 1, 1},
			{0, 0, 1}});
		put(Direction.NORTH, new int[][]{
			{0, 0, 0},
			{0, 1, 0},
			{1, 1, 0},
			{1, 0, 0}});
	}};
	
	
	@SuppressWarnings("serial")
	public static final Map<Direction, int[]> FACE_DIRECTION = new HashMap<Direction, int[]>()
	{{
		put(Direction.UP, new int[]{Y, MAX});
		put(Direction.DOWN, new int[]{Y, MIN});
		put(Direction.EAST, new int[]{X, MAX});
		put(Direction.WEST, new int[]{X, MIN});
		put(Direction.SOUTH, new int[]{Z, MAX});
		put(Direction.NORTH, new int[]{Z, MIN});
	}};
	
	@SuppressWarnings("serial")
	public static final Map<Direction, int[]> DIRECTION_NORMAL_MAP = new HashMap<Direction, int[]>()
	{{
		put(Direction.UP, new int[]{0, 1, 0});
		put(Direction.DOWN, new int[]{0, -1, 0});
		put(Direction.EAST, new int[]{1, 0, 0});
		put(Direction.WEST, new int[]{-1, 0, 0});
		put(Direction.SOUTH, new int[]{0, 0, 1});
		put(Direction.NORTH, new int[]{0, 0, -1});
	}};
	
	/** holds the box's x, y, z offset */
	public int[] boxOffset;
	/** holds the box's x, y, z width */
	public int[] boxWidth;
	
	/** Holds each direction's color */
	public Map<Direction, Integer> colorMap;
	/** The original color (before shading) of this box */
	public int color;
	/**  */
	public Map<Direction, int[]> adjHeight;
	public Map<Direction, int[]> adjDepth;
	
	/** Holds if the given direction should be culled or not */
	public Map<Direction, Boolean> culling;
	
	
	/** creates a empty box */
	@SuppressWarnings("serial")
	public Box()
	{
		boxOffset = new int[3];
		boxWidth = new int[3];
		
		colorMap = new HashMap<Direction, Integer>()
		{{
			put(Direction.UP, 0);
			put(Direction.DOWN, 0);
			put(Direction.EAST, 0);
			put(Direction.WEST, 0);
			put(Direction.SOUTH, 0);
			put(Direction.NORTH, 0);
		}};
		
		// TODO what does the 32 represent?
		adjHeight = new HashMap<Direction, int[]>()
		{{
			put(Direction.EAST, new int[32]);
			put(Direction.WEST, new int[32]);
			put(Direction.SOUTH, new int[32]);
			put(Direction.NORTH, new int[32]);
		}};
		adjDepth = new HashMap<Direction, int[]>()
		{{
			put(Direction.EAST, new int[32]);
			put(Direction.WEST, new int[32]);
			put(Direction.SOUTH, new int[32]);
			put(Direction.NORTH, new int[32]);
		}};
		
		culling = new HashMap<Direction, Boolean>()
		{{
			put(Direction.UP, false);
			put(Direction.DOWN, false);
			put(Direction.EAST, false);
			put(Direction.WEST, false);
			put(Direction.SOUTH, false);
			put(Direction.NORTH, false);
		}};
	}
	
	
	
	public void setColor(int color)
	{
		this.color = color;
		for (Direction direction : DIRECTIONS)
		{
			colorMap.put(direction, ColorUtil.applyShade(color, MinecraftWrapper.INSTANCE.getClientWorld().getShade(direction, true)));
		}
	}
	
	public int getColor(Direction direction)
	{
		if (LodConfig.CLIENT.debugging.debugMode.get() != DebugMode.SHOW_DETAIL)
		{
			return colorMap.get(direction);
		}
		else
		{
			return ColorUtil.applyShade(color, MinecraftWrapper.INSTANCE.getClientWorld().getShade(direction, true));
		}
	}
	
	
	/** clears this box, reseting everything to default values */
	public void reset()
	{
		Arrays.fill(boxWidth, 0);
		Arrays.fill(boxOffset, 0);
		
		for (Direction direction : DIRECTIONS)
		{
			colorMap.put(direction, 0);
		}
		
		for (Direction direction : ADJ_DIRECTIONS)
		{
			// TODO wouldn't we want to set all adjHeightAndDepth
			// to VOID_FACE regardless of the culled status?
			if (isCulled(direction))
				continue;
			
			for (int i = 0; i < adjHeight.get(direction).length; i++)
			{
				adjHeight.get(direction)[i] = VOID_FACE;
				adjDepth.get(direction)[i] = VOID_FACE;
			}
		}
	}
	
	/** determine which faces should be culled */
	public void setUpCulling(int cullingDistance, BlockPos playerPos)
	{
		for (Direction direction : DIRECTIONS)
		{
			if (direction == Direction.DOWN)
				culling.put(direction, playerPos.get(direction.getAxis()) > getFacePos(direction) + cullingDistance);
			else if (direction == Direction.UP)
				culling.put(direction, playerPos.get(direction.getAxis()) < getFacePos(direction) - cullingDistance);
			else if (direction == Direction.WEST)
				culling.put(direction, -playerPos.get(direction.getAxis()) > getFacePos(direction) + cullingDistance);
			else if (direction == Direction.NORTH)
				culling.put(direction, -playerPos.get(direction.getAxis()) > getFacePos(direction) + cullingDistance);
			else if (direction == Direction.EAST)
				culling.put(direction, -playerPos.get(direction.getAxis()) < getFacePos(direction) - cullingDistance);
			else if (direction == Direction.SOUTH)
				culling.put(direction, -playerPos.get(direction.getAxis()) < getFacePos(direction) - cullingDistance);
		}
	}
	
	public boolean isCulled(Direction direction)
	{
		return culling.get(direction);
	}
	
	public void setAdjData(Map<Direction, long[]> adjData)
	{
		int height;
		int depth;
		int minY = getMinY();
		int maxY = getMaxY();
		for (Direction direction : ADJ_DIRECTIONS)
		{
			long[] dataPoint = adjData.get(direction);
			if (dataPoint == null || DataPointUtil.isVoid(dataPoint[0]))
			{
				adjHeight.get(direction)[0] = maxY;
				adjDepth.get(direction)[0] = minY;
				adjHeight.get(direction)[1] = VOID_FACE;
				adjDepth.get(direction)[1] = VOID_FACE;
				continue;
			}
			
			//We order the adj list
			/**TODO remove this if the order is maintained naturally*/
			/*order[0] = 0;
			int count = 0;
			for (int i = 0; i < dataPoint.length; i++)
			{
				int j = i - 1;
				if(DataPointUtil.isItVoid(dataPoint[i]) || !DataPointUtil.doesItExist(dataPoint[i]))
				{
					continue;
				}
				while (j >= 0 && DataPointUtil.getHeight(order[j]) < DataPointUtil.getHeight(dataPoint[i]))
				{
					order[j + 1] = order[j];
					j = j - 1;
				}
				order[j + 1] = dataPoint[i];
				count++;
			}*/
			
			int i;
			int faceToDraw = 0;
			boolean firstFace = true;
			boolean toFinish = false;
			boolean allAbove = true;
			long singleAdjDataPoint;
			for (i = 0; i < dataPoint.length; i++)
			{
				singleAdjDataPoint = dataPoint[i];
				
				if(DataPointUtil.isVoid(singleAdjDataPoint) || !DataPointUtil.doesItExist(singleAdjDataPoint))
					break;
				
				height = DataPointUtil.getHeight(singleAdjDataPoint);
				depth = DataPointUtil.getDepth(singleAdjDataPoint);
				
				if (depth <= maxY)
				{
					allAbove = false;
					if (height < minY)
					{
						// the adj data is lower than the current data
						
						if (firstFace)
						{
							adjHeight.get(direction)[0] = getMaxY();
							adjDepth.get(direction)[0] = getMinY();
						}
						else
						{
							adjDepth.get(direction)[faceToDraw] = getMinY();
						}
						faceToDraw++;
						toFinish = false;
						
						// break since all the other data will be lower
						break;
					}
					else if (depth <= minY && height >= maxY)
					{
						// the adj data is inside the current data
						// don't draw the face
						adjHeight.get(direction)[0] = VOID_FACE;
						adjDepth.get(direction)[0] = VOID_FACE;
						break;
					}
					else if (depth <= minY)//&& height < maxY
					{
						// the adj data intersects the lower part of the current data
						// if this is the only face, use the maxY and break,
						// if there was another face we finish the last one and break
						if (firstFace)
						{
							adjHeight.get(direction)[0] = getMaxY();
							adjDepth.get(direction)[0] = height;
						}
						else
						{
							adjDepth.get(direction)[faceToDraw] = height;
						}
						toFinish = false;
						faceToDraw++;
						break;
					}
					else if (height >= maxY)//depth > minY &&
					{
						// the adj data intersects the higher part of the current data
						// we start the creation of a new face
						adjHeight.get(direction)[faceToDraw] = depth;
						firstFace = false;
						toFinish = true;
					}
					else
					{
						// if (depth > minY && height < maxY)
						
						// the adj data is contained in the current data
						if (firstFace)
						{
							adjHeight.get(direction)[0] = getMaxY();
						}
						
						adjDepth.get(direction)[faceToDraw] = height;
						faceToDraw++;
						adjHeight.get(direction)[faceToDraw] = depth;
						firstFace = false;
						toFinish = true;
					}
				}
				//else
				//{
				//	// the adj data is higher than the current data
				//	// we continue since there could be some other data that intersect the current
				//}
			}
			
			if(allAbove)
			{
				adjHeight.get(direction)[0] = getMaxY();
				adjDepth.get(direction)[0] = getMinY();
				faceToDraw++;
			}
			else if (toFinish)
			{
				adjDepth.get(direction)[faceToDraw] = minY;
				faceToDraw++;
			}
			
			adjHeight.get(direction)[faceToDraw] = VOID_FACE;
			adjDepth.get(direction)[faceToDraw] = VOID_FACE;
		}
	}
	
	public void setWidth(int xWidth, int yWidth, int zWidth)
	{
		boxWidth[X] = xWidth;
		boxWidth[Y] = yWidth;
		boxWidth[Z] = zWidth;
	}
	
	public void setOffset(int xOffset, int yOffset, int zOffset)
	{
		boxOffset[X] = xOffset;
		boxOffset[Y] = yOffset;
		boxOffset[Z] = zOffset;
	}
	
	
	// TODO what does this mean?
	public int getFacePos(Direction direction)
	{
		return boxOffset[FACE_DIRECTION.get(direction)[0]] + boxWidth[FACE_DIRECTION.get(direction)[0]] * FACE_DIRECTION.get(direction)[1];
	}
	
	// TODO is this still needed?
//	public int getCoord(Direction direction, int axis, int vertexIndex)
//	{
//		return box[OFFSET][axis] + boxWidth[axis] * DIRECTION_VERTEX_MAP.get(direction)[vertexIndex][axis];
//	}
	
	/**
	 * returns true if the given direction should be rendered.
	 */
	public boolean shouldRenderFace(Direction direction, int adjIndex)
	{
		if (direction == Direction.UP || direction == Direction.DOWN)
		{
			return adjIndex == 0;
		}
		return !(adjHeight.get(direction)[adjIndex] == VOID_FACE && adjDepth.get(direction)[adjIndex] == VOID_FACE);
	}
	
	
	
	
	// TODO what does vertexIndex mean, is it 0-3 and represent
	// the 4 vertices in the quad we send to the bufferBuilder?
	
	public int getX(Direction direction, int vertexIndex)
	{
		return boxOffset[X] + boxWidth[X] * DIRECTION_VERTEX_MAP.get(direction)[vertexIndex][X];
	}
	
	public int getY(Direction direction, int vertexIndex)
	{
		return boxOffset[Y] + boxWidth[Y] * DIRECTION_VERTEX_MAP.get(direction)[vertexIndex][Y];
	}
	
	public int getY(Direction direction, int vertexIndex, int adjIndex)
	{
		if (direction == Direction.DOWN || direction == Direction.UP)
		{
			return boxOffset[Y] + boxWidth[Y] * DIRECTION_VERTEX_MAP.get(direction)[vertexIndex][Y];
		}
		else
		{
			// this could probably be cleaned up more,
			// but it still works
			if (1 - DIRECTION_VERTEX_MAP.get(direction)[vertexIndex][Y] == ADJACENT_HEIGHT_INDEX)
			{
				return adjHeight.get(direction)[adjIndex];
			}
			else
			{
				return adjDepth.get(direction)[adjIndex];	
			}
		}
	}
	
	public int getZ(Direction direction, int vertexIndex)
	{
		return boxOffset[Z] + boxWidth[Z] * DIRECTION_VERTEX_MAP.get(direction)[vertexIndex][Z];
	}
	
	
	
	public int getMinX()
	{
		return boxOffset[X];
	}
	
	public int getMaxX()
	{
		return boxOffset[X] + boxWidth[X];
	}
	
	public int getMinY()
	{
		return boxOffset[Y];
	}
	
	public int getMaxY()
	{
		return boxOffset[Y] + boxWidth[Y];
	}
	
	public int getMinZ()
	{
		return boxOffset[Z];
	}
	
	public int getMaxZ()
	{
		return boxOffset[Z] + boxWidth[Z];
	}
	
}
