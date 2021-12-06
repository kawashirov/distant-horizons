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
 * implementation of a 3 element float vector.
 * 
 * @author James Seibel
 * @version 11-11-2021
 */
public class Vec3f
{
	public static Vec3f XNeg = new Vec3f(-1.0F, 0.0F, 0.0F);
	public static Vec3f XPos = new Vec3f(1.0F, 0.0F, 0.0F);
	public static Vec3f YNeg = new Vec3f(0.0F, -1.0F, 0.0F);
	public static Vec3f YPos = new Vec3f(0.0F, 1.0F, 0.0F);
	public static Vec3f ZNeg = new Vec3f(0.0F, 0.0F, -1.0F);
	public static Vec3f ZPos = new Vec3f(0.0F, 0.0F, 1.0F);
	
	
	public float x;
	public float y;
	public float z;
	
	
	
	public Vec3f()
	{
		
	}
	
	public Vec3f(float x, float y, float z)
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
			Vec3f Vec3f = (Vec3f) obj;
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
	
	public void clamp(float min, float max)
	{
		this.x = LodUtil.clamp(min, this.x, max);
		this.y = LodUtil.clamp(min, this.y, max);
		this.z = LodUtil.clamp(min, this.z, max);
	}
	
	public void set(float x, float y, float z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void add(float x, float y, float z)
	{
		this.x += x;
		this.y += y;
		this.z += z;
	}
	
	public void add(Vec3f vector)
	{
		this.x += vector.x;
		this.y += vector.y;
		this.z += vector.z;
	}
	
	public void subtract(Vec3f vector)
	{
		this.x -= vector.x;
		this.y -= vector.y;
		this.z -= vector.z;
	}
	
	public float dotProduct(Vec3f vector)
	{
		return this.x * vector.x + this.y * vector.y + this.z * vector.z;
	}
	
	public boolean normalize()
	{
		float squaredSum = this.x * this.x + this.y * this.y + this.z * this.z;
		if (squaredSum < 1.0E-5D)
		{
			return false;
		}
		else
		{
			float f1 = LodUtil.fastInvSqrt(squaredSum);
			this.x *= f1;
			this.y *= f1;
			this.z *= f1;
			return true;
		}
	}
	
	public void crossProduct(Vec3f vector)
	{
		float f = this.x;
		float f1 = this.y;
		float f2 = this.z;
		float f3 = vector.x;
		float f4 = vector.y;
		float f5 = vector.z;
		this.x = f1 * f5 - f2 * f4;
		this.y = f2 * f3 - f * f5;
		this.z = f * f4 - f1 * f3;
	}
	
	/* Matrix3f is not currently needed/implemented
	public void transform(Matrix3f p_229188_1_)
	{
		float f = this.x;
		float f1 = this.y;
		float f2 = this.z;
		this.x = p_229188_1_.m00 * f + p_229188_1_.m01 * f1 + p_229188_1_.m02 * f2;
		this.y = p_229188_1_.m10 * f + p_229188_1_.m11 * f1 + p_229188_1_.m12 * f2;
		this.z = p_229188_1_.m20 * f + p_229188_1_.m21 * f1 + p_229188_1_.m22 * f2;
	}
	*/
	
	/* Quaternions are not currently needed/implemented
	public void transform(Quaternion p_214905_1_)
	{
		Quaternion quaternion = new Quaternion(p_214905_1_);
		quaternion.mul(new Quaternion(this.x(), this.y(), this.z(), 0.0F));
		Quaternion quaternion1 = new Quaternion(p_214905_1_);
		quaternion1.conj();
		quaternion.mul(quaternion1);
		this.set(quaternion.i(), quaternion.j(), quaternion.k());
	}
	*/
	
	/* not currently needed
	 * percent may actually be partial ticks (which is available when rendering)
	public void linearInterp(Vec3f resultingVector, float percent)
	{
		float f = 1.0F - percent;
		this.x = this.x * f + resultingVector.x * percent;
		this.y = this.y * f + resultingVector.y * percent;
		this.z = this.z * f + resultingVector.z * percent;
	}
	*/
	
	/* Quaternions are not currently needed/implemented
	public Quaternion rotation(float p_229193_1_)
	{
		return new Quaternion(this, p_229193_1_, false);
	}
	
	
	@OnlyIn(Dist.CLIENT)
	public Quaternion rotationDegrees(float p_229187_1_)
	{
		return new Quaternion(this, p_229187_1_, true);
	}
	*/
	
	public Vec3f copy()
	{
		return new Vec3f(this.x, this.y, this.z);
	}
	
	/* not currently needed/implemented
	public void map(Float2FloatFunction p_229191_1_)
	{
		this.x = p_229191_1_.get(this.x);
		this.y = p_229191_1_.get(this.y);
		this.z = p_229191_1_.get(this.z);
	}
	*/
	
	@Override
	public String toString()
	{
		return "[" + this.x + ", " + this.y + ", " + this.z + "]";
	}
	
	// Forge start
	public Vec3f(float[] values)
	{
		set(values);
	}
	
	public void set(float[] values)
	{
		this.x = values[0];
		this.y = values[1];
		this.z = values[2];
	}
}
