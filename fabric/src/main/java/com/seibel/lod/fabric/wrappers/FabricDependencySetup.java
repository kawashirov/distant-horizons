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
 
package com.seibel.lod.fabric.wrappers;

import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.common.wrappers.config.LodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.IModChecker;
import com.seibel.lod.fabric.wrappers.modAccessor.ModChecker;

/**
 * Binds all necessary dependencies, so we
 * can access them in Core. <br>
 * This needs to be called before any Core classes
 * are loaded.
 * 
 * @author James Seibel
 * @author Ran
 * @version 3-5-2022
 */
public class FabricDependencySetup
{
	public static void createInitialBindings()
	{
		SingletonHandler.bind(IModChecker.class, ModChecker.INSTANCE);

		SingletonHandler.bind(ILodConfigWrapperSingleton.class, LodConfigWrapperSingleton.INSTANCE);
	}

	public static void finishBinding() {
		SingletonHandler.finishBinding();
	}
}
