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

import java.nio.FloatBuffer;

/**
 * A (almost) exact copy of Minecraft's 1.16.5
 * implementation of a 4x4 float matrix.
 * 
 * @author James Seibel
 * @version 11-11-2021
 */
public class Mat4f
{
	private float m00;
	private float m01;
	private float m02;
	private float m03;
	private float m10;
	private float m11;
	private float m12;
	private float m13;
	private float m20;
	private float m21;
	private float m22;
	private float m23;
	private float m30;
	private float m31;
	private float m32;
	private float m33;
	
	
	public Mat4f()
	{
		
	}
	
	public Mat4f(Mat4f sourceMatrix)
	{
		this.m00 = sourceMatrix.m00;
		this.m01 = sourceMatrix.m01;
		this.m02 = sourceMatrix.m02;
		this.m03 = sourceMatrix.m03;
		this.m10 = sourceMatrix.m10;
		this.m11 = sourceMatrix.m11;
		this.m12 = sourceMatrix.m12;
		this.m13 = sourceMatrix.m13;
		this.m20 = sourceMatrix.m20;
		this.m21 = sourceMatrix.m21;
		this.m22 = sourceMatrix.m22;
		this.m23 = sourceMatrix.m23;
		this.m30 = sourceMatrix.m30;
		this.m31 = sourceMatrix.m31;
		this.m32 = sourceMatrix.m32;
		this.m33 = sourceMatrix.m33;
	}
	
