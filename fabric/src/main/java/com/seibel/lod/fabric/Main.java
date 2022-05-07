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
import com.seibel.lod.core.api.internal.InternalApiShared;
import com.seibel.lod.core.handlers.dependencyInjection.ModAccessorHandler;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.IModChecker;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.IOptifineAccessor;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.ISodiumAccessor;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.IStarlightAccessor;
import com.seibel.lod.fabric.networking.NetworkHandler;
import com.seibel.lod.fabric.wrappers.modAccessor.OptifineAccessor;
import com.seibel.lod.fabric.wrappers.modAccessor.SodiumAccessor;
import com.seibel.lod.fabric.wrappers.modAccessor.StarlightAccessor;
import com.seibel.lod.fabric.wrappers.FabricDependencySetup;

import net.fabricmc.api.ClientModInitializer;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;

/**
 * Initialize and setup the Mod. <br>
 * If you are looking for the real start of the mod
 * check out the ClientProxy.
 * 
 * @author coolGi
 * @author Ran
 * @version 12-1-2021
 */
public class Main implements ClientModInitializer
{
	// This is a client mod so it should implement ClientModInitializer and in fabric.mod.json it should have "environment": "client"
	// Once it works on servers change the implement to ModInitializer and in fabric.mod.json it should be "environment": "*"
	
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
	
	public static ClientProxy client_proxy;


	// Do if implements ClientModInitializer
	// This loads the mod before minecraft loads which causes a lot of issues
	@Override
	public void onInitializeClient() {
		// no.
	}

	// This loads the mod after minecraft loads which doesn't causes a lot of issues
	public static void init() {
		LodCommonMain.startup(null, false, new NetworkHandler());
		FabricDependencySetup.createInitialBindings();
		FabricDependencySetup.finishBinding();
		LodCommonMain.initConfig();
		LOGGER.info(ModInfo.READABLE_NAME + ", Version: " + ModInfo.VERSION);

		// Check if this works
		client_proxy = new ClientProxy();
		client_proxy.registerEvents();
		if (SingletonHandler.get(IModChecker.class).isModLoaded("sodium")) {
			ModAccessorHandler.bind(ISodiumAccessor.class, new SodiumAccessor());
		}
		if (SingletonHandler.get(IModChecker.class).isModLoaded("starlight")) {
			ModAccessorHandler.bind(IStarlightAccessor.class, new StarlightAccessor());
		}
		if (SingletonHandler.get(IModChecker.class).isModLoaded("optifine")) {
			ModAccessorHandler.bind(IOptifineAccessor.class, new OptifineAccessor());
		}

		ModAccessorHandler.finishBinding();
	}

	public static void initServer() {
		LodCommonMain.startup(null, true, new NetworkHandler());
		FabricDependencySetup.createInitialBindings();
		FabricDependencySetup.finishBinding();
		LodCommonMain.initConfig();
		LOGGER.info(ModInfo.READABLE_NAME + ", Version: " + ModInfo.VERSION);
	}
}
