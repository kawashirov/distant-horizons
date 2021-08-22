package com.seibel.lod.util;

import com.seibel.lod.enums.DistanceCalculatorType;
import com.seibel.lod.enums.DistanceGenerationMode;

public class DetailUtil
{
    private static int initial = 200;
    private static double genMultiplier = 1.5;
    private static DistanceCalculatorType calculator = DistanceCalculatorType.LINEAR;
    private static final int maxDetail = 10;
    private static final int minDistance = 0;
    private static final int maxDistance = 1000000;
    private static DistanceGenerationMode[] distancesGenerators = {
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
        switch (calculator)
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
