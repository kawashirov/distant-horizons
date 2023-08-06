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

package com.seibel.distanthorizons.forge;

import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiAfterDhInitEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeDhInitEvent;
import com.seibel.distanthorizons.common.LodCommonMain;
import com.seibel.distanthorizons.common.forge.LodForgeMethodCaller;
import com.seibel.distanthorizons.common.wrappers.DependencySetup;
import com.seibel.distanthorizons.common.wrappers.gui.GetConfigScreen;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftClientWrapper;
import com.seibel.distanthorizons.coreapi.DependencyInjection.ApiEventInjector;
import com.seibel.distanthorizons.coreapi.ModInfo;
import com.seibel.distanthorizons.core.ReflectionHandler;
import com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IOptifineAccessor;
import com.seibel.distanthorizons.forge.wrappers.ForgeDependencySetup;

import com.seibel.distanthorizons.forge.wrappers.modAccessor.OptifineAccessor;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
#if POST_MC_1_19_2
import net.minecraft.util.RandomSource;
#endif
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.*;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
#if PRE_MC_1_17_1
import net.minecraftforge.fml.ExtensionPoint;
#elif MC_1_17_1
import net.minecraftforge.fmlclient.ConfigGuiHandler;
#elif POST_MC_1_18_2 && PRE_MC_1_19_2
import net.minecraftforge.client.ConfigGuiHandler;
#else
import net.minecraftforge.client.ConfigScreenHandler;
#endif

import org.apache.logging.log4j.Logger;

// these imports change due to forge refactoring classes in 1.19
#if PRE_MC_1_19_2
import net.minecraftforge.client.model.data.ModelDataMap;
import java.util.Random;
#else
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.client.model.data.ModelData;
#endif

import java.lang.invoke.MethodHandles;
import java.util.List;

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
		#elif MC_1_17_1 || MC_1_18_2 || PRE_MC_1_19_2
		ModLoadingContext.get().registerExtensionPoint(ConfigGuiHandler.ConfigGuiFactory.class,
				() -> new ConfigGuiHandler.ConfigGuiFactory((client, parent) -> GetConfigScreen.getScreen(parent)));
		#else
		ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
				() -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> GetConfigScreen.getScreen(parent)));
		#endif
		
		ForgeClientProxy.setupNetworkingListeners(event);
		
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

	#if PRE_MC_1_19_2
	private final ModelDataMap modelData = new ModelDataMap.Builder().build();
	#else
	private final ModelData modelData = ModelData.EMPTY;
	#endif

	@Override
	#if PRE_MC_1_19_2
	public List<BakedQuad> getQuads(MinecraftClientWrapper mc, Block block, BlockState blockState, Direction direction, Random random) {
		return mc.getModelManager().getBlockModelShaper().getBlockModel(block.defaultBlockState()).getQuads(blockState, direction, random, modelData);
	}
	#else
	public List<BakedQuad> getQuads(MinecraftClientWrapper mc, Block block, BlockState blockState, Direction direction, RandomSource random) {
		return mc.getModelManager().getBlockModelShaper().getBlockModel(block.defaultBlockState()).getQuads(blockState, direction, random, modelData #if POST_MC_1_19_2, RenderType.solid() #endif);
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
