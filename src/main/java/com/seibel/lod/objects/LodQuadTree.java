package com.seibel.lod.objects;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This object contains all data useful to render LodBlock in a region (32x32 chunk o 512x512 block)
 * for every node it contains the border of the block, the size, the position at it's level, the color, the height and the depth.
 */
public class LodQuadTree {
    //notes
    //The term node correspond to a LodQuadTree object


    /*
    Example on how it will be rendered (the number correspond to the level in the LodNodeData)
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
    //data useful to render
    //if children are present then lodNodeData should be a combination of the lodData of the child. This can be
    //turned off by deselecting the recursive update in all update method.
    private LodQuadTreeNode lodQuadTreeNode;
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


    //level completed is true if and only if all child are not null
    private boolean nodeFull;
    private boolean nodeEmpty;

    //the four child based on the four diagonal cardinal direction
    //the first index is for N and S and the second index is for W and S
    //children should always be null for level 0.
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
    public LodQuadTree(int regionX, int regionZ) {
        this(null, new LodQuadTreeNode(LodQuadTreeNode.REGION_LEVEL, regionX, regionZ));
    }

    /**
     * Constructor for generic level without LodNodeData
     *
     * @param parent parent of this node
     * @param level  level of this note
     * @param posX   position x in the level
     * @param posZ   position z in the level
     */
    public LodQuadTree(LodQuadTree parent, byte level, int posX, int posZ) {
        this(parent, new LodQuadTreeNode(level, posX, posZ));
    }

    /**
     * Constructor for generic level via the LodNodeData
     *
     * @param lodQuadTreeNode object containing all the information of this node
     */
    public LodQuadTree(LodQuadTree parent, LodQuadTreeNode lodQuadTreeNode) {
        this.parent = parent;
        this.lodQuadTreeNode = lodQuadTreeNode;
        this.children = new LodQuadTree[2][2];
        this.nodeEmpty = true;
        this.nodeFull = false;
    }

    /**
     * Constructor using a dataList
     *
     * @param dataList list of LodNodeData to put in this LodQuadTree
     * @param regionX  x region coordinate
     * @param regionZ  z region coordinate
     */
    public LodQuadTree(List<LodQuadTreeNode> dataList, int regionX, int regionZ) {
        this(null, new LodQuadTreeNode(LodQuadTreeNode.REGION_LEVEL, regionX, regionZ));
        this.setNodesAtLowerLevel(dataList, true);
    }


    /**
     * @param dataList          list of data to put in the node
     * @param updateHigherLevel will update the color and height of higher level only if true
     */
    public void setNodesAtLowerLevel(List<LodQuadTreeNode> dataList, boolean updateHigherLevel) {
        for (LodQuadTreeNode lodQuadTreeNode : dataList) {
            //this is slow, you could set update to false and use an only top down update method.
            this.setNodeAtLowerLevel(lodQuadTreeNode, updateHigherLevel);
        }
    }

    /**
     * @param newLodQuadTreeNode    data to put in the node
     * @param updateHigherLevel will update the color and height of higher level only if true
     * @return true only if the QuadTree has been changed
     */
    public boolean setNodeAtLowerLevel(LodQuadTreeNode newLodQuadTreeNode, boolean updateHigherLevel) {
        //check if we try to introduce a level that is higher or equal than the current one
        byte targetLevel = newLodQuadTreeNode.level;
        byte currentLevel = lodQuadTreeNode.level;
        if (targetLevel < currentLevel) {
            int posX = newLodQuadTreeNode.posX;
            int posZ = newLodQuadTreeNode.posZ;
            short widthRatio = (short) (lodQuadTreeNode.width / (2 * newLodQuadTreeNode.width));
            int WE = Math.abs(Math.floorDiv(posX , widthRatio) % 2);
            int NS = Math.abs(Math.floorDiv(posZ , widthRatio) % 2);
            //These two if fix the negative coordinate problema
            //I don't know why, there is some problem with the %2 operation
            /*
            if(posX<0) WE = 1 - WE;
            if(posZ<0) NS = 1 - NS;
            */
            if(posX<0) System.out.println(WE);
            if(posZ<0) System.out.println(NS);

            if (getChild(NS, WE) == null) {
                setChild(NS, WE);
            }
            LodQuadTree child = getChild(NS, WE);
            if (!newLodQuadTreeNode.real && child.isNodeReal()) {
                return false;
            } else {
                if (targetLevel == currentLevel - 1) {
                    child.setLodNodeData(newLodQuadTreeNode, true);
                    return true;
                } else {
                    return child.setNodeAtLowerLevel(newLodQuadTreeNode, updateHigherLevel);
                }
            }
        } else {
            return false;
        }

    }

