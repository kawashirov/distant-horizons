/*
 *    This file is part of the LOD Mod, licensed under the GNU GPL v3 License.
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
package com.seibel.lod.objects;

import java.awt.Color;
import java.util.List;
import java.util.Objects;

import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.handlers.LodQuadTreeDimensionFileHandler;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.Heightmap;

/**
 * This object contains position
 * and color data for an LOD object.
 * 
 * @author Leonardo Amato
 * @author James Seibel
 * @version 8-8-2021
 */
public class LodQuadTreeNode
{
	/** This is what separates each piece of data in the toData method */
	private static final char DATA_DELIMITER = LodQuadTreeDimensionFileHandler.DATA_DELIMITER;
	
	/** alpha used when drawing chunks in debug mode */
	private static final int DEBUG_ALPHA = 255; // 0 - 255
	@SuppressWarnings("unused")
	private static final Color DEBUG_BLACK = new Color(0, 0, 0, DEBUG_ALPHA);
	@SuppressWarnings("unused")
	private static final Color DEBUG_WHITE = new Color(255, 255, 255, DEBUG_ALPHA);
	@SuppressWarnings("unused")
	private static final Color INVISIBLE = new Color(0,0,0,0);
	
	
	/** If we ever have to use a heightmap for any reason, use this one. */
	public static final Heightmap.Type DEFAULT_HEIGHTMAP = Heightmap.Type.WORLD_SURFACE_WG;
	
	
	/** If this is set to true then toData will return
	 * the empty string */
	public boolean dontSave = false;
	
	
	
	/** this is how many pieces of data are exported when toData is called */
	public static final int NUMBER_OF_DELIMITERS = 10;
	
	
	//Complexity indicate how the block was built. This is important because we could use
	public DistanceGenerationMode complexity;
	
	/** Indicates how complicated this node is. <br>
	 * Goes from 0 to 9, 0 being the deepest (block size) and 9 being the highest (region size) */
	public final byte detailLevel;
	/** 512 blocks wide */
	public static final byte REGION_LEVEL = 9;
	/** 16 blocks wide */
	public static final byte CHUNK_LEVEL = 4;
	/** 1 block wide */
	public static final byte BLOCK_LEVEL = 0;
	
	/** Indicates the width in blocks of this node. <br>
	 * Goes from 1 to 512 */
	public final short width;
	/** detail level 9 */
	public static final short REGION_WIDTH = 512;
	/** detail level 4 */
	public static final short CHUNK_WIDTH = 16;
	/** detail level 0 */
	public static final short BLOCK_WIDTH = 1;
	
	// these 2 values indicate the position of the LOD in the relative Level
	// this will be useful in the generation process
	/** X position relative to the Quad tree. */
	public final int posX;
	/** Z position relative to the Quad tree */
	public final int posZ;
	
	//these 4 value indicate the corner of the LOD block
	//they can be named SW, SE, NW, NE as the cardinal direction.
	//the start values should always be smaller than the end values.
	//All this value could be calculated from level, posx and posz
	//so they could be removed and replaced with just a getter
	public final BlockPos startBlockPos;
	public final BlockPos endBlockPos;
	//these 2 value indicate the center of the LodNode in real coordinate. This
	//can be used to calculate the distance from the player
	public final BlockPos center;
	
	public LodDataPoint lodDataPoint;
	
	/** if true this node doesn't have any data */
	public boolean voidNode;
	/** if dirty is true, then this node have unsaved changes */
	public boolean dirty;
	
	
	/**TODO There should be a check for the level. Level must be positive, i could use runtime exception or simple if*/
	/**TODO There should be a good way to create node that must not be saved
	 * For example loading a 64 region wide dimension that is fully generated is too much memory heavy.
	 * There should be a way to create Node that are approximated and at region level, so you could load those
	 * for far region, and then when you get closer you load the actual region from the file or you generate it.
	 * */
	
	
	
	/**
	 * Creates and empty LodDataPoint
	 * This LodDataPoint only contains the position data
	 * @param detailLevel of the node
	 * @param posX position x in the level
	 * @param posZ position z in the level
	 */
	public LodQuadTreeNode(byte detailLevel, int posX, int posZ)
	{
		this.detailLevel = detailLevel;
		
		this.posX = posX;
		this.posZ = posZ;
		
		width = (short) Math.pow(2, detailLevel);
		
		startBlockPos = new BlockPos(posX * width, 0, posZ * width);
		endBlockPos = new BlockPos(startBlockPos.getX() + width - 1, 0, startBlockPos.getZ() + width - 1);
		
		center = new BlockPos(startBlockPos.getX() + width/2, 0, startBlockPos.getZ() + width/2);
		
		lodDataPoint = new LodDataPoint();
		
		complexity = DistanceGenerationMode.NONE;
		
		dirty = true;
		voidNode = true;
		dontSave = true;
	}
	
