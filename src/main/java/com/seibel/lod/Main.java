package com.seibel.lod;

import java.util.HashMap;
import java.util.Map;

import com.seibel.lod.builders.lodTemplates.Box;
import com.seibel.lod.config.LodConfig;
import com.seibel.lod.enums.HorizontalQuality;
import com.seibel.lod.enums.HorizontalScale;
import com.seibel.lod.util.DataPointUtil;

import net.minecraft.util.Direction;

public class Main
{
	public static void main(String[] args)
	{
		for(byte detail = 0; detail < 13; detail++)
		{
			byte minGenDetail = 0;
			byte maxDetail = 10;
			int distance;
			if (detail <= minGenDetail)
				distance = 0;
			else if (detail >= maxDetail)
				distance = 10000;
			else
			{
				int distanceUnit = HorizontalScale.LOW.distanceUnit;
				switch (HorizontalQuality.HIGH)
				{
					case LINEAR:
						;
						distance = (detail * distanceUnit);
					default:
						double base = HorizontalQuality.HIGH.quadraticBase;
						distance = (int) (Math.pow(base, detail) * distanceUnit);
				}
			}
			System.out.println(distance);
		}
	}
}