    /**
     * @param posX
     * @param posZ
     * @param level
     * @return
     */
    public LodQuadTreeNode getNodeAtLevelPosition(int posX, int posZ, byte level) {
        byte targetLevel = level;
        byte currentLevel = lodQuadTreeNode.level;
        if (targetLevel == currentLevel) {
            return lodQuadTreeNode;
        } else if (targetLevel < currentLevel) {
            short widthRatio = (short) (lodQuadTreeNode.width / (2 * Math.pow(2, level)));
            int WE = Math.abs(Math.floorDiv(posX , widthRatio) % 2);
            int NS = Math.abs(Math.floorDiv(posZ , widthRatio) % 2);
            if (getChild(NS, WE) == null) {
                return null;
            }
            LodQuadTree child = getChild(NS, WE);
            return child.getNodeAtLevelPosition(posX, posZ, level);
        } else {
            return null;
        }

    }


    public LodQuadTree getChild(int NS, int WE) {
        return children[NS][WE];
    }

    /**
     * setChild will put a child with given data in the given position
     *
     * @param newLodQuadTreeNode data to put in the child
     * @param NS             North-South position
     * @param WE             West-East position
     */
    public void setChild(LodQuadTreeNode newLodQuadTreeNode, int NS, int WE) {
        if (newLodQuadTreeNode.level == lodQuadTreeNode.level - 1) {
            children[NS][WE] = new LodQuadTree(this, lodQuadTreeNode);
        }
    }

    /**
     * setChild will put a child with given data in the given position
     *
     * @param newLodQuadTreeNode data to put in the child
     */
    public void setChild(LodQuadTreeNode newLodQuadTreeNode) {
        if (newLodQuadTreeNode.level == lodQuadTreeNode.level - 1) {
            int WE = newLodQuadTreeNode.posX % lodQuadTreeNode.posX;
            int NS = newLodQuadTreeNode.posZ % lodQuadTreeNode.posZ;
            children[NS][WE] = new LodQuadTree(this, lodQuadTreeNode);
        }
    }

    /**
     * setChild will put a child in the given position
     *
     * @param NS North-South position
     * @param WE West-East position
     */
    public void setChild(int NS, int WE) {
        int childX = lodQuadTreeNode.posX * 2 + WE;
        int childZ = lodQuadTreeNode.posZ * 2 + NS;
        children[NS][WE] = new LodQuadTree(this, (byte) (lodQuadTreeNode.level - 1), childX, childZ);
    }

    /**
     * Update level update the level data such as levelFull and lodNodeData.
     *
     * @param recursiveUpdate if recursive is true the update will rise up to the level 0
     */
    private void updateLevel(boolean recursiveUpdate) {
        boolean isFull = true;
        boolean isEmpty = true;
        List<LodQuadTreeNode> dataList = new ArrayList<>();
        for (int NS = 0; NS <= 1; NS++) {
            for (int WE = 0; WE <= 1; WE++) {
                if (getChild(NS,WE) != null) {
                    dataList.add(getChild(NS,WE).getLodNodeData());
                    isEmpty = false;
                } else {
                    isFull = false;
                }
            }
        }
        nodeFull = isFull;
        nodeEmpty = isEmpty;
        lodQuadTreeNode.combineData(dataList);
        if (lodQuadTreeNode.level < 9 && recursiveUpdate) {
            this.parent.updateLevel(recursiveUpdate);
        }
    }

