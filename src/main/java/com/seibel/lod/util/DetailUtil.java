package com.seibel.lod.util;

import com.seibel.lod.enums.DistanceCalculatorType;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.enums.LodQuality;
import com.seibel.lod.handlers.LodConfig;

public class DetailUtil
{
    private static double genMultiplier = 1.5;
    private static final int maxDetail = LodUtil.REGION_DETAIL_LEVEL + 1;
    private static final int minDistance = 0;
    private static final int maxDistance = LodConfig.CLIENT.lodChunkRenderDistane.get() * 16;
    private static DistanceGenerationMode[] distancesGenerators = {
            DistanceGenerationMode.SURFACE,
            DistanceGenerationMode.SURFACE,
            DistanceGenerationMode.SURFACE,
            DistanceGenerationMode.SURFACE,
            DistanceGenerationMode.SURFACE,
            DistanceGenerationMode.SURFACE,
            DistanceGenerationMode.SURFACE,
            DistanceGenerationMode.SURFACE,
            DistanceGenerationMode.SURFACE,
            DistanceGenerationMode.SURFACE};


    public static int getDistanceRendering(int detail)
    {
        int distance = 0;
        int initial = LodConfig.CLIENT.lodQuality.get() * 256;
        switch (LodConfig.CLIENT.lodDistanceCalculatorType.get())
        {
            case LINEAR:
                distance = (detail * initial);
            break;
            case QUADRATIC:
                if (detail == 0)
                    distance = minDistance;
                else if(detail == maxDetail)
                    distance = maxDistance;
                else
                    distance = (int) (Math.pow(2, detail) * initial);
            break;
        }
        return distance;
    }

    public static int getDistanceGeneration(int detail)
    {
        return (int) (getDistanceRendering(detail) * genMultiplier);
    }

    public static DistanceGenerationMode getDistanceGenerationMode(int detail)
    {
        return distancesGenerators[detail];
    }
}
