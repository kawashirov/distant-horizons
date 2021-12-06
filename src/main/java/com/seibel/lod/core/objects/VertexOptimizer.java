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

package com.seibel.lod.core.objects;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.enums.rendering.DebugMode;
import com.seibel.lod.core.objects.math.Vec3i;
import com.seibel.lod.core.util.ColorUtil;
import com.seibel.lod.core.util.DataPointUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;

/**
 * This class handles all the vertex optimization that's needed for a column of lods. W
 * @author Leonardo Amato
 * @version 10-2-2021
 */
public class VertexOptimizer
{
	private static final IMinecraftWrapper MC = SingletonHandler.get(IMinecraftWrapper.class);
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	
	public static final int ADJACENT_HEIGHT_INDEX = 0;
	public static final int ADJACENT_DEPTH_INDEX = 1;
	
	public static final int X = 0;
	public static final int Y = 1;
	public static final int Z = 2;
	
	public static final int MIN = 0;
	public static final int MAX = 1;
	
	public static final int VOID_FACE = 0;
	
	/** The six cardinal directions */
	public static final LodDirection[] DIRECTIONS = new LodDirection[] {
			LodDirection.UP,
			LodDirection.DOWN,
			LodDirection.WEST,
			LodDirection.EAST,
			LodDirection.NORTH,
			LodDirection.SOUTH };
	
	/** North, South, East, West */
	public static final LodDirection[] ADJ_DIRECTIONS = new LodDirection[] {
			LodDirection.EAST,
			LodDirection.WEST,
			LodDirection.SOUTH,
			LodDirection.NORTH };
	
	/** All the faces and vertices of a cube. This is used to extract the vertex from the column */
	@SuppressWarnings("serial")
	public static final Map<LodDirection, int[][]> DIRECTION_VERTEX_MAP = new HashMap<LodDirection, int[][]>()
	{{
		put(LodDirection.UP, new int[][] {
				{ 0, 1, 0 }, // 0
				{ 0, 1, 1 }, // 1
				{ 1, 1, 1 }, // 2
				
				{ 0, 1, 0 }, // 0
				{ 1, 1, 1 }, // 2
				{ 1, 1, 0 } // 3
		});
		put(LodDirection.DOWN, new int[][] {
				{ 1, 0, 0 }, // 0
				{ 1, 0, 1 }, // 1
				{ 0, 0, 1 }, // 2
				
				{ 1, 0, 0 }, // 0
				{ 0, 0, 1 }, // 2
				{ 0, 0, 0 } // 3
		 });
		put(LodDirection.EAST, new int[][] {
				{ 1, 1, 0 }, // 0
				{ 1, 1, 1 }, // 1
				{ 1, 0, 1 }, // 2
				
				{ 1, 1, 0 }, // 0
				{ 1, 0, 1 }, // 2
				{ 1, 0, 0 } }); // 3
		put(LodDirection.WEST, new int[][] {
				{ 0, 0, 0 }, // 0
				{ 0, 0, 1 }, // 1
				{ 0, 1, 1 }, // 2
				
				{ 0, 0, 0 }, // 0
				{ 0, 1, 1 }, // 2
				{ 0, 1, 0 } // 3
		});
		put(LodDirection.SOUTH, new int[][] {
				{ 1, 0, 1 }, // 0
				{ 1, 1, 1 }, // 1
				{ 0, 1, 1 }, // 2
				
				{ 1, 0, 1 }, // 0
				{ 0, 1, 1 }, // 2
				{ 0, 0, 1 } // 3
		});
		put(LodDirection.NORTH, new int[][] {
				{ 0, 0, 0 }, // 0
				{ 0, 1, 0 }, // 1
				{ 1, 1, 0 }, // 2
				
				{ 0, 0, 0 }, // 0
				{ 1, 1, 0 }, // 2
				{ 1, 0, 0 } // 3
		});
	}};
	
	
	/**
	 * This indicates which position is invariable in the DIRECTION_VERTEX_MAP.
	 * Is used to extract the vertex
	 */
	@SuppressWarnings("serial")
	public static final Map<LodDirection, int[]> FACE_DIRECTION = new HashMap<LodDirection, int[]>()
	{{
		put(LodDirection.UP, new int[] { Y, MAX });
		put(LodDirection.DOWN, new int[] { Y, MIN });
		put(LodDirection.EAST, new int[] { X, MAX });
		put(LodDirection.WEST, new int[] { X, MIN });
		put(LodDirection.SOUTH, new int[] { Z, MAX });
		put(LodDirection.NORTH, new int[] { Z, MIN });
	}};
	
	
	/**
	 * This is a map from Direction to the relative normal vector
	 * we are using this since I'm not sure if the getNormal create new object at every call
	 */
	@SuppressWarnings("serial")
	public static final Map<LodDirection, Vec3i> DIRECTION_NORMAL_MAP = new HashMap<LodDirection, Vec3i>()
	{{
		put(LodDirection.UP, LodDirection.UP.getNormal());
		put(LodDirection.DOWN, LodDirection.DOWN.getNormal());
		put(LodDirection.EAST, LodDirection.EAST.getNormal());
		put(LodDirection.WEST, LodDirection.WEST.getNormal());
		put(LodDirection.SOUTH, LodDirection.SOUTH.getNormal());
		put(LodDirection.NORTH, LodDirection.NORTH.getNormal());
	}};
	
