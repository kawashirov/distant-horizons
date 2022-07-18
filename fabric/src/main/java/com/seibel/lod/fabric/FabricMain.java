/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.fabric;

import com.seibel.lod.common.LodCommonMain;
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.handlers.dependencyInjection.ModAccessorInjector;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.IModChecker;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.IOptifineAccessor;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.ISodiumAccessor;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.IStarlightAccessor;
import com.seibel.lod.fabric.wrappers.modAccessor.OptifineAccessor;
import com.seibel.lod.fabric.wrappers.modAccessor.SodiumAccessor;
import com.seibel.lod.fabric.wrappers.modAccessor.StarlightAccessor;
import com.seibel.lod.fabric.wrappers.FabricDependencySetup;

import org.apache.logging.log4j.Logger;

/**
 * Initialize and setup the Mod. <br>
 * If you are looking for the real start of the mod
 * check out the ClientProxy.
 * 
 * @author coolGi
 * @author Ran
 * @version 12-1-2021
 */
public class FabricMain
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();

	public static void postInit() {
		LOGGER.info("Post-Initializing Mod");
		FabricDependencySetup.runDelayedSetup();
		LodCommonMain.initConfig();
		LOGGER.info("Mod Post-Initialized");
	}


	// This loads the mod after minecraft loads which doesn't causes a lot of issues
	public static void init() {
		LOGGER.info("Initializing Mod");
		LodCommonMain.startup(null);
		FabricDependencySetup.createInitialBindings();
		LOGGER.info(ModInfo.READABLE_NAME + ", Version: " + ModInfo.VERSION);

		if (SingletonInjector.INSTANCE.get(IModChecker.class).isModLoaded("sodium")) {
			ModAccessorInjector.INSTANCE.bind(ISodiumAccessor.class, new SodiumAccessor());
		}
		if (SingletonInjector.INSTANCE.get(IModChecker.class).isModLoaded("starlight")) {
			ModAccessorInjector.INSTANCE.bind(IStarlightAccessor.class, new StarlightAccessor());
		}
		if (SingletonInjector.INSTANCE.get(IModChecker.class).isModLoaded("optifine")) {
			ModAccessorInjector.INSTANCE.bind(IOptifineAccessor.class, new OptifineAccessor());
		}
		LOGGER.info(ModInfo.READABLE_NAME + " Initialized");
	}
}
