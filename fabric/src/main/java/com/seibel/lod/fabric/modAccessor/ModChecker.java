package com.seibel.lod.fabric.modAccessor;

import com.seibel.lod.core.wrapperInterfaces.modAccessor.IModChecker;
import net.fabricmc.loader.api.FabricLoader;

public class ModChecker implements IModChecker {
    public static final ModChecker INSTANCE = new ModChecker();

    @Override
    public boolean isModLoaded(String modid) {
        return FabricLoader.getInstance().isModLoaded(modid);
    }
}
