package com.seibel.lod.builders.lodTemplates;

import com.seibel.lod.config.LodConfig;
import com.seibel.lod.enums.ShadingMode;
import com.seibel.lod.util.ColorUtil;
import com.seibel.lod.util.DataPointUtil;
import com.seibel.lod.wrappers.MinecraftWrapper;
import net.minecraft.util.Direction;

import java.util.HashMap;
import java.util.Map;

public class Box
{

	public static final int OFFSET = 0;
	public static final int WIDTH = 1;

	public static final int X = 0;
	public static final int Y = 1;
	public static final int Z = 2;


	public static final int VOID_FACE = 0;

	public static final Direction[] DIRECTIONS = new Direction[]{
			Direction.UP,
			Direction.DOWN,
			Direction.WEST,
			Direction.EAST,
			Direction.NORTH,
			Direction.SOUTH};

	public static final Direction[] ADJ_DIRECTIONS = new Direction[]{
			Direction.WEST,
			Direction.EAST,
			Direction.NORTH,
			Direction.SOUTH};

	public static final Map<Direction, int[][]> DIRECTION_VERTEX_MAP = new HashMap()
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

	public static final Map<Direction, int[][]> DIRECTION_NORMAL_MAP = new HashMap()
	{{
		put(Direction.UP, new int[]{0, 1, 0});
		put(Direction.DOWN, new int[]{0, -1, 0});
		put(Direction.EAST, new int[]{1, 0, 0});
		put(Direction.WEST, new int[]{-1, 0, 0});
		put(Direction.SOUTH, new int[]{0, 0, 1});
		put(Direction.NORTH, new int[]{0, 0, -1});
	}};

	public int[][] box;
	public Map<Direction, int[]> colorMap;
	public Map<Direction, long[]> adjData;
	public int color;
	public Map<Direction, int[][]> adjHeightAndDepth = new HashMap()
	{{
		put(Direction.EAST, new int[256][2]);
		put(Direction.WEST, new int[256][2]);
		put(Direction.SOUTH, new int[256][2]);
		put(Direction.NORTH, new int[256][2]);
	}};

	public Box()
	{
		box = new int[2][3];
		colorMap = new HashMap()
		{{
			put(Direction.UP, new int[1]);
			put(Direction.DOWN, new int[1]);
			put(Direction.EAST, new int[1]);
			put(Direction.WEST, new int[1]);
			put(Direction.SOUTH, new int[1]);
			put(Direction.NORTH, new int[1]);
		}};
	}

	public void setColor(int color)
	{
		this.color = color;
		for (Direction direction : DIRECTIONS)
		{
			colorMap.get(direction)[0] = ColorUtil.applyShade(color, MinecraftWrapper.INSTANCE.getWorld().getShade(direction, true));
		}
	}

	public int getColor(Direction direction)
	{
		if (LodConfig.CLIENT.graphics.shadingMode.get() == ShadingMode.DARKEN_SIDES)
		{
			return colorMap.get(direction)[0];
		} else
		{
			return color;
		}
	}

