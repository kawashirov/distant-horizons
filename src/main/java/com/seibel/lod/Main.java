package com.seibel.lod;

import java.util.HashMap;
import java.util.Map;

import com.seibel.lod.builders.lodTemplates.Box;
import com.seibel.lod.util.DataPointUtil;

import net.minecraft.util.Direction;

public class Main
{
	public static void main(String[] args)
	{
		/*
		try
		{
			@SuppressWarnings("serial")
			Map<Direction, long[]> adjData = new HashMap<Direction, long[]>()
			{{
				put(Direction.EAST, new long[]{DataPointUtil.createDataPoint(60, 30, 0, 0, 0, 0), DataPointUtil.createDataPoint(25, 10, 0, 0, 0, 0)});
				put(Direction.WEST,  new long[]{DataPointUtil.createDataPoint(60, 10, 0, 0, 0, 0)});
				put(Direction.NORTH,  new long[]{DataPointUtil.createDataPoint(40, 20, 0, 0, 0, 0)});
				put(Direction.SOUTH,  new long[]{DataPointUtil.createDataPoint(40, 20, 0, 0, 0, 0)});
			}};

			Box box = new Box();
			int height = 60;
			int depth = 20;

			box.set(10, height - depth, 10);
			box.move(0, depth, 0);
			box.setAdjData(adjData);

			for(Direction direction : Box.DIRECTIONS)
			{
				int adjIndex = 0;
				while (box.shouldContinue(direction, adjIndex))
				{
					System.out.println(direction.toString());
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
		*/
	}
}