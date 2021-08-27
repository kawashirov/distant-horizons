package com.seibel.lod.objects.LevelPos;

import com.seibel.lod.util.LodUtil;

public interface MutableLevelPos
{
	public void convert(byte newDetailLevel);

	public void performRegionModule();

	public void applyOffset(int xOffset, int zOffset);

	public void changeParameters(byte newDetailLevel, int newPosX, int newPosZ);
}
