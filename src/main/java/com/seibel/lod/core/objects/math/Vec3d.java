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
 * This is closer to MC's implementation of a 
 * 3 element float vector than a 3 element double
 * vector. Hopefully that shouldn't cause any issues.
 * 
 * @author James Seibel
 * @version 11-18-2021
 */
public class Vec3d
{
	public static Vec3d XNeg = new Vec3d(-1.0F, 0.0F, 0.0F);
	public static Vec3d XPos = new Vec3d(1.0F, 0.0F, 0.0F);
	public static Vec3d YNeg = new Vec3d(0.0F, -1.0F, 0.0F);
	public static Vec3d YPos = new Vec3d(0.0F, 1.0F, 0.0F);
	public static Vec3d ZNeg = new Vec3d(0.0F, 0.0F, -1.0F);
	public static Vec3d ZPos = new Vec3d(0.0F, 0.0F, 1.0F);
	
	public static final Vec3d ZERO_VECTOR = new Vec3d(0.0D, 0.0D, 0.0D);
	
	public double x;
	public double y;
	public double z;
	
	
	
	public Vec3d()
	{
		
	}
	
	public Vec3d(double x, double y, double z)
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
			Vec3d Vec3f = (Vec3d) obj;
			if (Double.compare(Vec3f.x, this.x) != 0)
			{
				return false;
			}
			else if (Double.compare(Vec3f.y, this.y) != 0)
			{
				return false;
			}
			else
			{
				return Double.compare(Vec3f.z, this.z) == 0;
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
		long longVal = Double.doubleToLongBits(this.x);
		
		int intVal = (int) (longVal ^ longVal >>> 32);
		longVal = Double.doubleToLongBits(this.y);
		intVal = 31 * intVal + (int) (longVal ^ longVal >>> 32);
		longVal = Double.doubleToLongBits(this.z);
		
		return 31 * intVal + (int) (longVal ^ longVal >>> 32);
	}
	
	public void mul(double scalar)
	{
		this.x *= scalar;
		this.y *= scalar;
		this.z *= scalar;
	}
	
	public void mul(double x, double y, double z)
	{
		this.x *= x;
		this.y *= y;
		this.z *= z;
	}
	
	public void clamp(double min, double max)
	{
		this.x = LodUtil.clamp(min, this.x, max);
		this.y = LodUtil.clamp(min, this.y, max);
		this.z = LodUtil.clamp(min, this.z, max);
	}
	
	public void set(double x, double y, double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void add(double x, double y, double z)
	{
		this.x += x;
		this.y += y;
		this.z += z;
	}
	
	public void add(Vec3d vector)
	{
		this.x += vector.x;
		this.y += vector.y;
		this.z += vector.z;
	}
	
	public void subtract(Vec3d vector)
	{
		this.x -= vector.x;
		this.y -= vector.y;
		this.z -= vector.z;
	}
	
	public double dotProduct(Vec3d vector)
	{
		return this.x * vector.x + this.y * vector.y + this.z * vector.z;
	}
	
	public Vec3d normalize()
	{
		double value = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
		return value < 1.0E-4D ? ZERO_VECTOR : new Vec3d(this.x / value, this.y / value, this.z / value);
	}
	
	public void crossProduct(Vec3d vector)
	{
		double f = this.x;
		double f1 = this.y;
		double f2 = this.z;
		double f3 = vector.x;
		double f4 = vector.y;
		double f5 = vector.z;
		this.x = f1 * f5 - f2 * f4;
		this.y = f2 * f3 - f * f5;
		this.z = f * f4 - f1 * f3;
	}
	
	/* Matrix3f is not currently needed/implemented
	public void transform(Matrix3f p_229188_1_)
	{
		double f = this.x;
		double f1 = this.y;
		double f2 = this.z;
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
	public void linearInterp(Vec3f resultingVector, double percent)
	{
		double f = 1.0F - percent;
		this.x = this.x * f + resultingVector.x * percent;
		this.y = this.y * f + resultingVector.y * percent;
		this.z = this.z * f + resultingVector.z * percent;
	}
	*/
	
	/* Quaternions are not currently needed/implemented
	public Quaternion rotation(double p_229193_1_)
	{
		return new Quaternion(this, p_229193_1_, false);
	}
	
	
	@OnlyIn(Dist.CLIENT)
	public Quaternion rotationDegrees(double p_229187_1_)
	{
		return new Quaternion(this, p_229187_1_, true);
	}
	*/
	
	public Vec3d copy()
	{
		return new Vec3d(this.x, this.y, this.z);
	}
	
	/* not currently needed/implemented
	public void map(double2doubleFunction p_229191_1_)
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
	public Vec3d(double[] values)
	{
		set(values);
	}
	
	public void set(double[] values)
	{
		this.x = values[0];
		this.y = values[1];
		this.z = values[2];
	}
}
