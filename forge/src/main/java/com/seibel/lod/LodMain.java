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

package com.seibel.lod;

import com.seibel.lod.common.LodCommonMain;
import com.seibel.lod.common.forge.LodForgeMethodCaller;
import com.seibel.lod.common.wrappers.minecraft.MinecraftWrapper;
import com.seibel.lod.forge.ForgeClientProxy;
import com.seibel.lod.forge.ForgeConfig;
import com.seibel.lod.forge.wrappers.ForgeDependencySetup;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fmlserverevents.FMLServerStartedEvent;

import java.util.List;
import java.util.Random;

/**
 * Initialize and setup the Mod.
 * <br>
 * If you are looking for the real start of the mod
 * check out the ClientProxy.
 * 
 * @author James Seibel
 * @version 7-3-2021
 */
@Mod(ModInfo.ID)
public class LodMain implements LodForgeMethodCaller
{
	public static LodMain instance;
	
	public static ForgeClientProxy client_proxy;
	
	
	private void init(final FMLCommonSetupEvent event)
	{
		LodCommonMain.startup(this);
		ForgeDependencySetup.createInitialBindings();
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ForgeConfig.CLIENT_SPEC);
	}
	
	
	public LodMain()
	{
		// Register the methods
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::init);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientStart);
		
		// Register ourselves for server and other game events we are interested in
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	private void onClientStart(final FMLClientSetupEvent event)
	{
		client_proxy = new ForgeClientProxy();
		MinecraftForge.EVENT_BUS.register(client_proxy);
	}
	
	
	
	@SubscribeEvent
	public void onServerStarting(FMLServerStartedEvent event)
	{
		// this is called when the server starts
	}

	@Override
	public List<BakedQuad> getQuads(MinecraftWrapper mc, Block block, BlockState blockState, Direction direction, Random random, ModelDataMap dataMap) {
		return mc.getModelManager().getBlockModelShaper().getBlockModel(block.defaultBlockState()).getQuads(blockState, direction, random, dataMap);
	}
}
