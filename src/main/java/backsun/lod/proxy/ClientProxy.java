package backsun.lod.proxy;

import backsun.lod.renderer.LodRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * This is used by the client.
 * 
 * @author James_Seibel
 * @version 08-31-2020
 */
public class ClientProxy extends CommonProxy
{
	
	private LodRenderer renderer;
	
	
	
	/**
	 * constructor
	 */
	public ClientProxy()
	{
		renderer = new LodRenderer();
	}
	
	
	
	
	//==============//
	// render event //
	//==============//
	
	@SubscribeEvent
	public void renderWorldLastEvent(RenderWorldLastEvent event)
	{
		renderer.drawLODs(Minecraft.getMinecraft(), event.getPartialTicks());
	}
	
	
	
	
	
	//===============//
	// update events //
	//===============//
	
	@SubscribeEvent
	public void fovUpdateEvent(FOVUpdateEvent event)
	{
		renderer.updateFOVModifier(event.getNewfov());
	}
	
	@SubscribeEvent
	public void chunkLoadEvent(ChunkEvent event)
	{
		Chunk ch = event.getChunk();
		Minecraft mc = Minecraft.getMinecraft();
		if(renderer != null && ch != null && renderer.biomes != null && mc.world != null && mc.world.getBiomeProvider() != null)
		{
			try
			{
				if(renderer.distanceToPlayer(ch.x * 16, 70, ch.z * 16, mc.player.posX, 70, mc.player.posZ) > mc.gameSettings.renderDistanceChunks * 16 * 2)
				{
					int biome = Biome.getIdForBiome(ch.getBiome(new BlockPos(ch.x, 70, ch.z), mc.world.getBiomeProvider()));
					renderer.biomes[ch.x+32][ch.z+32] = biome;
				}
				else
				{
					renderer.biomes[ch.x+32][ch.z+32] = -1;
				}
			}
			catch(IndexOutOfBoundsException e)
			{
				// TODO fix so this isn't needed
			}
			
		}
	}
	
	
	/**
	 * this event is called whenever a chunk is created for the first time.
	 */
	@SubscribeEvent
	public void onChunkPopulate(PopulateChunkEvent event)
	{
		// later on this should save information about the chunk to be used later
	}
	
}
