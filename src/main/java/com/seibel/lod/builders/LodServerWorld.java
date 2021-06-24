package com.seibel.lod.builders;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.seibel.lod.objects.LodChunk;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.particles.IParticleData;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.DimensionType;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.ITickList;
import net.minecraft.world.LightType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.Heightmap.Type;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.lighting.WorldLightManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.IWorldInfo;

public class LodServerWorld implements ISeedReader {
	
	public HashMap<Heightmap.Type, Heightmap> heightmaps = new HashMap<>();
	
	public IChunk chunk;
	
	public LodServerWorld(IChunk newChunk)
	{
		chunk = newChunk;
	}
	
	
	
	@Override
	public int getHeight(Type heightmapType, int x, int z)
	{
		x = x % LodChunk.WIDTH;
		x = (x < 0) ? x + 16 : x;
		
		z = z % LodChunk.WIDTH;
		z = (z < 0) ? z + 16 : z;
		
		return chunk.getHeightmap(Type.WORLD_SURFACE_WG).getHeight(x, z);
	}
	
	@Override
	public Biome getBiome(BlockPos pos) {
		return chunk.getBiomes().getNoiseBiome(pos.getX(), pos.getY(), pos.getZ());
	}
	
	@Override
	public boolean setBlockState(BlockPos pos, BlockState state, int flags, int recursionLeft) {
		return chunk.setBlockState(pos, state, false) == state;
	}
	
	@Override
	public BlockState getBlockState(BlockPos pos) {
		return chunk.getBlockState(pos);
	}
	
	@Override
	public FluidState getFluidState(BlockPos pos) {
		return chunk.getFluidState(pos);
	}
	
	@Override
	public int getLightFor(LightType type, BlockPos pos)
	{
		return 0;
	}
	
	
	
	
	@Override
	public ServerWorld getWorld() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public ITickList<Block> getPendingBlockTicks() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public ITickList<Fluid> getPendingFluidTicks() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public IWorldInfo getWorldInfo() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public DifficultyInstance getDifficultyForLocation(BlockPos pos) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public AbstractChunkProvider getChunkProvider() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Random getRandom() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void playSound(PlayerEntity player, BlockPos pos, SoundEvent soundIn, SoundCategory category, float volume,
			float pitch) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void addParticle(IParticleData particleData, double x, double y, double z, double xSpeed, double ySpeed,
			double zSpeed) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void playEvent(PlayerEntity player, int type, BlockPos pos, int data) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public DynamicRegistries func_241828_r() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public List<Entity> getEntitiesInAABBexcluding(Entity entityIn, AxisAlignedBB boundingBox,
			Predicate<? super Entity> predicate) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public <T extends Entity> List<T> getEntitiesWithinAABB(Class<? extends T> clazz, AxisAlignedBB aabb,
			Predicate<? super T> filter) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public List<? extends PlayerEntity> getPlayers() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public IChunk getChunk(int x, int z, ChunkStatus requiredStatus, boolean nonnull) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public int getSkylightSubtracted() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public BiomeManager getBiomeManager() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Biome getNoiseBiomeRaw(int x, int y, int z) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean isRemote() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public int getSeaLevel() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public DimensionType getDimensionType() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public float func_230487_a_(Direction p_230487_1_, boolean p_230487_2_) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public WorldLightManager getLightManager() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public TileEntity getTileEntity(BlockPos pos) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public WorldBorder getWorldBorder() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean hasBlockState(BlockPos pos, Predicate<BlockState> state) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean removeBlock(BlockPos pos, boolean isMoving) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean destroyBlock(BlockPos pos, boolean dropBlock, Entity entity, int recursionLeft) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public long getSeed() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public Stream<? extends StructureStart<?>> func_241827_a(SectionPos p_241827_1_, Structure<?> p_241827_2_) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
