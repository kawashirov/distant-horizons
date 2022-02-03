package com.seibel.lod.common.wrappers.worldGeneration;

//FIXME: Move this outside the WorldGenerationStep thingy
public class Rolling {

	private final int size;
	private double total = 0d;
	private int index = 0;
	private final double[] samples;

	public Rolling(int size) {
		this.size = size;
		samples = new double[size];
		for (int i = 0; i < size; i++)
			samples[i] = 0d;
	}

	public void add(double x) {
		total -= samples[index];
		samples[index] = x;
		total += x;
		if (++index == size)
			index = 0; // cheaper than modulus
	}

	public double getAverage() {
		return total / size;
	}
}