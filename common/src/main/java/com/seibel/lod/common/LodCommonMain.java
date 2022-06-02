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
 
package com.seibel.lod.common;

import com.seibel.lod.common.forge.LodForgeMethodCaller;
import com.seibel.lod.common.networking.NetworkReceiver;
import com.seibel.lod.common.wrappers.DependencySetup;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.config.ConfigBase;

/**
 * This is the common main class
 * @author Ran
 */
public class LodCommonMain {
    public static boolean forge = false;
    public static LodForgeMethodCaller forgeMethodCaller;
    public static void startup(LodForgeMethodCaller caller) {
        if (caller != null) {
            LodCommonMain.forge = true;
            forgeMethodCaller = caller;
        }

        DependencySetup.createInitialBindings();

//        if (!serverSided) {
//            new NetworkReceiver().register_Client();
//        } else {
//            new NetworkReceiver().register_Server();
//        }
    }

    public static void initConfig() {
        ConfigBase.init(Config.class);
    }
}
