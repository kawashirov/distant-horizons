package com.seibel.lod.objects;

import com.seibel.lod.util.LodUtil;

public class LevelPos implements Cloneable{
    public final byte detailLevel;
    public final int posX;
    public final int posZ;

    public LevelPos(byte detailLevel, int posX, int posZ){
        this.posX = posX;
        this.posZ = posZ;
        this.detailLevel = detailLevel;
    }

    public LevelPos convert( byte newDetailLevel){
        if(newDetailLevel >= detailLevel) {
            return new LevelPos(
                    newDetailLevel,
                    Math.floorDiv(posX, (int) Math.pow(2, newDetailLevel - detailLevel)),
                    Math.floorDiv(posZ, (int) Math.pow(2, newDetailLevel - detailLevel)));
        }else{
            return new LevelPos(
                    newDetailLevel,
                    posX * (int) Math.pow(2, detailLevel - newDetailLevel),
                    posZ * (int) Math.pow(2, detailLevel - newDetailLevel));
        }
    }

    public LevelPos regionModule(){
        return new LevelPos(
                detailLevel,
                Math.floorMod(posX, (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel)),
                Math.floorMod(posZ, (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel)));
    }

    public RegionPos getRegionPos(){
        return new RegionPos(
                Math.floorDiv(posX, (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel)),
                Math.floorDiv(posZ, (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel)));
    }


    public LevelPos clone(){
        return new LevelPos(detailLevel,posX,posZ);
    }

    public String toString(){
        String s = (detailLevel + " " + posX + " " + posZ);
        return s;
    }
}
