package backsun.lod.proxy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.lwjgl.opengl.GL11;

import com.backsun.lodCore.util.RenderGlobalHook;

import backsun.lod.objects.LodChunk;
import backsun.lod.objects.LodDimension;
import backsun.lod.objects.LodRegion;
import backsun.lod.objects.LodWorld;
import backsun.lod.renderer.LodRenderer;
import backsun.lod.util.LodFileHandler;
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
 * @version 01-31-2021
 */
public class ClientProxy extends CommonProxy
{
	private LodRenderer renderer;
	private LodWorld lodWorld;
	private ExecutorService lodGenThreadPool = Executors.newFixedThreadPool(1);
	
	/** Default size of any LOD regions we use */
	private int regionWidth = 5;
	
	public ClientProxy()
	{
		
	}
	
	
	
	
	//==============//
	// render event //
	//==============//
	
	@SubscribeEvent
	public void renderWorldLast(RenderWorldLastEvent event)
	{
		RenderGlobalHook.endRenderingStencil();
		GL11.glStencilFunc(GL11.GL_EQUAL, 0, 0xFF);
		
		renderLods(event.getPartialTicks());
		
		GL11.glDisable(GL11.GL_STENCIL_TEST);
	}
	
	public void renderLods(float partialTicks)
	{
		int newWidth = Math.max(3, (Minecraft.getMinecraft().gameSettings.renderDistanceChunks * LodRenderer.VIEW_DISTANCE_MULTIPLIER) / LodRegion.SIZE);
		if (lodWorld != null && regionWidth != newWidth)
		{
			lodWorld.resizeDimensionRegionWidth(newWidth);
			regionWidth = newWidth;
			
			// skip this frame, hopefully the lodWorld
			// should have everything set up by then
			return;
		}
		
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.player == null)
			return;
		int dimId = mc.player.dimension;
		LodDimension lodDim = lodWorld.getLodDimension(dimId);
		
		// We can't render anything if the lodWorld or lodDim is null
		if (lodWorld == null || lodDim == null)
			return;
		
		
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
			renderer.drawLODs(Minecraft.getMinecraft(), partialTicks);
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
			int dimId = chunk.getWorld().provider.getDimension();
			
			Thread thread = new Thread(() ->
			{
				LodChunk lod = new LodChunk(chunk, mc.world);
				LodDimension lodDim;
				
				if (lodWorld == null)
				{
					lodWorld = new LodWorld(LodFileHandler.getWorldName());
				}
				else
				{
					// if we have a lodWorld make sure 
					// it is for this minecraft world
					if (!lodWorld.worldName.equals(LodFileHandler.getWorldName()))
					{
						// this lodWorld isn't for this minecraft world
						// delete it so we can get a new one
						lodWorld = null;
						
						// skip this frame
						// we'll get this set up next time
						return;
					}
				}
				
				
				if (lodWorld.getLodDimension(dimId) == null)
				{
					DimensionType dim = DimensionType.getById(dimId);
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
