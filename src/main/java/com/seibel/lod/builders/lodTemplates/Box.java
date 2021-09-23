package com.seibel.lod.builders.lodTemplates;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.seibel.lod.config.LodConfig;
import com.seibel.lod.enums.DebugMode;
import com.seibel.lod.util.ColorUtil;
import com.seibel.lod.util.DataPointUtil;
import com.seibel.lod.util.DetailDistanceUtil;
import com.seibel.lod.wrappers.MinecraftWrapper;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;


public class Box
{

	public static final int OFFSET = 0;
	public static final int WIDTH = 1;

	public static final int X = 0;
	public static final int Y = 1;
	public static final int Z = 2;

	public static final int MIN = 0;
	public static final int MAX = 1;

	public static final int VOID_FACE = 0;

	public static final Direction[] DIRECTIONS = new Direction[]{
			Direction.UP,
			Direction.DOWN,
			Direction.WEST,
			Direction.EAST,
			Direction.NORTH,
			Direction.SOUTH};

	public static final Direction[] ADJ_DIRECTIONS = new Direction[]{
			Direction.EAST,
			Direction.WEST,
			Direction.SOUTH,
			Direction.NORTH};

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

	public int[][] box;
	public long[] order;
	public Map<Direction, int[]> colorMap;
	public int color;
	public Map<Direction, int[][]> adjHeightAndDepth;
	public Map<Direction, boolean[]> culling;

	@SuppressWarnings("serial")
	public Box()
	{
		box = new int[2][3];
		//order = new long[DetailDistanceUtil.getMaxVerticalData(0)];
		colorMap = new HashMap<Direction, int[]>()
		{{
			put(Direction.UP, new int[1]);
			put(Direction.DOWN, new int[1]);
			put(Direction.EAST, new int[1]);
			put(Direction.WEST, new int[1]);
			put(Direction.SOUTH, new int[1]);
			put(Direction.NORTH, new int[1]);
		}};
		adjHeightAndDepth = new HashMap<Direction, int[][]>()
		{{
			put(Direction.EAST, new int[32][2]);
			put(Direction.WEST, new int[32][2]);
			put(Direction.SOUTH, new int[32][2]);
			put(Direction.NORTH, new int[32][2]);
		}};
		culling = new HashMap<Direction, boolean[]>()
		{{
			put(Direction.UP, new boolean[1]);
			put(Direction.DOWN, new boolean[1]);
			put(Direction.EAST, new boolean[1]);
			put(Direction.WEST, new boolean[1]);
			put(Direction.SOUTH, new boolean[1]);
			put(Direction.NORTH, new boolean[1]);
		}};
	}

	public void setColor(int color)
	{
		this.color = color;
		for (Direction direction : DIRECTIONS)
		{
			colorMap.get(direction)[0] = ColorUtil.applyShade(color, MinecraftWrapper.INSTANCE.getClientWorld().getShade(direction, true));
		}
	}

	public int getColor(Direction direction)
	{
		if (LodConfig.CLIENT.debugging.debugMode.get() != DebugMode.SHOW_DETAIL)
		{
			return colorMap.get(direction)[0];
		} else
		{
			return color;
		}
	}

	public void reset()
	{
		for (int i = 0; i < box.length; i++)
		{
			Arrays.fill(box[i], 0);
		}

		for (Direction direction : DIRECTIONS)
		{
			colorMap.get(direction)[0] = 0;
		}

		//Arrays.fill(order, DataPointUtil.EMPTY_DATA);
		for (Direction direction : ADJ_DIRECTIONS)
		{
			if(isCulled(direction)){
				continue;
			}
			for (int i = 0; i < adjHeightAndDepth.get(direction).length; i++)
			{
				adjHeightAndDepth.get(direction)[i][0] = VOID_FACE;
				adjHeightAndDepth.get(direction)[i][1] = VOID_FACE;
			}
		}
	}

