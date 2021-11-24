package com.seibel.lod.forge.wrappers;

import com.seibel.lod.core.handlers.IReflectionHandler;
import com.seibel.lod.core.handlers.ReflectionHandler;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockColorSingletonWrapper;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;
import com.seibel.lod.forge.wrappers.block.BlockColorSingletonWrapper;
import com.seibel.lod.forge.wrappers.config.LodConfigWrapperSingleton;
import com.seibel.lod.forge.wrappers.minecraft.MinecraftRenderWrapper;
import com.seibel.lod.forge.wrappers.minecraft.MinecraftWrapper;

/**
 * Binds all necessary dependencies so we
 * can access them in Core. <br>
 * This needs to be called before any Core classes
 * are loaded.
 * 
 * @author James Seibel
 * @version 11-20-2021
 */
public class ForgeDependencySetup
{
	public static void createInitialBindings()
	{
		SingletonHandler.bind(ILodConfigWrapperSingleton.class, LodConfigWrapperSingleton.INSTANCE);
		SingletonHandler.bind(IBlockColorSingletonWrapper.class, BlockColorSingletonWrapper.INSTANCE);
		SingletonHandler.bind(IMinecraftWrapper.class, MinecraftWrapper.INSTANCE);
		SingletonHandler.bind(IMinecraftRenderWrapper.class, MinecraftRenderWrapper.INSTANCE);
		SingletonHandler.bind(IWrapperFactory.class, WrapperFactory.INSTANCE);
		
		SingletonHandler.bind(IReflectionHandler.class, ReflectionHandler.createSingleton(MinecraftWrapper.INSTANCE.getOptions().getClass().getDeclaredFields(), MinecraftWrapper.INSTANCE.getOptions()));
	}
}
