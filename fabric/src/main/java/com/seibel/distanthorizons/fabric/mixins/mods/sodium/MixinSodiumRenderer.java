package com.seibel.distanthorizons.fabric.mixins.mods.sodium;

#if POST_MC_1_20_1
// Sodium 0.5
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.ISodiumAccessor;
import com.seibel.distanthorizons.fabric.wrappers.modAccessor.SodiumAccessor;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkCameraContext;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import me.jellysquid.mods.sodium.client.render.chunk.RegionChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RegionChunkRenderer.class)
public class MixinSodiumRenderer
{
    @Unique SodiumAccessor accessor = null;

    @Inject(remap = false, method = "render", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/ShaderChunkRenderer;begin(Lme/jellysquid/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;)V", shift = At.Shift.AFTER))
    private void injectDHLoDRendering(ChunkRenderMatrices matrices, CommandList commandList, RenderRegionManager regions, SortedRenderLists renderLists, TerrainRenderPass renderPass, ChunkCameraContext camera, CallbackInfo ci)
    {
        if (accessor == null)
        {
            accessor = (SodiumAccessor)ModAccessorInjector.INSTANCE.get(ISodiumAccessor.class);
        }

        if (renderPass.equals(DefaultTerrainRenderPasses.SOLID))
        {
            //TODO: use matrices.modelView() and matrices.projection() instead of
            // SodiumAccessor.mcModelViewMatrix,
            // SodiumAccessor.mcProjectionMatrix,
            ClientApi.INSTANCE.renderLods(accessor.levelWrapper,
                    accessor.mcModelViewMatrix,
                    accessor.mcProjectionMatrix,
                    accessor.partialTicks);
        }
    }


}

#elif POST_MC_1_17_1
// Sodium 0.3 to 0.4
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.ISodiumAccessor;
import com.seibel.distanthorizons.fabric.wrappers.modAccessor.SodiumAccessor;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkCameraContext;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import me.jellysquid.mods.sodium.client.render.chunk.RegionChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RegionChunkRenderer.class)
public class MixinSodiumRenderer 
{
    @Unique SodiumAccessor accessor = null;

    @Inject(remap = false, method = "render", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/ShaderChunkRenderer;begin(Lme/jellysquid/mods/sodium/client/render/chunk/passes/BlockRenderPass;)V", shift = At.Shift.AFTER))
    private void injectDHLoDRendering(ChunkRenderMatrices matrices, CommandList commandList, ChunkRenderList list, BlockRenderPass pass, ChunkCameraContext camera, CallbackInfo ci) 
    {
        if (accessor == null) 
		{
            accessor = (SodiumAccessor)ModAccessorInjector.INSTANCE.get(ISodiumAccessor.class);
        }

        if (pass.equals(BlockRenderPass.SOLID)) 
		{
            //TODO: use matrices.modelView() and matrices.projection() instead of
            // SodiumAccessor.mcModelViewMatrix,
            // SodiumAccessor.mcProjectionMatrix,
            ClientApi.INSTANCE.renderLods(accessor.levelWrapper,
                    accessor.mcModelViewMatrix,
                    accessor.mcProjectionMatrix,
                    accessor.partialTicks);
        }
    }
	
	
}
#else
// Sodium 0.2 and under
import com.mojang.blaze3d.vertex.PoseStack;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.ISodiumAccessor;
import com.seibel.distanthorizons.fabric.wrappers.modAccessor.SodiumAccessor;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SodiumWorldRenderer.class)
public class MixinSodiumRenderer
{
	@Unique SodiumAccessor accessor = null;
	
	@Inject(method="drawChunkLayer", remap = false, at = @At("HEAD"))
	private void drawChunkLayer(RenderType renderLayer, PoseStack matrixStack, double x, double y, double z, CallbackInfo callback)
	{
		if (this.accessor == null)
		{
			this.accessor = (SodiumAccessor) ModAccessorInjector.INSTANCE.get(ISodiumAccessor.class);
		}
		
		if (renderLayer == RenderType.solid())
		{
			ClientApi.INSTANCE.renderLods(this.accessor.levelWrapper,
					this.accessor.mcModelViewMatrix,
					this.accessor.mcProjectionMatrix,
					this.accessor.partialTicks);
		}
	}
}
	
#endif
