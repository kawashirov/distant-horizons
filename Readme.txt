This branch is used only to test the QuadTree data structure. This branch cannot be used to compile the
mode and the classes that are not listed below in the next section are not updated to the last version
of the main branch. If you want to see an example of use check the method createAndShowGui in the
QuadTreeImage class.

https://imgur.com/a/PwQnGZT old test
https://imgur.com/a/UCcI2Sc new test (check this)

========================
QUADTREE VERSION - HOW IT WORK AND HOW TO USE
========================
This are the file that you should use if you want to import the quadTree to your current version.

DistanceGenerationMode      (added NONE)
LodQuadTreeDimensionFileHandler
LodDataPoint        (added hash and equal function)
LodQuadTreeNode
LodQuadTree
LodQuadTreeDimension
LodQuadTreeWorld    (this is identical to LodWorld but uses LodQuadTreeDimension)

and those two optional classes
BiomeColorsUtils    is a class that i've created that contain various static methods to create colors
    from a biome
QuadTreeImage       uses the ChunkBase map generation to test the QuadTree. You should refresh
    dependencies to import the KaptainWutax code (check the build.gradle if you want to use them.
    I still can only use the code in intellij but can't import it in the final mod jar, because
    i'm still new to this stuff, so it wouldn't work in game at the moment)

You should then update the builders, the renderer, the templates... to work with this.


--HOW IT WORKs
I've tried to make this classes as similar as possible to yours. This way you could even do the
same stuff that you are doing now like using all the Lod with the same quality. LodDetail is
not used anywhere and is replaced by a level value in LodQuadTreeNode.

A LodQuadTree has a quad tree structure. So it has 4 children of the same type and a LodQuadTreeNode
that contain all the information of the node such as position, level (the level is the depth of
the quad tree) and the LodDataPoint. If in the future you want to add multiple LodDataPoint per
position (maybe you want to show floating island) you could still do it by transforming the
lodDataPoint variable in a LodDataPoint array.

The two most important factor of a Node is the level and the level position. At level 9 you
find the region (of width 2^9=512)at level 4 you find the chunk (of width 2^4=16) and at
level 0 you find the blocks (of width 2^0=1). The pos is like the region pos and the chunk
pos but for every level. The complexity of a node indicate how the node was built, so i've
used the DistanceGenerationMode enum. The complexity is ordered by the order in the enum
(NONE -> BIOME_ONLY -> BIOME_ONLY_SIMULATE_HEIGHT -> SURFACE -> FEATURES -> SERVER).
The idea is that you cannot override a node with a node that is less complex. This way you
could use different type of generation based on the distance.

LodQuadTreeDimensionFileHandler is used to save all the region (quadTree). I've just
converted your method of saving to work with this.

---HOW TO USE

You can create the LodQuadTreeWorld and the LodQuadTreeDimension in the same way as yours.

You can build a LodQuadTreeNode by specifying the level, the position in the level,
the LodDataPoint, the complexity (the DistanceGenerationMode setting selected to generate
the information of the node or NONE if the node is fake and empty).

--How to select the node that i want to generate?
At the moment you can select this in two ways:

FIRST: use the getLodNodeToGenerate in lodQuadTreeDimension.
getNodeToGenerate(int x, int z, byte level, DistanceGenerationMode complexity, int maxDistance, int minDistance)

The x and z are the position of the player (or you could just put 0,0 to test the system)
the level is the depth at witch we want to generate the LOD. The complexity indicate the
complexity that we want to use to generate. So is a node "node1" has complexity SERVER and
we want to use FEATURES complexity to generate the nodes then "node1" will not be selected
because is more complex and we don't want do override it.  maxDistance and minDistance
indicate the range of distances at witch we want to generate the nodes.

IMPORTANT those nodes are given as LodQuadTree and not LodQuadTreeNode. The idea
is that you take a LodQuadTree object, the you put in it 4 lodQuadTreeNode in the
startX,startZ,centerX,centerZ coords, as done in the QuadTreeImage class. To put the node
you use the setNodeAtLowerLevel with the UpdateHigherLevel set to true.

SECOND: you do it in the same way you are doing it now.
The getLodFromCoordinates has been converted to work with quadTree. So you could just threat
the quadTree as a matrix for the generation step.


--How to select the node that i want to render?
At the moment you can select this in two ways:

FIRST:  use the getNodeToRender in lodQuadTreeDimension.
getNodeToRender(int x, int z, byte level, Set<DistanceGenerationMode> complexityMask, int maxDistance, int minDistance)

Works in a similar way to getLodNodeToGenerate. The main difference is that you have to
use a complexityMask, which indicate the complexity that you want to render (this way you
could deselect the rendering of a precise type of complexity like the SERVER or FEATURE for
the debug phase).
This method return a list of LodQuadTreeNode. This node may have different width so you
should convert the template to work with this correctly.

SECOND:  you just use the getLodNodeToGenerate at a certain level to get the node that
you want to render (if this node does not exist the it will return NULL)

LATEST CHANGES:
Now the getNodesToGenerate correctly works with any technique of node adding. I was adding 4 node,
1 for each child, but now you could even add just one node and it will work in the same way
(I still think that adding 4 child is the best technique)
