package com.seibel.lod.fabric.wrappers.modAccessor;

import java.util.HashSet;

import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.IOptifineAccessor;

public class OptifineAccessor implements IOptifineAccessor
{

    @Override
    public String getModName()
    {
        return "Optifine-Fabric-1.18.X";
    }

    @Override
    public HashSet<AbstractChunkPosWrapper> getNormalRenderedChunks()
    {
        // TODO: Impl proper methods here
        return null;
    }

}
