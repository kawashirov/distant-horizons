package com.seibel.lod.quilt.mixins;

import org.objectweb.asm.tree.ClassNode;
import org.quiltmc.loader.api.QuiltLoader;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * @author coolGi
 * @author cortex
 */
// TODO: Move to common if possible
public class FabricMixinPlugin implements IMixinConfigPlugin {

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains(".mods.")) { // If the mixin wants to go into a mod then we check if that mod is loaded or not
            return QuiltLoader.isModLoaded(
                    mixinClassName
                            // What these 2 regex's do is get the mod name that we are checking out of the mixinClassName
                            // Eg. "com.seibel.lod.mixins.mods.sodium.MixinSodiumChunkRenderer" turns into "sodium"
                            .replaceAll("^.*mods.", "") // Replaces everything before the mods
                            .replaceAll("\\..*$", "") // Replaces everything after the mod name
            );
        }
        return true;
    }


    @Override
    public void onLoad(String mixinPackage) {

    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}