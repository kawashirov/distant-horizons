package com.seibel.lod.fabric.wrappers;

import com.seibel.lod.core.handlers.dependencyInjection.DependencyHandler;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.IModChecker;
import com.seibel.lod.fabric.wrappers.modAccessor.ModChecker;
import com.seibel.lod.common.wrappers.config.LodConfigWrapperSingleton;

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
public class FabricDependencySetup
{
	public static void createInitialBindings()
	{
		SingletonHandler.bind(ILodConfigWrapperSingleton.class, LodConfigWrapperSingleton.INSTANCE);;
		SingletonHandler.bind(IModChecker.class, ModChecker.INSTANCE);
	}

	public static void finishBinding()
	{
		SingletonHandler.finishBinding();
	}
}