	public void setAdjData(Map<Direction, long[]> adjData)
	{
		int height;
		int depth;
		this.adjData = adjData;

		for (Direction direction : ADJ_DIRECTIONS)
		{
			boolean noMatch = true;
			long[] dataPoint = adjData.get(direction);
			if (dataPoint == null)
			{
				adjHeightAndDepth.get(direction)[0][0] = getMaxY();
				adjHeightAndDepth.get(direction)[0][1] = getMinY();
				adjHeightAndDepth.get(direction)[1][0] = VOID_FACE;
				adjHeightAndDepth.get(direction)[1][1] = VOID_FACE;
				continue;
			}
			int i;
			int faceToDraw = 0;
			boolean firstFace = true;
			boolean toFinish = false;
			for (i = 0; i < dataPoint.length; i++)
			{
				height = DataPointUtil.getHeight(dataPoint[i]);
				depth = DataPointUtil.getDepth(dataPoint[i]);

				if (depth > getMaxY())
				{//the adj data is higher than the current data
					//we continue since there could be some other data that intersect the current
					//System.out.println("case 1 " + height + " " + depth);
					continue;
				} else if (height < getMinY())
				{//the adj data is lower than the current data
					//we break since all the other data will be lower

					if (firstFace)
					{
						//System.out.println("case 2-1 " + height + " " + depth);
						adjHeightAndDepth.get(direction)[0][0] = getMaxY();
						adjHeightAndDepth.get(direction)[0][1] = getMinY();
					} else
					{
						//System.out.println("case 2-2 " + height + " " + depth);
						adjHeightAndDepth.get(direction)[faceToDraw][1] = getMinY();
					}
					faceToDraw++;
					toFinish = false;
					break;
				} else if (depth <= getMinY() && height >= getMaxY())
				{//the adj data contains the current
					//we do not draw the face
					//System.out.println("case 3");
					adjHeightAndDepth.get(direction)[0][0] = VOID_FACE;
					adjHeightAndDepth.get(direction)[0][1] = VOID_FACE;
					break;
				} else if (depth <= getMinY() && height < getMaxY())
				{//the adj data intersect the lower part of the current data
					//if this is the only face we use the maxY and break
					//if there was other face we finish the last one and break
					if (firstFace)
					{
						//System.out.println("case 4-1 " + height + " " + depth);
						adjHeightAndDepth.get(direction)[0][0] = getMaxY();
						adjHeightAndDepth.get(direction)[0][1] = height;
					} else
					{
						//System.out.println("case 4-2 " + height + " " + depth);
						adjHeightAndDepth.get(direction)[faceToDraw][1] = height;
					}
					firstFace = false;
					toFinish = false;
					faceToDraw++;
					break;
				} else if (depth > getMinY() && height >= getMaxY())
				{//the adj data intersect the higher part of the current data
					//we start the creation of a new face
					//System.out.println("case 5 " + height + " " + depth);
					adjHeightAndDepth.get(direction)[faceToDraw][0] = depth;
					firstFace = false;
					toFinish = true;
					continue;
				} else if (depth > getMinY() && height < getMaxY())
				{//the adj data is contained in the current data
					if (firstFace)
					{
						//System.out.println("case 6-1 " + height + " " + depth);;
						adjHeightAndDepth.get(direction)[0][0] = getMaxY();
					} else
					{
						//System.out.println("case 6-2 " + height + " " + depth);
					}
					adjHeightAndDepth.get(direction)[faceToDraw][1] = height;
					faceToDraw++;
					adjHeightAndDepth.get(direction)[faceToDraw][0] = depth;
					firstFace = false;
					toFinish = true;
					continue;
				}
			}
			if(toFinish)
			{
				adjHeightAndDepth.get(direction)[faceToDraw][1] = getMinY();
				faceToDraw++;
			}
			adjHeightAndDepth.get(direction)[faceToDraw][0] = VOID_FACE;
			adjHeightAndDepth.get(direction)[faceToDraw][1] = VOID_FACE;
		}
	}

	public void set(int xWidth, int yWidth, int zWidth)
	{
		box[OFFSET][X] = 0;
		box[OFFSET][Y] = 0;
		box[OFFSET][Z] = 0;

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

	public int getCoord(Direction direction, int axis, int vertexIndex)
	{
		return box[OFFSET][axis] + box[WIDTH][axis] * DIRECTION_VERTEX_MAP.get(direction)[vertexIndex][axis];
	}

	public int getX(Direction direction, int vertexIndex)
	{
		return box[OFFSET][X] + box[WIDTH][X] * DIRECTION_VERTEX_MAP.get(direction)[vertexIndex][X];
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
			if (adjIndex == 0)
				return true;
			else
				return false;
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
