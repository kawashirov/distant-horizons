package backsun.lod.proxy;

import backsun.lod.renderer.CustomRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ClientProxy extends CommonProxy
{
	private CustomRenderer renderer;
	
	public ClientProxy()
	{
		renderer = new CustomRenderer();
	}
	
	@Override
	public void registerItemRender(Item item, int meta, String id)
	{
		ModelLoader.setCustomModelResourceLocation(item, meta, new ModelResourceLocation(item.getRegistryName(), id));;
	}
	
	@SubscribeEvent
	public void renderWorldLastEvent(RenderWorldLastEvent event)
	{
		renderer.drawTest(Minecraft.getMinecraft(), event.getPartialTicks());
	}
	
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
					System.out.println(renderer.biomes[ch.x+32][ch.z+32]);
				}
				else
				{
					renderer.biomes[ch.x+32][ch.z+32] = -1;
				}
			}
			catch(IndexOutOfBoundsException e)
			{
				
			}
			
		}
	}
}