    /**
     *
     * method to get certain nodes from the LodQuadTree
     *
     * @param complexityMask set of complexity to accept
     * @param getOnlyDirty if true it will return only dirty nodes
     * @param getOnlyLeaf  if true it will return only leaf nodes
     * @return list of nodes
     */
    public List<LodQuadTreeNode> getNodeList(Set<Integer> complexityMask, boolean getOnlyDirty, boolean getOnlyLeaf) {
        List<LodQuadTreeNode> nodeList = new ArrayList<>();
        if (isThereAnyChild()) {
            //There is at least 1 child
            if (!getOnlyLeaf
                    && !(getOnlyDirty && !lodQuadTreeNode.dirty)
                    && complexityMask.contains(lodQuadTreeNode.complexity)) {
                nodeList.add(lodQuadTreeNode);
            }
            for (int NS = 0; NS <= 1; NS++) {
                for (int WE = 0; WE <= 1; WE++) {
                    LodQuadTree child = children[NS][WE];
                    if (child != null) {
                        nodeList.addAll(child.getNodeList(complexityMask, getOnlyDirty, getOnlyLeaf));
                    }
                }
            }
        } else {
            //There are no children
            if (!(getOnlyDirty && !lodQuadTreeNode.dirty)
                    || (complexityMask.contains((int) lodQuadTreeNode.complexity))){
                nodeList.add(lodQuadTreeNode);
            }
        }

        return nodeList;
    }

    /**
     * This method will return all the nodes that can be rendered based on the data given
     *
     * @param x           position of the player
     * @param z           position of the player
     * @param targetLevel minimum level that can be rendered
     * @param maxDistance maximum distance from the player
     * @param minDistance minimum distance from the player
     * @return
     */
    public List<LodQuadTreeNode> getNodeToRender(int x, int z, byte targetLevel, int maxDistance, int minDistance) {
        int distance = (int) Math.sqrt(Math.pow(x - lodQuadTreeNode.centerX, 2) + Math.pow(z - lodQuadTreeNode.centerZ, 2));
        List<Integer> distances = new ArrayList();
        distances.add(distance);
        distances.add((int) Math.sqrt(Math.pow(x - lodQuadTreeNode.startX, 2) + Math.pow(z - lodQuadTreeNode.startZ, 2)));
        distances.add((int) Math.sqrt(Math.pow(x - lodQuadTreeNode.startX, 2) + Math.pow(z - lodQuadTreeNode.endZ, 2)));
        distances.add((int) Math.sqrt(Math.pow(x - lodQuadTreeNode.endX, 2) + Math.pow(z - lodQuadTreeNode.startZ, 2)));
        distances.add((int) Math.sqrt(Math.pow(x - lodQuadTreeNode.endX, 2) + Math.pow(z - lodQuadTreeNode.endZ, 2)));
        int min = distances.stream().mapToInt(Integer::intValue).min().getAsInt();
        int max = distances.stream().mapToInt(Integer::intValue).max().getAsInt();
        List<LodQuadTreeNode> nodeList = new ArrayList<>();
        if (targetLevel > lodQuadTreeNode.level) {
            return nodeList;
        }
        if ((min > maxDistance || max < minDistance) /*&& !isCoordinateInLevel(x,z)*/){
            return nodeList;
        }
        if (targetLevel == lodQuadTreeNode.level || !isNodeFull()) {
            if (lodQuadTreeNode.voidNode) {
                nodeList.add(lodQuadTreeNode);
                return nodeList;
            } else {
                nodeList.add(lodQuadTreeNode);
                return nodeList;
            }
        } else {
            for (int NS = 0; NS <= 1; NS++) {
                for (int WE = 0; WE <= 1; WE++) {
                    LodQuadTree child = getChild(NS,WE);
                    if (child != null) {
                        nodeList.addAll(child.getNodeToRender(x, z, targetLevel, maxDistance, minDistance));
                    }
                }
            }
        }
        return nodeList;
    }

