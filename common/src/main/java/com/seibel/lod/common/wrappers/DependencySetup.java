package com.seibel.lod.common.wrappers;

import com.seibel.lod.common.LodCommonMain;
import com.seibel.lod.common.wrappers.block.BlockColorSingletonWrapper;
import com.seibel.lod.common.wrappers.minecraft.MinecraftRenderWrapper;
import com.seibel.lod.common.wrappers.minecraft.MinecraftWrapper;
import com.seibel.lod.core.handlers.IReflectionHandler;
import com.seibel.lod.core.handlers.ReflectionHandler;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.IVersionConstants;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockColorSingletonWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;

/**
 * Binds all necessary dependencies, so we
 * can access them in Core. <br>
 * This needs to be called before any Core classes
 * are loaded.
 *
 * @author James Seibel
 * @author Ran
 * @version 12-1-2021
 */
public class DependencySetup {
    public static void createInitialBindings() {
        SingletonHandler.bind(IBlockColorSingletonWrapper.class, BlockColorSingletonWrapper.INSTANCE);
        if (!LodCommonMain.serverSided) {
            SingletonHandler.bind(IMinecraftWrapper.class, MinecraftWrapper.INSTANCE);
            SingletonHandler.bind(IMinecraftRenderWrapper.class, MinecraftRenderWrapper.INSTANCE);
            SingletonHandler.bind(IReflectionHandler.class, ReflectionHandler.createSingleton(MinecraftWrapper.INSTANCE.getOptions().getClass().getDeclaredFields(), MinecraftWrapper.INSTANCE.getOptions()));
        }
        SingletonHandler.bind(IVersionConstants.class, VersionConstants.INSTANCE);

        SingletonHandler.bind(IWrapperFactory.class, WrapperFactory.INSTANCE);
        DependencySetupDoneCheck.isDone = true;
    }
}
