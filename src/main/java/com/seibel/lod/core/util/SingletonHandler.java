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

import java.util.HashMap;
import java.util.Map;

/**
 * This class takes care of dependency injection
 * for singletons.
 * 
 * @author James Seibel
 * @version 11-20-2021
 */
public class SingletonHandler
{
	private static final Map<Class<?>, Object> singletons = new HashMap<Class<?>, Object>();
	
	
	
	

	/**
	 * Adds the given singleton so it can be referenced later.
	 * 
	 * @param interfaceClass
	 * @param singletonReference
	 * @throws IllegalStateException
	 */
	public static void bind(Class<?> interfaceClass, Object singletonReference) throws IllegalStateException
	{
		// make sure we haven't already bound this singleton
		if (singletons.containsKey(interfaceClass))
		{
			throw new IllegalStateException("The singleton [" + interfaceClass.getSimpleName() + "] has already been bound.");
		}
		
		
		// make sure the given singleton implements the interface
		boolean singletonImplementsInterface = false;
		for (Class<?> singletonInterface : singletonReference.getClass().getInterfaces())
		{
			if (singletonInterface.equals(interfaceClass))
			{
				singletonImplementsInterface = true;
				break;
			}
		}
		if (!singletonImplementsInterface)
		{
			throw new IllegalStateException("The singleton [" + interfaceClass.getSimpleName() + "] doesn't implement the interface [" + interfaceClass.getSimpleName() + "].");
		}
		
		
		singletons.put(interfaceClass, singletonReference);
	}

	/**
	 * Returns a singleton of type T
	 * if one has been bound.
	 * 
	 * @param <T> class of the singleton
	 * @param objectClass class of the singleton, but as a parameter!
	 * @return the singleton of type T
	 * @throws NullPointerException if no singleton of type T has been bound.
	 * @throws ClassCastException if the singleton isn't able to be cast to type T. (this shouldn't normally happen, unless the bound object changed somehow)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T get(Class<T> objectClass) throws NullPointerException, ClassCastException
	{
		// throw an error if the given singleton doesn't exist.
		if (!singletons.containsKey(objectClass))
		{
			throw new NullPointerException("The singleton [" + objectClass.getSimpleName() + "] was never bound. If you are calling [bind], make sure it is happening before you call [get].");
		}
		
		
		return (T) singletons.get(objectClass);
	}
	
}
