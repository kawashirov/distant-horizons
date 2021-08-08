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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.seibel.lod.enums.DistanceGenerationMode;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

/**
 * This object contains all data useful to render LodBlock in a region (32x32 chunk to 512x512 block)
 * for every node it contains the border of said node, its size, its block position in the world, 
 * color, height and depth.
 * 
 * @author Leonardo Amato
 * @author James Seibel
 * @version 8-7-2021
 */
public class LodQuadTree
{
	// note:
	// The term node correspond to a LodQuadTree object
	
	/*
    Example on how a LodQuadTreeRegion would be rendered (the number corresponds to the level of the LodNodeData)
    .___.___._______._______________.
    |6|6| 7 |       |               |
    |6|6|___|    8  |               |
    | 7 | 7 |       |               |
    |___|___|_______|       9       |
    |       |       |               |
    |    8  |    8  |               |
    |       |       |               |
    |_______|_______|_______________|
    |               |               |
    |               |               |
    |               |               |
    |       9       |       9       |
    |               |               |
    |               |               |
    |               |               |
    |_______________|_______________|
	 */
	
	
	/** If true SetNodesAtLowerLevel will update the color and height of all higher level nodes */
	public static boolean UPDATE_HIGHER_LEVEL = true;
	
	//data useful to render
	//if children are present then lodNodeData should be a combination of the lodData of the child. This can be
	//turned off by deselecting the recursive update in all update method.
	private LodQuadTreeNode lodNode;
	
	/*
    .____.____.
    | NW | NE | |
    |____|____| Z
    | SW | SE | |
    |____|____| V
    -----X---->

    North - negative z
    South - positive z
    West - negative x
    east - positive x
	 */
	
	
	/** treeFull is true if and only if all child are not null */
	private boolean treeFull;
	private boolean treeEmpty;
	
	/**
	 * The four child are based on the four diagonal cardinal directions.
	 * The first index is for North and South and the second index is for East and West. <br>
	 * Children should always be null for level 0.
	 */
	private final LodQuadTree[][] children;
	
	//parent should always be null for level 9, and always not null for other levels.
	private final LodQuadTree parent;
	
	
	
	
	
	/**
	 * Constructor for level 0 without LodNodeData (region level constructor)
	 *
	 * @param regionX indicate the x region position of the node
	 * @param regionZ indicate the z region position of the node
	 */
	//maybe the use of useLevelCoordinate could be changed. I could use a builder to do all this work.
	public LodQuadTree(RegionPos regionPos)
	{
		this(null, new LodQuadTreeNode(LodQuadTreeNode.REGION_LEVEL, regionPos.x, regionPos.z));
	}
	
	/**
	 * Constructor for generic world without LOD data
	 *
	 * @param parent parent of this node
	 * @param level  level of this note
	 * @param posX   position x in the level
	 * @param posZ   position z in the level
	 */
	public LodQuadTree(LodQuadTree parent, byte level, RegionPos regionPos)
	{
		this(parent, new LodQuadTreeNode(level, regionPos.x, regionPos.z));
	}
	
	/**
	 * Constructor for a generic world via the LodNodeData
	 *
	 * @param newLodNode LodQuadTreeNode containing all the information of this node
	 */
	public LodQuadTree(LodQuadTree newParent, LodQuadTreeNode newLodNode)
	{
		parent = newParent;
		lodNode = newLodNode;
		children = new LodQuadTree[2][2];
		treeEmpty = true;
		treeFull = false;
	}
	
	/**
	 * Constructor using a dataList
	 *
	 * @param dataList list of LodNodeData to put in this LodQuadTree
	 * @param regionX  x region coordinate
	 * @param regionZ  z region coordinate
	 */
	public LodQuadTree(List<LodQuadTreeNode> dataList, int regionX, int regionZ)
	{
		this(null, new LodQuadTreeNode(LodQuadTreeNode.REGION_LEVEL, regionX, regionZ));
		setNodesAtLowerLevel(dataList);
	}
	
	
	/**
	 * @param dataList list of data to put in the node
	 */
	public void setNodesAtLowerLevel(List<LodQuadTreeNode> dataList)
	{
		for (LodQuadTreeNode lodQuadTreeNode : dataList)
		{
			this.setNodeAtLowerLevel(lodQuadTreeNode);
		}
	}
	
