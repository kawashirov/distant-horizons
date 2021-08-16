package com.seibel.lod;

import com.seibel.lod.objects.LodDataPoint;
import com.seibel.lod.objects.LodRegion;
import com.seibel.lod.objects.RegionPos;

import java.awt.*;

public class Main {
    public static void main(String[] args){
        LodRegion lodRegion = new LodRegion((byte) 0,new RegionPos(0,0));
        lodRegion.setData((byte) 2,0,0, new LodDataPoint((short) 2,(short) 30, new Color(100,100,100)), (byte) 2,true);
        try {
            System.out.print("test ");
            System.out.println(lodRegion.getData((byte) 6, 0, 0));
        }catch (Exception e){
            e.printStackTrace();
        }
        return;
    }
}
