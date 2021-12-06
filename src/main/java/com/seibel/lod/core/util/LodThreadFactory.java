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

package com.seibel.lod.core.util;

import java.util.concurrent.ThreadFactory;

/**
 * Just a simple ThreadFactory to name ExecutorService
 * threads, which can be helpful when debugging.
 * @author James Seibel
 * @version 8-15-2021
 */
public class LodThreadFactory implements ThreadFactory
{
	public final String threadName;
	
	
	public LodThreadFactory(String newThreadName)
	{
		threadName = newThreadName + " Thread";
	}
	
	@Override
	public Thread newThread(Runnable r)
	{
		return new Thread(r, threadName);
	}
	
}
