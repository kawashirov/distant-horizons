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

package com.seibel.distanthorizons.common;

import com.seibel.distanthorizons.common.forge.LodForgeMethodCaller;
import com.seibel.distanthorizons.common.wrappers.DependencySetup;
import com.seibel.distanthorizons.coreapi.ModInfo;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.config.ConfigBase;

/**
 * This is the common main class
 *
 * @author Ran
 */
public class LodCommonMain
{
	public static boolean forge = false;
	public static LodForgeMethodCaller forgeMethodCaller;
	
	
	
	public static void startup(LodForgeMethodCaller forgeMethodCaller)
	{
		if (forgeMethodCaller != null)
		{
			LodCommonMain.forge = true;
			LodCommonMain.forgeMethodCaller = forgeMethodCaller;
		}
		
		DependencySetup.createSharedBindings();
		SharedApi.init();
//        if (!serverSided) {
//            new NetworkReceiver().register_Client();
//        } else {
//            new NetworkReceiver().register_Server();
//        }
	}
	
	public static void initConfig()
	{
		ConfigBase.INSTANCE = new ConfigBase(ModInfo.ID, ModInfo.NAME, Config.class, 1);
		Config.completeDelayedSetup();
	}
	
}