	/* Quaternions are not currently needed/implemented
	public Matrix4float(Quaternion p_i48104_1_)
	{
		float f = p_i48104_1_.i();
		float f1 = p_i48104_1_.j();
		float f2 = p_i48104_1_.k();
		float f3 = p_i48104_1_.r();
		float f4 = 2.0F * f * f;
		float f5 = 2.0F * f1 * f1;
		float f6 = 2.0F * f2 * f2;
		this.m00 = 1.0F - f5 - f6;
		this.m11 = 1.0F - f6 - f4;
		this.m22 = 1.0F - f4 - f5;
		this.m33 = 1.0F;
		float f7 = f * f1;
		float f8 = f1 * f2;
		float f9 = f2 * f;
		float f10 = f * f3;
		float f11 = f1 * f3;
		float f12 = f2 * f3;
		this.m10 = 2.0F * (f7 + f12);
		this.m01 = 2.0F * (f7 - f12);
		this.m20 = 2.0F * (f9 - f11);
		this.m02 = 2.0F * (f9 + f11);
		this.m21 = 2.0F * (f8 + f10);
		this.m12 = 2.0F * (f8 - f10);
	}
	*/
	
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		else if (obj != null && this.getClass() == obj.getClass())
		{
			Mat4f otherMatrix = (Mat4f) obj;
			return Float.compare(otherMatrix.m00, this.m00) == 0
					&& Float.compare(otherMatrix.m01, this.m01) == 0
					&& Float.compare(otherMatrix.m02, this.m02) == 0
					&& Float.compare(otherMatrix.m03, this.m03) == 0
					&& Float.compare(otherMatrix.m10, this.m10) == 0
					&& Float.compare(otherMatrix.m11, this.m11) == 0
					&& Float.compare(otherMatrix.m12, this.m12) == 0
					&& Float.compare(otherMatrix.m13, this.m13) == 0
					&& Float.compare(otherMatrix.m20, this.m20) == 0
					&& Float.compare(otherMatrix.m21, this.m21) == 0
					&& Float.compare(otherMatrix.m22, this.m22) == 0
					&& Float.compare(otherMatrix.m23, this.m23) == 0
					&& Float.compare(otherMatrix.m30, this.m30) == 0
					&& Float.compare(otherMatrix.m31, this.m31) == 0
					&& Float.compare(otherMatrix.m32, this.m32) == 0
					&& Float.compare(otherMatrix.m33, this.m33) == 0;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public int hashCode()
	{
		int i = this.m00 != 0.0F ? Float.floatToIntBits(this.m00) : 0;
		i = 31 * i + (this.m01 != 0.0F ? Float.floatToIntBits(this.m01) : 0);
		i = 31 * i + (this.m02 != 0.0F ? Float.floatToIntBits(this.m02) : 0);
		i = 31 * i + (this.m03 != 0.0F ? Float.floatToIntBits(this.m03) : 0);
		i = 31 * i + (this.m10 != 0.0F ? Float.floatToIntBits(this.m10) : 0);
		i = 31 * i + (this.m11 != 0.0F ? Float.floatToIntBits(this.m11) : 0);
		i = 31 * i + (this.m12 != 0.0F ? Float.floatToIntBits(this.m12) : 0);
		i = 31 * i + (this.m13 != 0.0F ? Float.floatToIntBits(this.m13) : 0);
		i = 31 * i + (this.m20 != 0.0F ? Float.floatToIntBits(this.m20) : 0);
		i = 31 * i + (this.m21 != 0.0F ? Float.floatToIntBits(this.m21) : 0);
		i = 31 * i + (this.m22 != 0.0F ? Float.floatToIntBits(this.m22) : 0);
		i = 31 * i + (this.m23 != 0.0F ? Float.floatToIntBits(this.m23) : 0);
		i = 31 * i + (this.m30 != 0.0F ? Float.floatToIntBits(this.m30) : 0);
		i = 31 * i + (this.m31 != 0.0F ? Float.floatToIntBits(this.m31) : 0);
		i = 31 * i + (this.m32 != 0.0F ? Float.floatToIntBits(this.m32) : 0);
		return 31 * i + (this.m33 != 0.0F ? Float.floatToIntBits(this.m33) : 0);
	}
	
	
	@Override
	public String toString()
	{
		StringBuilder stringbuilder = new StringBuilder();
		stringbuilder.append("Matrix4f:\n");
		stringbuilder.append(this.m00);
		stringbuilder.append(" ");
		stringbuilder.append(this.m01);
		stringbuilder.append(" ");
		stringbuilder.append(this.m02);
		stringbuilder.append(" ");
		stringbuilder.append(this.m03);
		stringbuilder.append("\n");
		stringbuilder.append(this.m10);
		stringbuilder.append(" ");
		stringbuilder.append(this.m11);
		stringbuilder.append(" ");
		stringbuilder.append(this.m12);
		stringbuilder.append(" ");
		stringbuilder.append(this.m13);
		stringbuilder.append("\n");
		stringbuilder.append(this.m20);
		stringbuilder.append(" ");
		stringbuilder.append(this.m21);
		stringbuilder.append(" ");
		stringbuilder.append(this.m22);
		stringbuilder.append(" ");
		stringbuilder.append(this.m23);
		stringbuilder.append("\n");
		stringbuilder.append(this.m30);
		stringbuilder.append(" ");
		stringbuilder.append(this.m31);
		stringbuilder.append(" ");
		stringbuilder.append(this.m32);
		stringbuilder.append(" ");
		stringbuilder.append(this.m33);
		stringbuilder.append("\n");
		return stringbuilder.toString();
	}
	
	
	public void store(FloatBuffer floatBuffer)
	{
		floatBuffer.put(bufferIndex(0, 0), this.m00);
		floatBuffer.put(bufferIndex(0, 1), this.m01);
		floatBuffer.put(bufferIndex(0, 2), this.m02);
		floatBuffer.put(bufferIndex(0, 3), this.m03);
		floatBuffer.put(bufferIndex(1, 0), this.m10);
		floatBuffer.put(bufferIndex(1, 1), this.m11);
		floatBuffer.put(bufferIndex(1, 2), this.m12);
		floatBuffer.put(bufferIndex(1, 3), this.m13);
		floatBuffer.put(bufferIndex(2, 0), this.m20);
		floatBuffer.put(bufferIndex(2, 1), this.m21);
		floatBuffer.put(bufferIndex(2, 2), this.m22);
		floatBuffer.put(bufferIndex(2, 3), this.m23);
		floatBuffer.put(bufferIndex(3, 0), this.m30);
		floatBuffer.put(bufferIndex(3, 1), this.m31);
		floatBuffer.put(bufferIndex(3, 2), this.m32);
		floatBuffer.put(bufferIndex(3, 3), this.m33);
	}
	
	private static int bufferIndex(int xIndex, int zIndex)
	{
		return (zIndex * 4) + xIndex;
	}
	
	
	public void setIdentity()
	{
		this.m00 = 1.0F;
		this.m01 = 0.0F;
		this.m02 = 0.0F;
		this.m03 = 0.0F;
		this.m10 = 0.0F;
		this.m11 = 1.0F;
		this.m12 = 0.0F;
		this.m13 = 0.0F;
		this.m20 = 0.0F;
		this.m21 = 0.0F;
		this.m22 = 1.0F;
		this.m23 = 0.0F;
		this.m30 = 0.0F;
		this.m31 = 0.0F;
		this.m32 = 0.0F;
		this.m33 = 1.0F;
	}
	
	/** adjugate and determinate */
	public float adjugateAndDet()
	{
		float f = this.m00 * this.m11 - this.m01 * this.m10;
		float f1 = this.m00 * this.m12 - this.m02 * this.m10;
		float f2 = this.m00 * this.m13 - this.m03 * this.m10;
		float f3 = this.m01 * this.m12 - this.m02 * this.m11;
		float f4 = this.m01 * this.m13 - this.m03 * this.m11;
		float f5 = this.m02 * this.m13 - this.m03 * this.m12;
		float f6 = this.m20 * this.m31 - this.m21 * this.m30;
		float f7 = this.m20 * this.m32 - this.m22 * this.m30;
		float f8 = this.m20 * this.m33 - this.m23 * this.m30;
		float f9 = this.m21 * this.m32 - this.m22 * this.m31;
		float f10 = this.m21 * this.m33 - this.m23 * this.m31;
		float f11 = this.m22 * this.m33 - this.m23 * this.m32;
		float f12 = this.m11 * f11 - this.m12 * f10 + this.m13 * f9;
		float f13 = -this.m10 * f11 + this.m12 * f8 - this.m13 * f7;
		float f14 = this.m10 * f10 - this.m11 * f8 + this.m13 * f6;
		float f15 = -this.m10 * f9 + this.m11 * f7 - this.m12 * f6;
		float f16 = -this.m01 * f11 + this.m02 * f10 - this.m03 * f9;
		float f17 = this.m00 * f11 - this.m02 * f8 + this.m03 * f7;
		float f18 = -this.m00 * f10 + this.m01 * f8 - this.m03 * f6;
		float f19 = this.m00 * f9 - this.m01 * f7 + this.m02 * f6;
		float f20 = this.m31 * f5 - this.m32 * f4 + this.m33 * f3;
		float f21 = -this.m30 * f5 + this.m32 * f2 - this.m33 * f1;
		float f22 = this.m30 * f4 - this.m31 * f2 + this.m33 * f;
		float f23 = -this.m30 * f3 + this.m31 * f1 - this.m32 * f;
		float f24 = -this.m21 * f5 + this.m22 * f4 - this.m23 * f3;
		float f25 = this.m20 * f5 - this.m22 * f2 + this.m23 * f1;
		float f26 = -this.m20 * f4 + this.m21 * f2 - this.m23 * f;
		float f27 = this.m20 * f3 - this.m21 * f1 + this.m22 * f;
		this.m00 = f12;
		this.m10 = f13;
		this.m20 = f14;
		this.m30 = f15;
		this.m01 = f16;
		this.m11 = f17;
		this.m21 = f18;
		this.m31 = f19;
		this.m02 = f20;
		this.m12 = f21;
		this.m22 = f22;
		this.m32 = f23;
		this.m03 = f24;
		this.m13 = f25;
		this.m23 = f26;
		this.m33 = f27;
		return f * f11 - f1 * f10 + f2 * f9 + f3 * f8 - f4 * f7 + f5 * f6;
	}
	
	public void transpose()
	{
		float f = this.m10;
		this.m10 = this.m01;
		this.m01 = f;
		f = this.m20;
		this.m20 = this.m02;
		this.m02 = f;
		f = this.m21;
		this.m21 = this.m12;
		this.m12 = f;
		f = this.m30;
		this.m30 = this.m03;
		this.m03 = f;
		f = this.m31;
		this.m31 = this.m13;
		this.m13 = f;
		f = this.m32;
		this.m32 = this.m23;
		this.m23 = f;
	}
	
	public boolean invert()
	{
		float det = this.adjugateAndDet();
		if (Math.abs(det) > 1.0E-6F)
		{
			this.multiply(det);
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public void multiply(Mat4f multMatrix)
	{
		float f = this.m00 * multMatrix.m00 + this.m01 * multMatrix.m10 + this.m02 * multMatrix.m20 + this.m03 * multMatrix.m30;
		float f1 = this.m00 * multMatrix.m01 + this.m01 * multMatrix.m11 + this.m02 * multMatrix.m21 + this.m03 * multMatrix.m31;
		float f2 = this.m00 * multMatrix.m02 + this.m01 * multMatrix.m12 + this.m02 * multMatrix.m22 + this.m03 * multMatrix.m32;
		float f3 = this.m00 * multMatrix.m03 + this.m01 * multMatrix.m13 + this.m02 * multMatrix.m23 + this.m03 * multMatrix.m33;
		float f4 = this.m10 * multMatrix.m00 + this.m11 * multMatrix.m10 + this.m12 * multMatrix.m20 + this.m13 * multMatrix.m30;
		float f5 = this.m10 * multMatrix.m01 + this.m11 * multMatrix.m11 + this.m12 * multMatrix.m21 + this.m13 * multMatrix.m31;
		float f6 = this.m10 * multMatrix.m02 + this.m11 * multMatrix.m12 + this.m12 * multMatrix.m22 + this.m13 * multMatrix.m32;
		float f7 = this.m10 * multMatrix.m03 + this.m11 * multMatrix.m13 + this.m12 * multMatrix.m23 + this.m13 * multMatrix.m33;
		float f8 = this.m20 * multMatrix.m00 + this.m21 * multMatrix.m10 + this.m22 * multMatrix.m20 + this.m23 * multMatrix.m30;
		float f9 = this.m20 * multMatrix.m01 + this.m21 * multMatrix.m11 + this.m22 * multMatrix.m21 + this.m23 * multMatrix.m31;
		float f10 = this.m20 * multMatrix.m02 + this.m21 * multMatrix.m12 + this.m22 * multMatrix.m22 + this.m23 * multMatrix.m32;
		float f11 = this.m20 * multMatrix.m03 + this.m21 * multMatrix.m13 + this.m22 * multMatrix.m23 + this.m23 * multMatrix.m33;
		float f12 = this.m30 * multMatrix.m00 + this.m31 * multMatrix.m10 + this.m32 * multMatrix.m20 + this.m33 * multMatrix.m30;
		float f13 = this.m30 * multMatrix.m01 + this.m31 * multMatrix.m11 + this.m32 * multMatrix.m21 + this.m33 * multMatrix.m31;
		float f14 = this.m30 * multMatrix.m02 + this.m31 * multMatrix.m12 + this.m32 * multMatrix.m22 + this.m33 * multMatrix.m32;
		float f15 = this.m30 * multMatrix.m03 + this.m31 * multMatrix.m13 + this.m32 * multMatrix.m23 + this.m33 * multMatrix.m33;
		this.m00 = f;
		this.m01 = f1;
		this.m02 = f2;
		this.m03 = f3;
		this.m10 = f4;
		this.m11 = f5;
		this.m12 = f6;
		this.m13 = f7;
		this.m20 = f8;
		this.m21 = f9;
		this.m22 = f10;
		this.m23 = f11;
		this.m30 = f12;
		this.m31 = f13;
		this.m32 = f14;
		this.m33 = f15;
	}
	
	/* Quaternions aren't currently needed/implemented
	public void multiply(Quaternion p_226596_1_)
	{
		this.multiply(new Matrix4f(p_226596_1_));
	}
	*/
	
	public void multiply(float scalar)
	{
		this.m00 *= scalar;
		this.m01 *= scalar;
		this.m02 *= scalar;
		this.m03 *= scalar;
		this.m10 *= scalar;
		this.m11 *= scalar;
		this.m12 *= scalar;
		this.m13 *= scalar;
		this.m20 *= scalar;
		this.m21 *= scalar;
		this.m22 *= scalar;
		this.m23 *= scalar;
		this.m30 *= scalar;
		this.m31 *= scalar;
		this.m32 *= scalar;
		this.m33 *= scalar;
	}
	
	public static Mat4f perspective(double fov, float widthHeightRatio, float nearClipPlane, float farClipPlane)
	{
		float f = (float) (1.0D / Math.tan(fov * ((float) Math.PI / 180F) / 2.0D));
		Mat4f matrix = new Mat4f();
		matrix.m00 = f / widthHeightRatio;
		matrix.m11 = f;
		matrix.m22 = (farClipPlane + nearClipPlane) / (nearClipPlane - farClipPlane);
		matrix.m32 = -1.0F;
		matrix.m23 = 2.0F * farClipPlane * nearClipPlane / (nearClipPlane - farClipPlane);
		return matrix;
	}
	
	
	/* not currently needed/implemented
	 * Also the parameter names should be double checked as they may be incorrect
	public static Matrix4Float orthographic(float left, float right, float top, float bottom)
	{
		Matrix4Float matrix4f = new Matrix4Float();
		matrix4f.m00 = 2.0F / left;
		matrix4f.m11 = 2.0F / right;
		float f = bottom - top;
		matrix4f.m22 = -2.0F / f;
		matrix4f.m33 = 1.0F;
		matrix4f.m03 = -1.0F;
		matrix4f.m13 = -1.0F;
		matrix4f.m23 = -(bottom + top) / f;
		return matrix4f;
	}
	*/
	
	/** 
	 * TODO: what kind of translation is this? 
	 * and how is this different from "multiplyTranslationMatrix"?
	 */
	public void translate(Vec3f vec)
	{
		this.m03 += vec.x;
		this.m13 += vec.y;
		this.m23 += vec.z;
	}
	
	/** originally "translate" from Minecraft's MatrixStack */
	public void multiplyTranslationMatrix(double x, double y, double z)
	{
		multiply(createTranslateMatrix((float)x, (float)y, (float)z));
	}
	
	public Mat4f copy()
	{
		return new Mat4f(this);
	}
	
	public static Mat4f createScaleMatrix(float x, float y, float z)
	{
		Mat4f matrix = new Mat4f();
		matrix.m00 = x;
		matrix.m11 = y;
		matrix.m22 = z;
		matrix.m33 = 1.0F;
		return matrix;
	}
	
	public static Mat4f createTranslateMatrix(float x, float y, float z)
	{
		Mat4f matrix = new Mat4f();
		matrix.m00 = 1.0F;
		matrix.m11 = 1.0F;
		matrix.m22 = 1.0F;
		matrix.m33 = 1.0F;
		matrix.m03 = x;
		matrix.m13 = y;
		matrix.m23 = z;
		return matrix;
	}
	
	
	// Forge start
	public Mat4f(float[] values)
	{
		m00 = values[0];
		m01 = values[1];
		m02 = values[2];
		m03 = values[3];
		m10 = values[4];
		m11 = values[5];
		m12 = values[6];
		m13 = values[7];
		m20 = values[8];
		m21 = values[9];
		m22 = values[10];
		m23 = values[11];
		m30 = values[12];
		m31 = values[13];
		m32 = values[14];
		m33 = values[15];
	}
	
	public Mat4f(FloatBuffer buffer)
	{
		this(buffer.array());
	}

	public void set(Mat4f mat)
	{
		this.m00 = mat.m00;
		this.m01 = mat.m01;
		this.m02 = mat.m02;
		this.m03 = mat.m03;
		this.m10 = mat.m10;
		this.m11 = mat.m11;
		this.m12 = mat.m12;
		this.m13 = mat.m13;
		this.m20 = mat.m20;
		this.m21 = mat.m21;
		this.m22 = mat.m22;
		this.m23 = mat.m23;
		this.m30 = mat.m30;
		this.m31 = mat.m31;
		this.m32 = mat.m32;
		this.m33 = mat.m33;
	}
	
	public void add(Mat4f other)
	{
		m00 += other.m00;
		m01 += other.m01;
		m02 += other.m02;
		m03 += other.m03;
		m10 += other.m10;
		m11 += other.m11;
		m12 += other.m12;
		m13 += other.m13;
		m20 += other.m20;
		m21 += other.m21;
		m22 += other.m22;
		m23 += other.m23;
		m30 += other.m30;
		m31 += other.m31;
		m32 += other.m32;
		m33 += other.m33;
	}
	
	public void multiplyBackward(Mat4f other)
	{
		Mat4f copy = other.copy();
		copy.multiply(this);
		this.set(copy);
	}
	
	public void setTranslation(float x, float y, float z)
	{
		this.m00 = 1.0F;
		this.m11 = 1.0F;
		this.m22 = 1.0F;
		this.m33 = 1.0F;
		this.m03 = x;
		this.m13 = y;
		this.m23 = z;
	}
}
