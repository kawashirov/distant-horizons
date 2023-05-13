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

import com.seibel.lod.api.methods.events.abstractEvents.DhApiAfterDhInitEvent;
import com.seibel.lod.api.methods.events.abstractEvents.DhApiBeforeDhInitEvent;
import com.seibel.lod.common.LodCommonMain;
import com.seibel.lod.common.forge.LodForgeMethodCaller;
import com.seibel.lod.common.wrappers.DependencySetup;
import com.seibel.lod.common.wrappers.gui.GetConfigScreen;
import com.seibel.lod.common.wrappers.minecraft.MinecraftClientWrapper;
import com.seibel.lod.coreapi.DependencyInjection.ApiEventInjector;
import com.seibel.lod.coreapi.ModInfo;
import com.seibel.lod.core.ReflectionHandler;
import com.seibel.lod.core.dependencyInjection.ModAccessorInjector;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.IOptifineAccessor;
import com.seibel.lod.forge.wrappers.ForgeDependencySetup;

import com.seibel.lod.forge.wrappers.modAccessor.OptifineAccessor;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
#if POST_MC_1_19
import net.minecraft.util.RandomSource;
#endif
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.*;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
#if PRE_MC_1_17_1
import net.minecraftforge.fml.ExtensionPoint;
#elif MC_1_17_1
import net.minecraftforge.fmlclient.ConfigGuiHandler;
#else // 1.18+
import net.minecraftforge.client.ConfigGuiHandler;
#endif
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Random;

/**
 * Initialize and setup the Mod. <br>
 * If you are looking for the real start of the mod
 * check out the ClientProxy.
 *
 * @author coolGi
 * @author Ran
 * @author James Seibel
 * @version 8-15-2022
 */
@Mod(ModInfo.ID)
public class ForgeMain implements LodForgeMethodCaller
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
	public static ForgeClientProxy client_proxy = null;
	public static ForgeServerProxy server_proxy = null;

	public ForgeMain()
	{
		DependencySetup.createClientBindings();

//		initDedicated(null);
//		initDedicated(null);
		// Register the mod initializer (Actual event registration is done in the different proxies)
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::initClient);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::initDedicated);
	}

	private void initClient(final FMLClientSetupEvent event)
	{
		ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeDhInitEvent.class, null);

		LOGGER.info("Initializing Mod");
		LodCommonMain.startup(this);
		ForgeDependencySetup.createInitialBindings();
		LOGGER.info(ModInfo.READABLE_NAME + ", Version: " + ModInfo.VERSION);

		client_proxy = new ForgeClientProxy();
		MinecraftForge.EVENT_BUS.register(client_proxy);
		server_proxy = new ForgeServerProxy(false);
		MinecraftForge.EVENT_BUS.register(server_proxy);

		if (ReflectionHandler.INSTANCE.optifinePresent()) {
			ModAccessorInjector.INSTANCE.bind(IOptifineAccessor.class, new OptifineAccessor());
		}
		#if PRE_MC_1_17_1
		ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY,
				() -> (client, parent) -> GetConfigScreen.getScreen(parent));
		#else
		ModLoadingContext.get().registerExtensionPoint(ConfigGuiHandler.ConfigGuiFactory.class,
				() -> new ConfigGuiHandler.ConfigGuiFactory((client, parent) -> GetConfigScreen.getScreen(parent)));
		#endif

		LOGGER.info(ModInfo.READABLE_NAME + " Initialized");

		ApiEventInjector.INSTANCE.fireAllEvents(DhApiAfterDhInitEvent.class, null);

		// Init config
		// The reason im initialising in this rather than the post init process is cus im using this for the auto updater
		LodCommonMain.initConfig();
	}

	private void initDedicated(final FMLDedicatedServerSetupEvent event)
	{
//		DependencySetup.createServerBindings();
//		initCommon();

//		server_proxy = new ForgeServerProxy(true);
//		MinecraftForge.EVENT_BUS.register(server_proxy);
//
		postInitCommon();
	}

	private void postInitCommon()
	{
		LOGGER.info("Post-Initializing Mod");
		ForgeDependencySetup.runDelayedSetup();

		LOGGER.info("Mod Post-Initialized");
	}

	private final ModelDataMap dataMap = new ModelDataMap.Builder().build();
	@Override
	#if PRE_MC_1_19
	public List<BakedQuad> getQuads(MinecraftClientWrapper mc, Block block, BlockState blockState, Direction direction, Random random) {
		return mc.getModelManager().getBlockModelShaper().getBlockModel(block.defaultBlockState()).getQuads(blockState, direction, random, dataMap);
	}
	#else
	public List<BakedQuad> getQuads(MinecraftClientWrapper mc, Block block, BlockState blockState, Direction direction, RandomSource random) {
		return mc.getModelManager().getBlockModelShaper().getBlockModel(block.defaultBlockState()).getQuads(blockState, direction, random, dataMap);
	}
	#endif

	@Override //TODO: Check this if its still needed
	public int colorResolverGetColor(ColorResolver resolver, Biome biome, double x, double z) {
		#if MC_1_17_1______Still_needed
		return resolver.m_130045_(biome, x, z);
		#else
		return resolver.getColor(biome, x, z);
		#endif

	}
}
