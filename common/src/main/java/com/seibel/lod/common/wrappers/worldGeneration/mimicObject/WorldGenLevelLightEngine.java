/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package com.seibel.lod.common.wrappers.worldGeneration.mimicObject;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
#if MC_VERSION_1_17_1 || MC_VERSION_1_18_1 || MC_VERSION_1_18_2
import net.minecraft.world.level.LevelHeightAccessor;
#endif
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.lighting.SkyLightEngine;

public class WorldGenLevelLightEngine extends LevelLightEngine {
	public static final int MAX_SOURCE_LEVEL = 15;
    public static final int LIGHT_SECTION_PADDING = 1;
    #if MC_VERSION_1_17_1 || MC_VERSION_1_18_1 || MC_VERSION_1_18_2
    protected final LevelHeightAccessor levelHeightAccessor;
    #endif
    @Nullable
    public final BlockLightEngine blockEngine;
    @Nullable
    public final SkyLightEngine skyEngine;

    public WorldGenLevelLightEngine(LightGetterAdaptor genRegion) {
    	super(genRegion, false, false);
        #if MC_VERSION_1_17_1 || MC_VERSION_1_18_1 || MC_VERSION_1_18_2
        this.levelHeightAccessor = genRegion.getLevelHeightAccessor();
        #endif
        this.blockEngine = new BlockLightEngine(genRegion);
        this.skyEngine = new SkyLightEngine(genRegion);
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
    public void onBlockEmissionIncrease(BlockPos blockPos, int i) {
        if (this.blockEngine != null) {
            this.blockEngine.onBlockEmissionIncrease(blockPos, i);
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
    public void updateSectionStatus(SectionPos sectionPos, boolean bl) {
        if (this.blockEngine != null) {
            this.blockEngine.updateSectionStatus(sectionPos, bl);
        }
        if (this.skyEngine != null) {
            this.skyEngine.updateSectionStatus(sectionPos, bl);
        }
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
        #if MC_VERSION_1_17_1 || MC_VERSION_1_18_1 || MC_VERSION_1_18_2
            chunkAccess.getSectionsCount()
        #elif MC_VERSION_1_16_5
            16
        #endif
        ; ++i) {
            LevelChunkSection levelChunkSection = levelChunkSections[i];
            #if MC_VERSION_1_16_5
            if (!LevelChunkSection.isEmpty(levelChunkSection)) {
                updateSectionStatus(SectionPos.of(chunkPos, i), false);
            }
            #elif MC_VERSION_1_17_1
            if (!LevelChunkSection.isEmpty(levelChunkSection)) {
                int j = this.levelHeightAccessor.getSectionYFromSectionIndex(i);
                updateSectionStatus(SectionPos.of(chunkPos, j), false);
            }
            #elif MC_VERSION_1_18_1 || MC_VERSION_1_18_2
            if (levelChunkSection.hasOnlyAir()) continue;
            int j = this.levelHeightAccessor.getSectionYFromSectionIndex(i);
            updateSectionStatus(SectionPos.of(chunkPos, j), false);
            #endif
        }
        enableLightSources(chunkPos, true);
        if (needLightBlockUpdate) {
            chunkAccess.getLights().forEach(blockPos ->
                    onBlockEmissionIncrease(blockPos, chunkAccess.getLightEmission(blockPos)));
        }

        chunkAccess.setLightCorrect(true);
    }

    @Override
    public String getDebugData(LightLayer lightLayer, SectionPos sectionPos) {
    	throw new UnsupportedOperationException("This should never be used!");
    }
    @Override
    public void queueSectionData(LightLayer lightLayer, SectionPos sectionPos, @Nullable DataLayer dataLayer, boolean bl) {
        if (lightLayer == LightLayer.BLOCK) {
            if (this.blockEngine != null) {
                this.blockEngine.queueSectionData(sectionPos.asLong(), dataLayer, bl);
            }
        } else if (this.skyEngine != null) {
            this.skyEngine.queueSectionData(sectionPos.asLong(), dataLayer, bl);
        }
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

    #if MC_VERSION_1_17_1 || MC_VERSION_1_18_1 || MC_VERSION_1_18_2
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