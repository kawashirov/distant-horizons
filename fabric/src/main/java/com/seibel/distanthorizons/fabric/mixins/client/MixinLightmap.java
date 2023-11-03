package com.seibel.distanthorizons.fabric.mixins.client;


import com.mojang.blaze3d.platform.NativeImage;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftRenderWrapper;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DynamicTexture.class)
public class MixinLightmap
{
	@Shadow
	@Final
	private NativeImage pixels;
	
	@Inject(method = "Lnet/minecraft/client/renderer/texture/DynamicTexture;upload()V", at = @At("HEAD"), cancellable = true)
	public void updateLightTexture(CallbackInfo ci)
	{
		// since the light map is always updated on the client render thread we should be able to access the client level at the same time
		IMinecraftClientWrapper mc = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
		if (
				mc == null &&
				mc.getWrappedClientLevel() == null
		)
			return;
		IClientLevelWrapper clientLevel = mc.getWrappedClientLevel();
		
		//ApiShared.LOGGER.info("Lightmap update");
		MinecraftRenderWrapper.INSTANCE.updateLightmap(this.pixels, clientLevel);
	}
	
}
