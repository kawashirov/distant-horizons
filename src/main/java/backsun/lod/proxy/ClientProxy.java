package backsun.lod.proxy;

import backsun.lod.renderer.LodRenderer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * This is used by the client.
 * 
 * @author James_Seibel
 * @version 09-18-2020
 */
public class ClientProxy extends CommonProxy
{
	private LodRenderer renderer;
	
	
	
	public ClientProxy()
	{
		
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
	
	

	private double distanceToPlayer(int x, int y, int z, double cameraX, double cameraY, double cameraZ)
	{
		if(cameraY == y)
			return Math.sqrt(Math.pow((x - cameraX),2) + Math.pow((z - cameraZ),2));
					
		return Math.sqrt(Math.pow((x - cameraX),2) + Math.pow((y - cameraY),2) + Math.pow((z - cameraZ),2));
	}
}
