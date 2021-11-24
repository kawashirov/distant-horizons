package com.seibel.lod.fabric.wrappers;

import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Stores any variables or code that
 * may be shared between wrapper objects.
 *
 * @author James Seibel
 * @version 11-20-2021
 */
public class WrapperUtil
{

	/** If we ever need to use a heightmap for any reason, use this one. */
	public static final Heightmap.Types DEFAULT_HEIGHTMAP = Heightmap.Types.WORLD_SURFACE_WG;
	
}
