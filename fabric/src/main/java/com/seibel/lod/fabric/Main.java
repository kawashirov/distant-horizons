/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.fabric;

import com.seibel.lod.common.LodCommonMain;
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.api.ModAccessorApi;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.IModChecker;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.IOptifineAccessor;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.ISodiumAccessor;
import com.seibel.lod.fabric.wrappers.modAccessor.ModChecker;
import com.seibel.lod.fabric.wrappers.modAccessor.OptifineAccessor;
import com.seibel.lod.fabric.wrappers.modAccessor.SodiumAccessor;
import com.seibel.lod.fabric.wrappers.DependencySetup;

import net.fabricmc.api.ClientModInitializer;

/**
 * Initialize and setup the Mod. <br>
 * If you are looking for the real start of the mod
 * check out the ClientProxy.
 *
 * @author coolGi2007
 * @author Ran
 * @version 12-1-2021
 */
public class Main implements ClientModInitializer
{
	// This is a client mod, so it should implement ClientModInitializer and in fabric.mod.json it should have "environment": "client"
	// Once it works on servers change the implement to ModInitializer and in fabric.mod.json it should be "environment": "*"

	public static ClientProxy client_proxy;


	// Do if implements ClientModInitializer
	// This loads the mod before minecraft loads which causes a lot of issues
	@Override
	public void onInitializeClient() {
		// no.
	}

	// This loads the mod after minecraft loads which doesn't causes a lot of issues
	public static void init() {
		LodCommonMain.initConfig();
		LodCommonMain.startup(null, false);
		DependencySetup.createInitialBindings();
		SingletonHandler.bind(IModChecker.class, ModChecker.INSTANCE);
		ClientApi.LOGGER.info(ModInfo.READABLE_NAME + ", Version: " + ModInfo.VERSION);

		// Check if this works
		client_proxy = new ClientProxy();
		client_proxy.registerEvents();
		if (SingletonHandler.get(IModChecker.class).isModLoaded("sodium")) {
			ModAccessorApi.bind(ISodiumAccessor.class, new SodiumAccessor());
		}
		if (SingletonHandler.get(IModChecker.class).isModLoaded("optifine")) {
			ModAccessorApi.bind(IOptifineAccessor.class, new OptifineAccessor());
		}
	}

	public static void initServer() {
		LodCommonMain.initConfig();
		LodCommonMain.startup(null, true);
		DependencySetup.createInitialBindings();
		ClientApi.LOGGER.info(ModInfo.READABLE_NAME + ", Version: " + ModInfo.VERSION);
	}
}
