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

package com.seibel.distanthorizons.fabric.wrappers.modAccessor;

#if MC_1_16_5 || MC_1_18_2 || MC_1_19_2 || MC_1_19_4 || MC_1_20_1

import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IIrisAccessor;
import net.coderbot.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;

public class IrisAccessor implements IIrisAccessor
{
	@Override
	public String getModName()
	{
		//return "Iris-Fabric";
		return Iris.MODID;
	}
	
	@Override
	public boolean isShaderPackInUse()
	{
		return IrisApi.getInstance().isShaderPackInUse();
	}
	
	@Override
	public boolean isRenderingShadowPass()
	{
		return IrisApi.getInstance().isRenderingShadowPass();
	}
}

#endif
