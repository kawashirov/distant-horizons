/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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
 
package com.seibel.lod.common;

import com.seibel.lod.common.forge.LodForgeMethodCaller;
import com.seibel.lod.common.networking.NetworkInterface;
import com.seibel.lod.common.wrappers.DependencySetup;
import com.seibel.lod.common.wrappers.config.ConfigGui;

/**
 * This is the common main class
 * @author Ran
 */
public class LodCommonMain {
    public static boolean forge = false;
    public static boolean serverSided;
    public static LodForgeMethodCaller forgeMethodCaller;
    public static NetworkInterface networkInterface;

    public static void startup(LodForgeMethodCaller caller, boolean serverSided, NetworkInterface networkInterface) {
        LodCommonMain.serverSided = serverSided;
        if (caller != null) {
            LodCommonMain.forge = true;
            forgeMethodCaller = caller;
        }

        DependencySetup.createInitialBindings();

        LodCommonMain.networkInterface = networkInterface;
        if (!serverSided) {
            networkInterface.register_Client();
        } else {
            networkInterface.register_Server();
        }
    }


    public static void initConfig() {
        ConfigGui.init(Config.class);
    }
}
