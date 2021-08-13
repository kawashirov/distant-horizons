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

import kaptainwutax.mcutils.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.Heightmap;

/**
 * This object contains position, size,
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
	
	/** If we ever have to use a heightmap for any reason, use this one. */
	public static final Heightmap.Type DEFAULT_HEIGHTMAP = Heightmap.Type.WORLD_SURFACE_WG;
	
	/** this is how many pieces of data are exported when toData is called */
	public static final int NUMBER_OF_DELIMITERS = 10;
	
	

	/** If this is set to true then toData will return
	 * the empty string */
	public boolean dontSave = false;
	
	/** X position relative to the Quad tree. */
	public final int posX;
	/** Z position relative to the Quad tree */
	public final int posZ;
	
	//Complexity indicate how the block was built. This is important because we could use
	public DistanceGenerationMode complexity;
	
	/** Indicates how complicated this node is. <br>
	 * Goes from 0 to 9; 0 being the deepest (block size) and 9 being the highest (region size) */
	public final byte detailLevel;
	
	/** Indicates the width in blocks of this node. <br>
	 * Goes from 1 to 512 */
	public final short width;

	
	/** holds the height, depth, and color data for this Node. */
	private LodDataPoint lodDataPoint;
	
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

		
		this.lodDataPoint = lodDataPoint;
		this.complexity = complexity;
		
		dirty = true;
		voidNode = false;
		dontSave = false;
	}

	public BlockPos getStart(){
		return new BlockPos(posX * width, 0, posZ * width);
	}

	public BlockPos getEnd(){
		return new BlockPos(posX * (width + 1) - 1, 0, posZ * (width + 1) - 1);
	}

	public BlockPos getCenter(){
		return new BlockPos(posX * width + width/2, 0, posZ * width + width/2);
	}

	/**
	 * @throws IllegalArgumentException if the data string doesn't have the correct number of delimited entries
	 */
	public LodQuadTreeNode(String data) throws IllegalArgumentException
	{
		// make sure there are the correct number of entries
		// in the data string
		int count = 0;
		
		for(int i = 0; i < data.length(); i++)
			if(data.charAt(i) == DATA_DELIMITER)
				count++;
		
		if(count != NUMBER_OF_DELIMITERS)
			throw new IllegalArgumentException("LodQuadTreeNode constructor givin an invalid string. The data given had " + count + " delimiters when it should have had " + NUMBER_OF_DELIMITERS + ".");
		
		
		// start reading the data string
		
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
		int red = Integer.parseInt(data.substring(lastIndex+1,index));
		lastIndex = index;
		index = data.indexOf(DATA_DELIMITER, lastIndex+1);
		int green = Integer.parseInt(data.substring(lastIndex+1,index));
		lastIndex = index;
		index = data.indexOf(DATA_DELIMITER, lastIndex+1);
		int blue = Integer.parseInt(data.substring(lastIndex+1,index));
		lastIndex = index;
		index = data.indexOf(DATA_DELIMITER, lastIndex+1);
		int alpha = Integer.parseInt(data.substring(lastIndex+1,index));
		Color color = new Color(red,green,blue,alpha);
		lodDataPoint = new LodDataPoint(height,depth,color);
		
		int isVoid = Integer.parseInt(data.substring(lastIndex+1,index));
		this.voidNode = (isVoid == 1);
		
		
		width = (short) Math.pow(2, detailLevel);
		
		dirty = false;
		dontSave = false;
	}
	
	
	
	//================//
	// data processes //
	//================//
	
	/**
	 * Replaces the data in this object.
	 */
	public void updateData(LodQuadTreeNode lodQuadTreeNode)
	{
		if (lodQuadTreeNode == null)
			return;
		
		lodDataPoint = lodQuadTreeNode.lodDataPoint;
		complexity = lodQuadTreeNode.complexity;
		voidNode = lodQuadTreeNode.voidNode;
		dontSave = lodQuadTreeNode.dontSave;
		
		dirty = true;
	}
	
	/**
	 * Combines and averages the data from a list of LodQuadTreeNodes into this node.
	 */
	public void combineData(List<LodQuadTreeNode> dataList)
	{
		if(dataList.isEmpty())
		{
			lodDataPoint = new LodDataPoint();
		}
		else
		{
			// TODO would it be better to use the average height/depth?
			// get the lowest height from the all the given LodQuadTreeNodes
			short height = (short) dataList.stream().mapToInt(x -> (int) x.getLodDataPoint().height).min().getAsInt();
			// get the highest depth
			short depth = (short) dataList.stream().mapToInt(x -> (int) x.getLodDataPoint().depth).max().getAsInt();
			
			// get the average color
			int red = dataList.stream().mapToInt(x -> x.getLodDataPoint().color.getRed()).sum() / dataList.size();
			int green = dataList.stream().mapToInt(x -> x.getLodDataPoint().color.getGreen()).sum() / dataList.size();
			int blue = dataList.stream().mapToInt(x -> x.getLodDataPoint().color.getBlue()).sum() / dataList.size();
			Color color = new Color(red,green,blue);
			
			lodDataPoint = new LodDataPoint(height,depth,color);
			
			
			// the new complexity is equal to the lowest complexity of the list
			DistanceGenerationMode minComplexity = DistanceGenerationMode.SERVER;
			for(LodQuadTreeNode node: dataList)
			{
				if (minComplexity.compareTo(node.complexity) > 0)
				{
					minComplexity = node.complexity;
				}
			}
			
			complexity = minComplexity;
			
			voidNode = lodDataPoint.isEmpty();
		}
		
		dirty = true;
		dontSave = false;
	}
	
	
	
	//===================//
	// basic comparisons //
	//===================//
	
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
	
	
	
	//=========================//
	// basic setters / getters //
	//=========================//
	
	public LodDataPoint getLodDataPoint()
	{
		return lodDataPoint;
	}
	
	public void setLodDataPoint(LodDataPoint newLodDataPoint)
	{
		lodDataPoint = newLodDataPoint;
		
		// update if this is node is currently void
		voidNode = (lodDataPoint == null);
	}
	
	public boolean isVoidNode()
	{
		return voidNode;
	}
	
	public boolean isDirty()
	{
		return dirty;
	}
	
	
	
	
	//========//
	// output //
	//========//
	
	@Override
	public int hashCode()
	{
		return Objects.hash(this.complexity, this.detailLevel, this.posX, this.posZ, this.lodDataPoint, this.voidNode);
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