	public void setUpCulling(int cullingDistance, BlockPos playerPos)
	{
		for (Direction direction : DIRECTIONS)
		{
			if(direction == Direction.DOWN)
				culling.get(direction)[0] = playerPos.get(direction.getAxis()) > getFacePos(direction) + cullingDistance;
			else if(direction == Direction.UP)
				culling.get(direction)[0] = playerPos.get(direction.getAxis()) < getFacePos(direction) - cullingDistance;
			else if(direction == Direction.WEST)
				culling.get(direction)[0] = -playerPos.get(direction.getAxis()) > getFacePos(direction) + cullingDistance;
			else if(direction == Direction.NORTH)
				culling.get(direction)[0] = -playerPos.get(direction.getAxis()) > getFacePos(direction) + cullingDistance;
			else if(direction == Direction.EAST)
				culling.get(direction)[0] = -playerPos.get(direction.getAxis()) < getFacePos(direction) - cullingDistance;
			else if(direction == Direction.SOUTH)
				culling.get(direction)[0] = -playerPos.get(direction.getAxis()) < getFacePos(direction) - cullingDistance;
		}
	}

	public boolean isCulled(Direction direction)
	{
		return culling.get(direction)[0];
	}

	public void setAdjData(Map<Direction, long[]> adjData)
	{
		int height;
		int depth;
		int minY = getMinY();
		int maxY = getMaxY();
		for (Direction direction : ADJ_DIRECTIONS)
		{
			/*if(isCulled(direction)){
				continue;
			}*/

			long[] dataPoint = adjData.get(direction);
			if (dataPoint == null || DataPointUtil.isItVoid(dataPoint[0]))
			{
				adjHeightAndDepth.get(direction)[0][0] = maxY;
				adjHeightAndDepth.get(direction)[0][1] = minY;
				adjHeightAndDepth.get(direction)[1][0] = VOID_FACE;
				adjHeightAndDepth.get(direction)[1][1] = VOID_FACE;
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
			/*for (i = 0; i < count; i++)
			{
				singleAdjDataPoint = order[i];*/

				if(DataPointUtil.isItVoid(singleAdjDataPoint) || !DataPointUtil.doesItExist(singleAdjDataPoint))
				{
					break;
				}
				height = DataPointUtil.getHeight(singleAdjDataPoint);
				depth = DataPointUtil.getDepth(singleAdjDataPoint);

				if (depth <= maxY) {
					allAbove = false;
					if (height < minY)
					{//the adj data is lower than the current data
						//we break since all the other data will be lower

						if (firstFace)
						{
							adjHeightAndDepth.get(direction)[0][0] = getMaxY();
							adjHeightAndDepth.get(direction)[0][1] = getMinY();
						} else
						{
							adjHeightAndDepth.get(direction)[faceToDraw][1] = getMinY();
						}
						faceToDraw++;
						toFinish = false;
						break;
					} else if (depth <= minY && height >= maxY)
					{//the adj data contains the current
						//we do not draw the face
						adjHeightAndDepth.get(direction)[0][0] = VOID_FACE;
						adjHeightAndDepth.get(direction)[0][1] = VOID_FACE;
						break;
					} else if (depth <= minY)//&& height < maxY
					{//the adj data intersect the lower part of the current data
						//if this is the only face we use the maxY and break
						//if there was other face we finish the last one and break
						if (firstFace)
						{
							adjHeightAndDepth.get(direction)[0][0] = getMaxY();
							adjHeightAndDepth.get(direction)[0][1] = height;
						} else
						{
							adjHeightAndDepth.get(direction)[faceToDraw][1] = height;
						}
						toFinish = false;
						faceToDraw++;
						break;
					} else if (height >= maxY)//depth > minY &&
					{//the adj data intersect the higher part of the current data
						//we start the creation of a new face
						adjHeightAndDepth.get(direction)[faceToDraw][0] = depth;
						firstFace = false;
						toFinish = true;
					} else {//if (depth > minY && height < maxY)
						//the adj data is contained in the current data
						if (firstFace)
						{
							adjHeightAndDepth.get(direction)[0][0] = getMaxY();
						}
						adjHeightAndDepth.get(direction)[faceToDraw][1] = height;
						faceToDraw++;
						adjHeightAndDepth.get(direction)[faceToDraw][0] = depth;
						firstFace = false;
						toFinish = true;
					}
				}
				//else {//the adj data is higher than the current data
					//we continue since there could be some other data that intersect the current
				//}
			}
			if(allAbove){
				adjHeightAndDepth.get(direction)[0][0] = getMaxY();
				adjHeightAndDepth.get(direction)[0][1] = getMinY();
				faceToDraw++;
			}
			else if (toFinish)
			{
				adjHeightAndDepth.get(direction)[faceToDraw][1] = minY;
				faceToDraw++;
			}
			adjHeightAndDepth.get(direction)[faceToDraw][0] = VOID_FACE;
			adjHeightAndDepth.get(direction)[faceToDraw][1] = VOID_FACE;
		}
	}

