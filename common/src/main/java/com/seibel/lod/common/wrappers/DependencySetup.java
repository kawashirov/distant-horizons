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
 
package com.seibel.lod.common.wrappers;

import com.seibel.lod.common.LodCommonMain;
import com.seibel.lod.common.wrappers.minecraft.MinecraftClientWrapper;
import com.seibel.lod.common.wrappers.minecraft.MinecraftRenderWrapper;
import com.seibel.lod.core.handlers.IReflectionHandler;
import com.seibel.lod.core.handlers.ReflectionHandler;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.IVersionConstants;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;

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
public class DependencySetup {
    public static void createInitialBindings()
    {
        SingletonHandler.bind(IVersionConstants.class, VersionConstants.INSTANCE);
        if (!LodCommonMain.serverSided)
        {
            SingletonHandler.bind(IMinecraftClientWrapper.class, MinecraftClientWrapper.INSTANCE);
            SingletonHandler.bind(IMinecraftRenderWrapper.class, MinecraftRenderWrapper.INSTANCE);
            SingletonHandler.bind(IReflectionHandler.class, ReflectionHandler.createSingleton(MinecraftClientWrapper.INSTANCE.getOptions().getClass().getDeclaredFields(), MinecraftClientWrapper.INSTANCE.getOptions()));
        }

        SingletonHandler.bind(IWrapperFactory.class, WrapperFactory.INSTANCE);
        DependencySetupDoneCheck.isDone = true;
    }
}
