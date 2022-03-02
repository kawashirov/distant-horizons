package com.seibel.lod.forge.wrappers;

import com.seibel.lod.common.wrappers.config.LodConfigWrapperSingleton;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;

/**
 * Binds all necessary dependencies so we
 * can access them in Core. <br>
 * This needs to be called before any Core classes
 * are loaded.
 * 
 * @author James Seibel
 * @author Ran
 * @version 12-1-2021
 */
public class ForgeDependencySetup
{
	public static void createInitialBindings()
	{
		SingletonHandler.bind(ILodConfigWrapperSingleton.class, LodConfigWrapperSingleton.INSTANCE);
	}
}