	/**
	 * @param newLowerLodNode data to put in the node
	 * @return true only if the QuadTree has been changed
	 */
	public boolean setNodeAtLowerLevel(LodQuadTreeNode newLowerLodNode)
	{
		byte targetLevel = newLowerLodNode.detailLevel;
		byte currentLevel = lodNode.detailLevel;
		
		if (targetLevel >= currentLevel)
		{
			// we can't add a node that has a equal or higher
			// detail level than this region
			return false;
		}
		
		short widthRatio = (short) (lodNode.width / (2 * newLowerLodNode.width));
		int WE = Math.abs(Math.floorDiv(newLowerLodNode.posX , widthRatio) % 2);
		int NS = Math.abs(Math.floorDiv(newLowerLodNode.posZ , widthRatio) % 2);
		
		if (getChild(NS, WE) == null)
		{
			// if this child doesn't exist, create an empty one
			setChild(NS, WE);
		}
		
		LodQuadTree child = getChild(NS, WE);
		if (lodNode.compareComplexity(newLowerLodNode) > 0)
		{
			// the node we want to introduce is less complex than the current node
			// we don't want to override higher complexity with lower complexity
			return false;
		}
		else
		{
			if (targetLevel == currentLevel - 1)
			{
				// we are at the level we want to add the newLowerLodNode
				child.setLodNodeData(newLowerLodNode);
				return true;
			}
			else
			{
				// recurse until we reach the level we want to add the newLowerLodNode
				return child.setNodeAtLowerLevel(newLowerLodNode);
			}
		}
		
	}
	
	/**
	 * Gets the LodQuadTreeNode at the given chunkPos and detailLevel.
	 * Returns null if no such LodQuadTreeNode exists.
	 */
	public LodQuadTreeNode getNodeAtChunkPos(ChunkPos chunkPos, int detailLevel)
	{
		if (detailLevel > LodQuadTreeNode.REGION_LEVEL)
			throw new IllegalArgumentException("getNodeAtChunkPos given a level of \"" + detailLevel + "\" when \"" + LodQuadTreeNode.REGION_LEVEL + "\" is the max.");
		
		
		byte currentDetailLevel = lodNode.detailLevel;
		if (detailLevel == currentDetailLevel)
		{
			return lodNode;
		}
		else if (detailLevel < currentDetailLevel)
		{
			// the detail level we need is lower, go down a layer
			short widthRatio = (short) (lodNode.width / (2 * Math.pow(2, detailLevel)));
			int WE = Math.abs(Math.floorDiv(chunkPos.x , widthRatio) % 2);
			int NS = Math.abs(Math.floorDiv(chunkPos.z , widthRatio) % 2);
			if (getChild(NS, WE) == null)
			{
				return null;
			}
			
			LodQuadTree child = getChild(NS, WE);
			return child.getNodeAtChunkPos(chunkPos, detailLevel);
		}
		else
		{
			// the detail level was higher than this region's
			return null;
		}
		
	}
	
	
	/**
	 * Put a child with the given data into the given position.
	 *
	 * @param newLodNode data to put in the child
	 */
	public void setChild(LodQuadTreeNode newLodNode)
	{
		// the child must be 1 detail level lower than this region
		if (newLodNode.detailLevel == lodNode.detailLevel - 1)
		{
			int WE = newLodNode.posX % lodNode.posX;
			int NS = newLodNode.posZ % lodNode.posZ;
			children[NS][WE] = new LodQuadTree(this, lodNode);
		}
	}
	
	/**
	 * Put an empty child in the given position.
	 *
	 * @param NS North-South position
	 * @param WE West-East position
	 */
	public void setChild(int NS, int WE)
	{
		// TODO is this correctly converting to a regionPos?
		int childRegionX = lodNode.posX * 2 + WE;
		int childRegionZ = lodNode.posZ * 2 + NS;
		
		children[NS][WE] = new LodQuadTree(this, (byte) (lodNode.detailLevel - 1), new RegionPos(childRegionX, childRegionZ));
	}
	
	/**
	 * Update this region's data, specifically levelFull and lodNodeData.
	 *
	 * @param recursiveUpdate if recursive is true the update will rise up to the level 0
	 */
	private void updateRegion(boolean recursiveUpdate)
	{
		boolean isFull = true;
		boolean isEmpty = true;
		
		// determine if this region is empty or full
		List<LodQuadTreeNode> dataList = new ArrayList<>();
		for (int NS = 0; NS <= 1; NS++)
		{
			for (int WE = 0; WE <= 1; WE++)
			{
				if (getChild(NS,WE) != null)
				{
					dataList.add(getChild(NS,WE).getLodNodeData());
					isEmpty = false;
				}
				else
				{
					isFull = false;
				}
			}
		}
		
		treeFull = isFull;
		treeEmpty = isEmpty;
		
		// update this regions
		lodNode.combineData(dataList);
		
		// update sub regions if requested
		if (lodNode.detailLevel < LodQuadTreeNode.REGION_LEVEL && recursiveUpdate)
		{
			this.parent.updateRegion(recursiveUpdate);
		}
	}
	
