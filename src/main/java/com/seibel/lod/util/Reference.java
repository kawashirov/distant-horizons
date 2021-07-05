/*
 *    This file is part of the LOD Mod, licensed under the GNU GPL v3 License.
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
package com.seibel.lod.util;

/**
 * This holds meta information about the mod.
 * 
 * @author James Seibel
 * @version 04-16-2020
 */
public class Reference
{
	/** the mod's identifier  */
	public static final String MOD_ID = "lod";
	/** the mod's name */
	public static final String NAME = "LOD Mod";
	/** the mod's version */
	public static final String VERSION = "1.0";
	/** the version of minecraft this mod is built for */
	public static final String ACCEPTED_VERSIONS = "[1.16.4]";
	
	/** where the client proxy class is */
	public static final String CLIENT_PROXY_CLASS = "com.backsun.lod.proxy.ClientProxy";
	/** where the  common proxy class is*/
	public static final String COMMON_PROXY_CLASS = "com.backsun.lod.proxy.CommonProxy";
	
	
}
