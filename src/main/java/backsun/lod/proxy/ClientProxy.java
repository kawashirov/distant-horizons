package backsun.lod.proxy;

import backsun.lod.objects.LodChunk;
import backsun.lod.objects.LodRegion;
import backsun.lod.renderer.LodRenderer;
import backsun.lod.util.LodRegionFileHandler;
import net.minecraft.client.Minecraft;
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
 * @version 10-22-2020
 */
public class ClientProxy extends CommonProxy
{
	private LodRenderer renderer;
	private LodRegionFileHandler rfHandler;
	//TODO have the ability to store multiple regions based on how large the user's view distance is
	private LodRegion region;
	
	public ClientProxy()
	{
		rfHandler = new LodRegionFileHandler();
		
		
		
	}
	
	
	
	
	//==============//
	// render event //
	//==============//
	
	@SubscribeEvent
	public void renderWorldLastEvent(RenderWorldLastEvent event)
	{
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
	
	// TODO determine if a old region should be unloaded
	// use the chunkUnloadedEvent
	
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
		generateLodChunk(Minecraft.getMinecraft().world.getChunkFromChunkCoords(event.getChunkX(), event.getChunkZ()));
	}
	
	
	
	
	/*
	 * 
	Use this for generating chunks and maybe determining if they are loaded at all?
	
 	chunk = Minecraft.getMinecraft().getIntegratedServer().getWorld(0).getChunkProvider().chunkGenerator.generateChunk(chunk.x, chunk.z);
	
	System.out.println(chunk.x + " " + chunk.z + "\tloaded: " + chunk.isLoaded() + "\tpop: " + chunk.isPopulated() + "\tter pop: " + chunk.isTerrainPopulated());
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
			LodChunk c = new LodChunk(chunk, Minecraft.getMinecraft().world);
			
			// TODO does this work with negative chunks?
			// TODO set up dynamic/multiple regions
			if (region == null || (region.x != (c.x / 32) && region.z != (c.z / 32)))
			{
				region = new LodRegion(c.x / 32, c.z / 32);
			}
			
			region.data[Math.abs(c.x % 32)][Math.abs(c.z % 32)] = c;
			
			if (renderer != null)
			{
				//TODO send data to renderer
				renderer.renderRegions = region;
			}
			
			rfHandler.saveRegionToDisk(region);
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
