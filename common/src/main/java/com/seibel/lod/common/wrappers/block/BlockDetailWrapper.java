/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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
 
package com.seibel.lod.common.wrappers.block;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.seibel.lod.common.Config;
import com.seibel.lod.common.wrappers.McObjectConverter;
import com.seibel.lod.common.wrappers.chunk.ChunkWrapper;
import com.seibel.lod.core.api.ApiShared;
import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.util.ColorUtil;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockDetailWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;

import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.core.Direction;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/*-- WARN: This class should NEVER hold reference to anything large,
 as this is never dealloc until the end of runtime!! --*/
public class BlockDetailWrapper extends IBlockDetailWrapper
{
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);

	public static final int FLOWER_COLOR_SCALE = 5;

	public static final Random random = new Random(0);

	enum ColorMode {
		Default,
		Flower,
		Leaves;
		static ColorMode getColorMode(Block b) {
			if (b instanceof LeavesBlock) return Leaves;
			if (b instanceof FlowerBlock) return Flower;
			return Default;
		}
	}
	//TODO: Perhaps make this not just use the first frame?
	private static int calculateColorFromTexture(TextureAtlasSprite texture, ColorMode colorMode) {
		
		int count = 0;
		double alpha = 0;
		double red = 0;
		double green = 0;
		double blue = 0;
	    int tempColor;
	    
	    {
	        // textures normally use u and v instead of x and y
	        for (int u = 0; u < texture.getWidth(); u++)
	        {
	            for (int v = 0; v < texture.getHeight(); v++)
	            {
	            	//note: Minecraft color format is: 0xAA BB GG RR
	            	//________ DH mod color format is: 0xAA RR GG BB
	            	//OpenGL RGBA format native order: 0xRR GG BB AA
	            	//_ OpenGL RGBA format Java Order: 0xAA BB GG RR
	                tempColor = TextureAtlasSpriteWrapper.getPixelRGBA(texture, 0, u, v);
	
	                double r = ((tempColor & 0x000000FF)      )/255.;
	                double g = ((tempColor & 0x0000FF00) >>> 8)/255.;
	                double b = ((tempColor & 0x00FF0000) >>> 16)/255.;
	                double a = ((tempColor & 0xFF000000) >>> 24)/255.;
	                int scale = 1;
	                
	                if (colorMode == ColorMode.Leaves) {
	                	r *= a;
	                	g *= a;
	                	b *= a;
	                	a = 1.;
	                } else if (a==0.) {
	                	continue;
	                } else if (colorMode == ColorMode.Flower && (g+0.1<b || g+0.1<r)) {
	                	scale = FLOWER_COLOR_SCALE;
	                }
	                
	                count += scale;
	                alpha += a*a*scale;
	                red += r*r*scale;
	                green += g*g*scale;
	                blue += b*b*scale;
	            }
	        }
	    }
	
	    if (count == 0)
	        // this block is entirely transparent
	        tempColor = ColorUtil.rgbToInt(255,255,0,255);
	    else
	    {
	        // determine the average color
	        tempColor = ColorUtil.rgbToInt(
	                (int) (Math.sqrt(alpha/count)*255.),
	                (int) (Math.sqrt(red / count)*255.),
	                (int) (Math.sqrt(green / count)*255.),
	                (int) (Math.sqrt(blue / count)*255.));
	    }
	    // TODO: Remove this when transparency is added!
	    double colorAlpha = ColorUtil.getAlpha(tempColor)/255.;
	    tempColor = ColorUtil.rgbToInt(
	    		ColorUtil.getAlpha(tempColor),
	    		(int)(ColorUtil.getRed(tempColor) * colorAlpha),
	    		(int)(ColorUtil.getGreen(tempColor) * colorAlpha),
	    		(int)(ColorUtil.getBlue(tempColor) * colorAlpha)
	    		);
		return tempColor;
	}

	private static final Block[] BLOCK_TO_AVOID = {Blocks.AIR, Blocks.CAVE_AIR, Blocks.BARRIER};

	private static final Direction[] DIRECTION_ORDER = {Direction.UP, Direction.NORTH, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.DOWN};
	
	private static boolean isBlockToBeAvoid(Block b) {
		for (Block bta : BLOCK_TO_AVOID)
			if (bta==b) return true;
		return false;
	}
	
	final BlockState state;
	
	//boolean isShapeResolved = false;
	//^ Shapes no longer lazy resolved due to memory leaks from needing
	// to keep a reference to the block getter

	boolean[] dontOccludeFaces = null;
	boolean noCollision = false;
	boolean noFullFace = false;
	
	boolean isColorResolved = false;
	int baseColor = 0; //TODO: Impl per-face color
	boolean needShade = true;
	boolean needPostTinting = false;
	int tintIndex = 0;
	
	public static BlockDetailWrapper NULL_BLOCK_DETAIL = new BlockDetailWrapper();
	
	public BlockDetailWrapper(BlockState state, BlockPos pos, LevelReader getter) {
		this.state = state;
		resolveShapes(getter, pos);
		//ApiShared.LOGGER.info("Created BlockDetailWrapper for blockstate {} at {}", state, pos);
	}
	
	private BlockDetailWrapper() {
		this.state = null;
	}

	static BlockDetailWrapper make(BlockState bs, BlockPos pos, LevelReader getter) {
		if(!bs.getFluidState().isEmpty()) { // Is a fluidBlock
			if (isBlockToBeAvoid(bs.getBlock())) return NULL_BLOCK_DETAIL;
			if (bs.isAir()) return NULL_BLOCK_DETAIL;
			return new BlockDetailWrapper(bs, pos, getter);
		} else {
			if (bs.getRenderShape() != RenderShape.MODEL) return NULL_BLOCK_DETAIL;
			if (isBlockToBeAvoid(bs.getBlock())) return NULL_BLOCK_DETAIL;
			return new BlockDetailWrapper(bs, pos, getter);
		}
	}
	
	private void resolveShapes(LevelReader sampleGetter, BlockPos samplePos) {
		//if (isShapeResolved) return;
		if (state.getFluidState().isEmpty()) {
			noCollision = state.getCollisionShape(sampleGetter, samplePos).isEmpty();
			dontOccludeFaces = new boolean[6];
			if (state.canOcclude()) {
				/* FIXME: Figure out how or if needed to impl per-face culling?
				for (Direction dir : Direction.values()) {
					dontOccludeFaces[McObjectConverter.Convert(dir).ordinal()]
							= state.getFaceOcclusionShape(sampleGetter, samplePos, dir).isEmpty();
				}*/
			} else {
				Arrays.fill(dontOccludeFaces, true);
			}
			
			VoxelShape voxelShape = state.getShape(sampleGetter, samplePos);
            if (voxelShape.isEmpty()) {
            	noFullFace = true;
            } else {
	            AABB bbox = voxelShape.bounds();
	            double xWidth = (bbox.maxX - bbox.minX);
	            double yWidth = (bbox.maxY - bbox.minY);
	            double zWidth = (bbox.maxZ - bbox.minZ);
	            noFullFace = xWidth < 1 && zWidth < 1 && yWidth < 1;
            }
		} else { // Liquid Block
			dontOccludeFaces = new boolean[6];
		}
	}
	
	private void resolveColors() {
		if (isColorResolved) return;
		if (state.getFluidState().isEmpty()) {
			List<BakedQuad> quads = null;
			for (Direction direction : DIRECTION_ORDER)
	        {
	        	quads = Minecraft.getInstance().getModelManager().getBlockModelShaper().
	        			getBlockModel(state).getQuads(state, direction, random);
	            if (!quads.isEmpty() &&
	            	!(state.getBlock() instanceof RotatedPillarBlock && direction == Direction.UP))
	                break;
	        };
			if (quads == null || quads.isEmpty()) {
				quads = Minecraft.getInstance().getModelManager().getBlockModelShaper().
						getBlockModel(state).getQuads(state, null, random);
			}
	        if (quads != null && !quads.isEmpty()) {
	        	needPostTinting = quads.get(0).isTinted();
	        	needShade = quads.get(0).isShade();
	        	tintIndex = quads.get(0).getTintIndex();
	        	baseColor = calculateColorFromTexture(
						#if PRE_MC_1_17_1 quads.get(0).sprite,
						#else quads.get(0).getSprite(), #endif
	        		ColorMode.getColorMode(state.getBlock()));
	        } else { // Backup method.
				needPostTinting = false;
				needShade = false;
				tintIndex = 0;
				baseColor = calculateColorFromTexture(Minecraft.getInstance().getModelManager().getBlockModelShaper().getParticleIcon(state),
						ColorMode.getColorMode(state.getBlock()));
			}
		} else { // Liquid Block
			needPostTinting = true;
			needShade = false;
			tintIndex = 0;
			baseColor = calculateColorFromTexture(Minecraft.getInstance().getModelManager().getBlockModelShaper().getParticleIcon(state),
					ColorMode.getColorMode(state.getBlock()));
		}
		isColorResolved = true;
	}
	
	private BlockAndTintGetter wrapColorResolver(LevelReader level) {
		int blendDistance = CONFIG.client().graphics().quality().getLodBiomeBlending();
		if (blendDistance == 0) {
			return new TintGetterOverrideFast(level);
		} else {
			return new TintGetterOverrideSmooth(level, blendDistance);
		}
	}
	
	@Override
	public int getAndResolveFaceColor(LodDirection dir, IChunkWrapper chunk, AbstractBlockPosWrapper blockPos)
	{
		// FIXME: impl per-face colors
		resolveColors();
		if (!needPostTinting) return baseColor;
		int tintColor = Minecraft.getInstance().getBlockColors()
				.getColor(state, wrapColorResolver(((ChunkWrapper)chunk).getColorResolver()),
					McObjectConverter.Convert(blockPos), tintIndex);
		if (tintColor == -1) return baseColor;
		return ColorUtil.multiplyARGBwithRGB(baseColor, tintColor);
	}

	@Override
	public boolean hasFaceCullingFor(LodDirection dir)
	{
		//resolveShapes();
		return !dontOccludeFaces[dir.ordinal()];
	}

	@Override
	public boolean hasNoCollision()
	{
		//resolveShapes();
		return noCollision;
	}

	@Override
	public boolean noFaceIsFullFace()
	{
		//resolveShapes();
		return noFullFace;
	}

	@Override
	public String serialize()
	{
		// FIXME: Impl this for the blockState Storage stuff
		return null;
	}

	@Override
	protected boolean isSame(IBlockDetailWrapper iBlockDetail)
	{
		return ((BlockDetailWrapper)iBlockDetail).state.getBlock().equals(state.getBlock());
	}

	public String toString() {
		return "BlockDetail{" + state + "}";
	}
}
