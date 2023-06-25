/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package com.seibel.distanthorizons.common.wrappers.worldGeneration.mimicObject;

import net.minecraft.world.level.lighting.*;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
#if POST_MC_1_17_1
import net.minecraft.world.level.LevelHeightAccessor;
#endif
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.PropertyKey;

public class WorldGenLevelLightEngine extends LevelLightEngine {
	public static final int MAX_SOURCE_LEVEL = 15;
    public static final int LIGHT_SECTION_PADDING = 1;
    #if POST_MC_1_17_1
    protected final LevelHeightAccessor levelHeightAccessor;
    #endif

    #if PRE_MC_1_20_1
    @Nullable
    public final BlockLightEngine blockEngine;
    @Nullable
    public final SkyLightEngine skyEngine;
    #else
    @Nullable
    public final LightEngine<?, ?> blockEngine;
    @Nullable
    public final LightEngine<?, ?> skyEngine;
    #endif


    public WorldGenLevelLightEngine(LightGetterAdaptor genRegion) {
    	super(genRegion, false, false);
        #if POST_MC_1_17_1
        this.levelHeightAccessor = genRegion.getLevelHeightAccessor();
        #endif
        this.blockEngine = new BlockLightEngine(genRegion);
        this.skyEngine = new SkyLightEngine(genRegion);
    }

    #if PRE_MC_1_20_1
    @Override
    public void onBlockEmissionIncrease(BlockPos blockPos, int i) {
        if (this.blockEngine != null) {
            this.blockEngine.onBlockEmissionIncrease(blockPos, i);
        }
    }

    @Override
    public int runUpdates(int i, boolean bl, boolean bl2) {
        if (this.blockEngine != null && this.skyEngine != null) {
            int j = i / 2;
            int k = this.blockEngine.runUpdates(j, bl, bl2);
            int l = i - j + k;
            int m = this.skyEngine.runUpdates(l, bl, bl2);
            if (k == 0 && m > 0) {
                return this.blockEngine.runUpdates(m, bl, bl2);
            }
            return m;
        }
        if (this.blockEngine != null) {
            return this.blockEngine.runUpdates(i, bl, bl2);
        }
        if (this.skyEngine != null) {
            return this.skyEngine.runUpdates(i, bl, bl2);
        }
        return i;
    }

    @Override
    public void enableLightSources(ChunkPos chunkPos, boolean bl) {
        if (this.blockEngine != null) {
            this.blockEngine.enableLightSources(chunkPos, bl);
        }
        if (this.skyEngine != null) {
            this.skyEngine.enableLightSources(chunkPos, bl);
        }
    }

    #else
    @Override
    public int runLightUpdates() {
        int $$0 = 0;
        if (this.blockEngine != null) {
            $$0 += this.blockEngine.runLightUpdates();
        }
        if (this.skyEngine != null) {
            $$0 += this.skyEngine.runLightUpdates();
        }
        return $$0;
    }

    @Override
    public void setLightEnabled(ChunkPos $$0, boolean $$1) {
        if (this.blockEngine != null) {
            this.blockEngine.setLightEnabled($$0, $$1);
        }

        if (this.skyEngine != null) {
            this.skyEngine.setLightEnabled($$0, $$1);
        }
    }

    @Override
    public void propagateLightSources(ChunkPos arg) {
        if (this.skyEngine != null) {
            this.skyEngine.propagateLightSources(arg);
        }
        if (this.blockEngine != null) {
            this.blockEngine.propagateLightSources(arg);
        }
    }

    public boolean lightOnInSection(SectionPos $$0) {
        long $$1 = $$0.asLong();
        return this.blockEngine == null
                /*Note: Somehow vanilla access the protected 'storage' field from the LevelLightEngine class... we're using mixin to do this.*/
                || this.blockEngine.storage.lightOnInSection($$1) && (this.skyEngine == null || this.skyEngine.storage.lightOnInSection($$1));
    }
    #endif

