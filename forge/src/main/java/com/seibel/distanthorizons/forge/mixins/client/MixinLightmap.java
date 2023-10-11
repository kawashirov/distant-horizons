package com.seibel.distanthorizons.forge.mixins.client;


import com.mojang.blaze3d.platform.NativeImage;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftRenderWrapper;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightTexture.class)
public class MixinLightmap
{
	@Shadow
	@Final
	private NativeImage lightPixels;
	
	@Inject(method = "updateLightTexture", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;upload()V"))
	public void updateLightTexture(float f, CallbackInfo ci)
	{
		// since the light map is always updated on the client render thread we should be able to access the client level at the same time
		IMinecraftClientWrapper mc = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
		IClientLevelWrapper clientLevel = mc.getWrappedClientLevel();
		
		//ApiShared.LOGGER.info("Lightmap update");
		MinecraftRenderWrapper.INSTANCE.updateLightmap(this.lightPixels, clientLevel);
	}
	
}
