package backsun.lod.proxy;

import backsun.lod.util.CustomRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.model.ModelLoader;
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
}