	public void set(int xWidth, int yWidth, int zWidth)
	{
		box[WIDTH][X] = xWidth;
		box[WIDTH][Y] = yWidth;
		box[WIDTH][Z] = zWidth;
	}

	public void move(int xOffset, int yOffset, int zOffset)
	{
		box[OFFSET][X] = xOffset;
		box[OFFSET][Y] = yOffset;
		box[OFFSET][Z] = zOffset;
	}



	public int getFacePos(Direction direction)
	{
		return box[OFFSET][FACE_DIRECTION.get(direction)[0]] + box[WIDTH][FACE_DIRECTION.get(direction)[0]] * FACE_DIRECTION.get(direction)[1];
	}

	public int getCoord(Direction direction, int axis, int vertexIndex)
	{
		return box[OFFSET][axis] + box[WIDTH][axis] * DIRECTION_VERTEX_MAP.get(direction)[vertexIndex][axis];
	}

	public int getX(Direction direction, int vertexIndex)
	{
		return box[OFFSET][X] + box[WIDTH][X] * DIRECTION_VERTEX_MAP.get(direction)[vertexIndex][X];
	}

	public int getY(Direction direction, int vertexIndex)
	{
		return box[OFFSET][Y] + box[WIDTH][Y] * DIRECTION_VERTEX_MAP.get(direction)[vertexIndex][Y];
	}

	public int getY(Direction direction, int vertexIndex, int adjIndex)
	{
		if (direction == Direction.DOWN || direction == Direction.UP)
		{
			return box[OFFSET][Y] + box[WIDTH][Y] * DIRECTION_VERTEX_MAP.get(direction)[vertexIndex][Y];
		} else
		{
			return adjHeightAndDepth.get(direction)[adjIndex][1 - DIRECTION_VERTEX_MAP.get(direction)[vertexIndex][Y]];
		}
	}

	public int getZ(Direction direction, int vertexIndex)
	{
		return box[OFFSET][Z] + box[WIDTH][Z] * DIRECTION_VERTEX_MAP.get(direction)[vertexIndex][Z];
	}

	public boolean shouldContinue(Direction direction, int adjIndex)
	{
		if (direction == Direction.UP || direction == Direction.DOWN)
		{
			return adjIndex == 0;
		}
		return !(adjHeightAndDepth.get(direction)[adjIndex][0] == VOID_FACE && adjHeightAndDepth.get(direction)[adjIndex][1] == VOID_FACE);

	}

	public int getMinX()
	{
		return box[OFFSET][X];
	}

	public int getMaxX()
	{
		return box[OFFSET][X] + box[WIDTH][X];
	}

	public int getMinY()
	{
		return box[OFFSET][Y];
	}

	public int getMaxY()
	{
		return box[OFFSET][Y] + box[WIDTH][Y];
	}

	public int getMinZ()
	{
		return box[OFFSET][Z];
	}

	public int getMaxZ()
	{
		return box[OFFSET][Z] + box[WIDTH][Z];
	}

}