	/** We use this index for all array that are going to */
	@SuppressWarnings("serial")
	public static final Map<LodDirection, Integer> DIRECTION_INDEX = new HashMap<LodDirection, Integer>()
	{{
		put(LodDirection.UP, 0);
		put(LodDirection.DOWN, 1);
		put(LodDirection.EAST, 2);
		put(LodDirection.WEST, 3);
		put(LodDirection.SOUTH, 4);
		put(LodDirection.NORTH, 5);
	}};
	
	@SuppressWarnings("serial")
	public static final Map<LodDirection, Integer> ADJ_DIRECTION_INDEX = new HashMap<LodDirection, Integer>()
	{{
		put(LodDirection.EAST, 0);
		put(LodDirection.WEST, 1);
		put(LodDirection.SOUTH, 2);
		put(LodDirection.NORTH, 3);
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
	public final Map<LodDirection, int[]> adjHeight;
	public final Map<LodDirection, int[]> adjDepth;
	public final Map<LodDirection, byte[]> skyLights;
	public byte blockLight;
	
	/** Holds if the given direction should be culled or not */
	public final boolean[] culling;
	
	
	/** creates an empty box */
	@SuppressWarnings("serial")
	public VertexOptimizer()
	{
		boxOffset = new int[3];
		boxWidth = new int[3];
		
		colorMap = new int[6];
		skyLights = new HashMap<LodDirection, byte[]>()
		{{
			put(LodDirection.UP, new byte[1]);
			put(LodDirection.DOWN, new byte[1]);
			put(LodDirection.EAST, new byte[LodUtil.MAX_NUMBER_OF_VERTICAL_LODS]);
			put(LodDirection.WEST, new byte[LodUtil.MAX_NUMBER_OF_VERTICAL_LODS]);
			put(LodDirection.SOUTH, new byte[LodUtil.MAX_NUMBER_OF_VERTICAL_LODS]);
			put(LodDirection.NORTH, new byte[LodUtil.MAX_NUMBER_OF_VERTICAL_LODS]);
		}};
		adjHeight = new HashMap<LodDirection, int[]>()
		{{
			put(LodDirection.EAST, new int[LodUtil.MAX_NUMBER_OF_VERTICAL_LODS]);
			put(LodDirection.WEST, new int[LodUtil.MAX_NUMBER_OF_VERTICAL_LODS]);
			put(LodDirection.SOUTH, new int[LodUtil.MAX_NUMBER_OF_VERTICAL_LODS]);
			put(LodDirection.NORTH, new int[LodUtil.MAX_NUMBER_OF_VERTICAL_LODS]);
		}};
		adjDepth = new HashMap<LodDirection, int[]>()
		{{
			put(LodDirection.EAST, new int[LodUtil.MAX_NUMBER_OF_VERTICAL_LODS]);
			put(LodDirection.WEST, new int[LodUtil.MAX_NUMBER_OF_VERTICAL_LODS]);
			put(LodDirection.SOUTH, new int[LodUtil.MAX_NUMBER_OF_VERTICAL_LODS]);
			put(LodDirection.NORTH, new int[LodUtil.MAX_NUMBER_OF_VERTICAL_LODS]);
		}};
		
		culling = new boolean[6];
	}
	
	/** Set the light of the columns */
	public void setLights(int skyLight, int blockLight)
	{
		this.blockLight = (byte) blockLight;
		skyLights.get(LodDirection.UP)[0] = (byte) skyLight;
	}
	
	/**
	 * Set the color of the columns
	 * @param color color to add
	 * @param adjShadeDisabled this array indicates which face does not need shading
	 */
	public void setColor(int color, boolean[] adjShadeDisabled)
	{
		this.color = color;
		for (LodDirection lodDirection : DIRECTIONS)
		{
			if (!adjShadeDisabled[DIRECTION_INDEX.get(lodDirection)])
				colorMap[DIRECTION_INDEX.get(lodDirection)] = ColorUtil.applyShade(color, MC.getShade(lodDirection));
			else
				colorMap[DIRECTION_INDEX.get(lodDirection)] = color;
		}
	}
	
	/**
	 * @param lodDirection of the face of which we want to get the color
	 * @return color of the face
	 */
	public int getColor(LodDirection lodDirection)
	{
		if (CONFIG.client().advanced().debugging().getDebugMode() != DebugMode.SHOW_DETAIL)
			return colorMap[DIRECTION_INDEX.get(lodDirection)];
		else
			return ColorUtil.applyShade(color, MC.getShade(lodDirection));
	}
	
	/**
	 */
	public byte getSkyLight(LodDirection lodDirection, int verticalIndex)
	{
		if(lodDirection == LodDirection.UP || lodDirection == LodDirection.DOWN)
			return skyLights.get(lodDirection)[0];
		else
			return skyLights.get(lodDirection)[verticalIndex];
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
		for (LodDirection lodDirection : ADJ_DIRECTIONS)
		{
			for (int i = 0; i < adjHeight.get(lodDirection).length; i++)
			{
				adjHeight.get(lodDirection)[i] = VOID_FACE;
				adjDepth.get(lodDirection)[i] = VOID_FACE;
				skyLights.get(lodDirection)[i] = 0;
			}
		}
	}
	
	/** determine which faces should be culled */
	public void setUpCulling(int cullingDistance, AbstractBlockPosWrapper playerPos)
	{
		for (LodDirection lodDirection : DIRECTIONS)
		{
			if (lodDirection == LodDirection.DOWN || lodDirection == LodDirection.WEST || lodDirection == LodDirection.NORTH)
				culling[DIRECTION_INDEX.get(lodDirection)] = playerPos.get(lodDirection.getAxis()) > getFacePos(lodDirection) + cullingDistance;
			
			else if (lodDirection == LodDirection.UP || lodDirection == LodDirection.EAST || lodDirection == LodDirection.SOUTH)
				culling[DIRECTION_INDEX.get(lodDirection)] = playerPos.get(lodDirection.getAxis()) < getFacePos(lodDirection) - cullingDistance;
			
			culling[DIRECTION_INDEX.get(lodDirection)] = false;
		}
	}
	
	/**
	 * @param lodDirection direction that we want to check if it's culled
	 * @return true if and only if the face of the direction is culled
	 */
	public boolean isCulled(LodDirection lodDirection)
	{
		return culling[DIRECTION_INDEX.get(lodDirection)];
	}
	
	
	/**
	 * This method create all the shared face culling based on the adjacent data
	 * @param adjData data adjacent to the column we are going to render
	 */
	public void setAdjData(Map<LodDirection, long[]> adjData)
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
		singleAdjDataPoint = adjData.get(LodDirection.DOWN)[0];
		if(DataPointUtil.doesItExist(singleAdjDataPoint))
			skyLights.get(LodDirection.DOWN)[0] = DataPointUtil.getLightSkyAlt(singleAdjDataPoint);
		else
			skyLights.get(LodDirection.DOWN)[0] = skyLights.get(LodDirection.UP)[0];
		//other sided
		//TODO clean some similar cases
		for (LodDirection lodDirection : ADJ_DIRECTIONS)
		{
			if (isCulled(lodDirection))
				continue;
			
			long[] dataPoint = adjData.get(lodDirection);
			if (dataPoint == null || DataPointUtil.isVoid(dataPoint[0]))
			{
				adjHeight.get(lodDirection)[0] = maxY;
				adjDepth.get(lodDirection)[0] = minY;
				adjHeight.get(lodDirection)[1] = VOID_FACE;
				adjDepth.get(lodDirection)[1] = VOID_FACE;
				skyLights.get(lodDirection)[0] = 15; //in void set full skylight
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
							adjHeight.get(lodDirection)[0] = getMaxY();
							adjDepth.get(lodDirection)[0] = getMinY();
							skyLights.get(lodDirection)[0] = DataPointUtil.getLightSkyAlt(singleAdjDataPoint); //skyLights.get(Direction.UP)[0];
						}
						else
						{
							adjDepth.get(lodDirection)[faceToDraw] = getMinY();
							skyLights.get(lodDirection)[faceToDraw] = DataPointUtil.getLightSkyAlt(singleAdjDataPoint);
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
							adjHeight.get(lodDirection)[0] = VOID_FACE;
							adjDepth.get(lodDirection)[0] = VOID_FACE;
						}
						else // height < maxY
						{
							// the adj data intersects the lower part of the current data
							// if this is the only face, use the maxY and break,
							// if there was another face we finish the last one and break
							if (firstFace)
							{
								adjHeight.get(lodDirection)[0] = getMaxY();
								adjDepth.get(lodDirection)[0] = height;
								skyLights.get(lodDirection)[0] = DataPointUtil.getLightSkyAlt(singleAdjDataPoint); //skyLights.get(Direction.UP)[0];
							}
							else
							{
								adjDepth.get(lodDirection)[faceToDraw] = height;
								skyLights.get(lodDirection)[faceToDraw] = DataPointUtil.getLightSkyAlt(singleAdjDataPoint);
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
						adjHeight.get(lodDirection)[faceToDraw] = depth;
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
							adjHeight.get(lodDirection)[0] = getMaxY();
						}
						
						adjDepth.get(lodDirection)[faceToDraw] = height;
						skyLights.get(lodDirection)[faceToDraw] = DataPointUtil.getLightSkyAlt(singleAdjDataPoint);
						faceToDraw++;
						adjHeight.get(lodDirection)[faceToDraw] = depth;
						firstFace = false;
						toFinish = true;
						toFinishIndex = i + 1;
					}
				}
			}
			
			if (allAbove)
			{
				adjHeight.get(lodDirection)[0] = getMaxY();
				adjDepth.get(lodDirection)[0] = getMinY();
				skyLights.get(lodDirection)[0] = skyLights.get(LodDirection.UP)[0];
				faceToDraw++;
			}
			else if (toFinish)
			{
				adjDepth.get(lodDirection)[faceToDraw] = minY;
				if(toFinishIndex < dataPoint.length)
				{
					singleAdjDataPoint = dataPoint[toFinishIndex];
					if (DataPointUtil.doesItExist(singleAdjDataPoint))
						skyLights.get(lodDirection)[faceToDraw] = DataPointUtil.getLightSkyAlt(singleAdjDataPoint);
					else
						skyLights.get(lodDirection)[faceToDraw] = skyLights.get(LodDirection.UP)[0];
				}
				faceToDraw++;
			}
			
			adjHeight.get(lodDirection)[faceToDraw] = VOID_FACE;
			adjDepth.get(lodDirection)[faceToDraw] = VOID_FACE;
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
	 * @param lodDirection that we want to check
	 * @return position in the axis of the face
	 */
	public int getFacePos(LodDirection lodDirection)
	{
		return boxOffset[FACE_DIRECTION.get(lodDirection)[0]] + boxWidth[FACE_DIRECTION.get(lodDirection)[0]] * FACE_DIRECTION.get(lodDirection)[1];
	}
	
	/**
	 * returns true if the given direction should be rendered.
	 */
	public boolean shouldRenderFace(LodDirection lodDirection, int adjIndex)
	{
		if (lodDirection == LodDirection.UP || lodDirection == LodDirection.DOWN)
			return adjIndex == 0;
		return !(adjHeight.get(lodDirection)[adjIndex] == VOID_FACE && adjDepth.get(lodDirection)[adjIndex] == VOID_FACE);
	}
	
	
	/**
	 * @param lodDirection direction of the face we want to render
	 * @param vertexIndex index of the vertex of the face (0-1-2-3)
	 * @return position x of the relative vertex
	 */
	public int getX(LodDirection lodDirection, int vertexIndex)
	{
		return boxOffset[X] + boxWidth[X] * DIRECTION_VERTEX_MAP.get(lodDirection)[vertexIndex][X];
	}
	
	/**
	 * @param lodDirection direction of the face we want to render
	 * @param vertexIndex index of the vertex of the face (0-1-2-3)
	 * @return position y of the relative vertex
	 */
	public int getY(LodDirection lodDirection, int vertexIndex)
	{
		return boxOffset[Y] + boxWidth[Y] * DIRECTION_VERTEX_MAP.get(lodDirection)[vertexIndex][Y];
	}
	
	/**
	 * @param lodDirection direction of the face we want to render
	 * @param vertexIndex index of the vertex of the face (0-1-2-3)
	 * @param adjIndex, index of the n-th culled face of this direction
	 * @return position x of the relative vertex
	 */
	public int getY(LodDirection lodDirection, int vertexIndex, int adjIndex)
	{
		if (lodDirection == LodDirection.DOWN || lodDirection == LodDirection.UP)
			return boxOffset[Y] + boxWidth[Y] * DIRECTION_VERTEX_MAP.get(lodDirection)[vertexIndex][Y];
		else
		{
			// this could probably be cleaned up more,
			// but it still works
			if (1 - DIRECTION_VERTEX_MAP.get(lodDirection)[vertexIndex][Y] == ADJACENT_HEIGHT_INDEX)
				return adjHeight.get(lodDirection)[adjIndex];
			else
				return adjDepth.get(lodDirection)[adjIndex];
		}
	}
	
	/**
	 * @param lodDirection direction of the face we want to render
	 * @param vertexIndex index of the vertex of the face (0-1-2-3)
	 * @return position z of the relative vertex
	 */
	public int getZ(LodDirection lodDirection, int vertexIndex)
	{
		return boxOffset[Z] + boxWidth[Z] * DIRECTION_VERTEX_MAP.get(lodDirection)[vertexIndex][Z];
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
