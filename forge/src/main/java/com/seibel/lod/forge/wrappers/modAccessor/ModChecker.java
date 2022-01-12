package com.seibel.lod.forge.wrappers.modAccessor;

import com.seibel.lod.core.wrapperInterfaces.modAccessor.IModChecker;
import net.minecraftforge.fml.ModList;

public class ModChecker implements IModChecker {
    public static final ModChecker INSTANCE = new ModChecker();

    @Override
    public boolean isModLoaded(String modid) {
        return ModList.get().isLoaded(modid);
    }
}