	/**
	 * Returns nodes that match the given mask.
	 *
	 * @param complexityMask holds the DistanceGenerationModes to accept
	 * @param getOnlyDirty if true it will return only dirty nodes
	 * @param getOnlyLeaf  if true it will return only leaf nodes
	 * @return list of nodes
	 */
	public List<LodQuadTreeNode> getNodeListWithMask(Set<DistanceGenerationMode> complexityMask, boolean getOnlyDirty, 
			boolean getOnlyLeaf)
	{
		List<LodQuadTreeNode> nodeList = new ArrayList<>();
		
		if (hasChildren())
		{
			//There is at least 1 child
			
			// this detail level's node
			if (!getOnlyLeaf && !(getOnlyDirty && !lodNode.isDirty())
					&& complexityMask.contains(lodNode.complexity)) 
			{
				nodeList.add(lodNode);
			}
			
			// search the children for valid nodes
			for (int NS = 0; NS <= 1; NS++)
			{
				for (int WE = 0; WE <= 1; WE++)
				{
					LodQuadTree child = children[NS][WE];
					
					if (child != null)
					{
						nodeList.addAll(child.getNodeListWithMask(complexityMask, getOnlyDirty, getOnlyLeaf));
					}
				}
			}
		}
		else
		{
			// This tree has no children
			
			if (!(getOnlyDirty && !lodNode.isDirty()) && (complexityMask.contains(lodNode.complexity)))
			{
				nodeList.add(lodNode);
			}
		}
		
		return nodeList;
	}
	
	/**
	 * This method will return all the nodes that can be rendered
	 *
	 * @param playerPos   position of the player
	 * @param targetLevel minimum level that can be rendered
	 * @param maxDistance maximum distance from the player
	 * @param minDistance minimum distance from the player
	 * @return
	 */
	public List<LodQuadTreeNode> getNodeToRender(BlockPos playerPos, int targetLevel, 
			Set<DistanceGenerationMode> complexityMask, int maxDistance, int minDistance)
	{
		int x = playerPos.getX();
		int z = playerPos.getZ();
		
		List<Integer> distances = new ArrayList<>();
		distances.add((int) Math.sqrt(Math.pow(x - lodNode.startBlockPos.getX(), 2) + Math.pow(z - lodNode.startBlockPos.getZ(), 2)));
		distances.add((int) Math.sqrt(Math.pow(x - lodNode.startBlockPos.getX(), 2) + Math.pow(z - lodNode.endBlockPos.getZ(), 2)));
		distances.add((int) Math.sqrt(Math.pow(x - lodNode.endBlockPos.getX(), 2) + Math.pow(z - lodNode.startBlockPos.getZ(), 2)));
		distances.add((int) Math.sqrt(Math.pow(x - lodNode.endBlockPos.getX(), 2) + Math.pow(z - lodNode.endBlockPos.getZ(), 2)));
		
		int min = distances.stream().mapToInt(Integer::intValue).min().getAsInt();
		int max = distances.stream().mapToInt(Integer::intValue).max().getAsInt();
		List<LodQuadTreeNode> nodeList = new ArrayList<>();
		
		
		if (targetLevel <= lodNode.detailLevel && ((min <= maxDistance && max >= minDistance)))
		{
			// TODO why is !isNodeFull() here?
			if (targetLevel == lodNode.detailLevel || !isNodeFull())
			{
				// we have either reached the right detail level or this tree isn't full 
				
				if (!lodNode.isVoidNode() && complexityMask.contains(lodNode.complexity))
				{
					// this node isn't void and has the complexity level we are looking for
					nodeList.add(lodNode);
				}
			}
			else
			{
				// look for the correct targetLevel
				for (int NS = 0; NS <= 1; NS++)
				{
					for (int WE = 0; WE <= 1; WE++)
					{
						LodQuadTree child = getChild(NS, WE);
						if (child != null)
						{
							nodeList.addAll(child.getNodeToRender(playerPos, targetLevel, complexityMask, maxDistance, minDistance));
						}
					}
				}
			}
		}
		return nodeList;
	}
	
	
	/**
	 * Returns nodes that should be generated. <br>
	 * A node is generated only if it has child, is higher than the target level, and in the distance range.
	 */
	public List<AbstractMap.SimpleEntry<LodQuadTreeNode, Integer>> getNodesToGenerate(BlockPos playerPos, byte targetLevel, 
			DistanceGenerationMode complexityToGenerate, int maxDistance, int minDistance)
	{
		int x = playerPos.getX();
		int z = playerPos.getZ();
		
		List<Integer> distances = new ArrayList<>();
		distances.add((int) Math.sqrt(Math.pow(x - lodNode.startBlockPos.getX(), 2) + Math.pow(z - lodNode.startBlockPos.getZ(), 2)));
		distances.add((int) Math.sqrt(Math.pow(x - lodNode.startBlockPos.getX(), 2) + Math.pow(z - lodNode.endBlockPos.getZ(), 2)));
		distances.add((int) Math.sqrt(Math.pow(x - lodNode.endBlockPos.getX(), 2) + Math.pow(z - lodNode.startBlockPos.getZ(), 2)));
		distances.add((int) Math.sqrt(Math.pow(x - lodNode.endBlockPos.getX(), 2) + Math.pow(z - lodNode.endBlockPos.getZ(), 2)));
		
		int min = distances.stream().mapToInt(Integer::intValue).min().getAsInt();
		int max = distances.stream().mapToInt(Integer::intValue).max().getAsInt();
		List<AbstractMap.SimpleEntry<LodQuadTreeNode, Integer>> nodeList = new ArrayList<>();
		
		
		// TODO what is the purpose of isCoordianteInLevel?
		if (targetLevel <= lodNode.detailLevel && ((min <= maxDistance && max >= minDistance) || isCoordinateInQuadTree(playerPos)))
		{
			// TODO shouldn't tagetLevel be != lodNode.detailLevel?
			if(!hasChildren() || targetLevel == lodNode.detailLevel)
			{
				if (this.lodNode.complexity.compareTo(complexityToGenerate) <= 0 )
				{
					nodeList.add(new AbstractMap.SimpleEntry<LodQuadTreeNode, Integer>(this.lodNode, min));
				}
			}
			else
			{
				// check if there are nodes further down that need generation
				
				for (int NS = 0; NS <= 1; NS++)
				{
					for (int WE = 0; WE <= 1; WE++)
					{
						if (getChild(NS, WE) == null)
						{
							setChild(NS, WE);
						}
						
						nodeList.addAll(getChild(NS, WE).getNodesToGenerate(playerPos, targetLevel, complexityToGenerate, maxDistance, minDistance));
					}
				}
			}
		}
		
		return nodeList;
	}
	