	/**
	 * Constructor for a LodNodeData
	 * @param level
	 * @param posX
	 * @param posZ
	 * @param height
	 * @param depth
	 * @param color
	 * @param complexity
	 */
	public LodQuadTreeNode(byte level, int posX, int posZ, short height, short depth , Color color, DistanceGenerationMode complexity)
	{
		this(level, posX, posZ, new LodDataPoint(height,depth,color), complexity);
	}
	
	/**
	 * Constructor for a LodNodeData
	 * @param level
	 * @param posX
	 * @param posZ
	 * @param height
	 * @param depth
	 * @param color
	 * @param complexity
	 */
	public LodQuadTreeNode(byte level, int posX, int posZ, int height, int depth, Color color, DistanceGenerationMode complexity)
	{
		this(level, posX, posZ, new LodDataPoint(height,depth,color), complexity);
	}
	
	/**
	 * Constructor for a LodNodeData
	 * @param detailLevel level of this
	 * @param posX
	 * @param posZ
	 * @param lodDataPoint
	 * @param complexity
	 */
	public LodQuadTreeNode(byte detailLevel, int posX, int posZ, LodDataPoint lodDataPoint, DistanceGenerationMode complexity)
	{
		this.detailLevel = detailLevel;
		
		this.posX = posX;
		this.posZ = posZ;
		
		width = (short) Math.pow(2, detailLevel);
		
		startBlockPos = new BlockPos(posX * width, 0, posZ * width);
		
		endBlockPos = new BlockPos(startBlockPos.getX() + width - 1, 0, startBlockPos.getZ() + width - 1);
		center = new BlockPos(startBlockPos.getX() + width/2, 0, startBlockPos.getZ() + width/2);
		
		this.lodDataPoint = lodDataPoint;
		this.complexity = complexity;
		
		dirty = true;
		voidNode = false;
		dontSave = false;
	}
	
	public LodQuadTreeNode(String data)
	{
		int index = 0;
		int lastIndex = 0;
		
		index = data.indexOf(DATA_DELIMITER, 0);
		this.detailLevel = (byte) Integer.parseInt(data.substring(0,index));
		
		lastIndex = index;
		index = data.indexOf(DATA_DELIMITER, lastIndex+1);
		this.posX = Integer.parseInt(data.substring(lastIndex+1,index));
		
		lastIndex = index;
		index = data.indexOf(DATA_DELIMITER, lastIndex+1);
		this.posZ = Integer.parseInt(data.substring(lastIndex+1,index));
		
		lastIndex = index;
		index = data.indexOf(DATA_DELIMITER, lastIndex+1);
		this.complexity = DistanceGenerationMode.valueOf(data.substring(lastIndex+1,index));
		
		lastIndex = index;
		index = data.indexOf(DATA_DELIMITER, lastIndex+1);
		short height = (short) Integer.parseInt(data.substring(lastIndex+1,index));
		
		lastIndex = index;
		index = data.indexOf(DATA_DELIMITER, lastIndex+1);
		short depth = (short) Integer.parseInt(data.substring(lastIndex+1,index));
		
		lastIndex = index;
		index = data.indexOf(DATA_DELIMITER, lastIndex+1);
		int r = Integer.parseInt(data.substring(lastIndex+1,index));
		lastIndex = index;
		index = data.indexOf(DATA_DELIMITER, lastIndex+1);
		int g = Integer.parseInt(data.substring(lastIndex+1,index));
		lastIndex = index;
		index = data.indexOf(DATA_DELIMITER, lastIndex+1);
		int b = Integer.parseInt(data.substring(lastIndex+1,index));
		lastIndex = index;
		index = data.indexOf(DATA_DELIMITER, lastIndex+1);
		int a = Integer.parseInt(data.substring(lastIndex+1,index));
		Color color = new Color(r,g,b,a);
		lodDataPoint = new LodDataPoint(height,depth,color);
		
		int val = Integer.parseInt(data.substring(lastIndex+1,index));
		this.voidNode = (val == 1);
		
		
		width = (short) Math.pow(2, detailLevel);
		
		startBlockPos = new BlockPos(posX * width, 0, posZ * width);
		endBlockPos = new BlockPos(startBlockPos.getX() + width - 1, 0, startBlockPos.getZ() + width - 1);
		
		center = new BlockPos(startBlockPos.getX() + width/2, 0, startBlockPos.getZ() + width/2);
		
		dirty = false;
		dontSave = false;
	}
	
	
	
	
	public void update(LodQuadTreeNode lodQuadTreeNode)
	{
		this.lodDataPoint = lodQuadTreeNode.lodDataPoint;
		this.complexity = lodQuadTreeNode.complexity;
		this.voidNode = lodQuadTreeNode.voidNode;
		dirty = true;
		dontSave = false;
	}
	
