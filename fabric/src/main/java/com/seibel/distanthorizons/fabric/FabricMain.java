/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2023 James Seibel
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

package com.seibel.distanthorizons.fabric;

import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiAfterDhInitEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeDhInitEvent;
import com.seibel.distanthorizons.core.config.ConfigBase;
import com.seibel.distanthorizons.core.jar.ModJarInfo;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.*;
import com.seibel.distanthorizons.common.LodCommonMain;
import com.seibel.distanthorizons.coreapi.ModInfo;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.coreapi.DependencyInjection.ApiEventInjector;
import com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.fabric.wrappers.FabricDependencySetup;
import com.seibel.distanthorizons.fabric.wrappers.modAccessor.*;

import org.apache.logging.log4j.Logger;

import javax.swing.*;

/**
 * Initialize and setup the Mod. <br>
 * If you are looking for the real start of the mod
 * check out the ClientProxy.
 *
 * @author coolGi
 * @author Ran
 * @version 9-2-2022
 */
public class FabricMain
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	public static void postInit()
	{
		LOGGER.info("Post-Initializing Mod");
		FabricDependencySetup.runDelayedSetup();
		
		if (Config.Client.Advanced.Graphics.Fog.disableVanillaFog.get() && SingletonInjector.INSTANCE.get(IModChecker.class).isModLoaded("bclib"))
			ModAccessorInjector.INSTANCE.get(IBCLibAccessor.class).setRenderCustomFog(false); // Remove BCLib's fog
		#if POST_MC_1_20_1
		if (SingletonInjector.INSTANCE.get(IModChecker.class).isModLoaded("sodium"))
			ModAccessorInjector.INSTANCE.get(ISodiumAccessor.class).setFogOcclusion(false); // FIXME: This is a tmp fix for sodium 0.5.0, and 0.5.1. This is fixed in sodium 0.5.2
		#endif
		
		if (ConfigBase.INSTANCE == null)
			throw new IllegalStateException("Config was not initialized. Make sure to call LodCommonMain.initConfig() before calling this method.");
		
		LOGGER.info("Mod Post-Initialized");
	}
	
	
	// This loads the mod after minecraft loads which doesn't causes a lot of issues
	public static void init()
	{
		ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeDhInitEvent.class, null);
		
		LOGGER.info("Initializing Mod");
		LodCommonMain.startup(null);
		FabricDependencySetup.createInitialBindings();
		LOGGER.info(ModInfo.READABLE_NAME + ", Version: " + ModInfo.VERSION);
		
		// Print git info (Useful for dev builds)
		LOGGER.info("DH Branch: " + ModJarInfo.Git_Branch);
		LOGGER.info("DH Commit: " + ModJarInfo.Git_Commit);
		LOGGER.info("DH Jar Build Source: " + ModJarInfo.Build_Source);
		
		IModChecker modChecker = SingletonInjector.INSTANCE.get(IModChecker.class);
		if (modChecker.isModLoaded("sodium"))
		{
			ModAccessorInjector.INSTANCE.bind(ISodiumAccessor.class, new SodiumAccessor());
			
			// If sodium is installed Indium is also necessary in order to use the Fabric rendering API
			if (!modChecker.isModLoaded("indium"))
			{
				// People don't read the crash logs!!!
				// So, just put a notification, so they hopefully realise what's the problem (and dont just open issues)
				new Thread(() -> {
					System.setProperty("java.awt.headless", "false"); // Required to make it work
					JOptionPane.showMessageDialog(null, ModInfo.READABLE_NAME + " now relies on Indium to work with Sodium.\nPlease download Indium from https://modrinth.com/mod/indium", ModInfo.READABLE_NAME, JOptionPane.INFORMATION_MESSAGE);
				}).start();
				
				IMinecraftClientWrapper mc = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
				String errorMessage = "loading Distant Horizons. Distant Horizons requires Indium in order to run with Sodium.";
				String exceptionError = "Distant Horizons conditional mod Exception";
				mc.crashMinecraft(errorMessage, new Exception(exceptionError));
			}
		}
		if (modChecker.isModLoaded("starlight"))
		{
			ModAccessorInjector.INSTANCE.bind(IStarlightAccessor.class, new StarlightAccessor());
		}
		if (modChecker.isModLoaded("optifine"))
		{
			ModAccessorInjector.INSTANCE.bind(IOptifineAccessor.class, new OptifineAccessor());
		}
		if (modChecker.isModLoaded("bclib"))
		{
			ModAccessorInjector.INSTANCE.bind(IBCLibAccessor.class, new BCLibAccessor());
		}
		
		#if MC_1_16_5 || MC_1_18_2 || MC_1_19_2 || MC_1_19_4 || MC_1_20_1
		// 1.17.1 won't support this since there isn't a matching Iris version
		if (modChecker.isModLoaded("iris"))
		{
			ModAccessorInjector.INSTANCE.bind(IIrisAccessor.class, new IrisAccessor());
		}
		#endif
		
		LOGGER.info(ModInfo.READABLE_NAME + " Initialized");
		
		ApiEventInjector.INSTANCE.fireAllEvents(DhApiAfterDhInitEvent.class, null);
	}
	
}
