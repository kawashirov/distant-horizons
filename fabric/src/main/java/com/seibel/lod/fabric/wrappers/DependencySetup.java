package com.seibel.lod.fabric.wrappers;

import com.seibel.lod.common.LodCommonMain;
import com.seibel.lod.common.wrappers.config.NewLodConfigWrapperSingleton;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
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
public class DependencySetup
{
	public static void createInitialBindings()
	{
		if (!LodCommonMain.IsNewConfig)
			SingletonHandler.bind(ILodConfigWrapperSingleton.class, LodConfigWrapperSingleton.INSTANCE);
		else
			SingletonHandler.bind(ILodConfigWrapperSingleton.class, NewLodConfigWrapperSingleton.INSTANCE);
	}
}
