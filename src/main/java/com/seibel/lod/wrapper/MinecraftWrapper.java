package com.seibel.lod.wrapper;

import java.io.File;

import com.seibel.lod.util.LodUtil;

import net.minecraft.client.GameSettings;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.IProfiler;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Direction;
import net.minecraft.world.DimensionType;

/**
 * A singleton that wraps the Minecraft class
 * to allow for easier movement between Minecraft versions.
 * 
 * @author James Seibel
 * @version 9-6-2021
 */
public class MinecraftWrapper
{
	public static MinecraftWrapper INSTANCE = new MinecraftWrapper();
	
	private Minecraft mc = Minecraft.getInstance();
	
	private MinecraftWrapper()
	{
		
	}
	
	
	
	//=================//
	// method wrappers //
	//=================//
	
	public float getShading(Direction direction)
	{
		return mc.level.getShade(Direction.UP, true);
	}
	
	public boolean hasSingleplayerServer()
	{
		return mc.hasSingleplayerServer();
	}
	
	public DimensionType getCurrentDimension()
	{
		return mc.player.level.dimensionType();
	}
	
	public String getCurrentDimensionId()
	{
		return LodUtil.getDimensionIDFromWorld(mc.level);
	}
	
	
	
	
	//=============//
	// Simple gets //
	//=============//
	
	public ClientPlayerEntity getPlayer()
	{
		return mc.player;
	}
	
	public GameSettings getOptions()
	{
		return mc.options;
	}
	
	/** Measured in chunks */
	public int getRenderDistance()
	{
		return mc.options.renderDistance;
	}
	
	public File getGameDirectory()
	{
		return mc.gameDirectory;
	}
	
	public IProfiler getProfiler()
	{
		return mc.getProfiler();
	}
	
	public ClientPlayNetHandler getConnection()
	{
		return mc.getConnection();
	}
	
	public GameRenderer getGameRenderer()
	{
		return mc.gameRenderer;
	}
	
	public Entity getCameraEntity()
	{
		return mc.cameraEntity;
	}
	
	public MainWindow getWindow()
	{
		return mc.getWindow();
	}
	
	public float getSkyDarken(float partialTicks)
	{
		return mc.level.getSkyDarken(partialTicks);
	}
	
	public IntegratedServer getSingleplayerServer()
	{
		return mc.getSingleplayerServer();
	}
	
	public ServerData getCurrentServer()
	{
		return mc.getCurrentServer();
	}
	
	public WorldRenderer getLevelRenderer()
	{
		return mc.levelRenderer;
	}
}
