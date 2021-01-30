package backsun.lod.proxy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import backsun.lod.objects.LodChunk;
import backsun.lod.objects.LodDimension;
import backsun.lod.objects.LodRegion;
import backsun.lod.objects.LodWorld;
import backsun.lod.renderer.LodRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.DimensionType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * This is used by the client.
 * 
 * @author James_Seibel
 * @version 1-20-2021
 */
public class ClientProxy extends CommonProxy
{
	private LodRenderer renderer;
	private LodWorld lodWorld;
	private ExecutorService lodGenThreadPool = Executors.newFixedThreadPool(1);
	
	// TODO make this change dynamically based on the render distance
	private int regionWidth = 5;
	
	public ClientProxy()
	{
		
	}
	
	
	
	
	//==============//
	// render event //
	//==============//
	
	@SubscribeEvent
	public void renderWorldLastEvent(RenderWorldLastEvent event)
	{
		// We can't render anything if the lodWorld is null
		if (lodWorld == null)
			return;
		
		Minecraft mc = Minecraft.getMinecraft();
		int dimId = mc.player.dimension;
		LodDimension lodDim = lodWorld.getLodDimension(dimId);
		
		
		double playerX = mc.player.posX;
		double playerZ = mc.player.posZ;
		
		int xOffset = ((int)playerX / (LodChunk.WIDTH * LodRegion.SIZE)) - lodDim.getCenterX();
		int zOffset = ((int)playerZ / (LodChunk.WIDTH * LodRegion.SIZE)) - lodDim.getCenterZ();
		
		if (xOffset != 0 || zOffset != 0)
		{
			lodDim.move(xOffset, zOffset);
		}
		
		
		// we wait to create the renderer until the first frame
		// to make sure that the EntityRenderer has
		// been created, that way we can get the fovModifer
		// method from it through reflection.
		if (renderer == null)
		{
			renderer = new LodRenderer();
		}
		else
		{
			renderer.drawLODs(Minecraft.getMinecraft(), event.getPartialTicks());
		}
	}	
	
	
	//===============//
	// update events //
	//===============//
	
	@SubscribeEvent
	public void chunkLoadEvent(ChunkEvent event)
	{
		generateLodChunk(event.getChunk());
	}
	
	/**
	 * this event is called whenever a chunk is created for the first time.
	 */
	@SubscribeEvent
	public void onChunkPopulate(PopulateChunkEvent event)
	{
		Minecraft mc = Minecraft.getMinecraft();
		if (mc != null && event != null)
		{
			WorldClient world = mc.world;
			
			if(world != null)
			{
				generateLodChunk(world.getChunkFromChunkCoords(event.getChunkX(), event.getChunkZ()));
			}
		}
	}
	
	/*
	 * 
	Use this for generating chunks and maybe determining if they are loaded at all?
	
	Could I create my own chunk generator and multithread it? It wouldn't save to the world, but could I save it for LODs?
	
 	chunk = Minecraft.getMinecraft().getIntegratedServer().getWorld(0).getChunkProvider().chunkGenerator.generateChunk(chunk.x, chunk.z);
	
	System.out.println(chunk.x + " " + chunk.z + "\tloaded: " + chunk.isLoaded() + "\tpop: " + chunk.isPopulated() + "\tter pop: " + chunk.isTerrainPopulated());
	 */
	
	/*
	use Minecraft.getMinecraft().world.getWorldInfo().getWorldName();
	or
	.getSaveHandler().getWorldDirectoryName()
	to clear the regions on world change
	
	*/
	
	private void generateLodChunk(Chunk chunk)
	{
		Minecraft mc = Minecraft.getMinecraft();
		
		// don't try to create an LOD object
		// if for some reason we aren't
		// given a valid chunk object
		// (Minecraft often gives back empty
		// or null chunks in this method)
		if (mc != null && mc.world != null && chunk != null && isValidChunk(chunk))
		{
			int dimId = mc.player.dimension;
			
			Thread thread = new Thread(() ->
			{
				LodChunk lod = new LodChunk(chunk, mc.world);
				LodDimension lodDim;
				
				if (lodWorld == null)
					lodWorld = new LodWorld();
				
				if (lodWorld.getLodDimension(dimId) == null)
				{
					DimensionType dim = DimensionType.getById(chunk.getWorld().provider.getDimension());
					lodDim = new LodDimension(dim, regionWidth);
					lodWorld.addLodDimension(lodDim);
				}
				else
				{
					lodDim = lodWorld.getLodDimension(dimId);
				}
				
				lodDim.addLod(lod);
				
				if (renderer != null)
				{
					renderer.regions = lodDim;
				}
			});
			
			lodGenThreadPool.execute(thread);
		}
	}
	
	/**
	 * Return whether the given chunk
	 * has any data in it.
	 */
	private boolean isValidChunk(Chunk chunk)
	{
		ExtendedBlockStorage[] data = chunk.getBlockStorageArray();
		
		for(ExtendedBlockStorage e : data)
		{
			if(e != null && !e.isEmpty())
			{
				return true;
			}
		}
		
		return false;
	}
	
	
}
