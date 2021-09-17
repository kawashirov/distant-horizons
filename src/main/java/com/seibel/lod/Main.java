package com.seibel.lod;

import com.seibel.lod.builders.lodTemplates.Box;
import com.seibel.lod.util.DataPointUtil;
import net.minecraft.util.Direction;

import java.util.HashMap;
import java.util.Map;

public class Main
{
	public static void main(String[] args)
	{
		try
		{
			Map<Direction, long[]> adjData = new HashMap()
			{{
				put(Direction.EAST, new long[]{DataPointUtil.createDataPoint(70, 50, 0, 0, 0, 0), DataPointUtil.createDataPoint(45, 30, 0, 0, 0, 0), DataPointUtil.createDataPoint(28, 25, 0, 0, 0, 0)});
				put(Direction.WEST, new long[]{DataPointUtil.createDataPoint(70, 10, 0, 0, 0, 0)});
				put(Direction.NORTH, new long[]{DataPointUtil.createDataPoint(50, 0, 0, 0, 0, 0)});
				put(Direction.SOUTH, new long[]{DataPointUtil.createDataPoint(50, 30, 0, 0, 0, 0)});
			}};

			Box box = new Box();
			int height = 60;
			int depth = 20;

			box.set(10, height - depth, 10);
			box.move(0, depth, 0);
			box.setAdjData(adjData);

			for(Direction direction : Box.ADJ_DIRECTIONS)
			{
				int adjIndex = 0;
				while (box.shouldContinue(direction, adjIndex))
				{
					for (int i = 0; i < 4; i++)
					{
						System.out.println(box.getX(direction, i) + " " + box.getY(direction, i, adjIndex) + " " + box.getZ(direction, i));
					}
					adjIndex++;
				}
			}

		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