	public LodDataPoint getLodDataPoint()
	{
		return lodDataPoint;
	}
	
	public void combineData(List<LodQuadTreeNode> dataList)
	{
		if(dataList.isEmpty())
		{
			lodDataPoint = new LodDataPoint();
		}
		else
		{
			short height = (short) dataList.stream().mapToInt(x -> (int) x.getLodDataPoint().height).min().getAsInt();
			short depth = (short) dataList.stream().mapToInt(x -> (int) x.getLodDataPoint().depth).max().getAsInt();
			int red = dataList.stream().mapToInt(x -> x.getLodDataPoint().color.getRed()).sum()/dataList.size();
			int green = dataList.stream().mapToInt(x -> x.getLodDataPoint().color.getGreen()).sum()/dataList.size();
			int blue = dataList.stream().mapToInt(x -> x.getLodDataPoint().color.getBlue()).sum()/dataList.size();
			Color color = new Color(red,green,blue);
			lodDataPoint = new LodDataPoint(height,depth,color);
			
			//the new complexity equal to the lowest complexity of the list
			DistanceGenerationMode minComplexity = DistanceGenerationMode.SERVER;
			for(LodQuadTreeNode node: dataList)
			{
				if (minComplexity.compareTo(node.complexity) > 0)
				{
					minComplexity = node.complexity;
				}
			}
			
			complexity = minComplexity;
			
			voidNode = dataList.stream().filter(x -> !x.voidNode).count() == 0;
		}
		
		dirty = true;
		dontSave = false;
	}
	
	
	@Override
	public int hashCode()
	{
		return Objects.hash(this.complexity, this.detailLevel, this.posX, this.posZ, this.lodDataPoint, this.voidNode);
	}
	
	public int compareComplexity(LodQuadTreeNode other)
	{
		return this.complexity.compareTo(other.complexity);
	}
	
	
	public boolean equals(LodQuadTreeNode other)
	{
		return (this.complexity == other.complexity
				&& this.detailLevel == other.detailLevel
				&& this.posX == other.posX
				&& this.posZ == other.posZ
				&& this.lodDataPoint.equals(other.lodDataPoint)
				&& this.complexity == other.complexity
				&& this.voidNode == other.voidNode);
	}
	
	
	public boolean isVoidNode()
	{
		return voidNode;
	}
	
	public boolean isDirty()
	{
		return dirty;
	}
	
	
	/**
	 * Outputs all data in a csv format
	 */
	public String toData()
	{
		if (dontSave)
			return "";
		
		String s = Integer.toString(detailLevel) + DATA_DELIMITER
				+ Integer.toString(posX) + DATA_DELIMITER
				+ Integer.toString(posZ) + DATA_DELIMITER
				+ complexity.toString() + DATA_DELIMITER
				+ Integer.toString((lodDataPoint.height)) + DATA_DELIMITER
				+ Integer.toString((lodDataPoint.depth)) + DATA_DELIMITER
				+ Integer.toString(lodDataPoint.color.getRed()) + DATA_DELIMITER
				+ Integer.toString(lodDataPoint.color.getGreen()) + DATA_DELIMITER
				+ Integer.toString(lodDataPoint.color.getBlue()) + DATA_DELIMITER
				+ Integer.toString(lodDataPoint.color.getAlpha()) + DATA_DELIMITER
				+ Integer.toString(voidNode ? 1 : 0);
		return s;
	}
	
	@Override
	public String toString()
	{
		return this.toData();
	}
	
}
