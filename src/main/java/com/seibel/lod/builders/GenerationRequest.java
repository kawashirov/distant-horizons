package com.seibel.lod.builders;

import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.enums.LodDetail;
import com.seibel.lod.objects.LevelPos;
import com.seibel.lod.util.LodUtil;
import net.minecraft.util.math.ChunkPos;

/**
 * @author Leonardo Amato
 * @version 22-08-2021
 */
public class GenerationRequest
{
    public final LevelPos levelPos;
    public final DistanceGenerationMode generationMode;
    public final LodDetail detail;

    public GenerationRequest(LevelPos levelPos, DistanceGenerationMode generationMode, LodDetail detail)
    {
        this.levelPos = levelPos;
        this.generationMode = generationMode;
        this.detail = detail;
    }

    public ChunkPos getChunkPos()
    {
        LevelPos chunkLevelPos = levelPos.convert(LodUtil.CHUNK_DETAIL_LEVEL);
        return new ChunkPos(chunkLevelPos.posX, chunkLevelPos.posZ);
    }
}