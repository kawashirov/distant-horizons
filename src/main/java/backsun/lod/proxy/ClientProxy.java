package backsun.lod.proxy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import backsun.lod.objects.LoadedRegions;
import backsun.lod.objects.LodChunk;
import backsun.lod.objects.LodRegion;
import backsun.lod.renderer.LodRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
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
	private LoadedRegions regions;
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
		// We can't render anything if the loaded regions is null
		if (regions == null)
			return;
		
		double playerX = Minecraft.getMinecraft().player.posX;
		double playerZ = Minecraft.getMinecraft().player.posZ;
		
		int xOffset = ((int)playerX / (LodChunk.WIDTH * LodRegion.SIZE)) - regions.getCenterX();
		int zOffset = ((int)playerZ / (LodChunk.WIDTH * LodRegion.SIZE)) - regions.getCenterZ();
		
		if (xOffset != 0 || zOffset != 0)
		{
			regions.move(xOffset, zOffset);
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
		// don't try to create an LOD object
		// if for some reason we aren't
		// given a valid chunk object
		// (Minecraft often gives back empty
		// or null chunks in this method)
		if (chunk != null && isValidChunk(chunk) && Minecraft.getMinecraft().world != null)
		{
			Thread thread = new Thread(() ->
			{ 
				Minecraft mc = Minecraft.getMinecraft();
				
				LodChunk lod = new LodChunk(chunk, mc.world);
				
				if (regions == null)
				{
					regions = new LoadedRegions(null, regionWidth);
				}
				
				regions.addLod(lod);
				
				if (renderer != null)
				{
					renderer.regions = regions;
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
	
	
	private double distanceToPlayer(int x, int y, int z, double cameraX, double cameraY, double cameraZ)
	{
		if(cameraY == y)
			return Math.sqrt(Math.pow((x - cameraX),2) + Math.pow((z - cameraZ),2));
					
		return Math.sqrt(Math.pow((x - cameraX),2) + Math.pow((y - cameraY),2) + Math.pow((z - cameraZ),2));
	}
}
