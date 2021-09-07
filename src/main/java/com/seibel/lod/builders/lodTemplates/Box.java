package com.seibel.lod.builders.lodTemplates;

public class Box
{
	public static final int DOWN = 0;
	public static final int UP = 1;
	public static final int EAST = 2;
	public static final int WEST = 3;
	public static final int SOUTH = 4;
	public static final int NORTH = 5;

	public static final int OFFSET = 0;
	public static final int WIDTH = 1;

	public static final int X = 0;
	public static final int Y = 1;
	public static final int Z = 2;

	public int[][] box;

	public Box(){
		box = new int[2][3];
	}

	public void set(int xWidth, int yWidth, int zWidth){
			box[OFFSET][X] = 0;
			box[OFFSET][Y] = 0;
			box[OFFSET][Z] = 0;

			box[WIDTH][X] = xWidth;
			box[WIDTH][Y] = yWidth;
			box[WIDTH][Z] = zWidth;
	}

	public void move(int xOffset, int yOffset, int zOffset){
		box[NORTH][X] = xOffset;
		box[NORTH][Y] = yOffset;
		box[NORTH][Z] = zOffset;
	}

	public int getMinX(){
		return box[OFFSET][X];
	}

	public int getMaxX(){
		return box[OFFSET][X] + box[WIDTH][X];
	}

	public int getMinY(){
		return box[OFFSET][Y];
	}

	public int getMaxY(){
		return box[OFFSET][Y] + box[WIDTH][Y];
	}

	public int getMinZ(){
		return box[OFFSET][Z];
	}

	public int getMaxZ(){
		return box[OFFSET][Z] + box[WIDTH][Z];
	}
}
