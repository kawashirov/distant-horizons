package com.seibel.lod.fabric.wrappers.modAccessor;

import com.seibel.lod.core.wrapperInterfaces.modAccessor.IImmersivePortalsAccessor;

public class ImmersivePortalsAccessor implements IImmersivePortalsAccessor {
    @Override
    public String getModName() {
        return "ImmersivePortals-Fabric";
    }

    public float partialTicks;
}
