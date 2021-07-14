This branch is used only to test the QuadTree data structure. This branch cannot be used to compile the mode and the classes
that are not listed below in the next section are not updated to the last version of the main branch.

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
BiomeColorsUtils    is a class that i've created that contain various static methods to create colors from a biom
QuadTreeImage       uses the ChunkBase map generation to test the QuadTree. You should refresh dependencies to import
    the KaptainWutax code (check the build.gradle if you want to use them. I still can only use the code in intellij
    but can't import it in the final mod jar, because i'm still new to this stuff, so it wouldn't work in game at the moment)

You should then update the builders, the renderer, the templates... to work with this.


--HOW IT WORK
I've tried to make this classes as similar as possible to yours. This way you could even do the same stuff that you are doing now
like using all the Lod with the same quality. LodDetail is not used anywhere and is replaced by a level value in
LodQuadTreeNode.

A LodQuadTree has a quad tree structure. So it has 4 children of the same type and a LodQuadTreeNode that contain all
the information of the node such as position, level (the level is the depth of the quad tree) and the LodDataPoint.
If in the future you want to add multiple LodDataPoint per position (maybe you want to show floating island) you could still
do it by transforming the lodDataPoint variable in a LodDataPoint array.

The two most important factor of a Node is the level and the level position. At level 9 you find the region (of width 2^9=512)
at level 4 you find the chunk (of width 2^4=16) and at level 0 you find the blocks (of width 2^0=1). The pos is like the
region pos and the chunk pos but for every level.
The complexity of a node indicate how the node was built, so i've used the DistanceGenerationMode enum. The complexity is
ordered by the order in the enum (NONE -> BIOME_ONLY -> BIOME_ONLY_SIMULATE_HEIGHT -> SURFACE -> FEATURES -> SERVER). The idea
is that you cannot override a node with a node that is less complex. This way you could use different type of generation based
on the distance.

LodQuadTreeDimensionFileHandler is used to save all the region (quadTree). I've just converted your method of saving
to work with this.

--HOW TO USE

You can create the LodQuadTreeWorld and the LodQuadTreeDimension in the same way as yours.

You can build a LodQuadTreeNode by specifying the level, the position in the level, the LodDataPoint, the complexity
(the DistanceGenerationMode setting selected to generate the information of the node or NONE if the node is fake and empty).

How to select the node that i want to generate?
At the moment you can select this in two ways: the first is to use the getLodNodeToGenerate