    @Override
    public void queueSectionData(LightLayer lightLayer, SectionPos sectionPos, @Nullable DataLayer dataLayer #if PRE_MC_1_20_1, boolean bl #endif) {
        if (lightLayer == LightLayer.BLOCK) {
            if (this.blockEngine != null) {
                this.blockEngine.queueSectionData(sectionPos.asLong(), dataLayer #if PRE_MC_1_20_1, bl #endif);
            }
        } else if (this.skyEngine != null) {
            this.skyEngine.queueSectionData(sectionPos.asLong(), dataLayer #if PRE_MC_1_20_1, bl #endif);
        }
    }

    @Override
    public void checkBlock(BlockPos blockPos) {
        if (this.blockEngine != null) {
            this.blockEngine.checkBlock(blockPos);
        }
        if (this.skyEngine != null) {
            this.skyEngine.checkBlock(blockPos);
        }
    }

    @Override
    public boolean hasLightWork() {
        if (this.skyEngine != null && this.skyEngine.hasLightWork()) {
            return true;
        }
        return this.blockEngine != null && this.blockEngine.hasLightWork();
    }

    @Override
    public void updateSectionStatus(SectionPos sectionPos, boolean bl) {
        if (this.blockEngine != null) {
            this.blockEngine.updateSectionStatus(sectionPos, bl);
        }
        if (this.skyEngine != null) {
            this.skyEngine.updateSectionStatus(sectionPos, bl);
        }
    }

    @Override
    public LayerLightEventListener getLayerListener(LightLayer lightLayer) {
        if (lightLayer == LightLayer.BLOCK) {
            if (this.blockEngine == null) {
                return LayerLightEventListener.DummyLightLayerEventListener.INSTANCE;
            }
            return this.blockEngine;
        }
        if (this.skyEngine == null) {
            return LayerLightEventListener.DummyLightLayerEventListener.INSTANCE;
        }
        return this.skyEngine;
    }

    @Override
    public int getRawBrightness(BlockPos blockPos, int i) {
        int j = this.skyEngine == null ? 0 : this.skyEngine.getLightValue(blockPos) - i;
        int k = this.blockEngine == null ? 0 : this.blockEngine.getLightValue(blockPos);
        return Math.max(k, j);
    }

    public void lightChunk(ChunkAccess chunkAccess, boolean needLightBlockUpdate) {
    	ChunkPos chunkPos = chunkAccess.getPos();
        chunkAccess.setLightCorrect(false);
        
        LevelChunkSection[] levelChunkSections = chunkAccess.getSections();
        for (int i = 0; i <
        #if POST_MC_1_17_1
            chunkAccess.getSectionsCount()
        #else
            16
        #endif
        ; ++i) {
            LevelChunkSection levelChunkSection = levelChunkSections[i];
            #if PRE_MC_1_17_1
            if (!LevelChunkSection.isEmpty(levelChunkSection)) {
                updateSectionStatus(SectionPos.of(chunkPos, i), false);
            }
            #elif PRE_MC_1_18_1
            if (!LevelChunkSection.isEmpty(levelChunkSection)) {
                int j = this.levelHeightAccessor.getSectionYFromSectionIndex(i);
                updateSectionStatus(SectionPos.of(chunkPos, j), false);
            }
            #else
            if (levelChunkSection.hasOnlyAir()) continue;
            int j = this.levelHeightAccessor.getSectionYFromSectionIndex(i);
            #if POST_MC_1_20_1
            queueSectionData(LightLayer.BLOCK, SectionPos.of(chunkPos, i), null);
            queueSectionData(LightLayer.SKY, SectionPos.of(chunkPos, i), null);
            #endif

            updateSectionStatus(SectionPos.of(chunkPos, j), false);
            #endif
        }
        #if PRE_MC_1_20_1
        enableLightSources(chunkPos, true);
        if (needLightBlockUpdate) {
            chunkAccess.getLights().forEach(blockPos ->
                    onBlockEmissionIncrease(blockPos, chunkAccess.getLightEmission(blockPos)));
        }
        #else
        //runLightUpdates();
        propagateLightSources(chunkPos);
        //runLightUpdates();
        #endif
        chunkAccess.setLightCorrect(true);
    }

    @Override
    public String getDebugData(LightLayer lightLayer, SectionPos sectionPos) {
    	throw new UnsupportedOperationException("This should never be used!");
    }
    @Override
    public void retainData(ChunkPos chunkPos, boolean bl) {
        if (this.blockEngine != null) {
            this.blockEngine.retainData(chunkPos, bl);
        }
        if (this.skyEngine != null) {
            this.skyEngine.retainData(chunkPos, bl);
        }
    }

    #if POST_MC_1_17_1
    @Override
    public int getLightSectionCount() {
    	throw new UnsupportedOperationException("This should never be used!");
    }
    @Override
    public int getMinLightSection() {
    	throw new UnsupportedOperationException("This should never be used!");
    }
    @Override
    public int getMaxLightSection() {
    	throw new UnsupportedOperationException("This should never be used!");
    }
    #endif
}