    /**
     * Nodes that can be generated in the approximated version
     * A level is generated only if it has child and is higher than the target level and in the distance range
     * @param x
     * @param z
     * @param targetLevel
     * @param maxDistance
     * @param minDistance
     * @return
     */
    public List<AbstractMap.SimpleEntry<LodQuadTree, Integer>> getLevelToGenerate(int x, int z, byte targetLevel, int maxDistance, int minDistance) {
        int distance = (int) Math.sqrt(Math.pow(x - lodQuadTreeNode.centerX, 2) + Math.pow(z - lodQuadTreeNode.centerZ, 2));
        List<Integer> distances = new ArrayList();
        distances.add(distance);
        distances.add((int) Math.sqrt(Math.pow(x - lodQuadTreeNode.startX, 2) + Math.pow(z - lodQuadTreeNode.startZ, 2)));
        distances.add((int) Math.sqrt(Math.pow(x - lodQuadTreeNode.startX, 2) + Math.pow(z - lodQuadTreeNode.endZ, 2)));
        distances.add((int) Math.sqrt(Math.pow(x - lodQuadTreeNode.endX, 2) + Math.pow(z - lodQuadTreeNode.startZ, 2)));
        distances.add((int) Math.sqrt(Math.pow(x - lodQuadTreeNode.endX, 2) + Math.pow(z - lodQuadTreeNode.endZ, 2)));
        int min = distances.stream().mapToInt(Integer::intValue).min().getAsInt();
        int max = distances.stream().mapToInt(Integer::intValue).max().getAsInt();
        List<AbstractMap.SimpleEntry<LodQuadTree, Integer>> nodeList = new ArrayList<>();
        if ( targetLevel > lodQuadTreeNode.level ) {
            return nodeList;
        }
        if ((min > maxDistance || max < minDistance)/* && !isCoordinateInLevel(x,z)*/){
            return nodeList;
        }
        if(isNodeFull()) {
            //THIS LEVEL HAS CHILD SO IT'S GENERATED.
            if (targetLevel != lodQuadTreeNode.level) {
                for (int NS = 0; NS <= 1; NS++) {
                    for (int WE = 0; WE <= 1; WE++) {
                        if (getChild(NS,WE) == null) {
                            setChild(NS,WE);
                        }
                        LodQuadTree child = getChild(NS,WE);
                        nodeList.addAll(child.getLevelToGenerate(x, z, targetLevel, maxDistance, minDistance));
                    }
                }
            }
        } else {
            nodeList.add(new AbstractMap.SimpleEntry<>(this, distance));
            /*
            if(isThereAnyChild()){
                for (int NS = 0; NS <= 1; NS++) {
                    for (int WE = 0; WE <= 1; WE++) {
                        if (children[NS][WE] == null) {
                            setChild(NS,WE);
                            LodQuadTree child = children[NS][WE];
                            distance = (int) Math.sqrt(Math.pow(x - child.lodNodeData.centerX, 2) + Math.pow(z -  child.lodNodeData.centerZ, 2));
                            nodeList.add(new AbstractMap.SimpleEntry<>(child, distance));
                        }
                    }
                }
            }else{
            nodeList.add(new AbstractMap.SimpleEntry<>(this, distance));
            }

             */
        }
        return nodeList;
    }

    /**
     * simple getter for lodNodeData
     *
     * @return lodNodeData
     */
    public LodQuadTreeNode getLodNodeData() {
        return lodQuadTreeNode;
    }

    /**
     * setter for lodNodeData, to maintain a correct relationship between level this method force update on all parent
     *
     * @param newLodQuadTreeNode    data to set
     * @param updateHigherLevel if true it will update all the upper levels.
     */
    public void setLodNodeData(LodQuadTreeNode newLodQuadTreeNode, boolean updateHigherLevel) {
        if (this.lodQuadTreeNode == null) {
            this.lodQuadTreeNode = newLodQuadTreeNode;
        } else {
            this.lodQuadTreeNode.update(newLodQuadTreeNode);
        }
        //a recursive update is necessary to change higher level
        if (parent != null && updateHigherLevel) parent.updateLevel(true);
    }

    public boolean isNodeFull() {
        return nodeFull;
    }

    public boolean isThereAnyChild() {
        return !nodeEmpty;
    }

    public boolean isNodeReal() {
        return lodQuadTreeNode.real;
    }

    public boolean isRenderable() {
        return (lodQuadTreeNode != null);
    }


    public boolean isCoordinateInLevel(int x, int z){
        return !(lodQuadTreeNode.startX > x || lodQuadTreeNode.startZ > z || lodQuadTreeNode.endX < x || lodQuadTreeNode.endZ < z);
    }

    public String toString(){
        String s = lodQuadTreeNode.toString();
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