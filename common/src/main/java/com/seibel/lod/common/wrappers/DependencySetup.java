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

import com.seibel.lod.common.wrappers.config.ConfigWrapper;
import com.seibel.lod.common.wrappers.minecraft.MinecraftDedicatedServerWrapper;
import com.seibel.lod.core.wrapperInterfaces.config.IConfigWrapper;
import com.seibel.lod.common.wrappers.minecraft.MinecraftClientWrapper;
import com.seibel.lod.common.wrappers.minecraft.MinecraftRenderWrapper;
import com.seibel.lod.core.handlers.IReflectionHandler;
import com.seibel.lod.core.handlers.ReflectionHandler;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.wrapperInterfaces.IVersionConstants;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.config.LodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftSharedWrapper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

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
    public static void createSharedBindings()
    {
        SingletonInjector.INSTANCE.bind(ILodConfigWrapperSingleton.class, LodConfigWrapperSingleton.INSTANCE); // TODO: Remove
        SingletonInjector.INSTANCE.bind(IConfigWrapper.class, ConfigWrapper.INSTANCE);
        SingletonInjector.INSTANCE.bind(IVersionConstants.class, VersionConstants.INSTANCE);
        SingletonInjector.INSTANCE.bind(IWrapperFactory.class, WrapperFactory.INSTANCE);
        DependencySetupDoneCheck.isDone = true;
    }

    //@Environment(EnvType.SERVER)
    public static void createServerBindings() {
        SingletonInjector.INSTANCE.bind(IMinecraftSharedWrapper.class, MinecraftDedicatedServerWrapper.INSTANCE);
    }

    //@Environment(EnvType.CLIENT)
    public static void createClientBindings() {
        SingletonInjector.INSTANCE.bind(IMinecraftClientWrapper.class, MinecraftClientWrapper.INSTANCE);
        SingletonInjector.INSTANCE.bind(IMinecraftSharedWrapper.class, MinecraftClientWrapper.INSTANCE);
        SingletonInjector.INSTANCE.bind(IMinecraftRenderWrapper.class, MinecraftRenderWrapper.INSTANCE);
        SingletonInjector.INSTANCE.bind(IReflectionHandler.class, ReflectionHandler.createSingleton());
    }
}
