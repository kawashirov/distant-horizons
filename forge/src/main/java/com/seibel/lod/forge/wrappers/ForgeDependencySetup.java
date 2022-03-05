package com.seibel.lod.forge.wrappers;

import com.seibel.lod.common.wrappers.config.LodConfigWrapperSingleton;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.IModChecker;
import com.seibel.lod.forge.wrappers.modAccessor.ModChecker;

/**
 * Binds all necessary dependencies so we
 * can access them in Core. <br>
 * This needs to be called before any Core classes
 * are loaded.
 * 
 * @author James Seibel
 * @author Ran
 * @version 3-5-2022
 */
public class ForgeDependencySetup
{
	public static void createInitialBindings()
	{
		SingletonHandler.bind(ILodConfigWrapperSingleton.class, LodConfigWrapperSingleton.INSTANCE);
		SingletonHandler.bind(IModChecker.class, ModChecker.INSTANCE);
	}
	
	public static void finishBinding()
	{
		SingletonHandler.finishBinding();
	}
}
