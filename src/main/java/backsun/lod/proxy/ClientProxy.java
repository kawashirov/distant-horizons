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
 * @version 09-27-2020
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
	
	@SubscribeEvent
	public void chunkLoadEvent(ChunkEvent event)
	{
		// don't try to create an LOD object
		// if for some reason we aren't
		// given a valid chunk object
		// (Minecraft often gives back empty
		// or null chunks in this method)
		if (event.getChunk() != null && isValidChunk(event.getChunk()))
		{
			// can be used for testing only the center chunk
//			if(event.getChunk().x == 0 && event.getChunk().z == 0)
//			{
//				LodChunk c = new LodChunk(event.getChunk());
//			}
			
			LodChunk c = new LodChunk(event.getChunk());
			
			
			// TODO determine if a old region should be unloaded
			
			if (region == null || (region.x != (c.x / 32) && region.z != (c.z / 32)))
			{
				region = new LodRegion(c.x / 32, c.z / 32);
			}
			
			region.data[Math.abs(c.x % 32)][Math.abs(c.z % 32)] = c;
			
			//TODO send data to renderer
			
			rfHandler.saveRegionToDisk(region);
		}
		
		
		
		
		//System.out.println(c);
		
//		Chunk ch = event.getChunk();
//		Minecraft mc = Minecraft.getMinecraft();
//		if(renderer != null && ch != null && renderer.biomes != null && mc.world != null && mc.world.getBiomeProvider() != null)
//		{
//			try
//			{
//				if(distanceToPlayer(ch.x * 16, 70, ch.z * 16, mc.player.posX, 70, mc.player.posZ) > mc.gameSettings.renderDistanceChunks * 16 * 2)
//				{
//					int biome = Biome.getIdForBiome(ch.getBiome(new BlockPos(ch.x, 70, ch.z), mc.world.getBiomeProvider()));
//					renderer.biomes[ch.x+32][ch.z+32] = biome;
//				}
//				else
//				{
//					renderer.biomes[ch.x+32][ch.z+32] = -1;
//				}
//			}
//			catch(IndexOutOfBoundsException e)
//			{
//				// TODO fix so this isn't needed
//			}
//			
//		}
	}
	
	/**
	 * this event is called whenever a chunk is created for the first time.
	 */
	@SubscribeEvent
	public void onChunkPopulate(PopulateChunkEvent event)
	{
		// later on this should save information about the chunk to be used later
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