	/**
	 * setter for lodNodeData, to maintain a correct relationship between worlds 
	 * this method forces an update on all parent nodes.
	 *
	 * @param newLodQuadTreeNode    data to set
	 */
	public void setLodNodeData(LodQuadTreeNode newLodQuadTreeNode)
	{
		if (this.lodNode == null)
		{
			this.lodNode = newLodQuadTreeNode;
		}
		else
		{
			this.lodNode.update(newLodQuadTreeNode);
		}
		
		//a recursive update is necessary to change the higher levels
		if (parent != null && UPDATE_HIGHER_LEVEL) 
		{
			parent.updateRegion(true);
		}
	}
	
	/**
	 * Returns if the given BlockPos is within the boundary of
	 * this LodQuadTree.
	 */
	public boolean isCoordinateInQuadTree(BlockPos pos)
	{
		return (lodNode.startBlockPos.getX() * lodNode.width <= pos.getX() && 
				lodNode.startBlockPos.getZ() * lodNode.width <= pos.getZ() && 
				lodNode.endBlockPos.getX() * lodNode.width  >= pos.getX() && 
				lodNode.endBlockPos.getZ() * lodNode.width  >= pos.getZ());
	}
	
	
	
	//================//
	// simple getters //
	//================//
	
	public LodQuadTree getChild(int NS, int WE)
	{
		return children[NS][WE];
	}
	
	public LodQuadTreeNode getLodNodeData()
	{
		return lodNode;
	}
	
	public boolean isNodeFull()
	{
		return treeFull;
	}
	
	public boolean hasChildren()
	{
		return !treeEmpty;
	}
	
	public boolean isRenderable()
	{
		return (lodNode != null);
	}
	
	
	@Override
	public String toString()
	{
		String s = lodNode.toString();
		return s;
		/*
        if(isThereAnyChild()){
            for (int NS = 0; NS <= 1; NS++) {
                for (int WE = 0; WE <= 1; WE++) {
                    LodQuadTree child = children[NS][WE];
                    if (child != null) {
                        s += '\n' + child.toString();
                    }
                }
            }
        }
        return s;
		*/
	}
}