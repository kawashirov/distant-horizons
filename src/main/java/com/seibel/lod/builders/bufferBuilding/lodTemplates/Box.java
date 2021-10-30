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

package com.seibel.lod.builders.bufferBuilding.lodTemplates;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.seibel.lod.config.LodConfig;
import com.seibel.lod.enums.DebugMode;
import com.seibel.lod.util.ColorUtil;
import com.seibel.lod.util.DataPointUtil;
import com.seibel.lod.util.LodUtil;
import com.seibel.lod.wrappers.MinecraftWrapper;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3i;

/**
 * Similar to Minecraft's AxisAlignedBoundingBox.
 * @author Leonardo Amato
 * @version 10-2-2021
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
	public static final Direction[] DIRECTIONS = new Direction[] {
			Direction.UP,
			Direction.DOWN,
			Direction.WEST,
			Direction.EAST,
			Direction.NORTH,
			Direction.SOUTH };
	
	/** North, South, East, West */
	public static final Direction[] ADJ_DIRECTIONS = new Direction[] {
			Direction.EAST,
			Direction.WEST,
			Direction.SOUTH,
			Direction.NORTH };
	
	/** All the faces and vertices of a cube. This is used to extract the vertex from the column */
	public static final Map<Direction, int[][]> DIRECTION_VERTEX_MAP = new HashMap<Direction, int[][]>()
	{{
		put(Direction.UP, new int[][] {
				{ 0, 1, 0 },
				{ 0, 1, 1 },
				{ 1, 1, 1 },
				{ 1, 1, 0 } });
		put(Direction.DOWN, new int[][] {
				{ 1, 0, 0 },
				{ 1, 0, 1 },
				{ 0, 0, 1 },
				{ 0, 0, 0 } });
		put(Direction.EAST, new int[][] {
				{ 1, 1, 0 },
				{ 1, 1, 1 },
				{ 1, 0, 1 },
				{ 1, 0, 0 } });
		put(Direction.WEST, new int[][] {
				{ 0, 0, 0 },
				{ 0, 0, 1 },
				{ 0, 1, 1 },
				{ 0, 1, 0 } });
		put(Direction.SOUTH, new int[][] {
				{ 1, 0, 1 },
				{ 1, 1, 1 },
				{ 0, 1, 1 },
				{ 0, 0, 1 } });
		put(Direction.NORTH, new int[][] {
				{ 0, 0, 0 },
				{ 0, 1, 0 },
				{ 1, 1, 0 },
				{ 1, 0, 0 } });
	}};
	
	
	/**
	 * This indicates which position is invariable in the DIRECTION_VERTEX_MAP.
	 * Is used to extract the vertex
	 */
	public static final Map<Direction, int[]> FACE_DIRECTION = new HashMap<Direction, int[]>()
	{{
		put(Direction.UP, new int[] { Y, MAX });
		put(Direction.DOWN, new int[] { Y, MIN });
		put(Direction.EAST, new int[] { X, MAX });
		put(Direction.WEST, new int[] { X, MIN });
		put(Direction.SOUTH, new int[] { Z, MAX });
		put(Direction.NORTH, new int[] { Z, MIN });
	}};
	
	
	/**
	 * This is a map from Direction to the relative normal vector
	 * we are using this since I'm not sure if the getNormal create new object at every call
	 */
	public static final Map<Direction, Vector3i> DIRECTION_NORMAL_MAP = new HashMap<Direction, Vector3i>()
	{{
		put(Direction.UP, Direction.UP.getNormal());
		put(Direction.DOWN, Direction.DOWN.getNormal());
		put(Direction.EAST, Direction.EAST.getNormal());
		put(Direction.WEST, Direction.WEST.getNormal());
		put(Direction.SOUTH, Direction.SOUTH.getNormal());
		put(Direction.NORTH, Direction.NORTH.getNormal());
	}};
	
	/** We use this index for all array that are going to */
	public static final Map<Direction, Integer> DIRECTION_INDEX = new HashMap<Direction, Integer>()
	{{
		put(Direction.UP, 0);
		put(Direction.DOWN, 1);
		put(Direction.EAST, 2);
		put(Direction.WEST, 3);
		put(Direction.SOUTH, 4);
		put(Direction.NORTH, 5);
	}};
	
	public static final Map<Direction, Integer> ADJ_DIRECTION_INDEX = new HashMap<Direction, Integer>()
	{{
		put(Direction.EAST, 0);
		put(Direction.WEST, 1);
		put(Direction.SOUTH, 2);
		put(Direction.NORTH, 3);
	}};
	/** holds the box's x, y, z offset */
	public final int[] boxOffset;
	/** holds the box's x, y, z width */
	public final int[] boxWidth;
	
	/** Holds each direction's color */
	public final int[] colorMap;
	/** The original color (before shading) of this box */
	public int color;
	/**
	 *
	 */
	public final Map<Direction, int[]> adjHeight;
	public final Map<Direction, int[]> adjDepth;
	public final Map<Direction, byte[]> skyLights;
	public byte blockLight;
	
	/** Holds if the given direction should be culled or not */
	public final boolean[] culling;
	
	
	/** creates an empty box */
	public Box()
	{
		boxOffset = new int[3];
		boxWidth = new int[3];
		
		colorMap = new int[6];
		skyLights = new HashMap<Direction, byte[]>()
		{{
			put(Direction.UP, new byte[1]);
			put(Direction.DOWN, new byte[1]);
			put(Direction.EAST, new byte[LodUtil.MAX_NUMBER_OF_VERTICAL_LODS]);
			put(Direction.WEST, new byte[LodUtil.MAX_NUMBER_OF_VERTICAL_LODS]);
			put(Direction.SOUTH, new byte[LodUtil.MAX_NUMBER_OF_VERTICAL_LODS]);
			put(Direction.NORTH, new byte[LodUtil.MAX_NUMBER_OF_VERTICAL_LODS]);
		}};
		adjHeight = new HashMap<Direction, int[]>()
		{{
			put(Direction.EAST, new int[LodUtil.MAX_NUMBER_OF_VERTICAL_LODS]);
			put(Direction.WEST, new int[LodUtil.MAX_NUMBER_OF_VERTICAL_LODS]);
			put(Direction.SOUTH, new int[LodUtil.MAX_NUMBER_OF_VERTICAL_LODS]);
			put(Direction.NORTH, new int[LodUtil.MAX_NUMBER_OF_VERTICAL_LODS]);
		}};
		adjDepth = new HashMap<Direction, int[]>()
		{{
			put(Direction.EAST, new int[LodUtil.MAX_NUMBER_OF_VERTICAL_LODS]);
			put(Direction.WEST, new int[LodUtil.MAX_NUMBER_OF_VERTICAL_LODS]);
			put(Direction.SOUTH, new int[LodUtil.MAX_NUMBER_OF_VERTICAL_LODS]);
			put(Direction.NORTH, new int[LodUtil.MAX_NUMBER_OF_VERTICAL_LODS]);
		}};
		
		culling = new boolean[6];
	}
	
	/** Set the light of the columns */
	public void setLights(int skyLight, int blockLight)
	{
		this.blockLight = (byte) blockLight;
		skyLights.get(Direction.UP)[0] = (byte) skyLight;
	}
	
	/**
	 * Set the color of the columns
	 * @param color color to add
	 * @param adjShadeDisabled this array indicates which face does not need shading
	 */
	public void setColor(int color, boolean[] adjShadeDisabled)
	{
		this.color = color;
		for (Direction direction : DIRECTIONS)
		{
			if (!adjShadeDisabled[DIRECTION_INDEX.get(direction)])
				colorMap[DIRECTION_INDEX.get(direction)] = ColorUtil.applyShade(color, MinecraftWrapper.INSTANCE.getClientWorld().getShade(direction, true));
			else
				colorMap[DIRECTION_INDEX.get(direction)] = color;
		}
	}
	
	/**
	 * @param direction of the face of which we want to get the color
	 * @return color of the face
	 */
	public int getColor(Direction direction)
	{
		if (LodConfig.CLIENT.advancedModOptions.debugging.debugMode.get() != DebugMode.SHOW_DETAIL)
			return colorMap[DIRECTION_INDEX.get(direction)];
		else
			return ColorUtil.applyShade(color, MinecraftWrapper.INSTANCE.getClientWorld().getShade(direction, true));
	}
	
	/**
	 */
	public byte getSkyLight(Direction direction, int verticalIndex)
	{
		if(direction == Direction.UP || direction == Direction.DOWN)
			return skyLights.get(direction)[0];
		else
			return skyLights.get(direction)[verticalIndex];
	}
	
	/**
	 */
	public int getBlockLight()
	{
		return blockLight;
	}
	/** clears this box, resetting everything to default values */
	public void reset()
	{
		Arrays.fill(boxWidth, 0);
		Arrays.fill(boxOffset, 0);
		Arrays.fill(colorMap, 0);
		blockLight = 0;
		for (Direction direction : ADJ_DIRECTIONS)
		{
			for (int i = 0; i < adjHeight.get(direction).length; i++)
			{
				adjHeight.get(direction)[i] = VOID_FACE;
				adjDepth.get(direction)[i] = VOID_FACE;
				skyLights.get(direction)[i] = 0;
			}
		}
	}
	
	/** determine which faces should be culled */
	public void setUpCulling(int cullingDistance, BlockPos playerPos)
	{
		for (Direction direction : DIRECTIONS)
		{
			if (direction == Direction.DOWN || direction == Direction.WEST || direction == Direction.NORTH)
				culling[DIRECTION_INDEX.get(direction)] = playerPos.get(direction.getAxis()) > getFacePos(direction) + cullingDistance;
			
			else if (direction == Direction.UP || direction == Direction.EAST || direction == Direction.SOUTH)
				culling[DIRECTION_INDEX.get(direction)] = playerPos.get(direction.getAxis()) < getFacePos(direction) - cullingDistance;
			
			culling[DIRECTION_INDEX.get(direction)] = false;
		}
	}
	
	/**
	 * @param direction direction that we want to check if it's culled
	 * @return true if and only if the face of the direction is culled
	 */
	public boolean isCulled(Direction direction)
	{
		return culling[DIRECTION_INDEX.get(direction)];
	}
	
	
	/**
	 * This method create all the shared face culling based on the adjacent data
	 * @param adjData data adjacent to the column we are going to render
	 */
	public void setAdjData(Map<Direction, long[]> adjData)
	{
		int height;
		int depth;
		int minY = getMinY();
		int maxY = getMaxY();
		long singleAdjDataPoint;
		
		/* TODO implement attached vertical face culling
		//Up direction case
		if(DataPointUtil.doesItExist(adjData.get(Direction.UP)))
		{
			height = DataPointUtil.getHeight(singleAdjDataPoint);
			depth = DataPointUtil.getDepth(singleAdjDataPoint);
		}*/
		//Down direction case
		singleAdjDataPoint = adjData.get(Direction.DOWN)[0];
		if(DataPointUtil.doesItExist(singleAdjDataPoint))
			skyLights.get(Direction.DOWN)[0] = (byte) DataPointUtil.getLightSkyAlt(singleAdjDataPoint);
		else
			skyLights.get(Direction.DOWN)[0] = skyLights.get(Direction.UP)[0];
		//other sided
		//TODO clean some similar cases
		for (Direction direction : ADJ_DIRECTIONS)
		{
			if (isCulled(direction))
				continue;
			
			long[] dataPoint = adjData.get(direction);
			if (dataPoint == null || DataPointUtil.isVoid(dataPoint[0]))
			{
				adjHeight.get(direction)[0] = maxY;
				adjDepth.get(direction)[0] = minY;
				adjHeight.get(direction)[1] = VOID_FACE;
				adjDepth.get(direction)[1] = VOID_FACE;
				skyLights.get(direction)[0] = 15; //in void set full sky light
				continue;
			}
			
			int i;
			int faceToDraw = 0;
			boolean firstFace = true;
			boolean toFinish = false;
			int toFinishIndex = 0;
			boolean allAbove = true;
			for (i = 0; i < dataPoint.length; i++)
			{
				singleAdjDataPoint = dataPoint[i];
				
				if (DataPointUtil.isVoid(singleAdjDataPoint) || !DataPointUtil.doesItExist(singleAdjDataPoint))
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
							skyLights.get(direction)[0] = (byte) DataPointUtil.getLightSkyAlt(singleAdjDataPoint); //skyLights.get(Direction.UP)[0];
						}
						else
						{
							adjDepth.get(direction)[faceToDraw] = getMinY();
							skyLights.get(direction)[faceToDraw] = (byte) DataPointUtil.getLightSkyAlt(singleAdjDataPoint);
						}
						faceToDraw++;
						toFinish = false;
						
						// break since all the other data will be lower
						break;
					}
					else if (depth <= minY)
					{
						if (height >= maxY)
						{
							// the adj data is inside the current data
							// don't draw the face
							adjHeight.get(direction)[0] = VOID_FACE;
							adjDepth.get(direction)[0] = VOID_FACE;
						}
						else // height < maxY
						{
							// the adj data intersects the lower part of the current data
							// if this is the only face, use the maxY and break,
							// if there was another face we finish the last one and break
							if (firstFace)
							{
								adjHeight.get(direction)[0] = getMaxY();
								adjDepth.get(direction)[0] = height;
								skyLights.get(direction)[0] = (byte) DataPointUtil.getLightSkyAlt(singleAdjDataPoint); //skyLights.get(Direction.UP)[0];
							}
							else
							{
								adjDepth.get(direction)[faceToDraw] = height;
								skyLights.get(direction)[faceToDraw] = (byte) DataPointUtil.getLightSkyAlt(singleAdjDataPoint);
							}
							toFinish = false;
							faceToDraw++;
						}
						break;
					}
					else if (height >= maxY)//depth > minY &&
					{
						// the adj data intersects the higher part of the current data
						// we start the creation of a new face
						adjHeight.get(direction)[faceToDraw] = depth;
						//skyLights.get(direction)[faceToDraw] = (byte) DataPointUtil.getLightSkyAlt(singleAdjDataPoint);
						firstFace = false;
						toFinish = true;
						toFinishIndex = i + 1;
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
						skyLights.get(direction)[faceToDraw] = (byte) DataPointUtil.getLightSkyAlt(singleAdjDataPoint);
						faceToDraw++;
						adjHeight.get(direction)[faceToDraw] = depth;
						firstFace = false;
						toFinish = true;
						toFinishIndex = i + 1;
					}
				}
			}
			
			if (allAbove)
			{
				adjHeight.get(direction)[0] = getMaxY();
				adjDepth.get(direction)[0] = getMinY();
				skyLights.get(direction)[0] = skyLights.get(Direction.UP)[0];
				faceToDraw++;
			}
			else if (toFinish)
			{
				adjDepth.get(direction)[faceToDraw] = minY;
				if(toFinishIndex < dataPoint.length)
				{
					singleAdjDataPoint = dataPoint[toFinishIndex];
					if (DataPointUtil.doesItExist(singleAdjDataPoint))
						skyLights.get(direction)[faceToDraw] = (byte) DataPointUtil.getLightSkyAlt(singleAdjDataPoint);
					else
						skyLights.get(direction)[faceToDraw] = skyLights.get(Direction.UP)[0];
				}
				faceToDraw++;
			}
			
			adjHeight.get(direction)[faceToDraw] = VOID_FACE;
			adjDepth.get(direction)[faceToDraw] = VOID_FACE;
		}
	}
	
	/** We use this method to set the various width of the column */
	public void setWidth(int xWidth, int yWidth, int zWidth)
	{
		boxWidth[X] = xWidth;
		boxWidth[Y] = yWidth;
		boxWidth[Z] = zWidth;
	}
	
	/** We use this method to set the various offset of the column */
	public void setOffset(int xOffset, int yOffset, int zOffset)
	{
		boxOffset[X] = xOffset;
		boxOffset[Y] = yOffset;
		boxOffset[Z] = zOffset;
	}
	
	/**
	 * This method return the position of a face in the axis of the face
	 * This is useful for the face culling
	 * @param direction that we want to check
	 * @return position in the axis of the face
	 */
	public int getFacePos(Direction direction)
	{
		return boxOffset[FACE_DIRECTION.get(direction)[0]] + boxWidth[FACE_DIRECTION.get(direction)[0]] * FACE_DIRECTION.get(direction)[1];
	}
	
	/**
	 * returns true if the given direction should be rendered.
	 */
	public boolean shouldRenderFace(Direction direction, int adjIndex)
	{
		if (direction == Direction.UP || direction == Direction.DOWN)
			return adjIndex == 0;
		return !(adjHeight.get(direction)[adjIndex] == VOID_FACE && adjDepth.get(direction)[adjIndex] == VOID_FACE);
	}
	
	
	/**
	 * @param direction direction of the face we want to render
	 * @param vertexIndex index of the vertex of the face (0-1-2-3)
	 * @return position x of the relative vertex
	 */
	public int getX(Direction direction, int vertexIndex)
	{
		return boxOffset[X] + boxWidth[X] * DIRECTION_VERTEX_MAP.get(direction)[vertexIndex][X];
	}
	
	/**
	 * @param direction direction of the face we want to render
	 * @param vertexIndex index of the vertex of the face (0-1-2-3)
	 * @return position y of the relative vertex
	 */
	public int getY(Direction direction, int vertexIndex)
	{
		return boxOffset[Y] + boxWidth[Y] * DIRECTION_VERTEX_MAP.get(direction)[vertexIndex][Y];
	}
	
	/**
	 * @param direction direction of the face we want to render
	 * @param vertexIndex index of the vertex of the face (0-1-2-3)
	 * @param adjIndex, index of the n-th culled face of this direction
	 * @return position x of the relative vertex
	 */
	public int getY(Direction direction, int vertexIndex, int adjIndex)
	{
		if (direction == Direction.DOWN || direction == Direction.UP)
			return boxOffset[Y] + boxWidth[Y] * DIRECTION_VERTEX_MAP.get(direction)[vertexIndex][Y];
		else
		{
			// this could probably be cleaned up more,
			// but it still works
			if (1 - DIRECTION_VERTEX_MAP.get(direction)[vertexIndex][Y] == ADJACENT_HEIGHT_INDEX)
				return adjHeight.get(direction)[adjIndex];
			else
				return adjDepth.get(direction)[adjIndex];
		}
	}
	
	/**
	 * @param direction direction of the face we want to render
	 * @param vertexIndex index of the vertex of the face (0-1-2-3)
	 * @return position z of the relative vertex
	 */
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
