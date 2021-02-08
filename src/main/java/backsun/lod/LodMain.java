package backsun.lod;

import java.util.Map;

import backsun.lod.asm.RenderGlobalClassTransformer;
import backsun.lod.proxy.ClientProxy;
import backsun.lod.proxy.CommonProxy;
import backsun.lod.util.Reference;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

/**
 * 
 * @author James Seibel
 * @version 02-07-2021
 */
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions({"backsun.lod.asm"})
@Mod(modid = Reference.MOD_ID, name = Reference.NAME, version = Reference.VERSION)
public class LodMain implements IFMLLoadingPlugin
{
	@Instance
	public static LodMain instance;
	
	@SidedProxy(clientSide = Reference.CLIENT_PROXY_CLASS, serverSide = Reference.COMMON_PROXY_CLASS)
	public static CommonProxy common_proxy;
	public static ClientProxy client_proxy;
	
	@EventHandler
	public static void PreInit(FMLPreInitializationEvent event)
	{
		Minecraft.getMinecraft().getFramebuffer().enableStencil();
	}
	
	@EventHandler
	public static void Init(FMLInitializationEvent event)
	{
		MinecraftForge.EVENT_BUS.register(common_proxy);
		client_proxy = new ClientProxy();
	}
	
	@EventHandler
	public static void PostInit(FMLPostInitializationEvent event)
	{
		
	}
	
	
	
	
	// required for IFMLLoadingPlugin
	
	@Override
	public String[] getASMTransformerClass()
	{
		return new String[] { RenderGlobalClassTransformer.class.getName() };
	}

	@Override
	public String getModContainerClass()
	{
		// unneeded since we are also running a normal mod
		return null;
	}

	@Override
	public String getSetupClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {
		// TODO Auto-generated method stub
	}

	@Override
	public String getAccessTransformerClass() {
		// TODO Auto-generated method stub
		return null;
	}
}
