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

package com.seibel.lod.forge;

import com.seibel.lod.common.LodCommonMain;
import com.seibel.lod.common.forge.LodForgeMethodCaller;
import com.seibel.lod.common.wrappers.config.ConfigGui;
import com.seibel.lod.common.wrappers.minecraft.MinecraftClientWrapper;
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.api.internal.InternalApiShared;
import com.seibel.lod.core.handlers.ReflectionHandler;
import com.seibel.lod.core.handlers.dependencyInjection.ModAccessorHandler;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.IOptifineAccessor;
import com.seibel.lod.forge.networking.NetworkHandler;
import com.seibel.lod.forge.wrappers.ForgeDependencySetup;

import com.seibel.lod.forge.wrappers.modAccessor.OptifineAccessor;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
#if PRE_MC_1_17_1
import net.minecraftforge.fml.ExtensionPoint;
import org.apache.logging.log4j.Logger;
#elif MC_1_17_1
import net.minecraftforge.fmlclient.ConfigGuiHandler;
#else // 1.18+
import net.minecraftforge.client.ConfigGuiHandler;
import org.apache.logging.log4j.Logger;
#endif

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Random;

/**
 * Initialize and setup the Mod. <br>
 * If you are looking for the real start of the mod
 * check out the ClientProxy.
 * 
 * @author James Seibel
 * @version 11-21-2021
 */
@Mod(ModInfo.ID)
public class ForgeMain implements LodForgeMethodCaller
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
	
	public static ForgeClientProxy forgeClientProxy;
	
	
	private void init(final FMLCommonSetupEvent event)
	{
		// make sure the dependencies are set up before the mod needs them
		LodCommonMain.initConfig();
		LodCommonMain.startup(this, !FMLLoader.getDist().isClient(), new NetworkHandler());
		ForgeDependencySetup.createInitialBindings();
		ForgeDependencySetup.finishBinding();
		LOGGER.info("Distant Horizons initializing...");
	}
	
	public ForgeMain()
	{
		// Register the methods for server and other game events we are interested in
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::init);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientStart);
	}

	private void onClientStart(final FMLClientSetupEvent event)
	{
		if (ReflectionHandler.instance.optifinePresent()) {
			ModAccessorHandler.bind(IOptifineAccessor.class, new OptifineAccessor());
		}
		
		ModAccessorHandler.finishBinding();

		#if PRE_MC_1_17_1
		ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY,
				() -> (client, parent) -> ConfigGui.getScreen(parent, ""));
		#else
		ModLoadingContext.get().registerExtensionPoint(ConfigGuiHandler.ConfigGuiFactory.class,
				() -> new ConfigGuiHandler.ConfigGuiFactory((client, parent) -> ConfigGui.getScreen(parent, "")));
		#endif
		forgeClientProxy = new ForgeClientProxy();
		MinecraftForge.EVENT_BUS.register(forgeClientProxy);
	}

	private final ModelDataMap dataMap = new ModelDataMap.Builder().build();
	@Override
	public List<BakedQuad> getQuads(MinecraftClientWrapper mc, Block block, BlockState blockState, Direction direction, Random random) {
		return mc.getModelManager().getBlockModelShaper().getBlockModel(block.defaultBlockState()).getQuads(blockState, direction, random, dataMap);
	}

	@Override
	public int colorResolverGetColor(ColorResolver resolver, Biome biome, double x, double z) {
		#if MC_1_17_1
		return resolver.m_130045_(biome, x, z);
		#else
		return resolver.getColor(biome, x, z);
		#endif

	}
}
