/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.core.objects.math;

import com.seibel.lod.core.util.LodUtil;

/**
 * A (almost) exact copy of Minecraft's 1.16.5
 * implementation of a 3 element integer vector.
 * 
 * @author James Seibel
 * @version 11-11-2021
 */
public class Vec3i
{
	public static Vec3i XNeg = new Vec3i(-1, 0, 0);
	public static Vec3i XPos = new Vec3i(1, 0, 0);
	public static Vec3i YNeg = new Vec3i(0, -1, 0);
	public static Vec3i YPos = new Vec3i(0, 1, 0);
	public static Vec3i ZNeg = new Vec3i(0, 0, -1);
	public static Vec3i ZPos = new Vec3i(0, 0, 1);
	
	
	public int x;
	public int y;
	public int z;
	
	
	
	public Vec3i()
	{
		
	}
	
	public Vec3i(int x, int y, int z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		else if (obj != null && this.getClass() == obj.getClass())
		{
			Vec3i Vec3f = (Vec3i) obj;
			if (Float.compare(Vec3f.x, this.x) != 0)
			{
				return false;
			}
			else if (Float.compare(Vec3f.y, this.y) != 0)
			{
				return false;
			}
			else
			{
				return Float.compare(Vec3f.z, this.z) == 0;
			}
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public int hashCode()
	{
		int i = Float.floatToIntBits(this.x);
		i = 31 * i + Float.floatToIntBits(this.y);
		return 31 * i + Float.floatToIntBits(this.z);
	}
	
	public void mul(float scalar)
	{
		this.x *= scalar;
		this.y *= scalar;
		this.z *= scalar;
	}
	
	public void mul(float x, float y, float z)
	{
		this.x *= x;
		this.y *= y;
		this.z *= z;
	}
	
	public void clamp(int min, int max)
	{
		this.x = LodUtil.clamp(min, this.x, max);
		this.y = LodUtil.clamp(min, this.y, max);
		this.z = LodUtil.clamp(min, this.z, max);
	}
	
	public void set(int x, int y, int z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void add(int x, int y, int z)
	{
		this.x += x;
		this.y += y;
		this.z += z;
	}
	
	public void add(Vec3i vector)
	{
		this.x += vector.x;
		this.y += vector.y;
		this.z += vector.z;
	}
	
	public void subtract(Vec3i vector)
	{
		this.x -= vector.x;
		this.y -= vector.y;
		this.z -= vector.z;
	}
	
	public double distSqr(double x, double y, double z, boolean centerOfBlock)
	{
		double offset = centerOfBlock ? 0.5 : 0.0;
		double xAdd = this.x + offset - x;
		double yAdd = this.y + offset - y;
		double zAdd = this.z + offset - z;
		return (xAdd * xAdd) + (yAdd * yAdd) + (zAdd * zAdd);
	}
	
	public int distManhattan(Vec3i otherVec)
	{
		float xSub = Math.abs(otherVec.x - this.x);
		float ySub = Math.abs(otherVec.y - this.y);
		float zSub = Math.abs(otherVec.z - this.z);
		return (int) (xSub + ySub + zSub);
	}
	
	/** inner product */
	public float dotProduct(Vec3i vector)
	{
		return (this.x * vector.x) + (this.y * vector.y) + (this.z * vector.z);
	}
	
	/** Cross product */
	public Vec3i cross(Vec3i otherVec)
	{
		return new Vec3i(
				(this.y * otherVec.z) - (this.z * otherVec.y), 
				(this.z * otherVec.x) - (this.x * otherVec.z), 
				(this.x * otherVec.y) - (this.y * otherVec.x));
	}
	
	public Vec3i copy()
	{
		return new Vec3i(this.x, this.y, this.z);
	}
	
	
	
	@Override
	public String toString()
	{
		return "[" + this.x + ", " + this.y + ", " + this.z + "]";
	}
	
	
	// Forge start
	public Vec3i(int[] values)
	{
		set(values);
	}
	
	public void set(int[] values)
	{
		this.x = values[0];
		this.y = values[1];
		this.z = values[2];
	}
}
