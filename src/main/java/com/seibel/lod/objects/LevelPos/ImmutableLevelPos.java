package com.seibel.lod.objects.LevelPos;

public interface ImmutableLevelPos
{
    public LevelPos getConvertedLevelPos(byte newDetailLevel);
    public LevelPos getRegionModuleLevelPos();
}
