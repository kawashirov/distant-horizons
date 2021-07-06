package com.seibel.lod.objects.quadTree;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
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
    private LodNodeData lodNodeData;
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
        this(null, new LodNodeData(LodNodeData.REGION_LEVEL, regionX, regionZ));
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
        this(parent, new LodNodeData(level, posX, posZ));
    }

    /**
     * Constructor for generic level via the LodNodeData
     *
     * @param lodNodeData object containing all the information of this node
     */
    public LodQuadTree(LodQuadTree parent, LodNodeData lodNodeData) {
        this.parent = parent;
        this.lodNodeData = lodNodeData;
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
    public LodQuadTree(List<LodNodeData> dataList, int regionX, int regionZ) {
        this(null, new LodNodeData(LodNodeData.REGION_LEVEL, regionX, regionZ));
        this.setNodesAtLowerLevel(dataList, true);
    }


    /**
     * @param dataList          list of data to put in the node
     * @param updateHigherLevel will update the color and height of higher level only if true
     */
    public void setNodesAtLowerLevel(List<LodNodeData> dataList, boolean updateHigherLevel) {
        for (LodNodeData lodNodeData : dataList) {
            //this is slow, you could set update to false and use an only top down update method.
            this.setNodeAtLowerLevel(lodNodeData, updateHigherLevel);
        }
    }

    /**
     * @param newLodNodeData    data to put in the node
     * @param updateHigherLevel will update the color and height of higher level only if true
     * @return true only if the QuadTree has been changed
     */
    public boolean setNodeAtLowerLevel(LodNodeData newLodNodeData, boolean updateHigherLevel) {
        //check if we try to introduce a level that is higher or equal than the current one
        byte targetLevel = newLodNodeData.level;
        byte currentLevel = lodNodeData.level;
        if (targetLevel < currentLevel) {
            int posX = newLodNodeData.posX;
            int posZ = newLodNodeData.posZ;
            short widthRatio = (short) (lodNodeData.width / newLodNodeData.width);
            int NS = (posX / widthRatio) % 2;
            int WE = (posZ / widthRatio) % 2;
            if (getChild(NS, WE) == null) {
                setChild(NS, WE);
            }
            LodQuadTree child = getChild(NS, WE);
            if (!newLodNodeData.real && child.isNodeReal()) {
                return false;
            } else {
                if (targetLevel == currentLevel - 1) {
                    child.setLodNodeData(newLodNodeData, true);
                    return true;
                } else {
                    return child.setNodeAtLowerLevel(newLodNodeData, updateHigherLevel);
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
    public LodNodeData getNodeAtLevelPosition(int posX, int posZ, byte level) {
        byte targetLevel = level;
        byte currentLevel = lodNodeData.level;
        if (targetLevel == currentLevel) {
            return lodNodeData;
        } else if (targetLevel < currentLevel) {
            short widthRatio = (short) (lodNodeData.width / Math.pow(2, level));
            int NS = (posX / widthRatio) % lodNodeData.posX;
            int WE = (posZ / widthRatio) % lodNodeData.posZ;
            if (getChild(NS, WE) == null) {
                return lodNodeData;
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
     * @param newLodNodeData data to put in the child
     * @param NS             North-South position
     * @param WE             West-East position
     */
    public void setChild(LodNodeData newLodNodeData, int NS, int WE) {
        if (newLodNodeData.level == lodNodeData.level - 1) {
            children[NS][WE] = new LodQuadTree(this, lodNodeData);
        }
    }

    /**
     * setChild will put a child with given data in the given position
     *
     * @param newLodNodeData data to put in the child
     */
    public void setChild(LodNodeData newLodNodeData) {
        if (newLodNodeData.level == lodNodeData.level - 1) {
            int NS = newLodNodeData.posX % lodNodeData.posX;
            int WE = newLodNodeData.posZ % lodNodeData.posZ;
            children[NS][WE] = new LodQuadTree(this, lodNodeData);
        }
    }

    /**
     * setChild will put a child in the given position
     *
     * @param NS North-South position
     * @param WE West-East position
     */
    public void setChild(int NS, int WE) {
        int childX = lodNodeData.posX * 2 + WE;
        int childZ = lodNodeData.posZ * 2 + NS;
        children[NS][WE] = new LodQuadTree(this, (byte) (lodNodeData.level - 1), childX, childZ);
    }

    /**
     * Update level update the level data such as levelFull and lodNodeData.
     *
     * @param recursiveUpdate if recursive is true the update will rise up to the level 0
     */
    private void updateLevel(boolean recursiveUpdate) {
        boolean isFull = true;
        boolean isEmpty = true;
        List<LodNodeData> dataList = new ArrayList<>();
        for (int NS = 0; NS <= 1; NS++) {
            for (int WE = 0; WE <= 1; WE++) {
                if (children[NS][WE] != null) {
                    dataList.add(children[NS][WE].getLodNodeData());
                    isEmpty = false;
                } else {
                    isFull = false;
                }
            }
        }
        nodeFull = isFull;
        nodeEmpty = isEmpty;
        lodNodeData.combineData(dataList);
        if (lodNodeData.level < 9 && recursiveUpdate) {
            this.parent.updateLevel(recursiveUpdate);
        }
    }

    /**
     * method to get certain nodes from the LodQuadTree
     *
     * @param getOnlyReal  if true it will return only real nodes
     * @param getOnlyDirty if true it will return only dirty nodes
     * @param getOnlyLeaf  if true it will return only leaf nodes
     * @return list of nodes
     */
    public List<LodNodeData> getNodeList(boolean getOnlyReal, boolean getOnlyDirty, boolean getOnlyLeaf) {
        List<LodNodeData> nodeList = new ArrayList<>();
        if (!isThereAnyChild()) {
            if (!(getOnlyReal && !lodNodeData.dirty)
                    && !(getOnlyReal && !lodNodeData.real)) {
                nodeList.add(lodNodeData);
            }
        } else {
            if (!getOnlyLeaf
                    && !(getOnlyDirty && !lodNodeData.dirty)
                    && !(getOnlyReal && !lodNodeData.real)) {
                nodeList.add(lodNodeData);
            }
            for (int NS = 0; NS <= 1; NS++) {
                for (int WE = 0; WE <= 1; WE++) {
                    LodQuadTree child = children[NS][WE];
                    if (child != null) {
                        nodeList.addAll(child.getNodeList(getOnlyReal, getOnlyDirty, getOnlyLeaf));
                    }
                }
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
    public List<LodNodeData> getNodeToRender(int x, int z, byte targetLevel, int maxDistance, int minDistance) {
        int distance = (int) Math.sqrt(Math.pow(x + lodNodeData.centerX, 2) + Math.pow(z + lodNodeData.centerZ, 2));
        List<LodNodeData> nodeList = new ArrayList<>();
        if (distance > maxDistance || distance < minDistance || targetLevel < lodNodeData.level) {
            return nodeList;
        }
        if (targetLevel == lodNodeData.level || !isThereAnyChild()) {
            if (!lodNodeData.voidNode) {
                nodeList.add(lodNodeData);
                return nodeList;
            } else {
                return nodeList;
            }
        } else {
            for (int NS = 0; NS <= 1; NS++) {
                for (int WE = 0; WE <= 1; WE++) {
                    LodQuadTree child = children[NS][WE];
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
     * A level is generated only if it has child and is higher than the target level
     * @param x
     * @param z
     * @param targetLevel
     * @param maxDistance
     * @param minDistance
     * @return
     */
    public List<AbstractMap.SimpleEntry<LodQuadTree, Integer>> getLevelToGenerate(int x, int z, byte targetLevel, int maxDistance, int minDistance) {
        int distance = (int) Math.sqrt(Math.pow(x + lodNodeData.centerX, 2) + Math.pow(z + lodNodeData.centerZ, 2));
        List<AbstractMap.SimpleEntry<LodQuadTree, Integer>> nodeList = new ArrayList<>();
        if (distance > maxDistance || distance < minDistance || targetLevel > lodNodeData.level || lodNodeData.real) {
            return nodeList;
        }
        if(isThereAnyChild()) {
            //THIS LEVEL HAS CHILD SO IT'S GENERATED.
            if (targetLevel != lodNodeData.level) {
                for (int NS = 0; NS <= 1; NS++) {
                    for (int WE = 0; WE <= 1; WE++) {
                        if (children[NS][WE] == null) {
                            setChild(NS,WE);
                        }
                        LodQuadTree child = children[NS][WE];
                        nodeList.addAll(child.getLevelToGenerate(x, z, targetLevel, maxDistance, minDistance));
                    }
                }
            }
        } else {
            nodeList.add(new AbstractMap.SimpleEntry<>(this, distance));
        }
        return nodeList;
    }

    /**
     * simple getter for lodNodeData
     *
     * @return lodNodeData
     */
    public LodNodeData getLodNodeData() {
        return lodNodeData;
    }

    /**
     * setter for lodNodeData, to maintain a correct relationship between level this method force update on all parent
     *
     * @param newLodNodeData    data to set
     * @param updateHigherLevel if true it will update all the upper levels.
     */
    public void setLodNodeData(LodNodeData newLodNodeData, boolean updateHigherLevel) {
        if (this.lodNodeData == null) {
            this.lodNodeData = newLodNodeData;
        } else {
            this.lodNodeData.update(newLodNodeData);
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
        return lodNodeData.real;
    }

    public boolean isRenderable() {
        return (lodNodeData != null);
    }

    public String toString(){
        String s = lodNodeData.toString();
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
    }
}