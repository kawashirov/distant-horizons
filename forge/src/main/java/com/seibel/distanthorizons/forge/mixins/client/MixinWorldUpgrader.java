package com.seibel.distanthorizons.forge.mixins.client;

import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiLevelType;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiDimensionTypeWrapper;
import com.seibel.distanthorizons.common.wrappers.block.BlockStateWrapper;
import com.seibel.distanthorizons.common.wrappers.world.DimensionTypeWrapper;
import com.seibel.distanthorizons.core.file.structure.LocalSaveStructure;
import com.seibel.distanthorizons.core.level.DhServerLevel;
import com.seibel.distanthorizons.core.pos.DhBlockPos;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IServerLevelWrapper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.worldupdate.WorldUpgrader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.nio.file.Path;

@Mixin(WorldUpgrader.class)
public class MixinWorldUpgrader {
    static class FakeLevelWrapper implements IServerLevelWrapper {
        private Path saveFolder;
        private LevelStem stem;
        private DimensionType dimension;
        private DimensionTypeWrapper dimensionTypeWrapper;

        public FakeLevelWrapper(LevelStorageSource.LevelStorageAccess storage, WorldGenSettings gen, ResourceKey<Level> dim) {
            saveFolder = storage.getDimensionPath(dim);
            stem = gen.dimensions().getOrThrow(WorldGenSettings.levelToLevelStem(dim));
            dimension = stem.typeHolder().value();
            dimensionTypeWrapper = DimensionTypeWrapper.getDimensionTypeWrapper(dimension);
        }

        @Override
        public EDhApiLevelType getLevelType() {
            return EDhApiLevelType.SERVER_LEVEL;
        }

        @Override
        public IDhApiDimensionTypeWrapper getDimensionType() {
            return dimensionTypeWrapper;
        }

        @Override
        public int getBlockLight(int x, int y, int z) {
            return 0;
        }

        @Override
        public int getSkyLight(int x, int y, int z) {
            return 0;
        }

        @Override
        public boolean hasCeiling() {
            return dimension.hasCeiling();
        }

        @Override
        public boolean hasSkyLight() {
            return dimension.hasSkyLight();
        }

        @Override
        public int getHeight() {
            return dimension.height();
        }

        @Override
        public int getMinHeight() {
            return dimension.minY();
        }

        @Override
        public boolean hasChunkLoaded(int chunkX, int chunkZ) {
            return false;
        }

        @Override
        public IBlockStateWrapper getBlockState(DhBlockPos pos) {
            return BlockStateWrapper.AIR;
        }

        @Override
        public IBiomeWrapper getBiome(DhBlockPos pos) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public Object getWrappedMcObject() {
            return null;
        }

        @Nullable
        @Override
        public IClientLevelWrapper tryGetClientLevelWrapper() {
            return null;
        }

        @Override
        public File getSaveFolder() {
            return saveFolder.toFile();
        }
    }

    @Unique
    private DhServerLevel dhServerLevel;
    @Unique
    private FakeLevelWrapper fakeLevelWrapper;
    @Unique
    public LocalSaveStructure saveStructure;

    @Shadow @Final
    private DimensionDataStorage overworldDataStorage;
    @Shadow @Final
    private LevelStorageSource.LevelStorageAccess levelStorage;
    @Shadow @Final
    private WorldGenSettings worldGenSettings;

    @Inject(method = "Lnet/minecraft/util/worldupdate/WorldUpgrader;work()V",
            at = @At(value = "INVOKE")
    )
    private void initWorldUpgrade() {
        saveStructure = new LocalSaveStructure();
    }

    @Inject(method = "Lnet/minecraft/util/worldupdate/WorldUpgrader;work()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/worldupdate/WorldUpgrader;getAllChunkPos(Lnet/minecraft/resources/ResourceKey;)Ljava/util/List;", shift = At.Shift.AFTER),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void startWorldUpgrade(CallbackInfo info, ResourceKey resourceKey) {
        ResourceKey<Level> key = resourceKey;
        fakeLevelWrapper = new FakeLevelWrapper(levelStorage, worldGenSettings, key);
        dhServerLevel = new DhServerLevel(saveStructure, fakeLevelWrapper);
    }


}
