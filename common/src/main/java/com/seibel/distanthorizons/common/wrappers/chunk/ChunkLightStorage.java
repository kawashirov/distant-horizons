package com.seibel.distanthorizons.common.wrappers.chunk;

import java.util.ArrayList;
import java.util.Arrays;

/**
compact, efficient storage for light levels.
all blocks only take up 4 bits in total,
and if a 16x16x16 area is detected to have the same light level in all positions,
then we store a single byte for that light level, instead of 2 kilobytes.

@author Builderb0y
*/
public class ChunkLightStorage {

	/** the minimum Y level in the chunk which this storage is storing light levels for (inclusive). */
	public int minY;
	/** the maximum Y level in the chunk which this storage is storing light levels for (exclusive). */
	public int maxY;
	/** the data stored in this storage, split up into 16x16x16 areas. */
	public Section[] sections;

	public ChunkLightStorage(int minY, int maxY) {
		this.minY = minY;
		this.maxY = maxY;
	}

	public int get(int x, int y, int z) {
		if (y < this.minY) return 0;
		if (y >= this.maxY) return 15;
		if (this.sections != null) {
			Section section = this.sections[(y - this.minY) >> 4];
			if (section != null) {
				return section.get(x, y, z);
			}
		}
		return 0;
	}

	public void set(int x, int y, int z, int lightLevel) {
		if (y < this.minY || y >= this.maxY) return;
		//populate array if it doesn't exist.
		if (this.sections == null) {
			this.sections = new Section[(this.maxY - this.minY) >> 4];
		}
		int index = (y - this.minY) >> 4;
		Section section = this.sections[index];
		//populate section in array if it doesn't exist.
		if (section == null) {
			section = new Section(0);
			this.sections[index] = section;
		}
		section.set(x, y, z, lightLevel);
	}

	public static class Section {

		public byte constantValue;
		public long[] data;
		public short[] counts;

		public Section(int initialValue) {
			this.constantValue = (byte)(initialValue);
			this.counts = new short[16];
			this.counts[initialValue] = 16 * 16 * 16;
		}

		public int get(int x, int y, int z) {
			if (this.constantValue >= 0) return this.constantValue;
			x &= 15;
			y &= 15;
			z &= 15;
			long bits = this.data[(z << 4) | x];
			return ((int)(bits >>> (y << 2))) & 15;
		}

		public void set(int x, int y, int z, int lightLevel) {
			int oldLightLevel = -1;
			if (this.constantValue >= 0) {
				oldLightLevel = this.constantValue;

				//if the light level didn't change, then there's nothing to do.
				if (oldLightLevel == lightLevel) return;

				//if we are a constant value and need to change something,
				//then that means we need to convert to a non-constant value.
				this.data = DataRecycler.get();

				//repeat oldLightLevel 16 times as a bit pattern.
				long payload = oldLightLevel;
				payload |= payload << 4;
				payload |= payload << 8;
				payload |= payload << 16;
				payload |= payload << 32;

				//fill our data with our constant value.
				Arrays.fill(this.data, payload);

				//we are no longer a constant value.
				this.constantValue = -1;
			}

			x &= 15;
			y &= 15;
			z &= 15;
			int index = (z << 4) | x;
			long bits = this.data[index];
			//if we weren't a constant value before, now's the time to initialize oldLightLevel.
			if (oldLightLevel < 0) {
				oldLightLevel = ((int)(bits >>> (y << 2))) & 15;
			}
			//clear the 4 bits that correspond to the light level at x, y, z...
			bits &= ~(15L << (y << 2));
			//...and then re-populate those bits with the new light level.
			bits |= ((long)(lightLevel)) << (y << 2);
			//store the updated bits in our data.
			this.data[index] = bits;

			//we have one less of the old light level...
			this.counts[oldLightLevel]--;
			//...and one more of the new level.
			//if the number associated with the new level is now 4096 (AKA 16 ^ 3),
			//then this implies every position in this section has the same light level,
			//and therefore we can convert back to a constant value.
			if (++this.counts[lightLevel] == 4096) {
				this.constantValue = (byte)(lightLevel);
				DataRecycler.reclaim(this.data);
				this.data = null;
			}
		}
	}

	static class DataRecycler {

		private static final ArrayList<long[]> recycled = new ArrayList<>(256);

		static synchronized long[] get() {
			if (recycled.isEmpty()) return new long[256];
			else return recycled.remove(recycled.size() - 1);
		}

		static synchronized void reclaim(long[] data) {
			if (recycled.size() < 256) recycled.add(data);
		}
	}
}